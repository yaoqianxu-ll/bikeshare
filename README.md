# BikeShare 自行车租赁系统

> 基于 Spring Boot 3 + Vue 3 的前后端分离单车租赁平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4.0-4FC08D?logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.5.0-409EFF?logo=element)](https://element-plus.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

## 项目简介

BikeShare 是一个完整的自行车租赁管理系统，涵盖用户租车、后台管理、社交互动、论坛交流等核心功能。采用前后端分离架构，支持 Docker 一键部署。

### 核心功能

| 模块 | 功能描述 |
|------|----------|
| 👤 用户系统 | 邮箱验证码注册/登录、个人资料管理、头像上传、密码找回 |
| 🚲 租车服务 | 车辆浏览/筛选、租赁/归还、租赁记录查询、价格计算 |
| 💬 社交聊天 | 好友搜索、好友申请、实时私信（WebSocket + RabbitMQ） |
| 📝 论坛社区 | 发帖/评论、多图上传、点赞收藏、帖子审核、作者资料展示 |
| 📊 数据统计 | 租赁统计、用户分析、图表可视化（ECharts） |
| 🔧 后台管理 | 车辆管理、库存管理、租赁记录、论坛审核、背景图管理 |

### 功能特性详解

**用户端功能：**
- ✅ 邮箱验证码注册与登录
- ✅  JWT 令牌自动刷新与无感登录
- ✅  自行车实时库存显示
- ✅  租赁订单状态追踪
- ✅  个人头像裁剪上传
- ✅  好友关系链管理
- ✅  实时聊天消息推送
- ✅  论坛图文混排发帖

**管理端功能：**
- ✅  数据看板（今日订单、活跃用户、营收统计）
- ✅  自行车批量导入/导出
- ✅  租赁记录分页查询与导出
- ✅  论坛内容审核（通过/拒绝/删除）
- ✅  背景轮播图配置
- ✅  用户数据可视化分析

## 技术架构

```
┌─────────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────────┐
│      前端展示层          │     │      后端服务层          │     │      数据存储层          │
│  Vue 3 + Vite 5         │ ──► │  Spring Boot 3.2        │ ──► │  MySQL 8.0 (主库)       │
│  Element Plus 2.5       │ ◄── │  Spring Security 6      │ ◄── │  Redis 6.x (缓存)       │
│  Pinia + ECharts 6      │     │  MyBatis-Plus 3.5       │     │  RabbitMQ (消息队列)    │
└─────────────────────────┘     └─────────────────────────┘     └─────────────────────────┘
                                         │
                                         ▼
                               ┌─────────────────────────┐
                               │   外部服务               │
                               │   MinIO (对象存储)       │
                               │   SMTP (邮件服务)        │
                               └─────────────────────────┘
```

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.x | 安全认证与授权 |
| JWT | - | 无状态 Token 鉴权 |
| MyBatis-Plus | 3.5.5 | ORM 框架 / 分页插件 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 6.x | 缓存/验证码/会话存储 |
| RabbitMQ | - | 异步消息队列/聊天事件分发 |
| WebSocket | - | 实时双向通信 |
| MinIO | - | 分布式对象存储 |
| Spring Mail | - | 邮件发送服务 |
| HikariCP | - | 高性能数据库连接池 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.0 | 渐进式前端框架 |
| Vue Router | 4.2.5 | 单页路由管理 |
| Pinia | 2.1.7 | 轻量级状态管理 |
| Element Plus | 2.5.0 | 企业级 UI 组件库 |
| Axios | 1.6.2 | HTTP 请求封装 |
| ECharts | 6.0.0 | 数据可视化图表 |
| Vite | 5.0.8 | 下一代构建工具 |

## 快速开始

### 环境要求

| 软件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| JDK | 17 | 17+ |
| Maven | 3.8 | 3.9+ |
| Node.js | 16 | 18+ LTS |
| npm | 8 | 9+ |
| MySQL | 8.0 | 8.0+ |
| Redis | 5.0 | 6.x |

> 💡 **提示**：推荐使用 JDK 17（LTS 版本），Node.js 18 LTS 可获得最佳兼容性。

### 方式一：本地开发（推荐用于二次开发）

#### 1. 克隆项目

```bash
git clone <repository-url>
cd bickdemo
```

#### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bickdemo
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;"

# 导入初始数据（包含基础表结构和测试数据）
mysql -u root -p bickdemo < sql/init.sql
```

#### 3. 配置后端

编辑配置文件 `bickdemo-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bickdemo?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

# MinIO 配置（图片上传功能需要）
minio:
  endpoint: http://localhost:9000
  access-key: your_access_key
  secret-key: your_secret_key
  bucket: bickdemo

# 邮箱配置（验证码功能需要）
spring:
  mail:
    host: smtp.qq.com
    username: your_email@qq.com
    password: your_smtp_auth_code
```

> ⚠️ **注意**：如果仅需测试基础租车功能，可先只配置 MySQL，Redis/MinIO/邮箱服务可后续补充。

#### 4. 启动后端

```bash
cd bickdemo-backend

# 方式 1：Maven 直接运行
mvn spring-boot:run

# 方式 2：打包后运行
mvn clean package -DskipTests
java -jar target/bickdemo-0.0.1-SNAPSHOT.jar
```

启动成功后，访问后端地址：http://localhost:8080

验证接口：
```bash
curl http://localhost:8080/api/health
```

#### 5. 启动前端

```bash
cd bickdemo-frontend

# 安装依赖（建议使用淘宝镜像）
npm config set registry https://registry.npmmirror.com
npm install

# 启动开发服务器（自动热重载）
npm run dev
```

启动成功后，访问前端地址：http://localhost:5173

> 💡 **提示**：开发模式下，前端会自动将 `/api` 请求代理到后端 `http://localhost:8080`。

### 方式二：Docker 部署（推荐用于生产环境）

#### 1. 前置要求

- Docker 20.10+
- Docker Compose 2.0+

#### 2. 配置文件

复制环境变量文件：
```bash
cp .env.example .env
```

编辑 `.env` 文件，配置你的环境变量：
```bash
MYSQL_ROOT_PASSWORD=your_strong_password
JWT_SECRET=your_jwt_secret_key
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
```

#### 3. 一键启动

```bash
# 构建并启动所有服务
docker compose -f script/prod/docker-compose.yml up -d --build

# 查看启动日志
docker compose -f script/prod/docker-compose.yml logs -f

# 查看单个服务日志
docker compose -f script/prod/docker-compose.yml logs -f app
docker compose -f script/prod/docker-compose.yml logs -f frontend
```

#### 4. 服务访问

| 服务 | 访问地址 | 说明 |
|------|----------|------|
| 前端页面 | http://localhost | Nginx 反向代理 |
| 后端 API | http://localhost:8080 | Spring Boot |
| MySQL | localhost:3306 | 数据库（建议不暴露外网） |
| MinIO | http://localhost:9001 | 对象存储控制台 |

#### 5. 停止与清理

```bash
# 停止所有服务
docker compose -f script/prod/docker-compose.yml down

# 停止并删除数据卷（谨慎使用）
docker compose -f script/prod/docker-compose.yml down -v
```

### 默认登录账号

| 角色 | 用户名 | 密码 | 权限说明 |
|------|--------|------|----------|
| 管理员 | admin | admin123 | 全部权限（车辆管理、审核、统计） |
| 普通用户 | user | user123 | 用户权限（租车、聊天、发帖） |

> ⚠️ **安全提醒**：首次部署后请立即修改默认密码！

## 项目结构详解

```
bickdemo/
│
├── bickdemo-backend/                    # Spring Boot 后端模块
│   ├── src/main/java/com/example/bickdemo/
│   │   ├── BickdemoApplication.java     # 启动类
│   │   ├── config/                      # 配置类
│   │   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   │   ├── JwtConfig.java           # JWT 相关配置
│   │   │   ├── WebSocketConfig.java     # WebSocket 配置
│   │   │   ├── RedisConfig.java         # Redis 配置
│   │   │   ├── MinioConfig.java         # MinIO 配置
│   │   │   └── CorsConfig.java          # 跨域配置
│   │   ├── controller/                  # REST API 控制器
│   │   │   ├── AuthController.java      # 认证接口（登录/注册）
│   │   │   ├── BicycleController.java   # 自行车接口
│   │   │   ├── RentalController.java    # 租赁接口
│   │   │   ├── UserController.java      # 用户接口
│   │   │   ├── FriendController.java    # 好友接口
│   │   │   ├── ChatController.java      # 聊天接口
│   │   │   ├── ForumController.java     # 论坛接口
│   │   │   └── AdminController.java     # 管理接口
│   │   ├── service/                     # 业务逻辑层
│   │   │   ├── AuthService.java         # 认证服务
│   │   │   ├── BicycleService.java      # 自行车服务
│   │   │   ├── RentalService.java       # 租赁服务
│   │   │   ├── UserService.java         # 用户服务
│   │   │   ├── FriendService.java       # 好友服务
│   │   │   ├── ChatService.java         # 聊天服务
│   │   │   ├── ForumService.java        # 论坛服务
│   │   │   └── StatService.java         # 统计服务
│   │   ├── mapper/                      # MyBatis Mapper
│   │   ├── entity/                      # JPA 实体类
│   │   ├── dto/                         # 数据传输对象
│   │   │   ├── request/                 # 请求 DTO
│   │   │   └── response/                # 响应 DTO
│   │   ├── exception/                   # 异常处理
│   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   └── ApiException.java        # API 异常
│   │   ├── component/                   # 组件
│   │   │   ├── DataInitializer.java     # 数据初始化
│   │   │   └── JwtAuthenticationFilter.java  # JWT 过滤器
│   │   └── util/                        # 工具类
│   │       ├── JwtUtil.java             # JWT 工具
│   │       ├── RedisUtil.java           # Redis 工具
│   │       └── MinioUtil.java           # MinIO 工具
│   ├── src/main/resources/
│   │   ├── application.yml              # 开发环境配置
│   │   ├── application-prod.yml         # 生产环境配置
│   │   └── mapper/                      # MyBatis XML 映射文件
│   ├── init-db/                         # Docker 初始化脚本
│   │   └── init.sql
│   ├── Dockerfile                       # 后端 Docker 配置
│   └── pom.xml                          # Maven 依赖配置
│
├── bickdemo-frontend/                   # Vue 3 前端模块
│   ├── src/
│   │   ├── api/                         # API 接口封装
│   │   │   ├── auth.js                  # 认证接口
│   │   │   ├── bicycle.js               # 自行车接口
│   │   │   ├── rental.js                # 租赁接口
│   │   │   ├── user.js                  # 用户接口
│   │   │   ├── friend.js                # 好友接口
│   │   │   ├── chat.js                  # 聊天接口
│   │   │   └── forum.js                 # 论坛接口
│   │   ├── components/                  # 公共组件
│   │   │   ├── Layout/                  # 布局组件
│   │   │   ├── BicycleCard/             # 自行车卡片
│   │   │   ├── ChatBox/                 # 聊天窗口
│   │   │   └── ImageUpload/             # 图片上传
│   │   ├── router/                      # 路由配置
│   │   │   └── index.js
│   │   ├── stores/                      # Pinia 状态管理
│   │   │   ├── user.js                  # 用户状态
│   │   │   ├── auth.js                  # 认证状态
│   │   │   └── app.js                   # 应用状态
│   │   ├── views/                       # 页面组件
│   │   │   ├── Home/                    # 首页
│   │   │   ├── Bicycles/                # 自行车列表
│   │   │   ├── Rentals/                 # 租赁记录
│   │   │   ├── Friends/                 # 好友页面
│   │   │   ├── Forum/                   # 论坛页面
│   │   │   ├── Statistics/              # 统计页面
│   │   │   ├── Admin/                   # 管理后台
│   │   │   └── Profile/                 # 个人资料
│   │   ├── utils/                       # 工具函数
│   │   ├── App.vue                      # 根组件
│   │   └── main.js                      # 入口文件
│   ├── public/                          # 静态资源
│   ├── nginx.conf                       # Nginx 配置
│   ├── Dockerfile                       # 前端 Docker 配置
│   ├── package.json                     # 依赖配置
│   └── vite.config.js                   # Vite 配置
│
├── script/                              # 脚本工具目录
│   ├── dev/                             # 开发环境脚本
│   └── prod/                            # 生产环境脚本与 Docker 编排
│       └── deploy/                      # Jenkins / 手动部署脚本与文档
├── sql/                                 # SQL 初始化脚本
│   └── init.sql
├── Jenkinsfile                          # Jenkins 流水线配置
└── README.md                            # 项目说明文档
```

## 页面路由与功能对应

### 用户端路由

| 路由 | 组件路径 | 功能描述 |
|------|----------|----------|
| `/` | `views/Home/index.vue` | 首页（轮播图、车辆推荐） |
| `/bicycles` | `views/Bicycles/index.vue` | 自行车列表（筛选、搜索、分页） |
| `/bicycles/:id` | `views/Bicycles/Detail.vue` | 自行车详情（库存、价格、租用按钮） |
| `/my-rentals` | `views/Rentals/index.vue` | 我的租赁记录（进行中/已完成/已取消） |
| `/friends` | `views/Friends/index.vue` | 好友列表（搜索、申请、聊天入口） |
| `/chat/:friendId` | `views/Friends/Chat.vue` | 私聊窗口（消息历史、实时推送） |
| `/forum` | `views/Forum/index.vue` | 论坛帖子列表（分类、搜索、分页） |
| `/forum/:id` | `views/Forum/Detail.vue` | 帖子详情（评论、回复、点赞、收藏） |
| `/forum/create` | `views/Forum/Create.vue` | 发帖页面（多图上传、富文本） |
| `/statistics` | `views/Statistics/index.vue` | 统计分析（ECharts 图表展示） |
| `/profile` | `views/Profile/index.vue` | 个人资料（头像裁剪、信息修改） |
| `/login` | `views/Login/index.vue` | 登录页面 |
| `/register` | `views/Register/index.vue` | 注册页面（邮箱验证码） |
| `/forgot-password` | `views/ForgotPassword/index.vue` | 找回密码（邮箱验证 + 重置） |

### 管理端路由

| 路由 | 组件路径 | 功能描述 |
|------|----------|----------|
| `/admin` | `views/Admin/index.vue` | 管理后台首页（数据概览） |
| `/admin/bicycles` | `views/Admin/Bicycles/index.vue` | 车辆管理（CRUD、批量操作、库存管理） |
| `/admin/rentals` | `views/Admin/Rentals/index.vue` | 租赁记录（查询、导出） |
| `/admin/users` | `views/Admin/Users/index.vue` | 用户管理（查询、禁用） |
| `/admin/forum` | `views/Admin/Forum/index.vue` | 论坛审核（帖子审核、删除） |
| `/admin/backgrounds` | `views/Admin/Backgrounds/index.vue` | 背景图管理（上传、排序） |

## API 接口规范

### 统一响应格式

所有 API 接口返回统一 JSON 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | number | 状态码（200=成功，其他=失败） |
| message | string | 响应消息/错误描述 |
| data | object | 响应数据主体 |

**成功响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin"
  }
}
```

**失败响应示例：**
```json
{
  "code": 401,
  "message": "未登录或 Token 已过期",
  "data": null
}
```

### 认证方式

需要认证的接口需在请求头中携带 JWT Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 主要 API 端点

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/login | 用户登录 | ❌ |
| POST | /api/auth/register | 用户注册 | ❌ |
| POST | /api/auth/forgot-password | 找回密码 | ❌ |
| GET | /api/auth/logout | 退出登录 | ✅ |
| GET | /api/bicycles | 获取车辆列表 | ✅ |
| GET | /api/bicycles/:id | 获取车辆详情 | ✅ |
| POST | /api/rentals | 创建租赁订单 | ✅ |
| POST | /api/rentals/:id/return | 归还车辆 | ✅ |
| GET | /api/rentals/my | 我的租赁记录 | ✅ |
| GET | /api/user/profile | 获取个人资料 | ✅ |
| PUT | /api/user/profile | 更新个人资料 | ✅ |
| POST | /api/user/avatar | 上传头像 | ✅ |
| GET | /api/friends | 好友列表 | ✅ |
| POST | /api/friends/apply | 好友申请 | ✅ |
| GET | /api/chat/history/:friendId | 聊天历史 | ✅ |
| GET | /api/forum/posts | 帖子列表 | ✅ |
| POST | /api/forum/posts | 创建帖子 | ✅ |
| POST | /api/forum/posts/:id/comment | 发表评论 | ✅ |
| POST | /api/upload | 图片上传 | ✅ |
| GET | /api/admin/statistics | 统计数据 | ✅ (ADMIN) |

## 数据库设计

### 核心数据表

| 表名 | 说明 |
|------|------|
| user | 用户基础表（账号、密码、邮箱） |
| user_profile | 用户资料扩展表（头像、简介） |
| bicycle | 自行车表（名称、描述、库存、价格） |
| rental_order | 租赁订单表（用户、车辆、时间、金额） |
| friend | 好友关系表 |
| friend_request | 好友申请表 |
| chat_message | 聊天消息表 |
| forum_post | 论坛帖子表 |
| forum_comment | 评论表 |
| forum_like | 点赞记录表 |
| forum_favorite | 收藏记录表 |
| background_image | 背景图表 |
| email_code | 邮箱验证码表 |

### 表关系简述

```
user (1) ──► (N) rental_order
user (1) ──► (1) user_profile
user (M) ◄──► (M) friend (through friend_request)
user (1) ──► (N) chat_message
user (1) ──► (N) forum_post
user (1) ──► (N) forum_comment
bicycle (1) ──► (N) rental_order
```

## 数据库迁移

当项目升级或拉取新代码后，如遇到字段或表不存在的问题，需要执行对应的迁移脚本。

### 迁移脚本列表

| 脚本文件 | 说明 | 执行命令示例 |
|----------|------|--------------|
| `sql/init.sql` | 全量初始化脚本 | `mysql -u root -p bickdemo < sql/init.sql` |

> 💡 **提示**：当前仓库默认维护的是全量初始化脚本 `sql/init.sql`。

## 常用开发命令速查

### 后端命令

```bash
cd bickdemo-backend

