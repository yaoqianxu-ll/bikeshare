#!/bin/bash

# BikeShare 部署到服务器脚本
# 通过环境变量覆盖部署地址
# 注意：请先在本地执行 mvn package 和 npm build

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
ENV_FILE="${BICKDEMO_ENV_FILE:-$ROOT_DIR/.env}"

if [ ! -f "$ENV_FILE" ] && [ -f "$HOME/.bickdemo.env" ]; then
    ENV_FILE="$HOME/.bickdemo.env"
fi

if [ -f "$ENV_FILE" ]; then
    set -a
    . "$ENV_FILE"
    set +a
fi

DEPLOY_HOST="${DEPLOY_HOST:-your-server-host}"
APP_PUBLIC_HOST="${APP_PUBLIC_HOST:-http://localhost}"
BACKEND_PUBLIC_HOST="${BACKEND_PUBLIC_HOST:-http://localhost:8080}"

if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
else
    echo "docker-compose 未安装"
    exit 1
fi

echo "=========================================="
echo "  BikeShare Docker 部署脚本"
echo "  服务器：${DEPLOY_HOST}"
echo "=========================================="

# 1. 停止并清理旧容器
echo ""
echo "[1/3] 停止旧容器..."
cd "$ROOT_DIR"
${DOCKER_COMPOSE_CMD} down || true

# 2. 清理无用镜像
echo ""
echo "[2/3] 清理无用镜像..."
docker image prune -f || true

# 3. 构建并启动新容器
echo ""
echo "[3/3] 启动新容器..."
${DOCKER_COMPOSE_CMD} up -d

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "服务访问地址："
echo "  前端：${APP_PUBLIC_HOST}"
echo "  后端：${BACKEND_PUBLIC_HOST}"
echo ""
echo "查看日志：${DOCKER_COMPOSE_CMD} logs -f"
echo "停止服务：${DOCKER_COMPOSE_CMD} down"
echo "=========================================="
