#!/bin/bash

# 本地构建并部署脚本

set -e

echo "=========================================="
echo "  BikeShare 本地构建 + 服务器部署"
echo "  服务器：124.221.113.208"
echo "=========================================="

# 1. 本地构建后端
echo ""
echo "[1/4] 本地构建后端..."
cd bickdemo-backend
mvn clean package -DskipTests -B
cd ..

# 2. 本地构建前端
echo ""
echo "[2/4] 本地构建前端..."
cd bickdemo-frontend
npm run build
cd ..

# 3. 上传到服务器
echo ""
echo "[3/4] 上传到服务器..."
echo "请使用 scp 或 WinSCP 将以下目录上传到服务器 /opt/bickdemo/:"
echo "  - bickdemo-backend/target/*.jar"
echo "  - bickdemo-frontend/dist/*"
echo "  - docker-compose.yml"
echo "  - init.sql"
echo "  - bickdemo-backend/Dockerfile.simple"
echo ""
read -p "上传完成后按回车继续..."

# 4. SSH 登录启动
echo ""
echo "[4/4] 请登录服务器启动服务..."
echo "ssh root@124.221.113.208"
echo "cd /opt/bickdemo"
echo "docker-compose up -d"

echo ""
echo "=========================================="
echo "  构建完成！"
echo "=========================================="