# 开发模式启动（支持热重载）
mvn spring-boot:run

# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 运行所有单元测试
mvn test

# 运行单个测试类
mvn test -Dtest=UserServiceTest

# 查看依赖树
mvn dependency:tree

# 格式化代码
mvn spotless:apply
```

### 前端命令

```bash
cd bickdemo-frontend

# 安装依赖
npm install

# 开发模式（热重载）
npm run dev

# 生产构建
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint

# 代码格式化
npm run format
```

### Docker 命令

```bash
# 构建并启动
docker compose -f script/prod/docker-compose.yml up -d --build

# 查看服务状态
docker compose -f script/prod/docker-compose.yml ps

# 查看实时日志
docker compose -f script/prod/docker-compose.yml logs -f --tail=100

# 重启单个服务
docker compose -f script/prod/docker-compose.yml restart app

# 停止并清理
docker compose -f script/prod/docker-compose.yml down

# 重新构建单个服务（不使用缓存）
docker compose -f script/prod/docker-compose.yml build --no-cache frontend
```

## 配置说明

### 后端核心配置

| 配置项 | 文件位置 | 说明 |
|--------|----------|------|
| 数据库连接 | `application.yml` | MySQL 地址、用户名、密码 |
| Redis 配置 | `application.yml` | Redis 主机、端口、密码 |
| RabbitMQ | `application.yml` | MQ 主机、虚拟主机、账号 |
| MinIO | `application.yml` | 端点、AccessKey、SecretKey、Bucket |
| 邮件服务 | `application.yml` | SMTP 主机、发件人、授权码 |
| JWT 密钥 | `application.yml` | Token 签名密钥（建议 32 位+） |
| 服务端口 | `application.yml` | Spring Boot 端口（默认 8080） |

### 前端核心配置

| 配置项 | 文件位置 | 说明 |
|--------|----------|------|
| API 代理 | `vite.config.js` | 开发环境代理目标地址 |
| 路由模式 | `router/index.js` | history/hash 模式选择 |
| 请求超时 | `api/request.js` | Axios 超时时间配置 |
| Token 存储 | `stores/auth.js` | Pinia 中管理 Token |

## CI/CD 自动化部署

项目内置了 Jenkins 持续集成支持，可实现代码推送后自动构建和部署。

### 部署流程

```
代码提交 (Gitea/GitHub)
        │
        ▼
  Webhook 触发
        │
        ▼
   Jenkins 拉取
        │
        ▼
   Maven 构建后端
        │
        ▼
   NPM 构建前端
        │
        ▼
  Docker 镜像构建
        │
        ▼
  容器编排部署
