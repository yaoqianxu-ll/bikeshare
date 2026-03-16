#!/bin/bash

# BikeShare 本地构建 + 服务器部署脚本
# 服务器 IP: 124.221.113.208

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"

echo "=========================================="
echo "  BikeShare 部署脚本"
echo "  步骤：本地构建 -> 上传服务器 -> 部署"
echo "=========================================="

# 1. 本地构建后端
echo ""
echo "[1/3] 本地构建后端..."
cd "$ROOT_DIR/bickdemo-backend"
mvn clean package -DskipTests -B

# 2. 本地构建前端
echo ""
echo "[2/3] 本地构建前端..."
cd "$ROOT_DIR/bickdemo-frontend"
npm run build

echo ""
echo "=========================================="
echo "  本地构建完成！"
echo "=========================================="
echo ""
echo "[3/3] 请执行以下命令上传到服务器："
echo ""
echo "# 方式 1: 使用 scp 命令（Linux/Mac）"
echo "scp -r ./* root@124.221.113.208:/opt/bickdemo/"
echo ""
echo "# 方式 2: 使用 WinSCP（Windows）"
echo "打开 WinSCP，连接到 root@124.221.113.208"
echo "将整个项目目录拖拽到 /opt/bickdemo/"
echo ""
echo "=========================================="
echo ""
echo "上传完成后，在服务器上执行："
echo "  ssh root@124.221.113.208"
echo "  cd /opt/bickdemo"
echo "  docker compose -f script/prod/docker-compose.yml up -d"
echo ""
echo "=========================================="
