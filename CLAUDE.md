# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

自行车租赁系统 (BikeShare) - 基于 **Spring Boot 3.2.0 + Vue 3.4.0** 的全栈应用

## 技术栈

**后端:**
- Spring Boot 3.2.0, Spring Security 6.x, JWT
- MyBatis-Plus 3.5.5, MySQL 8.0, HikariCP
- MinIO (对象存储)

**前端:**
- Vue 3.4.0, Vite 5.0.8
- Element Plus 2.5.0, Pinia 2.1.7, Vue Router 4.2.5
- Axios 1.6.2, ECharts 6.0.0

## 目录结构

```
bickdemo/
├── bickdemo-backend/          # Spring Boot 后端
│   ├── src/main/java/.../
│   │   ├── annotation/        # 自定义注解 (如权限控制)
│   │   ├── aspect/            # AOP 切面 (如 IP 限流)
│   │   ├── component/         # 初始化组件
│   │   ├── config/            # 安全/JWT/MinIO 配置
│   │   ├── controller/        # REST API
│   │   ├── dto/               # 数据传输对象
│   │   ├── entity/            # JPA 实体
│   │   ├── exception/         # 异常处理
│   │   ├── mapper/            # MyBatis Mapper
│   │   ├── service/           # 业务逻辑
│   │   └── util/              # 工具类 (JwtUtil, RedisUtil, MinioUtil)
│   ├── src/main/resources/
│   │   ├── application.yml    # 基础配置
│   │   └── application-prod.yml # 生产环境配置
│   ├── init-db/               # Docker 初始化 SQL
│   ├── Dockerfile
│   └── pom.xml
├── bickdemo-frontend/         # Vue 3 用户端前端
│   ├── src/
│   │   ├── api/               # API 封装
│   │   ├── components/        # 公共组件
│   │   ├── router/            # 路由配置
│   │   ├── stores/            # Pinia 状态管理
│   │   └── views/             # 页面组件
│   ├── nginx.conf
│   ├── Dockerfile
│   └── package.json
├── bickdemo-admin/            # Vue 3 管理端前端
│   ├── src/
│   │   ├── api/
│   │   ├── layouts/           # 布局组件
│   │   ├── router/
│   │   ├── stores/
│   │   └── views/             # 管理页面
│   ├── nginx.conf
│   └── package.json
├── script/
│   ├── dev/                   # 开发环境脚本
│   └── prod/                  # 生产环境脚本
│       ├── docker-compose.yml
│       └── deploy/            # 部署脚本
└── sql/
    └── init.sql               # 数据库初始化脚本
```

## 常用命令

### 后端命令

```bash
cd bickdemo-backend

# 开发模式启动
mvn spring-boot:run

# 打包
mvn clean package -DskipTests

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=ClassNameTest

# 查看依赖树
mvn dependency:tree

# 代码格式化
mvn spotless:apply
```

### 前端命令

```bash
cd bickdemo-frontend

# 安装依赖 (使用淘宝镜像)
npm config set registry https://registry.npmmirror.com
npm install

# 开发模式
npm run dev        # http://localhost:5173

# 生产构建
npm run build

# 预览构建
npm run preview
```

### 管理端命令

```bash
cd bickdemo-admin

# 安装依赖
npm install

# 开发模式
npm run dev        # http://localhost:5174
```

### Docker 命令

```bash
# 一键部署
docker compose -f script/prod/docker-compose.yml up -d --build

# 查看日志
docker compose -f script/prod/docker-compose.yml logs -f app
docker compose -f script/prod/docker-compose.yml logs -f frontend

# 重新构建单个服务
docker compose -f script/prod/docker-compose.yml build --no-cache frontend

# 停止服务
docker compose -f script/prod/docker-compose.yml down -v
```

### Windows/PowerShell 提示

- 如果 `rg` (ripgrep) 被阻止，使用 PowerShell 替代：`Get-ChildItem -Recurse -File | Select-String -Pattern "..."`
- 如果中文显示乱码：`Get-Content -Encoding UTF8 path/to/file`

