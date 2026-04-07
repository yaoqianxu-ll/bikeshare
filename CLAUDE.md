# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

自行车租赁系统 (BikeShare) - 基于 **Spring Boot 3.2.0 + Vue 3.4.0** 的全栈应用，涵盖以下功能模块：

- 自行车租赁与管理
- 社区论坛（发帖、评论、点赞）
- 二手交易市场
- 社交聊天（好友、WebSocket 实时消息）
- 骑行活动管理
- 系统公告与工单反馈

## 技术栈

### 后端

| 分类 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.0, Spring Security 6.x |
| 认证 | JWT (jjwt 0.12.x) |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0, HikariCP |
| 缓存 | Redis (Spring Cache), Caffeine (本地缓存) |
| 消息队列 | RabbitMQ (AMQP) |
| 对象存储 | MinIO |
| 实时通信 | WebSocket (STOMP) |
| 工具 | Lombok, Hutool, Jackson |

| 分类 | 技术 |
|------|------|
| 框架 | Vue 3.4.0, Vite 5.0.8 |
| UI 库 | Element Plus 2.5.0 |
| 状态管理 | Pinia 2.1.7 |
| 路由 | Vue Router 4.2.5 |
| HTTP | Axios 1.6.2 |
| 图表 | ECharts 6.0.0 |

## 目录结构

```
bickdemo/
├── bickdemo-backend/              # Spring Boot 后端
│   ├── src/main/java/.../
│   │   ├── annotation/           # 自定义注解 (如权限控制 @Admin)
│   │   ├── aspect/              # AOP 切面
│   │   │   ├── AdminOperationLogAspect.java   # 管理端操作日志
│   │   │   └── IpAccessControlAspect.java     # IP 访问控制
│   │   ├── component/           # 初始化组件
│   │   │   ├── MinioInitializer.java        # MinIO 存储桶初始化
│   │   │   └── DataInitializer.java         # 初始数据初始化
│   │   ├── config/              # 配置类
│   │   │   ├── SecurityConfig.java          # Spring Security 配置
│   │   │   ├── JwtConfig.java               # JWT 配置
│   │   │   ├── CorsConfig.java              # 跨域配置
│   │   │   ├── RabbitMqConfig.java          # RabbitMQ 配置
│   │   │   ├── MinioConfig.java             # MinIO 配置
│   │   │   └── WebSocketConfig.java         # WebSocket 配置
│   │   ├── controller/          # REST API 控制器
│   │   ├── dto/                 # 数据传输对象
│   │   │   ├── request/         # 请求 DTO
│   │   │   └── response/        # 响应 DTO
│   │   ├── entity/              # MyBatis-Plus 实体 (支持逻辑删除)
│   │   ├── exception/           # 全局异常处理
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── mapper/              # MyBatis Mapper 接口
│   │   ├── service/             # 业务逻辑
│   │   │   └── impl/           # 业务实现
│   │   └── util/                # 工具类
│   │       ├── JwtUtil.java              # JWT 工具
│   │       ├── RedisUtil.java            # Redis 操作工具
│   │       ├── MinioUtil.java            # MinIO 操作工具
│   │       ├── IpAddressUtils.java       # IP 地址解析工具
│   │       └── AliYunSmsUtil.java        # 阿里云短信工具
│   └── src/main/resources/
│       ├── application.yml      # 基础配置
│       └── application-prod.yml  # 生产环境配置
├── bickdemo-frontend/            # Vue 3 用户端前端
│   ├── src/
│   │   ├── api/                 # API 请求模块
│   │   ├── assets/             # 静态资源
│   │   ├── components/          # 公共组件
│   │   │   ├── ThemeToggle.vue  # 主题切换
│   │   │   └── PageSizeDropdown.vue  # 分页大小选择
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   │   └── user.js         # 用户状态
│   │   ├── utils/              # 工具函数
│   │   │   └── format.js       # 格式化工具
│   │   └── views/              # 页面组件
│   └── vite.config.js
├── bickdemo-admin/              # Vue 3 管理端前端
│   └── src/
│       ├── api/                 # API 请求
│       ├── components/          # 公共组件
│       ├── router/              # 路由配置
│       ├── stores/               # 状态管理
│       └── views/               # 页面组件
│           ├── ForumModeration.vue   # 论坛审核
│           ├── BackgroundManage.vue  # 背景管理
│           └── ...
├── script/                     # 部署脚本
│   └── prod/
│       └── docker-compose.yml  # 生产环境编排
└── sql/
    └── init.sql                # 数据库初始化脚本
```

