# 部署配置说明

## 命名规范

本项目统一使用 `bikeshare` 作为项目标识名称。

### 容器命名

| 容器名 | 服务 | 说明 |
|--------|------|------|
| `bikeshare-mysql` | MySQL 8.0 | 数据库 |
| `bikeshare-redis` | Redis 7 | 缓存 |
| `bikeshare-rabbitmq` | RabbitMQ 3 | 消息队列 |
| `bikeshare-minio` | MinIO | 对象存储 |
| `bikeshare-app` | Spring Boot | 后端应用 |
| `bikeshare-nginx` | Nginx | 反向代理 |
| `bikeshare-frontend` | Vue 3 | 用户端前端 |
| `bikeshare-admin` | Vue 3 | 管理端前端 |
| `bikeshare-backup` | MySQL | 自动备份服务 |
| `bikeshare-jenkins` | Jenkins | CI/CD 服务 |

### 网络命名

- `bikeshare-network` - 主服务网络
- `jenkins-network` - Jenkins 网络

### 数据卷命名

| 数据卷 | 挂载点 | 说明 |
|--------|--------|------|
| `bikeshare-jenkins-jenkins-data` | `/var/jenkins_home` | Jenkins 主目录 |
| `bikeshare-jenkins-maven-repo` | `/root/.m2` | Maven 仓库缓存 |
| `bikeshare-jenkins-node-cache` | `/root/.npm` | Node.js 缓存 |

### 数据库命名

- 数据库名：`bikeshare`
- Redis Key 前缀：`bikeshare:prod:`

### 域名规划

| 域名 | 服务 | SSL |
|------|------|-----|
| `bikeshare.online` | 用户端 | ✅ |
| `admin.bikeshare.online` | 管理端 | ✅ |
| `minio.bikeshare.online` | MinIO 控制台 | ✅ |

---

## 环境变量配置

### 核心配置

```ini
# 数据库配置
MYSQL_ROOT_PASSWORD=强密码
MYSQL_DATABASE=bikeshare
MYSQL_USERNAME=root
MYSQL_PASSWORD=强密码

# Redis 配置
REDIS_PASSWORD=强密码
APP_REDIS_KEY_PREFIX=bikeshare:prod:

# MinIO 配置
MINIO_ROOT_USER=bikeshare-admin
MINIO_ROOT_PASSWORD=强密码
MINIO_BUCKET=bicycles

# JWT 配置
JWT_SECRET=至少 32 字符的随机字符串
JWT_PREVIOUS_SECRETS=

# 邮件配置
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=SMTP 授权码
MAIL_FROM_NAME=BikeShare
```

### 生成 JWT 密钥

```bash
# 方法 1: 使用 openssl
openssl rand -base64 32

# 方法 2: 使用 head 和 md5sum
head -c 32 /dev/urandom | md5sum

# 方法 3: 使用 python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## 目录结构

```
deploy-optimized/
├── docker-compose.yml          # 主配置文件
├── .env.example                # 环境变量示例
├── .env                        # 环境变量 (手动创建)
├── nginx/
│   ├── nginx.conf              # Nginx 主配置
│   └── conf.d/
│       ├── default.conf        # 用户端配置
│       ├── admin.conf          # 管理端配置
│       └── minio.conf          # MinIO 配置
├── scripts/
│   ├── backup.sh               # 备份脚本
│   ├── restore.sh              # 恢复脚本
│   └── deploy.sh               # 部署脚本
├── jenkins/
│   ├── docker-compose.yml      # Jenkins 配置
│   ├── Jenkinsfile             # 流水线配置
│   └── README.md               # Jenkins 使用指南
├── volumes/                    # 数据持久化
│   ├── mysql/
│   ├── redis/
│   ├── rabbitmq/
│   └── minio/
├── backups/                    # 备份数据
│   └── auto/
└── logs/                       # 日志文件
```

---

## 快速部署

### 1. 准备环境

```bash
cd /opt/bickdemo/deploy-optimized

# 复制环境变量
cp .env.example .env

# 编辑配置
vi .env
```

### 2. 配置 SSL 证书

**使用现有证书（推荐）：**

```bash
# 直接复制现有证书
cp ../script/prod/ssl/bikeshare.online_nginx/* nginx/ssl/

# 证书文件说明：
# - bikeshare.online_bundle.crt  # 证书链
# - bikeshare.online.key         # 私钥
```

### 3. 配置 Jenkins 自动部署（推荐）

```bash
# 在现有 Jenkins 中创建新任务
# 1. 新建任务 -> 流水线
# 2. 源码管理：Git (你的 Gitea 仓库)
# 3. 脚本路径：deploy-optimized/jenkins/Jenkinsfile
# 4. 点击"立即构建"
```

**或者手动部署（仅首次测试）：**

```bash
bash scripts/deploy.sh --backup --build
```

### 4. 验证部署

```bash
# 查看状态
docker-compose ps

# 测试访问
curl -I https://bikeshare.online
curl -I https://admin.bikeshare.online
curl -I https://minio.bikeshare.online
```

---

## 常用命令

```bash
# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart app

# 停止服务
docker-compose down

# 备份数据
bash scripts/backup.sh

# 进入 MySQL 容器
docker exec -it bikeshare-mysql bash

# 进入后端容器
docker exec -it bikeshare-app bash
```

---

## 注意事项

1. **密码安全**: 所有默认密码必须修改为强密码
2. **SSL 证书**: 必须覆盖所有子域名或使用通配符证书
3. **定期备份**: 建议配置定时备份任务
4. **资源限制**: 每个容器都有内存限制，根据服务器配置调整
5. **健康检查**: 所有关键服务都配置了健康检查

---

## 版本信息

| 组件 | 版本 |
|------|------|
| MySQL | 8.0 |
| Redis | 7 (Alpine) |
| RabbitMQ | 3 (Management) |
| MinIO | Latest |
| Nginx | Alpine |
| Jenkins | LTS (JDK 17) |
