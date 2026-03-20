# 优化部署方案

> **完整部署指南**: 查看 [DEPLOY.md](./DEPLOY.md)

## 目录

- [1. 部署架构](#1-部署架构)
- [2. 快速开始](#2-快速开始)
- [3. 目录结构](#3-目录结构)
- [4. 配置说明](#4-配置说明)
- [5. 数据备份](#5-数据备份)
- [6. Jenkins 部署](#6-jenkins 部署)
- [7. 常见问题](#7-常见问题)

---

## 1. 部署架构

### 1.1 服务组成

| 服务 | 域名/端口 | 说明 |
|------|----------|------|
| 用户端 | `https://bikeshare.online` | Vue3 用户前端 |
| 管理端 | `https://admin.bikeshare.online` | Vue3 管理前端 |
| MinIO | `https://minio.bikeshare.online` | 对象存储服务 |
| 后端 API | `https://bikeshare.online/api` | Spring Boot 后端 |
| Jenkins | `http://服务器 IP:8081` | CI/CD 构建服务 |

### 1.2 架构图

```
                         ┌─────────────────────────────────────┐
                         │         Nginx (反向代理)             │
                         │  端口：80, 443 (SSL 终止)              │
                         └─────────────────────────────────────┐
                               │              │              │
                ┌──────────────┘              │              └──────────────┐
                ▼                             ▼                             ▼
    ┌───────────────────────┐     ┌───────────────────────┐     ┌───────────────────────┐
    │   用户端 (80)         │     │   管理端 (81)         │     │   MinIO (9000)        │
    │   bikeshare.online    │     │   admin.bikeshare...  │     │   minio.bikeshare...  │
    └───────────────────────┘     └───────────────────────┘     └───────────────────────┘
                │                             │                             │
                └─────────────────────────────┼─────────────────────────────┘
                                              ▼
                                    ┌───────────────────────┐
                                    │   Spring Boot (8080)  │
                                    │   后端 API 服务          │
                                    └───────────────────────┘
                                              │
                ┌─────────────────────────────┼─────────────────────────────┐
                ▼                             ▼                             ▼
    ┌───────────────────────┐     ┌───────────────────────┐     ┌───────────────────────┐
    │   MySQL (3306)        │     │   Redis (6379)        │     │   RabbitMQ (5672)     │
    │   数据库               │     │   缓存                │     │   消息队列             │
    └───────────────────────┘     └───────────────────────┘     └───────────────────────┘
```

### 1.3 与原部署方案的区别

| 特性 | 原方案 | 优化方案 |
|------|--------|----------|
| 管理端访问 | `http://IP:3001` | `https://admin.bikeshare.online` |
| MinIO 访问 | `http://localhost:9000` | `https://minio.bikeshare.online` |
| SSL 配置 | 单域名 | 通配符证书/多域名 |
| 数据备份 | 基础卷持久化 | 定时备份 + 异地备份 |
| Jenkins 集成 | 基础配置 | 优化构建流程 + 自动备份 |

---

## 2. 快速开始

### 2.1 前提条件

- Docker 20.10+ 和 Docker Compose 2.0+
- 服务器端口开放：80, 443, 8081
- SSL 证书 (通配符证书或多域名证书)
- 至少 4GB 可用内存

### 2.2 部署步骤

#### 步骤 1: 准备环境

```bash
# 1. 克隆/进入项目目录
cd /opt/bickdemo

# 2. 复制环境变量文件
cp deploy-optimized/.env.example deploy-optimized/.env

# 3. 编辑环境变量 (修改密码和密钥)
vi deploy-optimized/.env
```

#### 步骤 2: 配置 SSL 证书

**使用现有证书（推荐）：**

```bash
# 复制现有证书到优化方案
cp ../script/prod/ssl/bikeshare.online_nginx/* nginx/ssl/

# 证书文件：
# - bikeshare.online_bundle.crt  # 证书链
# - bikeshare.online.key         # 私钥
```

#### 步骤 3: 配置 Jenkins 自动部署

**使用现有 Jenkins（推荐）：**

```bash
# 1. 访问现有 Jenkins Web 界面
# 2. 新建任务 -> 流水线 -> 脚本路径：deploy-optimized/jenkins/Jenkinsfile
# 3. 点击"立即构建"，Jenkins 会自动执行 docker-compose up -d
```

**手动部署（仅首次测试）：**

```bash
# 进入部署目录
cd deploy-optimized

# 启动所有服务
docker-compose up -d
```

#### 步骤 4: 验证部署

```bash
# 检查服务状态
curl -I https://bikeshare.online
curl -I https://admin.bikeshare.online
curl -I https://minio.bikeshare.online

# 检查后端 API
curl https://bikeshare.online/api/health
```

---

## 3. 目录结构

```
deploy-optimized/
├── docker-compose.yml          # 主部署配置文件
├── docker-compose.backup.yml   # 备份服务配置
├── nginx/
│   ├── nginx.conf              # Nginx 主配置
│   ├── conf.d/
│   │   ├── default.conf        # 用户端配置
│   │   ├── admin.conf          # 管理端配置
│   │   └── minio.conf          # MinIO 配置
│   └── ssl/
│       ├── fullchain.pem       # SSL 证书链
│       └── privkey.pem         # SSL 私钥
├── scripts/
│   ├── backup.sh               # 数据备份脚本
│   ├── restore.sh              # 数据恢复脚本
│   └── deploy.sh               # 部署脚本
├── jenkins/
│   ├── Jenkinsfile             # Jenkins 流水线配置
│   └── docker-compose.yml      # Jenkins 配置
├── .env                        # 环境变量 (需手动创建)
├── .env.example                # 环境变量示例
└── README.md                   # 本文件
```

---

## 4. 配置说明

### 4.1 环境变量

复制 `.env.example` 为 `.env` 并修改以下配置：

```ini
# MySQL 配置
MYSQL_ROOT_PASSWORD=你的强密码

# MinIO 配置
MINIO_ROOT_USER=你的管理员账号
MINIO_ROOT_PASSWORD=你的强密码

# JWT 配置
JWT_SECRET=你的 JWT 密钥 (至少 32 字符)

# 邮件配置
MAIL_USERNAME=你的邮箱
MAIL_PASSWORD=SMTP 授权码

# 服务器配置
SERVER_HOST=bikeshare.online
```

### 4.2 Nginx 配置

Nginx 配置支持三个子域名：

- `bikeshare.online` → 用户端 (端口 80)
- `admin.bikeshare.online` → 管理端 (端口 81)
- `minio.bikeshare.online` → MinIO (端口 9000)

所有流量通过 HTTPS (443 端口) 加密。

---

## 5. 数据备份

### 5.1 自动备份

```bash
# 启用定时备份 (每天凌晨 3 点)
crontab -e

# 添加以下行
0 3 * * * /opt/bickdemo/deploy-optimized/scripts/backup.sh
```

### 5.2 手动备份

```bash
# 备份所有数据
./scripts/backup.sh

# 备份 MySQL 数据库
./scripts/backup.sh --database-only

# 备份 MinIO 数据
./scripts/backup.sh --minio-only
```

### 5.3 数据恢复

```bash
# 从备份恢复
./scripts/restore.sh /path/to/backup/backup-2024-01-01.tar.gz
```

---

## 6. Jenkins 部署

### 6.1 启动 Jenkins

```bash
# 进入 Jenkins 目录
cd deploy-optimized/jenkins

# 启动 Jenkins
docker-compose up -d

# 获取初始管理员密码
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 6.2 配置 Jenkins

1. 访问 `http://服务器 IP:8081`
2. 输入初始管理员密码
3. 安装推荐插件
4. 创建管理员账户

### 6.3 配置 Jenkins 自动部署

**使用优化方案的 Jenkins：**

```bash
# 1. 进入 Jenkins 目录
cd deploy-optimized/jenkins

# 2. 复制并配置环境变量
cp .env.example .env
vi .env  # 修改 Git 仓库信息

# 3. 启动 Jenkins
docker-compose up -d

# 4. 访问 Jenkins: http://服务器 IP:8081
# 5. 新建任务 -> 流水线 -> 脚本路径：deploy-optimized/jenkins/Jenkinsfile
# 6. 点击"立即构建"
```

---

## 7. 常见问题

### Q1: SSL 证书配置问题

确保上传的 SSL 证书覆盖所有子域名，或使用通配符证书 (`*.bikeshare.online`)。

### Q2: 端口冲突

如果端口被占用，修改 `docker-compose.yml` 中的端口映射。

### Q3: 数据库连接失败

检查环境变量中的 MySQL 配置，确保容器网络正常。

### Q4: MinIO 无法访问

确保 `minio.bikeshare.online` 域名已正确解析到服务器。
