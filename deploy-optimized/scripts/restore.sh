#!/bin/bash
################################################################################
# BikeShare 数据恢复脚本
# 功能：从备份恢复 MySQL、Redis、MinIO 数据
# 使用：./restore.sh <备份目录>
################################################################################

set -e

# ==================== 配置 ====================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# MySQL 配置
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-bikeshare}"

# Redis 配置
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

# ==================== 颜色输出 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

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
    echo "BikeShare 数据恢复脚本"
    echo ""
    echo "用法：$0 <备份目录> [选项]"
    echo ""
    echo "选项:"
    echo "  --database-only  仅恢复数据库"
    echo "  --minio-only     仅恢复 MinIO 数据"
    echo "  --redis-only     仅恢复 Redis 数据"
    echo "  --force          强制恢复，不确认"
    echo "  --help           显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 /path/to/backup/20240101_120000"
    echo "  $0 /path/to/backup/20240101_120000 --database-only"
    exit 0
}

# ==================== 参数解析 ====================
if [ $# -lt 1 ]; then
    log_error "请指定备份目录"
    show_help
fi

BACKUP_DIR="$1"
shift

if [ ! -d "${BACKUP_DIR}" ]; then
    log_error "备份目录不存在：${BACKUP_DIR}"
    exit 1
fi

FORCE=false
RESTORE_ALL=true
RESTORE_DATABASE=false
RESTORE_MINIO=false
RESTORE_REDIS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --database-only)
            RESTORE_ALL=false
            RESTORE_DATABASE=true
            shift
            ;;
        --minio-only)
            RESTORE_ALL=false
            RESTORE_MINIO=true
            shift
            ;;
        --redis-only)
            RESTORE_ALL=false
            RESTORE_REDIS=true
            shift
            ;;
        --force)
            FORCE=true
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

# ==================== 确认恢复 ====================
if [ "$FORCE" = false ]; then
    log_warn "即将从以下备份恢复数据："
    log_warn "备份目录：${BACKUP_DIR}"
    log_warn ""
    log_warn "此操作将覆盖当前数据，是否继续？(y/N)"
    read -r confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "恢复已取消"
        exit 0
    fi
fi

# ==================== MySQL 恢复 ====================
restore_mysql() {
    log_info "开始恢复 MySQL 数据库..."

    local backup_file=$(find "${BACKUP_DIR}/mysql" -name "*.sql.gz" | head -n 1)

    if [ -z "${backup_file}" ]; then
        log_error "未找到 MySQL 备份文件"
        return 1
    fi

    log_info "找到备份文件：${backup_file}"

    # 解压并恢复
    if gunzip -c "${backup_file}" | docker exec -i bikeshare-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}"; then
        log_info "MySQL 恢复成功"
    else
        log_error "MySQL 恢复失败"
        return 1
    fi
}

# ==================== Redis 恢复 ====================
restore_redis() {
    log_info "开始恢复 Redis 数据..."

    local rdb_file=$(find "${BACKUP_DIR}/redis" -name "*.rdb" | head -n 1)

    if [ -z "${rdb_file}" ]; then
        log_warn "未找到 Redis RDB 备份文件"
        return 0
    fi

    log_info "找到 RDB 文件：${rdb_file}"

    # 停止 Redis 服务
    log_info "停止 Redis 服务..."
    docker stop bikeshare-redis || true

    # 复制 RDB 文件
    log_info "复制 RDB 文件到 Redis 数据目录..."
    if docker run --rm -v "${rdb_file}":/source/dump.rdb -v bikeshare-redis-data:/data cp /source/dump.rdb /data/dump.rdb; then
        log_info "RDB 文件已复制"
    else
        log_error "RDB 文件复制失败"
        return 1
    fi

    # 启动 Redis 服务
    log_info "启动 Redis 服务..."
    docker start bikeshare-redis

    log_info "Redis 恢复完成"
}

# ==================== MinIO 恢复 ====================
restore_minio() {
    log_info "MinIO 数据通过 volume 自动持久化，无需手动恢复"
    log_info "如需恢复特定文件，请使用 MinIO 客户端 (mc) 工具"
}

# ==================== 主函数 ====================
main() {
    log_info "=========================================="
    log_info "BikeShare 数据恢复开始"
    log_info "=========================================="

    if [ "$RESTORE_ALL" = true ]; then
        restore_mysql
        restore_redis
        restore_minio
    else
        if [ "$RESTORE_DATABASE" = true ]; then
            restore_mysql
        fi
        if [ "$RESTORE_REDIS" = true ]; then
            restore_redis
        fi
        if [ "$RESTORE_MINIO" = true ]; then
            restore_minio
        fi
    fi

    log_info "=========================================="
    log_info "BikeShare 数据恢复完成"
    log_info "=========================================="
}

# 运行主函数
main
