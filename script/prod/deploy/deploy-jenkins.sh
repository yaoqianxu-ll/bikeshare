#!/bin/bash

# BikeShare Jenkins 部署脚本
# 用于 Jenkins 自动化部署

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
SERVER_IP="60.205.169.251"
PROJECT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"

echo_log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error_log() {
    echo -e "${RED}[ERROR]$(NC) $1"
}

warn_log() {
    echo -e "${YELLOW}[WARN]$(NC) $1"
}

echo "=========================================="
echo "  BikeShare 部署脚本 (Jenkins 版)"
echo "  服务器：${SERVER_IP}"
echo "=========================================="

# 检查 Docker 是否可用
check_docker() {
    echo_log "检查 Docker 环境..."
    if ! command -v docker &> /dev/null; then
        error_log "Docker 未安装"
        exit 1
    fi

    if ! docker info &> /dev/null; then
        error_log "Docker 服务未运行或无权限"
        exit 1
    fi

    echo_log "Docker 版本：$(docker --version)"

    # 检查 docker-compose
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        error_log "docker-compose 未安装"
        exit 1
    fi

    echo_log "使用 ${DOCKER_COMPOSE_CMD}"
}

# 构建后端
build_backend() {
    echo_log "[1/5] 构建后端..."
    cd "${PROJECT_DIR}/bickdemo-backend"

    # 检查 Maven
    if ! command -v mvn &> /dev/null; then
        warn_log "Maven 未安装，使用 Docker 构建..."
        docker run --rm -v "$(pwd)":/usr/src/app -w /usr/src/app maven:3.9.6-eclipse-temurin-17 mvn clean package -DskipTests -B
    else
        mvn clean package -DskipTests -B
    fi

    cd "${PROJECT_DIR}"
}

# 构建前端
build_frontend() {
    echo_log "[2/5] 构建前端..."
    cd "${PROJECT_DIR}/bickdemo-frontend"

    # 检查 Node.js
    if ! command -v node &> /dev/null; then
        warn_log "Node.js 未安装，使用 Docker 构建..."
        docker run --rm -v "$(pwd)":/app -w /app node:20 npm ci --legacy-peer-deps
        docker run --rm -v "$(pwd)":/app -w /app node:20 npm run build
    else
        npm ci --legacy-peer-deps || npm install --legacy-peer-deps
        npm run build
    fi

    cd "${PROJECT_DIR}"
}

# 停止旧容器
stop_containers() {
    echo_log "[3/5] 停止旧容器..."
    cd "${PROJECT_DIR}"
    ${DOCKER_COMPOSE_CMD} down || true
}

# 清理旧镜像
cleanup_images() {
    echo_log "清理悬空镜像..."
    docker image prune -f
}

# 构建并启动
start_containers() {
    echo_log "[4/5] 构建并启动新容器..."
    cd "${PROJECT_DIR}"
    ${DOCKER_COMPOSE_CMD} build --no-cache
    ${DOCKER_COMPOSE_CMD} up -d
}

# 健康检查
health_check() {
    echo_log "[5/5] 健康检查..."

    # 等待后端启动
    echo_log "等待后端服务启动 (最多 120 秒)..."
    for i in {1..24}; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
            echo_log "✅ 后端服务健康检查通过"
            return 0
        fi
        sleep 5
    done

    error_log "后端服务健康检查失败"
    echo_log "查看日志："
    ${DOCKER_COMPOSE_CMD} logs app
    return 1
}

# 主函数
main() {
    check_docker
    build_backend
    build_frontend
    stop_containers
    cleanup_images
    start_containers

    if health_check; then
        echo ""
        echo "=========================================="
        echo "  ✅ 部署完成！"
        echo "=========================================="
        echo ""
        echo "服务访问地址："
        echo "  前端：http://${SERVER_IP}"
        echo "  后端：http://${SERVER_IP}:8080"
        echo "  MinIO: http://${SERVER_IP}:9000"
        echo ""
        echo "默认账号："
        echo "  管理员：admin / admin123"
        echo "  用户：user / user123"
        echo ""
        echo "查看日志：${DOCKER_COMPOSE_CMD} logs -f"
        echo "停止服务：${DOCKER_COMPOSE_CMD} down"
        echo "=========================================="
        exit 0
    else
        error_log "部署失败"
        exit 1
    fi
}

# 支持参数
case "${1:-deploy}" in
    deploy)
        main
        ;;
    start)
        check_docker
        start_containers
        ;;
    stop)
        check_docker
        stop_containers
        ;;
    restart)
        check_docker
        stop_containers
        start_containers
        ;;
    logs)
        check_docker
        cd "${PROJECT_DIR}"
        ${DOCKER_COMPOSE_CMD} logs -f
        ;;
    status)
        check_docker
        cd "${PROJECT_DIR}"
        ${DOCKER_COMPOSE_CMD} ps
        ;;
    clean)
        check_docker
        stop_containers
        cleanup_images
        ;;
    *)
        echo "用法：$0 {deploy|start|stop|restart|logs|status|clean}"
        exit 1
        ;;
esac
