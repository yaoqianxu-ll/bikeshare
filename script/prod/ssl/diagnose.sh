#!/bin/bash

# BikeShare SSL 诊断和修复脚本
# 在服务器上执行

set -e

echo "=========================================="
echo "  SSL 诊断工具"
echo "=========================================="

# 1. 检查容器状态
echo ""
echo "[1/6] 检查容器状态..."
cd /opt/bickdemo || { echo "目录不存在"; exit 1; }
docker compose ps

# 2. 检查 frontend 容器日志
echo ""
echo "[2/6] 检查 Nginx 容器日志..."
docker compose logs frontend | tail -50

# 3. 检查 SSL 证书挂载
echo ""
echo "[3/6] 检查 SSL 证书文件..."
echo "宿主机证书目录："
ls -la /opt/bickdemo/ssl/bikeshare.online_nginx/ 2>/dev/null || echo "目录不存在"

echo ""
echo "容器内证书目录："
docker exec bickdemo-frontend-1 ls -la /etc/nginx/ssl/ 2>/dev/null || echo "容器内目录不存在或容器未运行"

# 4. 检查 Nginx 配置
echo ""
echo "[4/6] 测试 Nginx 配置..."
docker exec bickdemo-frontend-1 nginx -t 2>&1 || echo "Nginx 配置测试失败"

# 5. 检查端口监听
echo ""
echo "[5/6] 检查端口监听..."
netstat -tlnp 2>/dev/null | grep -E ":(80|443)" || ss -tlnp | grep -E ":(80|443)"

# 6. 检查防火墙
echo ""
echo "[6/6] 检查防火墙规则..."
iptables -L -n 2>/dev/null | grep -E "(80|443)" || echo "无法检查 iptables (可能需要 root 权限)"

echo ""
echo "=========================================="
echo "  诊断完成"
echo "=========================================="

# 提供修复建议
echo ""
echo "常见问题和修复命令："
echo ""
echo "1. 如果 SSL 证书未挂载 - 确保证书文件在 /opt/bickdemo/ssl/bikeshare.online_nginx/"
echo "2. 如果 Nginx 配置错误 - 执行：docker exec bickdemo-frontend-1 nginx -t"
echo "3. 如果容器未运行 - 执行：cd /opt/bickdemo && docker compose up -d"
echo "4. 如果需要重启 - 执行：docker compose restart frontend"
echo ""
echo "执行修复（重新启动容器）："
read -p "是否重新启动 frontend 容器？(y/n): " confirm
if [ "$confirm" = "y" ]; then
    docker compose restart frontend
    echo "容器已重启，请等待 10 秒后测试访问..."
fi