```

### 相关文档

详细部署步骤请参考：
- 📄 [Jenkins 部署指南](script/prod/deploy/JENKINS-DEPLOY.md)
- 📄 [Jenkins + Gitea 集成文档](script/prod/deploy/JENKINS-GITEA-DEPLOY.md)

## 常见问题 (FAQ)

<details>
<summary>❓ 后端启动失败，提示数据库连接错误</summary>

**排查步骤：**

1. 确认 MySQL 服务已启动
   ```bash
   # Linux/Mac
   sudo systemctl status mysql

   # Windows
   services.msc  # 查找 MySQL 服务
   ```

2. 检查数据库配置是否正确
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/bickdemo?useSSL=false&serverTimezone=Asia/Shanghai
       username: root
       password: your_password
   ```

3. 验证数据库已创建并导入数据
   ```bash
   mysql -u root -p -e "SHOW DATABASES;"
   mysql -u root -p bickdemo -e "SHOW TABLES;"
   ```

4. 检查数据库用户权限
   ```sql
   SHOW GRANTS FOR 'root'@'localhost';
   ```

</details>

<details>
<summary>❓ 前端能打开页面，但接口请求失败</summary>

**排查步骤：**

1. 确认后端服务运行在 8080 端口
   ```bash
   curl http://localhost:8080/api/health
   ```

