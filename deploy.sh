#!/bin/bash

# BikeShare 部署脚本
# 服务器 IP: 60.205.169.251

set -e

echo "=========================================="
echo "  BikeShare 部署脚本"
echo "  服务器：60.205.169.251"
echo "=========================================="

# 1. 构建后端
echo ""
echo "[1/4] 构建后端..."
cd bickdemo-backend
mvn clean package -DskipTests -B
cd ..

# 2. 构建前端
echo ""
echo "[2/4] 构建前端..."
cd bickdemo-frontend
npm run build
cd ..

# 3. 停止并清理旧容器
echo ""
echo "[3/4] 停止旧容器..."
docker-compose down || true

# 4. 启动新容器
echo ""
echo "[4/4] 启动新容器..."
docker-compose up -d --build

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "服务访问地址："
echo "  前端：http://60.205.169.251"
echo "  后端：http://60.205.169.251:8080"
echo "  MinIO: http://60.205.169.251:9000"
echo ""
echo "默认账号："
echo "  管理员：admin / admin123"
echo "  用户：user / user123"
echo ""
echo "查看日志：docker-compose logs -f"
echo "停止服务：docker-compose down"
echo "=========================================="
