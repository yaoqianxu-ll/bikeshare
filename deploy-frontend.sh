#!/bin/bash

# 前端部署脚本 - 使用本地构建产物
# 使用方法：在服务器上运行

set -e

FRONTEND_PATH="/opt/bickdemo/frontend"
NGINX_PATH="/usr/share/nginx/html"

echo "创建目录..."
mkdir -p $FRONTEND_PATH

echo "清理旧文件..."
rm -rf $FRONTEND_PATH/*

echo "复制构建产物..."
# 假设 dist 目录已经上传到当前目录
cp -r dist/* $FRONTEND_PATH/

echo "创建 nginx 配置..."
cat > /etc/nginx/conf.d/bickdemo.conf << 'EOF'
server {
    listen 80;
    server_name localhost;

    root $FRONTEND_PATH;
    index index.html;

    # 前端静态文件
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端服务
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
EOF

echo "测试 nginx 配置..."
nginx -t

echo "重启 nginx..."
systemctl restart nginx || nginx -s reload

echo "部署完成！"
