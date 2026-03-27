# BikeShare 自行车租赁系统

<div align="center">


[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4.0-4FC08D?logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.5.0-409EFF?logo=element)](https://element-plus.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?logo=redis)](https://redis.io/)
[![JDK](https://img.shields.io/badge/JDK-17+-4370D7?logo=openjdk)](https://adoptium.net/)


> 🚴 基于 Spring Boot 3 + Vue 3 的前后端分离单车租赁平台

</div>

---

## ✨ 特性亮点

<div align="center">

| 🚀 技术架构 | 🔐 安全认证 | ⚡ 性能优化 | 🎨 用户体验 |
|:---:|:---:|:---:|:---:|
| Spring Boot 3 + Vue 3<br>RESTful API<br>MyBatis-Plus | JWT 无状态认证<br>Spring Security 6.x<br>BCrypt 加密 | Redis 多级缓存<br>HikariCP 连接池<br>RabbitMQ 队列 | Element Plus<br>玻璃态设计<br>暗色模式 |

</div>

---

## 🏃 快速开始

### 📋 环境要求

| 环境 | 最低版本 | 推荐版本 |
|:---:|:---:|:---:|
| JDK | 17 | 21 |
| Node.js | 18 | 20 LTS |
| MySQL | 8.0 | 8.0.35+ |
| Redis | 6.0 | 7.0+ |
| Maven | 3.8 | 3.9+ |
| Docker | 24.0 | Latest |

### 🚀 快速启动 (Docker)

```bash
# 克隆项目
git clone <repository-url>
cd bickdemo

# 一键启动 (包含 MySQL + Redis + Backend + Frontend)
docker compose -f script/prod/docker-compose.yml up -d --build

# 访问应用
# 用户端: http://localhost
# 管理端: http://localhost:5174
# API:    http://localhost:8080/api
```

### 💻 本地开发

```bash
# 1. 克隆项目
git clone <repository-url>
cd bickdemo

# 2. 初始化数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bickdemo DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p bickdemo < sql/init.sql

# 3. 启动后端
cd bickdemo-backend
mvn spring-boot:run
# → http://localhost:8080

# 4. 启动用户端
cd ../bickdemo-frontend
npm install && npm run dev
# → http://localhost:5173

# 5. 启动管理端
cd ../bickdemo-admin
npm install && npm run dev
# → http://localhost:5174
```

---

## 📁 项目结构

```
bickdemo/
├── bickdemo-backend/                    # 🚀 Spring Boot 后端
│   └── src/main/java/com/example/bickdemo/
│       ├── annotation/                  # 自定义注解 (@RequirePermission)
│       ├── aspect/                      # AOP 切面 (IP 限流)
│       ├── config/                      # 配置类
│       │   ├── SecurityConfig           # Spring Security 配置
│       │   ├── CorsConfig               # 跨域配置
│       │   └── JwtConfig                # JWT 配置
│       ├── component/                   # 组件 (WebSocket)
│       ├── controller/                  # REST API 控制器
│       │   ├── AuthController           # 认证模块
│       │   ├── BicycleController        # 车辆模块
│       │   ├── RentalController         # 租赁模块
│       │   ├── SocialController         # 社交模块
│       │   ├── ForumController          # 论坛模块
│       │   ├── ActivityController       # 活动模块
│       │   ├── TicketController         # 工单模块
│       │   └── AdminController         # 管理模块
│       ├── dto/                         # 数据传输对象
│       │   ├── request/                 # 请求 DTO
│       │   └── response/                # 响应 DTO
│       ├── entity/                      # 实体类
│       │   ├── User, Bicycle, Rental
│       │   ├── Activity, Forum, Comment
│       │   └── Friend, Message, Ticket
│       ├── enums/                       # 枚举类
│       │   ├── ActivityDifficulty       # 难度等级
│       │   ├── ActivityStatus          # 活动状态
│       │   ├── RentalStatus            # 租赁状态
│       │   └── ForumStatus             # 帖子状态
│       ├── exception/                   # 异常处理
│       ├── mapper/                      # MyBatis Mapper
│       ├── service/                     # 业务逻辑
│       └── util/                        # 工具类
│           ├── JwtUtil                  # JWT 工具
│           ├── RedisUtil               # Redis 工具
│           └── MinioUtil               # MinIO 工具
│
├── bickdemo-frontend/                   # 🎨 Vue 3 用户端
│   └── src/
│       ├── api/                         # API 封装
│       │   ├── auth.js, bicycle.js, rental.js
│       │   ├── social.js, forum.js, activity.js
│       │   └── file.js
│       ├── components/                  # 公共组件
│       │   ├── ThemeToggle.vue         # 主题切换
│       │   ├── ImageUpload.vue         # 图片上传
│       │   └── CitySelector.vue        # 城市选择器
│       ├── router/                      # 路由配置
│       ├── stores/                      # Pinia 状态
│       │   ├── user.js, theme.js
│       │   └── websocket.js
│       ├── styles/                      # 样式文件
│       │   ├── global.css              # 全局样式
│       │   └── element.css            # Element Plus 覆盖
│       ├── utils/                       # 工具函数
│       │   └── request.js             # Axios 封装
│       └── views/                       # 页面组件
│           ├── Home.vue, Login.vue, Register.vue
│           ├── Marketplace.vue, BicycleDetail.vue
│           ├── Rentals.vue, Activities.vue
│           ├── Forum.vue, PostDetail.vue
│           ├── Messages.vue, Friends.vue
│           └── Profile.vue
│
├── bickdemo-admin/                      # 📊 Vue 3 管理端
│   └── src/
│       ├── api/                         # API 封装
│       ├── layouts/                     # 布局组件
│       ├── router/                      # 路由配置
│       ├── stores/                      # Pinia 状态
│       └── views/                       # 管理页面
│           ├── Dashboard.vue            # 数据看板
│           ├── Users.vue, Bicycles.vue
│           ├── Activities.vue, Tickets.vue
│           └── Notices.vue, Logs.vue
│
├── script/                              # 部署脚本
│   ├── dev/                            # 开发脚本
│   └── prod/                           # 生产部署
│       ├── docker-compose.yml
│       └── nginx.conf
│
└── sql/                                 # 数据库脚本
    ├── init.sql                        # 初始化脚本
    └── V00*_*.sql                     # 增量迁移
```

---

## 🎯 核心功能

### 用户系统
- ✅ 邮箱注册/登录 + JWT Token 认证
- ✅ 找回密码（邮箱验证码）
- ✅ 个人资料管理、头像上传
- ✅ 会员等级系统（根据骑行时长计算）

### 租车服务
```
┌─────────┐    ┌──────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ 浏览车辆 │ → │ 选择车辆 │ → │ 开始租车 │ → │ 骑行中   │ → │ 结束租车 │
└─────────┘    └──────────┘    └─────────┘    └─────────┘    └─────────┘
                                         │
                                     💰 支付租金
```
- ✅ 车辆浏览、筛选、搜索
- ✅ 实时库存显示
- ✅ 租赁订单管理
- ✅ 支持按小时/按天计费

### 社交聊天
- ✅ 好友申请与管理
- ✅ WebSocket 实时聊天
- ✅ 离线消息推送
- ✅ 聊天记录查询

### 论坛社区
- ✅ 图文发帖、多图上传
- ✅ 评论、点赞、收藏
- ✅ 标签分类、热门话题
- ✅ 管理员审核机制

### 活动管理
- ✅ 骑行活动发布与管理
- ✅ **省市区三级联动选择器**
- ✅ 活动报名、签到管理
- ✅ 难度等级（简单/中等/困难）

### 后台管理
| 模块 | 功能 |
|:---|:---|
| 📈 数据看板 | ECharts 统计图表（用户/车辆/订单/活动） |
| 👥 用户管理 | 用户列表、状态启用/禁用、角色管理 |
| 🚲 车辆管理 | 车辆 CRUD、批量导入、位置管理 |
| 📋 订单管理 | 租赁订单查询、退款处理、异常订单 |
| 🎪 活动管理 | 活动审核、签到管理、数据统计 |
| 🎫 工单管理 | 用户反馈处理、进度跟踪 |
| 📝 系统日志 | 登录日志、操作日志、访客统计 |

---

## 📐 系统架构

### 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              客户端层                                    │
├──────────────────────────────┬──────────────────────────────────────────┤
│   🚴 用户端 (bickdemo-frontend)   │      📊 管理端 (bickdemo-admin)           │
│       Vue 3 + Vite 5             │         Vue 3 + Element Plus          │
│      http://localhost:5173       │        http://localhost:5174          │
└──────────────┬─────────────────┴──────────────────────┬────────────────┘
               │                                        │
               └────────────────────┬────────────────────┘
                                    │ HTTP / WebSocket
                 ┌─────────────────▼─────────────────┐
                 │              Nginx 网关层              │
                 │         (负载均衡 + 静态资源)         │
                 └─────────────────┬─────────────────┘
                                   │
┌──────────────────────────────────▼──────────────────────────────────────┐
│                           应用服务层                                     │
│                      Spring Boot 3.2.0 Backend                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐      │
│  │  Auth   │  │Bicycle │  │ Rental  │  │Social   │  │  Forum  │      │
│  │  认证    │  │  车辆   │  │  租赁   │  │  社交   │  │  论坛   │      │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘      │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐      │
│  │Activity │  │ Ticket  │  │  Admin  │  │  Stats  │  │  File   │      │
│  │  活动   │  │  工单   │  │  管理   │  │  统计   │  │  文件   │      │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘      │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼──────────────────────────────────────┐
│                            数据层                                        │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐            │
│  │  MySQL  │    │  Redis  │    │RabbitMQ │    │  MinIO  │            │
│  │   8.0   │    │   7.0   │    │         │    │ 对象存储 │            │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 技术栈

#### 后端技术

| 技术 | 版本 | 说明 |
|:---|:---:|:---|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.x | 认证授权 |
| JWT | - | Token 认证 |
| MyBatis-Plus | 3.5.5 | ORM 增强 |
| MySQL | 8.0 | 关系数据库 |
| Redis | 7.0 | 缓存/会话 |
| RabbitMQ | - | 消息队列 |
| MinIO | - | 对象存储 |
| Lombok | - | 简化代码 |

#### 前端技术

| 技术 | 版本 | 说明 |
|:---|:---:|:---|
| Vue | 3.4.0 | 核心框架 |
| Vite | 5.0.8 | 构建工具 |
| Element Plus | 2.5.0 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.2.5 | 路由管理 |
| Axios | 1.6.2 | HTTP 客户端 |
| ECharts | 6.0.0 | 统计图表 |
| element-china-area-data | - | 省市区数据 |

---

## 📡 API 文档

### 基础信息

| 项目 | 说明 |
|:---|:---|
| Base URL | `/api` |
| 认证方式 | `Authorization: Bearer <JWT Token>` |
| 响应格式 | JSON |

**统一响应结构:**
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 接口列表

#### 认证接口 `/auth`

| 方法 | 路径 | 说明 | 认证 |
|:---:|:---|:---|:---:|
| POST | /auth/register | 用户注册 | ❌ |
| POST | /auth/login | 用户登录 | ❌ |
| POST | /auth/logout | 退出登录 | ✅ |
| POST | /auth/forget | 忘记密码 | ❌ |
| GET | /auth/me | 获取当前用户 | ✅ |

#### 车辆接口 `/bicycles`

| 方法 | 路径 | 说明 | 认证 |
|:---:|:---|:---|:---:|
| GET | /bicycles | 车辆列表 | ✅ |
| GET | /bicycles/{id} | 车辆详情 | ✅ |
| POST | /bicycles | 添加车辆 | 管理员 |
| PUT | /bicycles/{id} | 更新车辆 | 管理员 |
| DELETE | /bicycles/{id} | 删除车辆 | 管理员 |

#### 租赁接口 `/rentals`

| 方法 | 路径 | 说明 | 认证 |
|:---:|:---|:---|:---:|
| POST | /rentals/start | 开始租车 | ✅ |
| POST | /rentals/{id}/end | 结束租车 | ✅ |
| GET | /rentals | 租赁记录 | ✅ |
| GET | /rentals/{id} | 租赁详情 | ✅ |

#### 活动接口 `/activities`

| 方法 | 路径 | 说明 | 认证 |
|:---:|:---|:---|:---:|
| GET | /activities | 活动列表 | ✅ |
| GET | /activities/{id} | 活动详情 | ✅ |
| POST | /activities | 创建活动 | ✅ |
| PUT | /activities/{id} | 更新活动 | ✅ |
| DELETE | /activities/{id} | 删除活动 | ✅ |
| POST | /activities/{id}/join | 报名活动 | ✅ |
| POST | /activities/{id}/signin | 签到 | ✅ |

#### 论坛接口 `/forums`

| 方法 | 路径 | 说明 | 认证 |
|:---:|:---|:---|:---:|
| GET | /forums | 帖子列表 | ✅ |
| GET | /forums/{id} | 帖子详情 | ✅ |
| POST | /forums | 发布帖子 | ✅ |
| PUT | /forums/{id} | 更新帖子 | ✅ |
| DELETE | /forums/{id} | 删除帖子 | ✅ |
| POST | /forums/{id}/like | 点赞 | ✅ |
| POST | /forums/{id}/collect | 收藏 | ✅ |
| POST | /forums/{id}/comment | 评论 | ✅ |

#### 社交接口 `/friends`

| 方法 | 路径 | 说明 | 认证 |
|:---:|:---|:---|:---:|
| GET | /friends | 好友列表 | ✅ |
| POST | /friends/request | 发送好友申请 | ✅ |
| PUT | /friends/request/{id} | 处理申请 | ✅ |
| DELETE | /friends/{id} | 删除好友 | ✅ |
| GET | /messages | 消息列表 | ✅ |
| GET | /messages/conversation/{userId} | 会话详情 | ✅ |

### 错误码

| code | 说明 |
|:---:|:---|
| 200 | ✅ 成功 |
| 400 | ❌ 请求参数错误 |
| 401 | 🔐 未授权（未登录/Token 过期） |
| 403 | 🚫 权限不足 |
| 404 | 🔍 资源不存在 |
| 500 | 💥 服务器内部错误 |

---

## 🗄️ 数据库设计

### ER 关系图

```
                        ┌─────────────┐
                        │    User     │
                        ├─────────────┤
                        │ id (PK)     │
                        │ email       │
                        │ password    │
                        │ nickname    │
                        │ avatar      │
                        │ level       │
                        └──────┬──────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   Bicycle     │    │   Activity    │    │    Forum      │
├───────────────┤    ├───────────────┤    ├───────────────┤
│ id (PK)       │    │ id (PK)       │    │ id (PK)       │
│ code          │    │ title         │    │ user_id       │
│ type          │    │ location      │    │ title         │
│ status        │    │ start_time    │    │ content       │
│ price         │    │ status        │    │ images        │
└───────┬───────┘    │ difficulty    │    │ status        │
        │            │ organizer_id  │    │ likes         │
        │            └───────────────┘    └───────┬───────┘
        │                                          │
        ▼                                          ▼
┌───────────────┐                          ┌───────────────┐
│    Rental     │                          │   Comment     │
├───────────────┤                          ├───────────────┤
│ id (PK)       │                          │ id (PK)       │
│ user_id       │                          │ forum_id      │
│ bicycle_id    │                          │ user_id       │
│ start_time    │                          │ content       │
│ end_time      │                          │ parent_id     │
│ fee           │                          └───────────────┘
│ status        │
└───────────────┘

        ┌───────────────┐    ┌───────────────┐
        │    Friend     │    │    Message    │
        ├───────────────┤    ├───────────────┤
        │ id (PK)       │    │ id (PK)       │
        │ user_id       │    │ from_id       │
        │ friend_id     │    │ to_id         │
        │ status        │    │ content       │
        └───────────────┘    │ read          │
                            └───────────────┘
```

### 主要表结构

#### users (用户表)

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | BIGINT | 主键 |
| email | VARCHAR(100) | 邮箱(唯一) |
| password | VARCHAR(255) | BCrypt 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(500) | 头像 URL |
| level | INT | 会员等级 |
| bike_count | INT | 骑行次数 |
| total_time | INT | 累计骑行时长(分钟) |
| status | TINYINT | 状态(0禁用/1启用) |
| role | VARCHAR(20) | 角色(USER/ADMIN) |
| deleted | TINYINT | 逻辑删除 |

#### bicycles (车辆表)

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | BIGINT | 主键 |
| code | VARCHAR(50) | 车辆编号(唯一) |
| type | VARCHAR(20) | 类型(山地车/公路车/共享) |
| brand | VARCHAR(50) | 品牌 |
| status | TINYINT | 状态(可用/租用中/维护) |
| location | VARCHAR(200) | 当前位置 |
| price_per_hour | DECIMAL(10,2) | 每小时价格 |
| quantity | INT | 库存数量 |
| image | VARCHAR(500) | 图片 URL |

#### rentals (租赁订单表)

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| bicycle_id | BIGINT | 车辆ID |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration | INT | 租用时长(分钟) |
| total_fee | DECIMAL(10,2) | 总费用 |
| status | VARCHAR(20) | 状态 |
| quantity | INT | 租赁数量 |

#### activities (活动表)

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | BIGINT | 主键 |
| title | VARCHAR(200) | 活动标题 |
| description | TEXT | 活动描述 |
| location | VARCHAR(200) | 活动地点(文本) |
| location_code | VARCHAR(20) | 区级代码 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| difficulty | VARCHAR(20) | 难度(EASY/MEDIUM/HARD) |
| max_participants | INT | 最大参与人数 |
| status | VARCHAR(20) | 状态 |
| organizer_id | BIGINT | 组织者ID |
| created_at | DATETIME | 创建时间 |

#### forums (论坛帖子表)

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 发布者ID |
| title | VARCHAR(200) | 帖子标题 |
| content | TEXT | 帖子内容 |
| images | VARCHAR(2000) | 图片JSON数组 |
| tag | VARCHAR(50) | 标签 |
| status | VARCHAR(20) | 状态 |
| likes | INT | 点赞数 |
| views | INT | 浏览数 |
| created_at | DATETIME | 创建时间 |

---

## 🚢 部署指南

### Docker 部署 (生产环境)

```bash
# 进入部署目录
cd script/prod

# 配置环境变量
cp .env.example .env
# 编辑 .env 设置数据库密码、JWT 密钥等

# 启动所有服务
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止服务
docker compose down -v
```

### 服务端口

| 服务 | 端口 | 说明 |
|:---|:---:|:---|
| 🚴 用户端 | 80 | Nginx 用户端 |
| 📊 管理端 | 81 | Nginx 管理端 |
| ⚙️ 后端 | 8080 | Spring Boot API |
| 🐬 MySQL | 3306 | 数据库 |
| 📦 Redis | 6379 | 缓存 |

### Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 用户端
    location / {
        root /usr/share/nginx/html/frontend;
        try_files $uri $uri/ /index.html;
    }

    # 管理端
    location /admin {
        alias /usr/share/nginx/html/admin;
        try_files $uri $uri/ /admin/index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket 代理
    location /ws {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## ⚙️ 环境变量

### 后端环境变量

| 变量名 | 默认值 | 说明 |
|:---|:---|:---|
| `MYSQL_HOST` | localhost | MySQL 主机 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_DATABASE` | bickdemo | 数据库名 |
| `MYSQL_USERNAME` | root | MySQL 用户名 |
| `MYSQL_PASSWORD` | - | MySQL 密码(必填) |
| `JWT_SECRET` | - | JWT 签名密钥(必填) |
| `REDIS_HOST` | localhost | Redis 主机 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | - | Redis 密码 |
| `MINIO_ENDPOINT` | http://localhost:9000 | MinIO 端点 |
| `MINIO_ACCESS_KEY` | - | MinIO 访问密钥 |
| `MINIO_SECRET_KEY` | - | MinIO 密钥 |
| `MINIO_BUCKET` | bicycles | MinIO 存储桶 |
| `RABBITMQ_HOST` | localhost | RabbitMQ 主机 |
| `RABBITMQ_USERNAME` | guest | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | guest | RabbitMQ 密码 |
| `MAIL_HOST` | smtp.qq.com | 邮件服务器 |
| `MAIL_USERNAME` | - | 发件人邮箱 |
| `MAIL_PASSWORD` | - | SMTP 授权码 |

### 前端环境变量

| 变量名 | 默认值 | 说明 |
|:---|:---|:---|
| `VITE_API_BASE_URL` | /api | API 基础路径 |
| `VITE_WS_URL` | ws://localhost:8080/ws | WebSocket 地址 |

---

## 🎨 UI 设计规范

### 玻璃态设计 (Glassmorphism)

项目多处使用玻璃态效果，提供现代化的视觉体验：

```css
/* 基础玻璃效果 */
backdrop-filter: blur(12px) saturate(180%);
background: rgba(255, 255, 255, 0.08);
border: 1px solid rgba(255, 255, 255, 0.18);

/* 导航栏 */
.navbar {
  background: color-mix(in srgb, var(--bs-surface-solid) 88%, transparent);
  backdrop-filter: blur(12px) saturate(135%);
}

/* 标签组件 */
.el-tag {
  backdrop-filter: blur(12px) saturate(180%);
  background: rgba(64, 158, 255, 0.15);
  border: 1px solid rgba(64, 158, 255, 0.30);
}

/* 主题切换按钮 */
.theme-toggle__button {
  border-radius: 999px;
  background: color-mix(in srgb, var(--bs-surface-solid) 88%, transparent);
  backdrop-filter: blur(12px) saturate(135%);
  opacity: 0.95;
}
```

### 响应式断点

| 断点 | 屏幕宽度 | 布局效果 |
|:---|:---:|:---|
| 超小屏幕 | ≤480px | 紧凑单列 |
| 手机端 | ≤768px | 简化导航 |
| 平板 | ≤1050px | 汉堡菜单模式 |
| 小屏PC | ≤1300px | 导航图标模式 |
| 大屏PC | >1300px | 完整导航 |

```css
/* 1050px: 显示汉堡菜单 */
@media (max-width: 1050px) {
  .nav-links { display: none !important; }
  .menu-toggle { display: flex; }
}

/* 1300px: 隐藏导航文字 */
@media (max-width: 1300px) {
  .nav-link > span:last-child { display: none; }
  .logo-text-section { display: none; }
}
```

### 暗色模式

项目支持暗色模式切换，核心 CSS 变量：

```css
:root {
  --bs-bg: #ffffff;
  --bs-surface: #f8fafc;
  --bs-ink: #1e293b;
}

html.dark {
  --bs-bg: #0f172a;
  --bs-surface: #1e293b;
  --bs-ink: #f1f5f9;
}
```

---

## 📋 枚举值对照

### 活动难度等级 (ActivityDifficulty)

| 前端值 | 后端值 | 说明 |
|:---:|:---:|:---|
| 简单 | `EASY` | 初学者友好 |
| 中等 | `MEDIUM` | 需要一定基础 |
| 困难 | `HARD` | 需要丰富经验 |

### 活动状态 (ActivityStatus)

| 前端值 | 后端值 | 说明 |
|:---:|:---:|:---|
| 草稿 | `DRAFT` | 暂不发布 |
| 已发布 | `PUBLISHED` | 开放报名 |
| 进行中 | `IN_PROGRESS` | 活动进行中 |
| 已完成 | `COMPLETED` | 活动已结束 |
| 已取消 | `CANCELLED` | 活动已取消 |

### 租赁状态 (RentalStatus)

| 前端值 | 后端值 | 说明 |
|:---:|:---:|:---|
| 进行中 | `IN_PROGRESS` | 租用中 |
| 已完成 | `COMPLETED` | 已归还 |
| 已取消 | `CANCELLED` | 已取消 |

### 论坛帖子状态 (ForumStatus)

| 前端值 | 后端值 | 说明 |
|:---:|:---:|:---|
| 草稿 | `DRAFT` | 暂不发布 |
| 已发布 | `PUBLISHED` | 正常显示 |
| 已隐藏 | `HIDDEN` | 管理员隐藏 |
| 已删除 | `DELETED` | 已删除 |

### 车辆状态 (BicycleStatus)

| 前端值 | 后端值 | 说明 |
|:---:|:---:|:---|
| 可用 | `AVAILABLE` | 可租用 |
| 租用中 | `IN_USE` | 正在使用 |
| 维护中 | `MAINTENANCE` | 维护中 |
| 已报废 | `SCRAPPED` | 已报废 |

---

## 🔑 演示账号

| 角色 | 用户名 | 密码 | 说明 |
|:---:|:---:|:---:|:---|
| 👑 管理员 | admin | admin123 | 系统管理、审核、统计 |
| 🚴 用户 | user | user123 | 租车、社交、发帖 |

> ⚠️ **安全提醒**: 首次部署后请立即修改默认密码，生产环境务必使用强密码！

---

## ❓ 常见问题

### Q: 数据库连接失败?

```bash
# 检查 MySQL 服务状态
mysql -u root -p -e "SELECT 1"

# 检查端口是否开放
netstat -an | grep 3306

# 查看 Docker MySQL 日志
docker compose logs mysql
```

### Q: 前端无法访问后端 API?

```bash
# 检查后端是否正常启动
curl http://localhost:8080/api/auth/me

# 检查 Nginx 代理配置
docker compose exec nginx cat /etc/nginx/nginx.conf

# 检查跨域配置 (application.yml)
```

### Q: Redis 连接失败?

```bash
# 检查 Redis 服务
redis-cli ping
# 应返回: PONG

# 如果使用 Docker
docker compose logs redis
```

### Q: 如何重置数据库?

```bash
# 停止服务
docker compose down

# 删除数据卷(慎用!)
docker volume rm bickdemo_mysql_data

# 重新初始化
docker compose up -d
mysql -u root -p bickdemo < sql/init.sql
```

---

## 🤝 贡献指南

欢迎提交 Pull Request 或 Issue！

### 分支命名规范

| 类型 | 格式 | 示例 |
|:---:|:---|:---|
| 功能分支 | `feature/` | `feature/user-auth` |
| 修复分支 | `fix/` | `fix/login-bug` |
| 文档分支 | `docs/` | `docs/readme` |
| 重构分支 | `refactor/` | `refactor/api` |

### 提交信息规范

```
<type>: <subject>

feat: 添加用户注册功能
fix: 修复登录超时问题
docs: 更新 README 文档
style: 格式化代码
refactor: 重构认证模块
test: 添加单元测试
chore: 更新依赖版本
```

### Pull Request 流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 🗺️ 项目路线图

- [x] 用户系统 (注册/登录/JWT)
- [x] 租车服务 (浏览/租用/归还)
- [x] 社交聊天 (好友/WebSocket)
- [x] 论坛社区 (发帖/评论/点赞)
- [x] 活动管理 (省市区选择器/报名签到)
- [x] 后台管理 (看板/用户/车辆管理)
- [x] UI 玻璃态效果
- [x] 暗色模式
- [ ] 微信/支付宝支付集成
- [ ] 短信验证码登录
- [ ] 地图定位功能
- [ ] 性能优化与压力测试

---

## 📜 更新日志

### [v1.0.0] - 2026-03

#### ✨ 新功能
- 实现用户系统(注册/登录/JWT认证)
- 实现租车服务(浏览/租用/归还)
- 实现社交聊天(WebSocket实时消息)
- 实现论坛社区(发帖/评论/点赞)
- 实现活动管理(省市区选择器/报名签到)
- 实现后台管理(数据看板/用户/车辆管理)

#### 🎨 优化
- UI 玻璃态效果优化
- 响应式布局完善
- 暗色模式支持

---

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

<div align="center">

**Built with ❤️ by BikeShare Team**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4.0-4FC08D?style=for-the-badge&logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.5.0-409EFF?style=for-the-badge&logo=element)](https://element-plus.org/)

</div>
