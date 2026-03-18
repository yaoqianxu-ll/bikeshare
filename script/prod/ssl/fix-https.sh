#!/bin/bash

# BikeShare HTTPS 一键修复脚本
# 使用方法：在服务器上执行 bash /tmp/fix-https.sh

set -e

echo "=========================================="
echo "  BikeShare HTTPS 修复脚本"
echo "=========================================="

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 检查 SSL 证书
echo ""
echo -e "${YELLOW}[1/5] 检查 SSL 证书...${NC}"
SSL_DIR="/opt/bickdemo/ssl/bikeshare.online_nginx"
if [ ! -d "$SSL_DIR" ]; then
    echo -e "${RED}SSL 目录不存在：$SSL_DIR${NC}"
    echo "请先上传证书文件到该目录"
    exit 1
fi

if [ ! -f "$SSL_DIR/bikeshare.online_bundle.crt" ] || [ ! -f "$SSL_DIR/bikeshare.online.key" ]; then
    echo -e "${RED}证书文件缺失${NC}"
    ls -la "$SSL_DIR"
    exit 1
fi

echo -e "${GREEN}SSL 证书文件存在${NC}"

# 2. 停止容器
echo ""
echo -e "${YELLOW}[2/5] 停止容器...${NC}"
cd /opt/bickdemo
docker compose down

# 3. 重新构建
echo ""
echo -e "${YELLOW}[3/5] 重新构建 frontend 和 admin...${NC}"
docker compose build --no-cache frontend admin

# 4. 启动服务
echo ""
echo -e "${YELLOW}[4/5] 启动服务...${NC}"
docker compose up -d

# 5. 等待启动
echo "等待服务启动..."
sleep 10

# 6. 检查状态
echo ""
echo -e "${YELLOW}[5/5] 检查服务状态...${NC}"
docker compose ps

echo ""
echo "检查端口监听："
ss -tlnp | grep -E ":(80|443|3001|3002)" || netstat -tlnp | grep -E ":(80|443|3001|3002)"

echo ""
echo "=========================================="
echo -e "${GREEN}  修复完成！${NC}"
echo "=========================================="
echo ""
echo "访问地址："
echo "  用户端 (HTTP):  http://bikeshare.online (会自动跳转 HTTPS)"
echo "  用户端 (HTTPS): https://bikeshare.online"
echo "  管理端 (HTTPS): https://bikeshare.online:3002"
echo ""
echo "如仍有问题，查看日志：docker compose logs frontend"
