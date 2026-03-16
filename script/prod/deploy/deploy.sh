#!/bin/bash

# BikeShare 部署脚本
# 服务器 IP: 124.221.113.208

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
echo "  BikeShare 部署脚本"
echo "  服务器：124.221.113.208"
echo "=========================================="

# 1. 构建后端
echo ""
echo "[1/4] 构建后端..."
cd "$ROOT_DIR/bickdemo-backend"
mvn clean package -DskipTests -B

# 2. 构建前端
echo ""
echo "[2/4] 构建前端..."
cd "$ROOT_DIR/bickdemo-frontend"
npm run build

# 3. 停止并清理旧容器
echo ""
echo "[3/4] 停止旧容器..."
${DOCKER_COMPOSE_CMD} -f "$COMPOSE_FILE" down || true

# 4. 启动新容器
echo ""
echo "[4/4] 启动新容器..."
${DOCKER_COMPOSE_CMD} -f "$COMPOSE_FILE" up -d --build

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "服务访问地址："
echo "  前端：http://124.221.113.208"
echo "  后端：http://124.221.113.208:8080"
echo "  MinIO: http://124.221.113.208:9000"
echo ""
echo "默认账号："
echo "  管理员：admin / admin123"
echo "  用户：user / user123"
echo ""
echo "查看日志：${DOCKER_COMPOSE_CMD} -f script/prod/docker-compose.yml logs -f"
echo "停止服务：${DOCKER_COMPOSE_CMD} -f script/prod/docker-compose.yml down"
echo "=========================================="