## 后端 Controller 模块

| 模块 | Controller | 说明 |
|------|-----------|------|
| 认证 | `AuthController` | 登录/注册/邮箱验证/密码重置/登录日志 |
| 自行车 | `BicycleController` | 自行车 CRUD、搜索、可用列表 |
| 租赁 | `RentalController` | 租赁订单、计费、开始/结束租赁 |
| 论坛 | `ForumController` | 帖子/评论/点赞/ Reactions |
| 市场 | `MarketplaceController` | 二手物品发布、审核、购买申请 |
| 社交 | `SocialController` | 好友请求、好友列表、聊天消息 |
| 公告 | `NoticeController` | 系统公告 CRUD |
| 工单 | `TicketController` | 用户反馈工单、留言、评价 |
| 活动 | `ActivityController` | 骑行活动、报名、留言 |
| 管理 | `Admin*Controller` | 各模块管理端 API |
| 系统 | `AdminSystemController` | 系统日志、黑名单、统计数据 |

## API 路由

### 用户端 API (`/api/*`)

| 方法 | 路由 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/logout` | 用户登出 |
| GET | `/api/auth/userinfo` | 获取用户信息 |
| POST | `/api/auth/email-login` | 邮箱验证码登录 |
| POST | `/api/auth/reset-password` | 邮箱验证码重置密码 |
| GET | `/api/bicycles` | 获取自行车列表 |
| GET | `/api/bicycles/{id}` | 获取自行车详情 |
| POST | `/api/rentals` | 创建租赁订单 |
| GET | `/api/rentals/my` | 获取我的租赁记录 |
| POST | `/api/rentals/{id}/end` | 结束租赁 |
| GET | `/api/forum/posts` | 获取帖子列表 |
| POST | `/api/forum/posts` | 发布帖子 |
| GET | `/api/forum/posts/{id}` | 获取帖子详情 |
| POST | `/api/forum/posts/{id}/comments` | 评论帖子 |
| POST | `/api/forum/posts/{id}/react` | 点赞/点踩 |
| GET | `/api/marketplace/listings` | 获取商品列表 |
| POST | `/api/marketplace/listings` | 发布商品 |
| GET | `/api/social/friends` | 获取好友列表 |
| POST | `/api/social/friend-requests` | 发送好友请求 |
| POST | `/api/social/friend-requests/{id}/accept` | 接受好友请求 |
| GET | `/api/notice` | 获取公告列表 |
| GET | `/api/ticket` | 获取工单列表 |
| POST | `/api/ticket` | 创建工单 |
| POST | `/api/ticket/{id}/messages` | 工单留言 |
| GET | `/api/activity` | 获取活动列表 |
| GET | `/api/activity/{id}` | 获取活动详情 |
| POST | `/api/activity/{id}/signup` | 报名活动 |
| POST | `/api/activity/{id}/message` | 活动留言 |

### 管理端 API (`/api/admin/*`)

| 方法 | 路由 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/role` | 修改用户角色 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |
| GET | `/api/admin/system/login-logs` | 登录日志 |
| GET | `/api/admin/system/stats` | 系统统计 |
| POST | `/api/admin/system/blacklist` | 添加 IP 黑名单 |
| GET | `/api/admin/forum/posts` | 所有帖子列表 |
| GET | `/api/admin/forum/posts/pending` | 待审核帖子 |
| POST | `/api/admin/forum/posts/{id}/approve` | 审核通过 |
| POST | `/api/admin/forum/posts/{id}/reject` | 审核驳回 |
| POST | `/api/admin/forum/posts/{id}/pin` | 置顶/取消置顶 |
| GET | `/api/admin/marketplace/listings` | 所有商品列表 |
| POST | `/api/admin/marketplace/listings/{id}/approve` | 审核通过 |
| POST | `/api/admin/marketplace/listings/{id}/reject` | 审核驳回 |
| GET | `/api/admin/notice` | 公告列表 |
| POST | `/api/admin/notice` | 创建公告 |
| PUT | `/api/admin/notice/{id}` | 更新公告 |
| DELETE | `/api/admin/notice/{id}` | 删除公告 |
| GET | `/api/admin/ticket` | 工单列表 |
| PUT | `/api/admin/ticket/{id}/status` | 更新工单状态 |
| POST | `/api/admin/ticket/{id}/reply` | 回复工单 |
| GET | `/api/admin/activity` | 活动列表 |
| POST | `/api/admin/activity` | 创建活动 |
| PUT | `/api/admin/activity/{id}` | 更新活动 |
| DELETE | `/api/admin/activity/{id}` | 删除活动 |
| GET | `/api/admin/activity/{id}/signups` | 报名列表 |

### WebSocket

| 端点 | 说明 |
|------|------|
| `/api/ws` | STOMP 协议 WebSocket 端点 |

## 常用命令

### 后端

```bash
cd bickdemo-backend

# 开发模式启动
mvn spring-boot:run

# 打包 (跳过测试)
mvn clean package -DskipTests

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassNameTest

# 查看依赖树
mvn dependency:tree

# 代码格式化 (需配置 spotless)
mvn spotless:apply
```

### 前端 (用户端/管理端)

```bash
cd bickdemo-frontend  # 或 bickdemo-admin

# 安装依赖
npm config set registry https://registry.npmmirror.com
npm install

# 开发模式
npm run dev

# 生产构建
npm run build

# 预览构建
npm run preview
```

### Docker 部署

```bash
# 一键部署 (在 script/prod 目录)
docker compose up -d --build

# 查看日志
docker compose logs -f app
docker compose logs -f frontend

# 重新构建单个服务
docker compose build --no-cache frontend

# 停止服务
docker compose down -v

# 强制重启
docker compose restart app
```

### Windows/PowerShell 提示

- 如果 `rg` (ripgrep) 被阻止，使用 PowerShell：`Get-ChildItem -Recurse -File | Select-String -Pattern "..."`
- 如果中文显示乱码：`Get-Content -Encoding UTF8 path/to/file`

## 核心架构

### 后端分层

```
Controller → Service → Mapper
    ↓           ↓          ↓
   DTO/Entity  业务逻辑   MyBatis
       ↓
   Annotation/Aspect (AOP)
```

- **Controller**: REST API 端点，统一返回格式 `{code, message, data}`
- **Service**: 业务逻辑，事务管理 (`@Transactional`)
- **Mapper**: MyBatis-Plus 数据访问接口
- **Entity**: 数据库实体，支持逻辑删除 (`deleted` 字段)
- **DTO**: 数据传输对象，用于请求/响应
- **Annotation**: 自定义注解 (如 `@Admin` 权限控制)
- **Aspect**: AOP 切面 (IP 限流、操作日志)

### 认证流程

```
1. 用户登录 → POST /api/auth/login
2. 后端验证 → 返回 JWT Token
3. 前端存储 Token (Pinia)
4. 请求拦截器 → 自动添加 Header: Authorization: Bearer {token}
5. 后端 JwtAuthenticationFilter → 验证 Token
6. Spring Security → 基于角色授权 (USER / ADMIN)
```

### 缓存策略

| 缓存类型 | 使用场景 | 清除方式 |
|----------|----------|----------|
| Redis | 全局缓存 (用户信息、统计等) | `@CacheEvict` |
| Caffeine | 热点数据 (自行车列表) | `@CacheEvict` |
| JVM 本地 | 临时数据 | TTL 过期 |

### IP 限流

- 基于 AOP (`AdminOperationLogAspect`)
- 未登录用户：60 次/分钟
- 已登录用户：60 次/分钟
- 封禁时长：15 分钟 (可配置)
- 黑名单：`IpBlacklist` entity

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 (未登录/Token 过期) |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 逻辑删除

所有实体支持逻辑删除：
- 删除时设置 `deleted = 1`
- 查询时 MyBatis-Plus 自动过滤 `deleted = 0`
- 配置：`mybatis-plus.global-config.db-config.logic-delete-field: deleted`

## 数据库

### 配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:bickdemo}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:change-me-db-password}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### 应用特定配置 (`app.*`)

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `app.mail.code-expire-minutes` | 邮箱验证码过期时间 | 10 分钟 |
| `app.gaode.map.api.key` | 高德地图 API Key | - |
| `app.redis.key-prefix` | Redis key 前缀 | bickdemo: |
| `app.rental.range-check.max-distance` | 租赁距离校验最大距离 | 10 km |
| `app.security.ip-control.max-request-per-minute` | IP 每分钟最大请求数 | 60 |
| `app.security.ip-control.ban-duration-minutes` | IP 封禁时长 | 15 分钟 |

### 数据库迁移

```bash
# 全量初始化
mysql -u root -p bickdemo < sql/init.sql
```

**重要规则**：
- SQL 迁移文件统一管理在 `sql/init.sql`
- 新增表或字段时，需要同步更新 `sql/init.sql`
- 已有数据库可单独执行 ALTER 语句

### 常见字段补充

```sql
-- 自行车库存字段
ALTER TABLE bicycles ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量（库存）';

-- 租赁数量字段
ALTER TABLE rentals ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '租赁数量';

-- 活动地点区级代码
ALTER TABLE activities ADD COLUMN location_code VARCHAR(20) DEFAULT '' COMMENT '活动地点区级代码';
```

### 核心表结构

| 表名 | 说明 |
|------|------|
| `users` | 用户表 |
| `bicycles` | 自行车表 |
| `rentals` | 租赁记录表 |
| `forum_posts` | 论坛帖子表 |
| `forum_post_comments` | 帖子评论表 |
| `forum_post_reactions` | 帖子反应表 |
| `marketplace_listings` | 二手商品表 |
| `friendships` | 好友关系表 |
| `chat_messages` | 聊天消息表 |
| `activities` | 活动表 |
| `activity_signups` | 活动报名表 |
| `notices` | 系统公告表 |
| `tickets` | 工单表 |
| `login_logs` | 登录日志表 |
| `operation_logs` | 操作日志表 |
| `ip_blacklists` | IP 黑名单表 |

## Docker 部署

### 服务组成

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | bickdemo-mysql | 3306 | 数据库 |
| Backend | bickdemo-app | 8080 | Spring Boot 后端 |
| Frontend | bickdemo-frontend | 80 | Nginx (用户端) |
| Admin | bickdemo-admin | 5174 | Nginx (管理端) |
| RabbitMQ | rabbitmq | 5672, 15672 | 消息队列 |
| Redis | redis | 6379 | 缓存 |
| MinIO | minio | 9000, 9001 | 对象存储 |

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | localhost | MySQL 主机 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_DATABASE` | bickdemo | 数据库名 |
| `MYSQL_USERNAME` | root | MySQL 用户名 |
| `MYSQL_PASSWORD` | change-me-db-password | MySQL 密码 |
| `JWT_SECRET` | change-me-jwt-secret... | JWT 签名密钥 |
| `MINIO_ENDPOINT` | http://localhost:9000 | MinIO 端点 |
| `MINIO_ACCESS_KEY` | change-me-minio-access-key | MinIO 访问密钥 |
| `MINIO_SECRET_KEY` | change-me-minio-secret-key | MinIO 密钥 |
| `MINIO_BUCKET` | bicycles | MinIO 存储桶名 |
| `REDIS_HOST` | localhost | Redis 主机 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `RABBITMQ_HOST` | localhost | RabbitMQ 主机 |
| `RABBITMQ_USERNAME` | guest | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | guest | RabbitMQ 密码 |
| `MAIL_HOST` | smtp.qq.com | 邮件服务器 |
| `MAIL_USERNAME` | (空) | 发件人邮箱 |
| `MAIL_PASSWORD` | (空) | SMTP 授权码 |

**安全建议**：生产环境部署前，务必将默认密码和密钥修改为强密码。

### Nginx 配置要点

```nginx
location /api {
    proxy_pass http://bickdemo-app:8080;
    client_max_body_size 50M;  # 上传大小限制
}

location /ws {
    proxy_pass http://bickdemo-app:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 用户 | user | user123 |

## 模块详解

### 活动管理 (Activity)

**实体**：`Activity`, `ActivitySignup`, `ActivityMessage`

**功能**：
- 创建/编辑/删除骑行活动
- 活动报名 (需审核)
- 活动留言与管理员回复
- 自动结束活动 (定时任务)

**状态**：`DRAFT` → `PUBLISHED` → `COMPLETED` / `CANCELLED`

**难度等级**：`EASY`, `MODERATE`, `HARD`, `EXTREME`

**地点存储**：
- `location`: 路径文本，如 "北京市/市辖区/朝阳区"
- `locationCode`: 区级代码，如 `110101`

**报名截止**：`signupDeadline` 字段控制

### 社区论坛 (Forum)

**实体**：`ForumPost`, `ForumPostComment`, `ForumPostReaction`, `ForumPostImage`

**功能**：
- 发帖/删帖/查看帖子列表
- 评论/回复评论
- 点赞/点踩 reactions
- 多图上传 (MinIO)
- 管理员审核/置顶

**帖子状态**：`PUBLISHED`, `DELETED`

**反应类型**：`LIKE`, `DISLIKE`

### 二手市场 (Marketplace)

**实体**：`MarketplaceListing`, `MarketplaceApplication`

**功能**：
- 发布闲置物品
- 物品审核 (管理员)
- 申请购买/联系卖家
- 配送方式：自提/快递/面交

**物品状态**：`AVAILABLE`, `SOLD`, `DELETED`

**审核状态**：`PENDING`, `APPROVED`, `REJECTED`

### 社交 (Social)

**实体**：`Friendship`, `FriendRequest`, `ChatMessage`

**功能**：
- 好友请求/接受/拒绝
- 好友列表管理
- 实时聊天 (WebSocket + RabbitMQ)

**消息类型**：`TEXT`, `IMAGE`, `SYSTEM`

### 租赁 (Rental)

**实体**：`Rental`

**功能**：
- 创建租赁订单
- 开始/结束租赁
- 计费 (按小时)
- 位置校验 (检查是否超出范围)

**状态**：`ACTIVE`, `COMPLETED`, `CANCELLED`

### 定时任务

| 任务 | 说明 | 表达式 |
|------|------|--------|
| `ActivitySchedulerService` | 自动结束已过期活动 | 每 5 分钟 |
| `RentalLocationGuardService` | 检查租赁位置异常 | 实时 |

## UI 设计规范

### 玻璃模糊效果

```css
backdrop-filter: blur(12px) saturate(180%);
```

**应用场景**：
- 导航栏背景
- Tag 标签
- ThemeToggle 按钮
- 卡片浮层

### 响应式断点

| 断点 | 效果 |
|------|------|
| ≤480px | 超小屏幕 |
| ≤768px | 手机端 |
| ≤1050px | 显示汉堡菜单，隐藏导航链接 |
| ≤1300px | 导航文字隐藏，只显示图标 |
| >1300px | 完整导航显示 |

### ThemeToggle 组件

主题切换按钮使用胶囊形状 + 玻璃模糊效果：
- 圆角胶囊：`border-radius: 999px`
- 背景：`color-mix(in srgb, var(--bs-surface-solid) 88%, transparent)`
- 透明度：0.95
- 无悬停动画效果

### 通用分页组件 (PageSizeDropdown)

```vue
<PageSizeDropdown v-model="pageSize" @change="loadData" />
```

支持 `page-sizes` 属性自定义选项数组。

## Jenkins 部署 (server2)

**Jenkins 地址**：http://124.221.113.208:8081

**Job 名称**：`bike-deploy`

**构建流程**：
1. Gitea 拉取代码
2. 构建前端 (npm install + vite build)
3. 构建后端 (mvn package)
4. 构建 Docker 镜像
5. docker-compose up -d 部署

**相关服务容器**：
- `bike-deploy-app-1` - 后端
- `bike-deploy-frontend-1` - 用户端
- `bike-deploy-admin-1` - 管理端
- `bike-deploy-mysql-1` - MySQL
- `rabbitmq` - RabbitMQ
