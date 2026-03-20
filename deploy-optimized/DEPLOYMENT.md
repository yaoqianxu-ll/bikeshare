# 优化部署方案详细指南

## 目录

1. [部署架构](#1-部署架构)
2. [前置要求](#2-前置要求)
3. [快速开始](#3-快速开始)
4. [详细配置](#4-详细配置)
5. [数据备份与恢复](#5-数据备份与恢复)
6. [Jenkins CI/CD](#6-jenkins-cicd)
7. [监控与维护](#7-监控与维护)
8. [常见问题](#8-常见问题)

---

## 1. 部署架构

### 1.1 服务组成

| 服务 | 容器名 | 域名/端口 | 说明 |
|------|--------|----------|------|
| MySQL | bikeshare-mysql | 3306 | 数据库 |
| Redis | bikeshare-redis | 6379 | 缓存 |
| RabbitMQ | bikeshare-rabbitmq | 5672, 15672 | 消息队列 |
| MinIO | bikeshare-minio | 9000, 9001 | 对象存储 |
| Spring Boot | bikeshare-app | 8080 | 后端 API |
| Nginx | bikeshare-nginx | 80, 443 | 反向代理 |
| 用户端 | bikeshare-frontend | - | Vue3 用户前端 |
| 管理端 | bikeshare-admin | - | Vue3 管理前端 |
| 备份 | bikeshare-backup | - | 自动备份服务 |

### 1.2 域名规划

| 服务 | 域名 | SSL |
|------|------|-----|
| 用户端 | `bikeshare.online` | ✅ |
| 管理端 | `admin.bikeshare.online` | ✅ |
| MinIO | `minio.bikeshare.online` | ✅ |

### 1.3 与原方案对比

| 特性 | 原方案 | 优化方案 |
|------|--------|----------|
| 管理端访问 | `http://IP:3001` | `https://admin.bikeshare.online` |
| MinIO 访问 | 内网 | `https://minio.bikeshare.online` |
| SSL 证书 | 单域名 | 通配符/多域名 |
| 数据备份 | 手动 | 自动定时备份 |
| 数据持久化 | Docker volumes | 绑定挂载 (便于管理) |
| 资源限制 | 无 | 有 (内存限制) |

---

## 2. 前置要求

### 2.1 硬件要求

- CPU: 4 核及以上
- 内存：8GB 及以上
- 磁盘：50GB 及以上 SSD
- 网络：公网 IP

### 2.2 软件要求

- Docker 20.10+
- Docker Compose 2.0+
- Git

### 2.3 端口开放

| 端口 | 用途 | 说明 |
|------|------|------|
| 80 | HTTP | 自动跳转到 HTTPS |
| 443 | HTTPS | 加密访问 |
| 3306 | MySQL | 数据库 (可选开放) |
| 8081 | Jenkins | CI/CD 管理 |

---

## 3. 快速开始

### 3.1 克隆项目

```bash
# SSH 登录服务器
ssh root@your-server

# 创建项目目录
mkdir -p /opt/bickdemo
cd /opt/bickdemo

# 克隆项目 (或使用已有项目)
git clone <your-repo-url> .
```

### 3.2 准备环境变量

```bash
# 进入部署目录
cd deploy-optimized

# 复制环境变量文件
cp .env.example .env

# 编辑环境变量
vi .env
# 或使用 nano
# nano .env
```

**重要配置项：**

```ini
# MySQL 密码 (务必修改为强密码)
MYSQL_ROOT_PASSWORD=YourStrongRootPassword123!

# MinIO 管理员密码
MINIO_ROOT_USER=bikeshare-admin
MINIO_ROOT_PASSWORD=YourStrongMinioPassword123!

# JWT 密钥 (至少 32 字符)
JWT_SECRET=your-jwt-secret-key-at-least-32-characters-long-random-string

# 邮件配置
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=your-smtp-auth-code
```

### 3.3 配置 SSL 证书

```bash
# 创建 SSL 目录
mkdir -p nginx/ssl

# 上传证书文件
# 方式 1: 使用 scp 从本地上传
# 在本地执行：
# scp /path/to/fullchain.pem root@server:/opt/bickdemo/deploy-optimized/nginx/ssl/
# scp /path/to/privkey.pem root@server:/opt/bickdemo/deploy-optimized/nginx/ssl/

# 方式 2: 使用 Let's Encrypt 自动申请
# 安装 certbot
apt install certbot python3-certbot-nginx -y

# 申请通配符证书 (需要 DNS 验证)
certbot certonly --manual --preferred-challenges dns \
  -d bikeshare.online -d *.bikeshare.online
```

### 3.4 启动服务

```bash
# 方式 1: 使用部署脚本 (推荐)
bash scripts/deploy.sh --backup --build

# 方式 2: 手动启动
docker-compose up -d
```

### 3.5 验证部署

```bash
# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 测试访问
curl -I https://bikeshare.online
curl -I https://admin.bikeshare.online
```

---

## 4. 详细配置

### 4.1 MySQL 配置

```yaml
# 在 docker-compose.yml 中
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    MYSQL_DATABASE: bickdemo
  volumes:
    - ./volumes/mysql:/var/lib/mysql  # 数据持久化
    - ./backups/mysql:/backups        # 备份目录
```

### 4.2 MinIO 配置

MinIO 提供两个端口：
- 9000: API 端口 (SDK 访问)
- 9001: 控制台端口 (Web 界面)

访问 MinIO 控制台：`https://minio.bikeshare.online/console/`

### 4.3 Nginx 配置

三个子域名的配置分别位于：
- `nginx/conf.d/default.conf` - 用户端
- `nginx/conf.d/admin.conf` - 管理端
- `nginx/conf.d/minio.conf` - MinIO

### 4.4 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 必填 |
| `REDIS_PASSWORD` | Redis 密码 | 必填 |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | bikeshare |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | 必填 |
| `MINIO_ROOT_USER` | MinIO 管理员账号 | bikeshare-admin |
| `MINIO_ROOT_PASSWORD` | MinIO 管理员密码 | 必填 |
| `JWT_SECRET` | JWT 签名密钥 | 必填 |
| `BACKUP_RETENTION_DAYS` | 备份保留天数 | 7 |

---

## 5. 数据备份与恢复

### 5.1 自动备份

备份服务默认每天凌晨 3 点自动运行，备份位置：`./backups/auto/`

### 5.2 手动备份

```bash
# 备份所有数据
bash scripts/backup.sh

# 仅备份数据库
bash scripts/backup.sh --database-only

# 仅备份 MinIO
bash scripts/backup.sh --minio-only
```

### 5.3 数据恢复

```bash
# 从备份恢复
bash scripts/restore.sh ./backups/auto/20240101_030000

# 仅恢复数据库
bash scripts/restore.sh ./backups/auto/20240101_030000 --database-only --force
```

### 5.4 异地备份

```bash
# 压缩备份
tar -czf bickdemo-backup-$(date +%Y%m%d).tar.gz ./volumes

# 上传到其他服务器
scp bickdemo-backup-*.tar.gz user@backup-server:/backups/

# 或使用 rsync
rsync -avz ./volumes/ user@backup-server:/backups/volumes/
```

---

## 6. Jenkins CI/CD

### 6.1 启动 Jenkins

```bash
# 进入 Jenkins 目录
cd deploy-optimized/jenkins

# 复制环境变量文件
cp .env.example .env

# 编辑配置（修改 Git 仓库信息）
vi .env

# 启动 Jenkins
docker-compose up -d

# 获取初始管理员密码
docker exec bikeshare-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 6.2 访问 Jenkins

浏览器访问：`http://服务器 IP:8081`

### 6.3 配置流水线

1. 登录 Jenkins
2. 点击 **新建任务**
3. 输入任务名称：`bikeshare-optimized`
4. 选择 **流水线** 类型
5. 滚动到下方，选择 **Pipeline script from SCM**
6. 配置 Git 仓库
7. 脚本路径：`deploy-optimized/jenkins/Jenkinsfile`

### 6.4 配置 Webhook

**Gitee:**
1. 进入仓库 -> 管理 -> WebHooks
2. 添加 WebHook: `http://服务器 IP:8081/gitee-webhook/`

**GitHub:**
1. 进入仓库 -> Settings -> Webhooks
2. Add webhook: `http://服务器 IP:8081/github-webhook/`

### 6.5 构建参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `SKIP_BUILD` | 跳过构建，仅部署 | false |
| `CLEAN_WORKSPACE` | 清空工作区 | false |
| `BACKUP_BEFORE_DEPLOY` | 部署前备份 | true |
| `DEPLOY_TARGET` | 部署目标 | all |

---

## 7. 监控与维护

### 7.1 查看日志

```bash
# 所有服务日志
docker-compose logs -f

# 单个服务日志
docker-compose logs -f app
docker-compose logs -f nginx
docker-compose logs -f mysql
```

### 7.2 容器管理

```bash
# 查看容器状态
docker-compose ps

# 重启服务
docker-compose restart app

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build
```

### 7.3 数据库管理

```bash
# 进入 MySQL 容器
docker exec -it bikeshare-mysql mysql -uroot -p

# 导出数据库
docker exec bikeshare-mysql mysqldump -uroot -p密码 bickdemo > backup.sql

# 导入数据库
docker exec -i bikeshare-mysql mysql -uroot -p密码 bickdemo < backup.sql
```

### 7.4 MinIO 管理

访问 MinIO 控制台：`https://minio.bikeshare.online/console/`

默认账号密码从 `.env` 文件中获取。

---

## 8. 常见问题

### Q1: SSL 证书问题

**现象**: HTTPS 无法访问，浏览器显示证书错误

**解决**:
1. 检查证书文件是否存在：`ls nginx/ssl/`
2. 检查证书权限：`chmod 644 fullchain.pem`, `chmod 600 privkey.pem`
3. 检查域名解析：`ping bikeshare.online`

### Q2: 容器无法启动

**现象**: `docker-compose ps` 显示容器退出

**解决**:
```bash
# 查看具体错误
docker-compose logs <服务名>

# 检查环境变量
cat .env

# 重新启动
docker-compose down
docker-compose up -d
```

### Q3: 数据库连接失败

**现象**: 后端启动失败，提示无法连接 MySQL

**解决**:
```bash
# 检查 MySQL 是否就绪
docker exec bikeshare-mysql mysqladmin ping -uroot -p

# 等待 MySQL 健康检查通过
docker-compose ps mysql
```

### Q4: 内存不足

**现象**: 容器频繁重启

**解决**:
```bash
# 查看内存使用
docker stats

# 增加服务器内存或关闭部分服务
# 或调整 docker-compose.yml 中的内存限制
```

### Q5: 备份失败

**现象**: 备份脚本报错

**解决**:
```bash
# 检查备份目录权限
chmod 755 ./backups

# 手动执行备份查看详细错误
bash scripts/backup.sh
```

### Q6: Jenkins 构建失败

**现象**: 构建在某个阶段失败

**解决**:
1. 查看构建日志
2. 检查 Docker 权限：`docker exec bikeshare-jenkins docker ps`
3. 检查工作区空间：`df -h`
4. 检查环境变量配置

---

## 9. 升级指南

### 9.1 代码升级

```bash
# 拉取最新代码
cd /opt/bickdemo
git pull

# 重新构建部署
cd deploy-optimized
bash scripts/deploy.sh --backup --build
```

### 9.2 配置升级

修改配置后重启服务：

```bash
# 修改 docker-compose.yml 或 .env 后
docker-compose down
docker-compose up -d
```

### 9.3 证书续期

```bash
# Let's Encrypt 证书续期
certbot renew

# 重载 Nginx
docker-compose restart nginx
```

---

## 10. 安全建议

1. **定期更新密码**: 建议每 90 天更新一次
2. **启用防火墙**: 仅开放必要端口
3. **定期备份**: 确保数据安全
4. **监控日志**: 及时发现异常
5. **限制 Jenkins 访问**: 配置 IP 白名单
6. **使用强密码**: 至少 12 位，包含大小写、数字、特殊字符

---

## 附录 A: 快速命令参考

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启单个服务
docker-compose restart <服务名>

# 进入容器
docker exec -it <容器名> bash

# 备份数据
bash scripts/backup.sh

# 健康检查
curl https://bikeshare.online/health
```

---

## 附录 B: 目录结构

```
deploy-optimized/
├── docker-compose.yml          # 主配置文件
├── .env                        # 环境变量 (需手动创建)
├── .env.example                # 环境变量示例
├── nginx/
│   ├── nginx.conf              # Nginx 主配置
│   ├── conf.d/
│   │   ├── default.conf        # 用户端配置
│   │   ├── admin.conf          # 管理端配置
│   │   └── minio.conf          # MinIO 配置
│   └── ssl/                    # SSL 证书目录
├── scripts/
│   ├── backup.sh               # 备份脚本
│   ├── restore.sh              # 恢复脚本
│   └── deploy.sh               # 部署脚本
├── jenkins/
│   ├── docker-compose.yml      # Jenkins 配置
│   └── Jenkinsfile             # 流水线配置
├── volumes/                    # 数据持久化目录
│   ├── mysql/
│   ├── redis/
│   ├── rabbitmq/
│   └── minio/
├── backups/                    # 备份目录
│   └── auto/
└── logs/                       # 日志目录
```
