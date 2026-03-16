#!/bin/bash

# BikeShare 部署到服务器脚本
# 服务器 IP: 124.221.113.208
# 注意：请先在本地执行 mvn package 和 npm build

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/script/prod/docker-compose.yml"

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
echo "  服务器：124.221.113.208"
echo "=========================================="

# 1. 停止并清理旧容器
echo ""
echo "[1/3] 停止旧容器..."
${DOCKER_COMPOSE_CMD} -f "$COMPOSE_FILE" down || true

# 2. 清理无用镜像
echo ""
echo "[2/3] 清理无用镜像..."
docker image prune -f || true

# 3. 构建并启动新容器
echo ""
echo "[3/3] 启动新容器..."
${DOCKER_COMPOSE_CMD} -f "$COMPOSE_FILE" up -d

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "服务访问地址："
echo "  前端：http://124.221.113.208"
echo "  后端：http://124.221.113.208:8080"
echo ""
echo "查看日志：${DOCKER_COMPOSE_CMD} -f script/prod/docker-compose.yml logs -f"
echo "停止服务：${DOCKER_COMPOSE_CMD} -f script/prod/docker-compose.yml down"
echo "=========================================="
