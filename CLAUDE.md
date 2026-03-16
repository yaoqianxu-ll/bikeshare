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
│   │   ├── component/         # 初始化组件
│   │   ├── config/            # 安全/JWT/MinIO 配置
│   │   ├── controller/        # REST API
│   │   ├── dto/               # 数据传输对象
│   │   ├── entity/            # JPA 实体
│   │   ├── exception/         # 异常处理
│   │   ├── mapper/            # MyBatis Mapper
│   │   └── service/           # 业务逻辑
│   ├── src/main/resources/
│   │   ├── application.yml    # 基础配置
│   │   └── application-prod.yml # 生产环境配置
│   ├── Dockerfile
│   └── pom.xml
└── bickdemo-frontend/         # Vue 3 前端
    ├── src/
    │   ├── api/               # API 封装
    │   ├── components/        # 公共组件
    │   ├── router/            # 路由配置
    │   ├── stores/            # Pinia 状态管理
    │   ├── views/             # 页面组件
    │   ├── App.vue
    │   └── main.js
    ├── nginx.conf             # Nginx 配置
    ├── Dockerfile
    └── package.json
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

## 核心架构

### 后端分层

```
Controller → Service → Mapper
    ↓
  DTO/Entity
```

- **Controller**: REST API 端点，统一返回格式 `{code, message, data}`
- **Service**: 业务逻辑，事务管理
- **Mapper**: MyBatis-Plus 数据访问
- **Entity**: JPA 实体，支持逻辑删除
- **DTO**: 数据传输对象，用于请求/响应

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

## 数据库配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:mysql}:3306/bickdemo
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:Lile200623}
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
```yaml
MYSQL_HOST, MYSQL_PORT, MYSQL_USERNAME, MYSQL_PASSWORD
MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET
JWT_SECRET
```

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