2. 检查前端代理配置（vite.config.js）
   ```javascript
   server: {
     proxy: {
       '/api': {
         target: 'http://localhost:8080',
         changeOrigin: true
       }
     }
   }
   ```

3. 浏览器开发者工具查看 Network 标签，确认请求 URL 是否正确

4. 检查是否有跨域错误（CORS）

</details>

<details>
<summary>❓ 图片上传失败</summary>

**排查步骤：**

1. 确认 MinIO 服务可访问
   ```bash
   curl http://localhost:9000/minio/health/live
   ```

2. 检查 Bucket 是否存在
   - 登录 MinIO 控制台 http://localhost:9001
   - 确认 `bickdemo` bucket 已创建

3. 验证后端 MinIO 配置
   ```yaml
   minio:
     endpoint: http://localhost:9000
     access-key: minioadmin
     secret-key: minioadmin
     bucket: bickdemo
   ```

4. 检查上传文件大小限制
   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 50MB
         max-request-size: 50MB
   ```

</details>

<details>
<summary>❓ 聊天/好友功能不可用</summary>

**排查步骤：**

1. 确认 RabbitMQ 服务状态
   ```bash
   # 访问管理界面
   http://localhost:15672
   # 默认账号：guest/guest
   ```

2. 检查 WebSocket 连接
   - 打开浏览器开发者工具 → Network → WS
   - 确认 WebSocket 连接成功

3. 验证好友关系
   - 确保已发送/接受好友申请
   - 检查好友列表是否有目标好友

4. 查看后端日志是否有 MQ 连接错误

</details>

<details>
<summary>❓ 新代码拉取后页面或接口报字段不存在</summary>

**解决方案：**

这是数据库结构未同步导致的。根据新功能执行对应的迁移脚本：

```bash
# 查看当前有哪些迁移脚本
ls sql

