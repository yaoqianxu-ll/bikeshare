# Bickdemo Docker 部署指南

## 项目说明

这是一个自行车租赁系统，包含：
- **前端**: Vue 3 + Element Plus + Vite
- **后端**: Spring Boot 3.2.0 + Java 17
- **数据库**: MySQL 8.0 (Docker)
- **缓存**: Redis (现有)
- **对象存储**: MinIO (现有)

## 架构说明

```
                    ┌──────────────────────────────────────────┐
                    │         60.205.169.251                   │
                    │                                          │
┌─────────────┐     │   ┌──────────────┐                       │
│   用户浏览器  │─────┼──▶│  frontend:80 │                       │
│             │     │   │   (Nginx)    │                       │
└─────────────┘     │   └──────┬───────┘                       │
                    │          │                                 │
                    │          ▼                                 │
                    │   ┌──────────────┐     ┌───────────────┐   │
                    │   │  app:8080    │────▶│  Redis        │   │
                    │   │(Spring Boot) │     │(现有 Docker)  │   │
                    │   └──────┬───────┘     └───────────────┘   │
                    │          │                                 │
                    │          ▼                                 │
                    │   ┌──────────────┐     ┌───────────────┐   │
                    │   │  mysql:3306  │     │  MinIO        │   │
                    │   │ (Docker 新建) │    │(现有 Docker)  │   │
                    │   └──────────────┘     └───────────────┘   │
                    └──────────────────────────────────────────┘
```

## 服务端口

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|----------|------------|------|
| frontend | 80 | 80 | Nginx 前端服务 |
| app | 8080 | - | Spring Boot 后端（内网） |
| mysql | 3306 | 3306 | MySQL 数据库 |

## 完整部署流程

### 第一步：上传项目到服务器

在本地 Windows 上执行（PowerShell 或 Git Bash）：

```bash
# 方式 1: 使用 scp 上传
cd C:\VsProjact\bickdemo
scp -r . root@60.205.169.251:/opt/bickdemo/

# 方式 2: 使用 Xftp/WinSCP 等工具
# 将整个 bickdemo 文件夹拖拽到服务器的 /opt/ 目录下
```

### 第二步：登录服务器

```bash
ssh root@60.205.169.251
```

### 第三步：进入项目目录

```bash
cd /opt/bickdemo
```

### 第四步：构建并启动所有服务

```bash
# 1. 构建所有 Docker 镜像（首次需要，耗时较长）
docker-compose build

# 2. 启动所有服务
docker-compose up -d

# 3. 查看构建和启动日志
docker-compose logs -f
```

### 第五步：验证部署

```bash
# 查看所有容器状态
docker-compose ps

# 应该看到 3 个容器都在运行 (Up 状态)：
# bickdemo-app
# bickdemo-frontend
# bickdemo-mysql

# 查看各个服务日志
docker-compose logs app       # 后端日志
docker-compose logs frontend  # 前端日志
docker-compose logs mysql     # MySQL 日志

# 测试服务
curl http://localhost                    # 测试前端
curl http://localhost:8080/actuator/health  # 测试后端
```

## 访问地址

- **前端页面**: http://60.205.169.251
- **后端 API**: http://60.205.169.251/api/xxx
- **MySQL**: 60.205.169.251:3306

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | user | user123 |

## 常用运维命令

```bash
# 进入项目目录
cd /opt/bickdemo

# 查看服务状态
docker-compose ps

# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart app
docker-compose restart frontend
docker-compose restart mysql

# 查看实时日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f app
docker-compose logs -f frontend

# 进入容器内部
docker-compose exec app sh
docker-compose exec frontend sh
docker-compose exec mysql bash

# 重新构建并部署（代码更新后）
docker-compose up -d --build

# 停止并删除数据卷（⚠️ 会删除 MySQL 数据）
docker-compose down -v
```

## 环境变量配置

可以在 `docker-compose.yml` 中修改以下环境变量：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| MYSQL_ROOT_PASSWORD | root123456 | MySQL root 密码 |
| REDIS_HOST | 60.205.169.251 | Redis 主机地址 |
| REDIS_PASSWORD | yaoqianxuL | Redis 密码 |
| MINIO_ENDPOINT | http://60.205.169.251:9000 | MinIO 地址 |
| MINIO_ACCESS_KEY | tTrQL3XQCic9Dc93jbQ7 | MinIO 访问密钥 |
| MINIO_SECRET_KEY | AtFjrLwasDoEgr4yZFAgwQqRzqGXrDndFHHQLL7f | MinIO 密钥 |

## 数据持久化

- **MySQL**: 数据存储在 Docker volume `mysql-data` 中
- **应用日志**: 挂载到 `./logs` 目录
- **数据库初始化**: `./bickdemo-backend/init-db/init.sql`（首次启动时自动执行）

## 故障排查

### 应用启动失败

```bash
# 查看详细日志
docker-compose logs app

# 检查 MySQL 是否就绪
docker-compose logs mysql

# 检查容器状态
docker-compose ps
```

### 前端无法访问后端

1. 确认后端服务已启动：`docker-compose ps app`
2. 检查网络连接：`docker-compose exec frontend ping bickdemo-app`
3. 检查 Nginx 配置：`docker-compose exec frontend cat /etc/nginx/conf.d/default.conf`

### 无法连接 Redis/MinIO

1. 确认 Redis/MinIO 容器已启动
2. 检查密码配置是否正确
3. 检查网络连通性：`telnet 60.205.169.251 6379`

### MySQL 数据问题

```bash
# 进入 MySQL 容器
docker-compose exec mysql bash

# 登录 MySQL
mysql -uroot -proot123456

# 查看数据库
SHOW DATABASES;
USE bickdemo;
SHOW TABLES;
```

## 生产环境建议

1. **修改默认密码**: 修改 MySQL、Redis、MinIO 的默认密码
2. **配置 HTTPS**: 使用 Nginx 反向代理并配置 SSL 证书
3. **备份数据**: 定期备份 MySQL 数据卷
4. **监控日志**: 配置日志轮转，避免磁盘空间不足
