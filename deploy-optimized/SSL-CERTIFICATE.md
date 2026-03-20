# SSL 证书配置指南

## 概述

本优化方案支持三种 SSL 证书配置方式：

1. **通配符证书** (推荐) - 一张证书覆盖所有子域名
2. **多域名证书** - 一张证书包含多个域名
3. **Let's Encrypt** - 免费证书，自动续期

## 证书要求

需要覆盖的域名：

| 域名 | 用途 |
|------|------|
| `bikeshare.online` | 用户端 |
| `www.bikeshare.online` | 用户端 (备用) |
| `admin.bikeshare.online` | 管理端 |
| `minio.bikeshare.online` | MinIO |

---

## 方式一：通配符证书 (推荐)

### 申请通配符证书

通配符证书使用 `*.bikeshare.online` 覆盖所有子域名。

#### 使用 Let's Encrypt

```bash
# 安装 certbot
apt install certbot python3-certbot-dns-cloudflare -y
# 或使用其他 DNS 提供商的插件

# 申请通配符证书 (需要 DNS 验证)
certbot certonly --manual --preferred-challenges dns \
  -d bikeshare.online -d *.bikeshare.online
```

#### 使用阿里云 DNS

```bash
# 安装 certbot 和阿里云 DNS 插件
pip install certbot-dns-aliyun

# 配置阿里云 API 密钥
export ALIYUN_API_KEY="your-api-key"
export ALIYUN_API_SECRET="your-api-secret"

# 申请证书
certbot certonly --dns-aliyun \
  -d bikeshare.online -d *.bikeshare.online
```

### 部署证书

```bash
# 复制证书到部署目录
cp /etc/letsencrypt/live/bikeshare.online/fullchain.pem \
   /opt/bickdemo/deploy-optimized/nginx/ssl/fullchain.pem

cp /etc/letsencrypt/live/bikeshare.online/privkey.pem \
   /opt/bickdemo/deploy-optimized/nginx/ssl/privkey.pem

# 设置权限
chmod 644 /opt/bickdemo/deploy-optimized/nginx/ssl/fullchain.pem
chmod 600 /opt/bickdemo/deploy-optimized/nginx/ssl/privkey.pem

# 重启 Nginx
docker-compose restart nginx
```

---

## 方式二：多域名证书

### 申请多域名证书

```bash
certbot certonly --manual --preferred-challenges http \
  -d bikeshare.online \
  -d www.bikeshare.online \
  -d admin.bikeshare.online \
  -d minio.bikeshare.online
```

### 部署证书

同通配符证书。

---

## 方式三：上传已有证书

### 从证书提供商下载

1. 从证书提供商下载证书文件
2. 通常包含以下文件：
   - `xxx_bundle.crt` 或 `fullchain.pem` - 证书链
   - `xxx.key` 或 `privkey.pem` - 私钥

### 上传到服务器

```bash
# 方式 1: 使用 SCP
scp /path/to/cert.pem root@server:/opt/bickdemo/deploy-optimized/nginx/ssl/fullchain.pem
scp /path/to/key.pem root@server:/opt/bickdemo/deploy-optimized/nginx/ssl/privkey.pem

# 方式 2: 使用 SFTP
sftp root@server
cd /opt/bickdemo/deploy-optimized/nginx/ssl
put /path/to/cert.pem fullchain.pem
put /path/to/key.pem privkey.pem
```

---

## 证书续期

### Let's Encrypt 自动续期

```bash
# 编辑 crontab
crontab -e

# 添加定时任务 (每天凌晨 2 点检查)
0 2 * * * certbot renew --quiet && docker-compose restart nginx
```

### 手动续期

```bash
# 续期证书
certbot renew

# 重启 Nginx
docker-compose restart nginx
```

---

## 验证证书

### 检查证书信息

```bash
# 查看证书详情
openssl s_client -connect bikeshare.online:443 -servername bikeshare.online | openssl x509 -noout -dates

# 检查证书链
openssl s_client -connect bikeshare.online:443 -showcerts
```

### 在线验证

访问以下网站验证证书配置：

- [SSL Labs](https://www.ssllabs.com/ssltest/)
- [MySSL](https://myssl.com/)

---

## 证书文件格式

### PEM 格式 (推荐)

```
-----BEGIN CERTIFICATE-----
(Multiple lines of base64 encoded data)
-----END CERTIFICATE-----
```

### 转换格式

如果需要转换证书格式：

```bash
# CRT 转 PEM
openssl x509 -in certificate.crt -out certificate.pem -outform PEM

# PFX 转 PEM (Windows IIS 导出)
openssl pkcs12 -in certificate.pfx -nocerts -out private.key
openssl pkcs12 -in certificate.pfx -clcerts -nokeys -out certificate.pem
```

---

## 证书文件权限

```bash
# 证书文件权限
chmod 644 nginx/ssl/fullchain.pem  # 证书可被 Nginx 读取
chmod 600 nginx/ssl/privkey.pem    # 私钥仅 Nginx 可读取
chown root:root nginx/ssl/*        # 所有者为 root
```

---

## 常见问题

### Q1: 证书不受信任

**原因**: 证书链不完整

**解决**:
```bash
# 使用完整的证书链
cat fullchain.pem > bundle.pem
# 或
cat certificate.crt intermediate.crt > fullchain.pem
```

### Q2: HTTPS 无法访问

**检查**:
```bash
# 检查证书文件
ls -la nginx/ssl/

# 检查 Nginx 配置
docker exec bikeshare-nginx nginx -t

# 查看 Nginx 日志
docker-compose logs nginx
```

### Q3: 证书续期失败

**解决**:
```bash
# 手动续期
certbot renew --force-renewal

# 检查 DNS 解析
dig bikeshare.online
dig admin.bikeshare.online
```

### Q4: 混合内容警告

**原因**: 页面中包含 HTTP 资源

**解决**:
1. 检查页面资源是否都使用 HTTPS
2. 在 Nginx 配置中添加:
```nginx
add_header Content-Security-Policy "upgrade-insecure-requests";
```

---

## 证书推荐

| 用途 | 推荐证书 | 价格 |
|------|----------|------|
| 个人/测试 | Let's Encrypt | 免费 |
| 企业官网 | Let's Encrypt | 免费 |
| 电商平台 | DigiCert / GlobalSign | ¥2000+/年 |
| 金融/政府 | DigiCert EV | ¥5000+/年 |

---

## 附录：证书文件说明

| 文件 | 说明 | 来源 |
|------|------|------|
| `fullchain.pem` | 完整证书链 (服务器证书 + 中间证书) | CA 或 Let's Encrypt |
| `privkey.pem` | 私钥 | 生成 CSR 时产生 |
| `certificate.pem` | 服务器证书 | CA 颁发 |
| `chain.pem` | 中间证书链 | CA 提供 |

---

## 参考链接

- [Let's Encrypt 官方文档](https://letsencrypt.org/docs/)
- [Certbot 使用指南](https://certbot.eff.org/)
- [SSL 配置最佳实践](https://wiki.mozilla.org/Security/Server_Side_TLS)