# 执行对应功能的迁移脚本
mysql -u root -p bickdemo < sql/init.sql
```

或者重新导入完整的 `sql/init.sql`（**注意：会清空现有数据**）：
```bash
mysql -u root -p -e "DROP DATABASE bickdemo; CREATE DATABASE bickdemo;"
mysql -u root -p bickdemo < sql/init.sql
```

</details>

<details>
<summary>❓ Docker 部署后无法访问服务</summary>

**排查步骤：**

1. 检查容器是否正常运行
   ```bash
docker compose -f script/prod/docker-compose.yml ps
   ```

2. 查看问题容器日志
   ```bash
docker compose -f script/prod/docker-compose.yml logs app
docker compose -f script/prod/docker-compose.yml logs frontend
   ```

3. 确认端口未被占用
   ```bash
   # Windows/Mac
   netstat -ano | findstr :8080

   # Linux
   lsof -i :8080
   ```

4. 检查防火墙设置
   ```bash
   # Linux
   sudo ufw status
   sudo ufw allow 8080
   ```

5. 重新启动服务
   ```bash
docker compose -f script/prod/docker-compose.yml down
docker compose -f script/prod/docker-compose.yml up -d --build
   ```

</details>

## 安全建议

⚠️ **生产环境部署前，请务必完成以下安全检查：**

### 1. 敏感配置迁移

不要将以下信息硬编码在配置文件中：

- [ ] 数据库密码
- [ ] JWT 密钥
- [ ] MinIO 密钥
- [ ] 邮箱 SMTP 授权码
- [ ] Redis 密码

**推荐做法：**
- 使用环境变量
- 使用密钥管理系统（如 HashiCorp Vault）
- 使用配置中心（如 Nacos、Apollo）

### 2. 修改默认账号

```sql
-- 修改管理员密码
UPDATE user SET password = '加密后的新密码' WHERE username = 'admin';
```

### 3. 清理 Git 历史

如果已提交过敏感信息：

```bash
# 使用 BFG Repo-Cleaner 清理
bfg --delete-files .env
# 或
bfg --replace-text passwords.txt
```

### 4. 开启 HTTPS

生产环境务必使用 HTTPS 加密传输：
- 使用 Nginx 反向代理配置 SSL
- 申请免费证书（Let's Encrypt）

### 5. 限制访问

- 数据库端口不要暴露在公网
- 配置防火墙白名单
- 使用反向代理隐藏真实端口

## 性能优化建议

### 后端优化

1. **数据库索引**：为常用查询字段添加索引
2. **缓存策略**：热点数据（如统计信息）使用 Redis 缓存
3. **连接池调优**：根据并发量调整 HikariCP 参数
4. **异步处理**：邮件发送、消息推送使用异步

### 前端优化

1. **路由懒加载**：按需加载页面组件
2. **组件懒加载**：大型组件使用动态导入
3. **图片压缩**：上传前压缩、CDN 加速
4. **构建优化**：开启 Gzip、代码分割

## 开发贡献

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到远程 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 代码规范

- 后端：遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 前端：遵循 [Vue.js Style Guide](https://vuejs.org/style-guide/)

## 项目截图

> 📸 此处可添加项目截图展示（首页、列表页、管理后台等）

<!-- 示例：
![首页预览](./screenshots/home.png)
![管理后台](./screenshots/admin.png)
-->

## 许可证

本项目采用 [MIT License](LICENSE) 协议开源。

## 联系方式

| 渠道 | 链接/说明 |
|------|----------|
| 📧 Email | your-email@example.com |
| 📱 Issues | [GitHub Issues](https://github.com/yourname/bickdemo/issues) |
| 📖 Wiki | [项目 Wiki](https://github.com/yourname/bickdemo/wiki) |

---

<p align="center">
  Made with ❤️ by BikeShare Team
</p>
