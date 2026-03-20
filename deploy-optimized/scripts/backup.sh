#!/bin/bash
################################################################################
# BikeShare 数据备份脚本
# 功能：自动备份 MySQL、Redis、MinIO 数据
# 使用：./backup.sh [选项]
# 选项：
#   --database-only  仅备份数据库
#   --minio-only     仅备份 MinIO 数据
#   --redis-only     仅备份 Redis 数据
#   --help           显示帮助信息
################################################################################

set -e

# ==================== 配置 ====================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_BASE_DIR="${SCRIPT_DIR}/backups/auto"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_BASE_DIR}/${DATE}"

# MySQL 配置
MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-bikeshare}"

# Redis 配置
REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

# MinIO 配置
MINIO_HOST="${MINIO_HOST:-minio}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-}"

# 备份保留天数
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

# ==================== 颜色输出 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# ==================== 帮助信息 ====================
show_help() {
    echo "BikeShare 数据备份脚本"
    echo ""
    echo "用法：$0 [选项]"
    echo ""
    echo "选项:"
    echo "  --database-only  仅备份数据库"
    echo "  --minio-only     仅备份 MinIO 数据"
    echo "  --redis-only     仅备份 Redis 数据"
    echo "  --help           显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 备份所有数据"
    echo "  $0 --database-only    # 仅备份数据库"
    echo "  $0 --minio-only       # 仅备份 MinIO 数据"
    exit 0
}

# ==================== 参数解析 ====================
BACKUP_ALL=true
BACKUP_DATABASE=false
BACKUP_MINIO=false
BACKUP_REDIS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --database-only)
            BACKUP_ALL=false
            BACKUP_DATABASE=true
            shift
            ;;
        --minio-only)
            BACKUP_ALL=false
            BACKUP_MINIO=true
            shift
            ;;
        --redis-only)
            BACKUP_ALL=false
            BACKUP_REDIS=true
            shift
            ;;
        --help)
            show_help
            ;;
        *)
            log_error "未知选项：$1"
            show_help
            ;;
    esac
done

# ==================== 创建备份目录 ====================
create_backup_dir() {
    log_info "创建备份目录：${BACKUP_DIR}"
    mkdir -p "${BACKUP_DIR}/mysql"
    mkdir -p "${BACKUP_DIR}/redis"
    mkdir -p "${BACKUP_DIR}/minio"
}

# ==================== MySQL 备份 ====================
backup_mysql() {
    log_info "开始备份 MySQL 数据库..."

    local backup_file="${BACKUP_DIR}/mysql/${MYSQL_DATABASE}_${DATE}.sql.gz"

    # 使用 docker exec 备份
    if docker exec bikeshare-mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
        --single-transaction \
        --quick \
        --lock-tables=false \
        "${MYSQL_DATABASE}" | gzip > "${backup_file}"; then
        log_info "MySQL 备份成功：${backup_file}"

        # 计算备份大小
        local size=$(du -h "${backup_file}" | cut -f1)
        log_info "备份大小：${size}"
    else
        log_error "MySQL 备份失败"
        return 1
    fi
}

# ==================== Redis 备份 ====================
backup_redis() {
    log_info "开始备份 Redis 数据..."

    # 触发 RDB 保存
    if docker exec bikeshare-redis redis-cli -a "${REDIS_PASSWORD}" BGSAVE > /dev/null 2>&1; then
        log_info "Redis BGSAVE 已触发"

        # 等待 RDB 文件生成
        sleep 5

        # 复制 RDB 文件
        local rdb_file="${BACKUP_DIR}/redis/dump_${DATE}.rdb"
        if docker cp bikeshare-redis:/data/dump.rdb "${rdb_file}"; then
            log_info "Redis 备份成功：${rdb_file}"
        else
            log_warn "Redis RDB 文件不存在或复制失败"
        fi
    else
        log_warn "Redis BGSAVE 触发失败，尝试直接复制..."
        local rdb_file="${BACKUP_DIR}/redis/dump_${DATE}.rdb"
        if docker cp bikeshare-redis:/data/dump.rdb "${rdb_file}" 2>/dev/null; then
            log_info "Redis 备份成功：${rdb_file}"
        else
            log_warn "Redis 无 RDB 文件或为空"
        fi
    fi
}

# ==================== MinIO 备份 ====================
backup_minio() {
    log_info "开始备份 MinIO 数据..."

    # MinIO 数据已经通过 volume 挂载到本地
    # 这里创建元数据备份
    local minio_meta_file="${BACKUP_DIR}/minio/metadata_${DATE}.json"

    # 如果 MinIO 服务可用，导出 bucket 信息
    if command -v mc &> /dev/null; then
        log_info "MinIO 客户端可用，导出 bucket 信息..."

        # 配置 MinIO 客户端
        mc alias set bikeshare "http://${MINIO_HOST}:9000" \
            "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" 2>/dev/null || true

        # 列出 bucket 信息
        mc admin info bikeshare > "${minio_meta_file}" 2>/dev/null || true
        log_info "MinIO 元数据已保存：${minio_meta_file}"
    else
        log_warn "MinIO 客户端 (mc) 未安装，跳过元数据备份"

        # 创建标记文件
        echo "MinIO data backup - ${DATE}" > "${minio_meta_file}"
    fi

    log_info "MinIO 数据通过 volume 自动持久化"
}

# ==================== 清理旧备份 ====================
cleanup_old_backups() {
    log_info "清理 ${BACKUP_RETENTION_DAYS} 天前的备份..."

    find "${BACKUP_BASE_DIR}" -maxdepth 1 -type d -mtime +${BACKUP_RETENTION_DAYS} -exec rm -rf {} \; 2>/dev/null || true

    log_info "清理完成"
}

# ==================== 主函数 ====================
main() {
    log_info "=========================================="
    log_info "BikeShare 数据备份开始"
    log_info "=========================================="

    # 创建备份目录
    create_backup_dir

    # 执行备份
    if [ "$BACKUP_ALL" = true ]; then
        backup_mysql
        backup_redis
        backup_minio
    else
        if [ "$BACKUP_DATABASE" = true ]; then
            backup_mysql
        fi
        if [ "$BACKUP_REDIS" = true ]; then
            backup_redis
        fi
        if [ "$BACKUP_MINIO" = true ]; then
            backup_minio
        fi
    fi

    # 清理旧备份
    cleanup_old_backups

    log_info "=========================================="
    log_info "BikeShare 数据备份完成"
    log_info "备份位置：${BACKUP_DIR}"
    log_info "=========================================="
}

# 运行主函数
main
