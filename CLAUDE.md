# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

自行车租赁系统 (BikeShare) - 基于 **Spring Boot 3.2.0 + Vue 3.4.0** 的全栈应用，涵盖自行车租赁、社区论坛、二手市场、社交聊天、活动管理等功能。

## 技术栈

**后端:**
- Spring Boot 3.2.0, Spring Security 6.x, JWT
- MyBatis-Plus 3.5.5, MySQL 8.0, HikariCP
- MinIO (对象存储), Redis (缓存), RabbitMQ (消息队列)
- WebSocket (实时聊天), Caffeine (本地缓存)

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
│   │   ├── aspect/            # AOP 切面 (IP 限流、操作日志)
│   │   ├── component/         # 初始化组件 (MinIO、数据初始化)
│   │   ├── config/            # 安全/JWT/MinIO/RabbitMQ/WebSocket 配置
│   │   ├── controller/        # REST API (按模块分组)
│   │   ├── dto/               # 数据传输对象
│   │   ├── entity/            # MyBatis-Plus 实体 (支持逻辑删除)
│   │   ├── exception/         # 全局异常处理
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
├── bickdemo-admin/            # Vue 3 管理端前端
├── script/                    # 部署脚本
└── sql/
    └── init.sql               # 数据库初始化脚本
```

**后端 Controller 模块分组：**
| 模块 | Controller | 说明 |
|------|-----------|------|
| 认证 | `AuthController` | 登录/注册/邮箱验证/密码重置 |
| 自行车 | `BicycleController` | 自行车 CRUD |
| 租赁 | `RentalController` | 租赁订单/计费 |
| 论坛 | `ForumController` | 帖子/评论/点赞 |
| 市场 | `MarketplaceController` | 二手物品交易 |
| 社交 | `SocialController` | 好友/聊天/好友请求 |
| 公告 | `NoticeController` | 系统公告 |
| 工单 | `TicketController` | 用户反馈 |
| 活动 | `ActivityController` | 骑行活动 |
| 管理后台 | `Admin*Controller` | 各模块管理端 API |
| 系统 | `AdminSystemController` | 系统日志/黑名单/统计 |

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
| `/api/auth` | 认证相关 (登录/注册/邮箱验证/密码重置) |
| `/api/bicycles` | 自行车相关 |
| `/api/rentals` | 租赁订单相关 |
| `/api/statistics` | 统计数据 |
| `/api/files` | 文件上传 |
| `/api/backgrounds` | 背景图管理 |
| `/api/forum` | 社区帖子/评论/ Reactions |
| `/api/marketplace` | 二手交易市场 |
| `/api/social` | 好友/聊天/好友请求 |
| `/api/notice` | 系统公告 |
| `/api/ticket` | 工单/反馈 |
| `/api/activity` | 骑行活动 |
| `/api/ws` | WebSocket 实时聊天 |

**管理端 API (`/api/admin/*`)：**
| 路由 | 说明 |
|------|------|
| `/api/admin/users` | 用户管理 |
| `/api/admin/system` | 系统日志/黑名单/IP 封禁 |
| `/api/admin/marketplace` | 市场审核 |
| `/api/admin/forum` | 论坛管理 |
| `/api/admin/notice` | 公告管理 |
| `/api/admin/ticket` | 工单管理 |
| `/api/admin/activity` | 活动管理 |

### 认证流程

1. 用户登录 → `/api/auth/login` → 返回 JWT Token
2. 前端存储 Token 到 Pinia
3. 请求拦截器自动添加 `Authorization: Bearer {token}`
4. 后端 `JwtAuthenticationFilter` 验证 Token
5. Spring Security 基于角色授权 (USER/ADMIN)
6. 支持邮箱验证码登录 (`/api/auth/email-login`)
7. 支持邮箱验证码重置密码 (`/api/auth/reset-password`)
8. 登录日志记录 (`LoginLog` entity)

### 实时通信

**WebSocket (`/api/ws`)：**
- 基于 STOMP 协议的 WebSocket 端点
- 用于实时聊天 (`SocialService` 发布聊天消息事件)
- 聊天消息通过 RabbitMQ 异步处理

**RabbitMQ 消息队列：**
- `social.events` 队列：处理聊天消息、好友请求等社交事件
- `RentalLocationGuardService`：监听租赁位置相关事件

### 缓存策略

- **Redis**：全局缓存 (`spring.cache.type=redis`)
- **Caffeine**：本地缓存（如热点数据）
- `JwtService`：Token 黑名单/有效期管理
- `IpBlacklistService`：IP 黑名单封禁

### IP 限流

- 基于 AOP (`AdminOperationLogAspect`)
- 未登录用户：60 次/分钟
- 已登录用户：60 次/分钟
- 封禁时长：15 分钟 (可配置)
- 封禁记录：`IpBlacklist` entity，`VisitLog` 记录访问日志

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

### 逻辑删除

所有实体支持逻辑删除（`deleted` 字段）：
- 删除时设置 `deleted = 1`
- 查询时自动过滤 `deleted = 0`
- MyBatis-Plus 配置：`logic-delete-field: deleted`

## 数据库配置

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
```

### 应用特定配置 (`app.*`)

| 配置 | 说明 |
|------|------|
| `app.mail.code-expire-minutes` | 邮箱验证码过期时间 (默认 10 分钟) |
| `app.gaode.map.api.key` | 高德地图 API Key (用于经纬度反查地区) |
| `app.redis.key-prefix` | Redis key 前缀 |
| `app.rental.range-check.*` | 租赁距离校验 (最大距离 10km) |
| `app.security.ip-control.*` | IP 限流配置 |

### 数据库迁移

当项目升级或拉取新代码后，如遇到字段或表不存在的问题，需要执行对应的迁移脚本。

```bash
# 全量初始化
mysql -u root -p bickdemo < sql/init.sql
```

**重要规则：**
- SQL 迁移文件统一管理在 `sql/init.sql`
- 新增表或字段时，需要同步更新 `sql/init.sql`

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
| Frontend | bickdemo-frontend | 80 | Nginx (用户端) |
| Admin | bickdemo-admin | 5174 | Nginx (管理端) |

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

## 活动管理模块

### 活动地点省市区选择器

管理端活动表单中的活动地点使用省市区三级联动选择器：

**前端实现：**
- 使用 `element-china-area-data` npm 包
- Element Plus `el-cascader` 组件
- 数据源：`regionData`（包含真实行政区划代码）

**后端存储：**
- `location`: 路径文本，如 "北京市/市辖区/朝阳区"
- `locationCode`: 区级代码，如 `110101`

### 活动报名截止

- `Activity` 实体包含 `signupDeadline` 字段
- 报名截止后，用户无法再报名参加活动
- `ActivitySignup` 实体记录报名信息，包含 `SignupStatus` (PENDING/CONFIRMED/CANCELLED)

**数据库迁移：**
```sql
ALTER TABLE activities ADD COLUMN location_code VARCHAR(20) DEFAULT '' COMMENT '活动地点区级代码';
```

### 枚举值对照

**难度等级 (ActivityDifficulty)：**
| 前端值 | 说明 |
|--------|------|
| `EASY` | 简单 |
| `MEDIUM` | 中等 |
| `HARD` | 困难 |

**活动状态 (ActivityStatus)：**
| 前端值 | 说明 |
|--------|------|
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已发布 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |

## 社区论坛模块 (Forum)

**实体：** `ForumPost`, `ForumPostComment`, `ForumPostReaction`, `ForumPostImage`

**功能：**
- 发帖/删帖/查看帖子列表
- 评论/回复评论
- 点赞/点踩 reactions
- 多图上传

**枚举值：**
- `ForumPostStatus`: PUBLISHED, DELETED
- `ForumReactionType`: LIKE, DISLIKE

## 二手市场模块 (Marketplace)

**实体：** `MarketplaceListing`, `MarketplaceApplication`

**功能：**
- 发布闲置物品 (`MarketplaceDeliveryMode`: 自提/快递/面交)
- 物品审核 (`MarketplaceReviewStatus`: PENDING/APPROVED/REJECTED)
- 申请购买/联系卖家

**枚举值：**
- `MarketplaceListingStatus`: AVAILABLE, SOLD, DELETED
- `MarketplaceApplicationStatus`: PENDING, ACCEPTED, REJECTED, CANCELLED

## 社交模块 (Social)

**实体：** `Friendship`, `FriendRequest`, `ChatMessage`

**功能：**
- 好友请求/接受/拒绝
- 好友列表管理
- 实时聊天 (WebSocket + RabbitMQ)
- `ChatMessageType`: TEXT, IMAGE, SYSTEM

## UI 设计规范

### 玻璃模糊效果

项目中多处使用玻璃态（glassmorphism）设计：

```css
backdrop-filter: blur(12px) saturate(180%);
```

**应用场景：**
- 导航栏背景
- Tag 标签
- 按钮（如 ThemeToggle 主题切换按钮）
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
- 透明度：0.95（微弱透明）
- 无悬停动画效果