## 核心架构

### 后端分层

```
Controller → Service → Mapper
    ↓
  DTO/Entity
    ↓
  Annotation/Aspect (AOP)
```

- **Controller**: REST API 端点，统一返回格式 `{code, message, data}`
- **Service**: 业务逻辑，事务管理
- **Mapper**: MyBatis-Plus 数据访问
- **Entity**: JPA 实体，支持逻辑删除 (`deleted` 字段)
- **DTO**: 数据传输对象，用于请求/响应
- **Annotation**: 自定义注解 (权限控制等)
- **Aspect**: AOP 切面 (IP 限流等)

### API 基础路由

| 路由 | 说明 |
|------|------|
| `/api/auth` | 认证相关 (登录/注册/找回密码) |
| `/api/bicycles` | 自行车相关 |
| `/api/rentals` | 租赁订单相关 |
| `/api/statistics` | 统计数据 |
| `/api/files` | 文件上传 |
| `/api/backgrounds` | 背景图管理 |

### 认证流程

1. 用户登录 → `/api/auth/login` → 返回 JWT Token
2. 前端存储 Token 到 Pinia
3. 请求拦截器自动添加 `Authorization: Bearer {token}`
4. 后端 `JwtAuthenticationFilter` 验证 Token
5. Spring Security 基于角色授权 (USER/ADMIN)

### API 请求封装

前端使用 Axios 统一封装：
- 基础路径：`/api`
- 请求拦截：自动添加 JWT Token
- 响应拦截：统一错误处理，401 自动跳转登录

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 数据库配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:bickdemo}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:change-me-db-password}
```

### 数据库迁移

当项目升级或拉取新代码后，如遇到字段或表不存在的问题，需要执行对应的迁移脚本。

```bash
# 全量初始化
mysql -u root -p bickdemo < sql/init.sql
```

### 数据库迁移 (常见字段补充)

如果遇到表结构缺失，可执行以下 SQL 补充：

```sql
-- 自行车库存字段
ALTER TABLE bicycles ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量（库存）';

-- 租赁数量字段
ALTER TABLE rentals ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '租赁数量';
```

## Docker 部署配置

### 服务组成

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | bickdemo-mysql | 3306 | 数据库 |
| Backend | bickdemo-app | 8080 | Spring Boot |
| Frontend | bickdemo-frontend | 80 | Nginx |

### 环境变量

后端依赖的环境变量：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | localhost | MySQL 主机 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_USERNAME` | root | MySQL 用户名 |
| `MYSQL_PASSWORD` | change-me-db-password | MySQL 密码 |
| `JWT_SECRET` | change-me-jwt-secret... | JWT 签名密钥 |
| `MINIO_ENDPOINT` | http://localhost:9000 | MinIO 端点 |
| `MINIO_ACCESS_KEY` | change-me-minio-access-key | MinIO 访问密钥 |
| `MINIO_SECRET_KEY` | change-me-minio-secret-key | MinIO 密钥 |
| `MINIO_BUCKET` | bicycles | MinIO 存储桶 |
| `REDIS_HOST` | localhost | Redis 主机 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `RABBITMQ_HOST` | localhost | RabbitMQ 主机 |
| `RABBITMQ_USERNAME` | guest | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | guest | RabbitMQ 密码 |
| `MAIL_HOST` | smtp.qq.com | 邮件服务器 |
| `MAIL_USERNAME` | (空) | 发件人邮箱 |
| `MAIL_PASSWORD` | (空) | SMTP 授权码 |

**安全建议**：生产环境部署前，务必将默认密码和密钥修改为强密码，并使用环境变量或密钥管理系统管理敏感配置。

## Nginx 配置要点

```nginx
location /api {
    proxy_pass http://bickdemo-app:8080;
    client_max_body_size 50M;  # 上传大小限制
}
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 用户 | user | user123 |
