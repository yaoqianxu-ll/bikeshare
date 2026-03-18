# SSL 证书部署指南

## 概述

本文档说明如何在 Jenkins 构建部署流程中配置 HTTPS。

## 已修改的文件

| 文件 | 修改内容 |
|------|----------|
| `script/prod/nginx.conf` | 添加 HTTPS 配置（443 端口）和 HTTP 自动重定向 |
| `script/prod/docker-compose.yml` | 添加 443 端口映射和 SSL 证书挂载 |
| `docker-compose.yml` | 同上（根目录主配置文件） |

## SSL 证书文件

位于 `script/prod/ssl/bikeshare.online_nginx/` 目录：

```
bikeshare.online_bundle.crt   # 证书链
bikeshare.online.key          # 私钥
bikeshare.online_bundle.pem   # PEM 格式证书
bikeshare.online.csr          # 证书签名请求（备用）
```

## 快速诊断

### 检查域名解析

```bash
# 在本地电脑执行
ping bikeshare.online
# 应该返回 124.221.113.208
```

### 检查服务器端口

```bash
# 检查 443 端口是否开放
telnet bikeshare.online 443
# 或
curl -I https://bikeshare.online
```

## 部署步骤

### 1. 上传 SSL 证书到服务器

在服务器上创建目录：

```bash
# SSH 登录服务器
ssh root@124.221.113.208

# 创建 SSL 证书目录
mkdir -p /opt/bickdemo/ssl/bikeshare.online_nginx
```

上传证书文件（三种方式任选）：

**方式 A: 使用 scp**
```bash
# 在本地 PowerShell/CMD 执行
scp script/prod/ssl/bikeshare.online_nginx/* root@124.221.113.208:/opt/bickdemo/ssl/bikeshare.online_nginx/
```

**方式 B: 使用 Git（如果目录未被忽略）**
```bash
# 证书文件已存在于 Git 仓库中，服务器上直接从仓库复制
cd /opt/bickdemo
cp -r script/prod/ssl/bikeshare.online_nginx/* ssl/bikeshare.online_nginx/
```

**方式 C: 手动复制**
```bash
# 在服务器上直接创建文件，然后粘贴证书内容
cat > /opt/bickdemo/ssl/bikeshare.online_nginx/bikeshare.online_bundle.crt << 'EOF'
-----BEGIN CERTIFICATE-----
(粘贴证书内容)
-----END CERTIFICATE-----
EOF
```

### 2. 更新 Jenkins 节点环境变量

在 Jenkins 服务器上编辑 `.env.jenkins.current`：

```bash
# 编辑环境变量文件
vi /opt/bickdemo/script/prod/deploy/.env.jenkins.current
```

确保 `APP_PUBLIC_HOST` 和 `ADMIN_PUBLIC_HOST` 使用 HTTPS：

```ini
APP_PUBLIC_HOST=https://bikeshare.online
ADMIN_PUBLIC_HOST=https://bikeshare.online:3001
BACKEND_PUBLIC_HOST=https://bikeshare.online:8080
MINIO_PUBLIC_HOST=https://bikeshare.online:9000
JENKINS_PUBLIC_HOST=http://124.221.113.208:8081
GITEA_PUBLIC_HOST=http://124.221.113.208:3000
```

### 3. 在 Jenkins 中重新构建

1. 访问 Jenkins: `http://124.221.113.208:8081`
2. 找到 `bickdemo-deploy` 任务
3. 点击 **立即构建**
4. 等待构建完成

### 4. 验证 HTTPS 配置

```bash
# 测试 HTTP 是否自动跳转到 HTTPS
curl -I http://bikeshare.online
# 预期：HTTP/1.1 301 Moved Permanently
# 预期：Location: https://bikeshare.online/

# 测试 HTTPS 是否正常工作
curl -I https://bikeshare.online
# 预期：HTTP/2 200

# 检查 SSL 证书信息
openssl s_client -connect bikeshare.online:443 -servername bikeshare.online
```

### 5. 浏览器访问

- 用户端：`https://bikeshare.online`
- 管理端：`https://bikeshare.online:3001`
- 后端 API: `https://bikeshare.online/api`

## Nginx 配置说明

### HTTP 重定向
```nginx
server {
    listen 80;
    server_name bikeshare.online;
    return 301 https://$server_name$request_uri;
}
```
- 所有 HTTP 请求自动 301 重定向到 HTTPS
- 保持原始路径和参数

### HTTPS 服务器
```nginx
server {
    listen 443 ssl http2;
    server_name bikeshare.online;

    ssl_certificate /etc/nginx/ssl/bikeshare.online_bundle.crt;
    ssl_certificate_key /etc/nginx/ssl/bikeshare.online.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5:!3DES;

    # HSTS 强制 HTTPS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}
```

## 常见问题

### Q1: 证书文件权限问题
```bash
# 确保证书文件权限正确
chmod 644 /opt/bickdemo/ssl/bikeshare.online_nginx/bikeshare.online_bundle.crt
chmod 600 /opt/bickdemo/ssl/bikeshare.online_nginx/bikeshare.online.key
```

### Q2: Nginx 容器启动失败
```bash
# 查看 Nginx 日志
docker compose logs frontend

# 检查证书路径是否正确
docker exec bickdemo-frontend-1 ls -la /etc/nginx/ssl/
```

### Q3: HTTPS 无法访问
```bash
# 检查防火墙是否开放 443 端口
iptables -L -n | grep 443

# 如使用云服务器，检查安全组规则是否允许 443 端口
```

### Q4: 证书过期怎么办
1. 重新申请 SSL 证书
2. 替换 `script/prod/ssl/bikeshare.online_nginx/` 目录下的证书文件
3. 在 Jenkins 中重新构建部署
4. 重启 Nginx 容器：`docker compose restart frontend`

## 安全建议

1. **定期更新证书** - SSL 证书通常有效期 1 年，设置到期提醒
2. **保护私钥** - 私钥文件权限设置为 600，仅 root 可读
3. **启用 HSTS** - 已配置，强制浏览器使用 HTTPS
4. **监控证书状态** - 可使用在线工具监控证书有效期

## 回滚方案

如果 HTTPS 配置出现问题需要回滚：

```bash
# 1. 恢复原 nginx.conf
cd /opt/bickdemo
git checkout script/prod/nginx.conf

# 2. 恢复原 docker-compose.yml
git checkout script/prod/docker-compose.yml
git checkout docker-compose.yml

# 3. 重新部署
docker compose down
docker compose up -d
```
