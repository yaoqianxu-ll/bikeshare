#!/bin/bash
################################################################################
# BikeShare 快速部署脚本
# 功能：一键部署所有服务
# 使用：./deploy.sh [选项]
################################################################################

set -e

# ==================== 配置 ====================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
cd "${PROJECT_ROOT}"

# ==================== 颜色输出 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# ==================== 帮助信息 ====================
show_help() {
    echo "BikeShare 快速部署脚本"
    echo ""
    echo "用法：$0 [选项]"
    echo ""
    echo "选项:"
    echo "  --build        强制重新构建镜像"
    echo "  --no-build     跳过构建，仅启动服务"
    echo "  --clean        清理旧数据后部署"
    echo "  --backup       部署前先备份"
    echo "  --help         显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 快速部署"
    echo "  $0 --build            # 重新构建后部署"
    echo "  $0 --backup --build   # 备份后重新构建部署"
    exit 0
}

# ==================== 参数解析 ====================
FORCE_BUILD=false
SKIP_BUILD=false
CLEAN=false
BACKUP_FIRST=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            FORCE_BUILD=true
            shift
            ;;
        --no-build)
            SKIP_BUILD=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --backup)
            BACKUP_FIRST=true
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

# ==================== 检查前置条件 ====================
check_prerequisites() {
    log_step "检查前置条件..."

    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装"
        exit 1
    fi

    # 检查 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装"
        exit 1
    fi

    # 检查环境变量文件
    if [ ! -f "${SCRIPT_DIR}/.env" ]; then
        log_warn "环境变量文件不存在，复制示例配置..."
        cp "${SCRIPT_DIR}/.env.example" "${SCRIPT_DIR}/.env"
        log_warn "请编辑 ${SCRIPT_DIR}/.env 文件配置环境变量"
        log_warn "配置完成后重新运行部署脚本"
        exit 1
    fi

    # 检查 SSL 证书
    if [ ! -f "${SCRIPT_DIR}/nginx/ssl/fullchain.pem" ] || [ ! -f "${SCRIPT_DIR}/nginx/ssl/privkey.pem" ]; then
        log_warn "SSL 证书不存在，请上传证书到："
        log_warn "  ${SCRIPT_DIR}/nginx/ssl/fullchain.pem"
        log_warn "  ${SCRIPT_DIR}/nginx/ssl/privkey.pem"
        log_warn ""
        log_warn "或者使用 Let's Encrypt 自动申请证书"
    fi

    log_info "前置检查完成"
}

# ==================== 备份数据 ====================
backup_data() {
    log_step "备份现有数据..."

    if [ -f "${SCRIPT_DIR}/scripts/backup.sh" ]; then
        bash "${SCRIPT_DIR}/scripts/backup.sh"
    else
        log_warn "备份脚本不存在，跳过备份"
    fi
}

# ==================== 清理旧数据 ====================
clean_old_data() {
    log_step "清理旧数据..."

    # 停止所有容器
    docker-compose -f "${SCRIPT_DIR}/docker-compose.yml" down 2>/dev/null || true

    # 清理悬空镜像
    docker image prune -f

    log_info "清理完成"
}

# ==================== 构建镜像 ====================
build_images() {
    log_step "构建 Docker 镜像..."

    if [ "$FORCE_BUILD" = true ]; then
        log_info "强制重新构建所有镜像..."
        docker-compose -f "${SCRIPT_DIR}/docker-compose.yml" build --no-cache
    else
        log_info "使用缓存构建镜像..."
        docker-compose -f "${SCRIPT_DIR}/docker-compose.yml" build
    fi

    log_info "镜像构建完成"
}

# ==================== 启动服务 ====================
start_services() {
    log_step "启动服务..."

    # 创建必要的目录
    mkdir -p "${SCRIPT_DIR}/logs"
    mkdir -p "${SCRIPT_DIR}/backups"
    mkdir -p "${SCRIPT_DIR}/volumes"

    # 启动所有服务
    docker-compose -f "${SCRIPT_DIR}/docker-compose.yml" up -d

    log_info "服务启动完成"
}

# ==================== 健康检查 ====================
health_check() {
    log_step "执行健康检查..."

    local max_wait=120
    local interval=5
    local elapsed=0

    echo "等待服务启动..."

    while [ $elapsed -lt $max_wait ]; do
        # 检查 MySQL
        if docker ps --format '{{.Names}}' | grep -q bikeshare-mysql; then
            if docker exec bikeshare-mysql mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD:-}" &>/dev/null; then
                echo -e "\e[32m✓\e[0m MySQL 就绪"
            else
                echo -e "\e[33m◌\e[0m MySQL 启动中..."
            fi
        fi

        # 检查后端
        if curl -s http://localhost:8080/actuator/health &>/dev/null; then
            echo -e "\e[32m✓\e[0m 后端 API 就绪"
        else
            echo -e "\e[33m◌\e[0m 后端 API 启动中..."
        fi

        # 检查 Nginx
        if curl -s http://localhost/health &>/dev/null; then
            echo -e "\e[32m✓\e[0m Nginx 就绪"
        else
            echo -e "\e[33m◌\e[0m Nginx 启动中..."
        fi

        sleep $interval
        elapsed=$((elapsed + interval))
    done

    echo ""
    log_info "健康检查完成"
}

# ==================== 显示访问信息 ====================
show_access_info() {
    echo ""
    echo "=========================================="
    echo "        BikeShare 部署完成！"
    echo "=========================================="
    echo ""
    echo "访问地址:"
    echo "  用户端：https://bikeshare.online"
    echo "  管理端：https://admin.bikeshare.online"
    echo "  MinIO:   https://minio.bikeshare.online"
    echo ""
    echo "默认账号:"
    echo "  管理员：admin / admin123"
    echo "  用户：  user / user123"
    echo ""
    echo "常用命令:"
    echo "  docker-compose ps          # 查看容器状态"
    echo "  docker-compose logs -f     # 查看日志"
    echo "  ./scripts/backup.sh        # 手动备份"
    echo ""
    echo "=========================================="
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo "=========================================="
    echo "     BikeShare 自动化部署脚本"
    echo "=========================================="
    echo ""

    # 检查前置条件
    check_prerequisites

    # 备份数据
    if [ "$BACKUP_FIRST" = true ]; then
        backup_data
    fi

    # 清理旧数据
    if [ "$CLEAN" = true ]; then
        clean_old_data
    fi

    # 构建镜像
    if [ "$SKIP_BUILD" = false ]; then
        build_images
    else
        log_info "跳过构建步骤"
    fi

    # 启动服务
    start_services

    # 健康检查
    health_check

    # 显示访问信息
    show_access_info
}

# 运行主函数
main
