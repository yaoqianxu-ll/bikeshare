# BikeShare 自行车租赁系统

> 基于 Spring Boot 3 + Vue 3 的前后端分离单车租赁平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4.0-4FC08D?logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.5.0-409EFF?logo=element)](https://element-plus.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

## 目录

- [项目概述](#项目概述)
- [项目亮点](#项目亮点)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [功能详解](#功能详解)
- [页面路由](#页面路由)
- [API 接口](#api-接口)
- [数据库设计](#数据库设计)
- [配置说明](#配置说明)
- [常用命令](#常用命令)
- [常见问题](#常见问题)
- [安全建议](#安全建议)
- [CI/CD 部署](#cicd-部署)
- [开发贡献](#开发贡献)

---

## 项目概述

### 项目简介

BikeShare 是一个完整的自行车租赁管理系统，涵盖用户租车、后台管理、社交互动、论坛交流等核心功能。采用前后端分离架构，支持 Docker 一键部署。

本项目旨在提供一个**功能完整、架构清晰、易于部署**的单车租赁平台解决方案，适用于：
- 🎓 **毕业设计/课程设计** - 功能完善，文档齐全
- 💼 **企业项目参考** - 规范的分层架构，可快速二次开发
- 📚 **学习实战** - 涵盖主流技术栈，适合全栈学习

### 基本信息

| 项目 | 说明 |
|------|------|
| 项目名称 | BikeShare 自行车租赁系统 |
| 开发语言 | Java 17 + JavaScript |
| 前端框架 | Vue 3.4 + Vite 5 |
| 后端框架 | Spring Boot 3.2 |
| 数据库 | MySQL 8.0 |
| 缓存中间件 | Redis 6.x |
| 消息队列 | RabbitMQ 3.x |
| 对象存储 | MinIO 8.x |
| 部署方式 | Docker / 本地部署 |
| 许可证 | MIT License |

### 设计目标

- **功能完整性** - 覆盖租车业务的全流程，从用户注册、租车、归还到后台管理
- **技术先进性** - 采用主流技术栈，Spring Boot 3 + Vue 3 最新组合
- **架构规范性** - 清晰的分层架构，Controller → Service → Mapper
- **易部署性** - 支持 Docker 一键部署，降低部署门槛
- **可扩展性** - 模块化设计，便于功能扩展和二次开发

---

## 项目亮点

### 技术架构亮点

| 亮点 | 说明 |
|------|------|
| 🚀 **Spring Boot 3** | 采用最新一代 Spring Boot 框架，性能更优，支持 Java 17+ 新特性 |
| 🎯 **Vue 3 Composition API** | 使用 Composition API 编写，代码组织更清晰，逻辑复用更方便 |
| 🔐 **JWT + Spring Security** | 无状态认证，支持 Token 自动刷新，实现无感登录 |
| 📦 **MyBatis-Plus** | 强大的 ORM 框架，支持逻辑删除、分页查询、代码生成 |
| 🗄️ **HikariCP** | 高性能数据库连接池，快速响应数据库请求 |
| ⚡ **WebSocket** | 实时双向通信，支持聊天消息即时推送 |
| 📨 **RabbitMQ** | 异步消息队列，削峰填谷，提升系统稳定性 |
| 💾 **MinIO** | 分布式对象存储，支持图片上传、存储、访问 |
| 🔥 **Redis** | 多级缓存策略，提升系统性能，支持验证码、会话存储 |
| 📊 **ECharts 6** | 强大数据可视化库，展示租赁统计、用户分析图表 |

### 业务功能亮点

| 亮点 | 说明 |
|------|------|
| 📧 **邮箱验证码** | 安全的注册/找回密码机制，验证码 10 分钟有效期 |
| 🔄 **无感登录** | JWT Token 自动刷新，用户操作无感知，体验流畅 |
| 📸 **头像裁剪** | 前端图片裁剪 + 自动压缩，节省存储空间 |
| 🚲 **实时库存** | 车辆库存实时显示，避免超卖问题 |
| 💬 **实时聊天** | WebSocket + RabbitMQ 双重保障，消息必达 |
| 🏷️ **好友备注** | 支持好友备注名，管理好友更方便 |
| 📝 **图文发帖** | 论坛支持多图上传，富文本编辑 |
| 🔍 **智能筛选** | 车辆、帖子、订单支持多维度筛选搜索 |
| 📈 **数据看板** | 实时展示订单、用户、营收等核心指标 |
| 🔒 **内容审核** | 论坛帖子审核机制，保障内容合规 |

### 安全特性亮点

| 亮点 | 说明 |
|------|------|
| 🛡️ **Spring Security** | 强大的安全框架，支持基于角色的权限控制 |
| 🔑 **JWT 双密钥** | 支持当前密钥和旧密钥，Token 刷新无缝切换 |
| 🚫 **IP 限流** | 基于 AOP 的限流机制，防止恶意请求 |
| 📝 **操作日志** | 记录所有管理操作，便于审计追溯 |
| 📋 **登录日志** | 记录登录时间、IP、设备，异常登录可追溯 |
| ⚠️ **黑名单机制** | 支持将违规用户加入黑名单，限制访问 |
| 🔒 **密码加密** | BCrypt 强哈希算法，密码安全存储 |
| 🚫 **逻辑删除** | 数据软删除，避免误删导致数据丢失 |

### 开发体验亮点

| 亮点 | 说明 |
|------|------|
| 📦 **Docker 部署** | 一键启动所有服务，开发/生产环境一致 |
| 🔧 **热重载** | 后端支持热重载，前端支持热更新，开发效率高 |
| 📝 **统一返回** | 统一 API 响应格式 `{code, message, data}` |
| ⚠️ **全局异常** | 统一异常处理，友好的错误提示 |
| 📋 **数据验证** | 前后端双重验证，数据更安全 |
| 🧪 **单元测试** | 核心业务编写单元测试，保障代码质量 |
| 📖 **文档齐全** | README、API 文档、部署文档完整 |
| 🎨 **UI 美观** | Element Plus 组件库，界面美观统一 |

### 性能优化亮点

| 亮点 | 说明 |
|------|------|
| ⚡ **懒加载** | 后端延迟初始化，前端路由懒加载 |
| 💾 **多级缓存** | Caffeine 本地缓存 + Redis 分布式缓存 |
| 📦 **连接池** | HikariCP 高性能数据库连接池 |
| 🔄 **异步处理** | 邮件发送、消息推送异步执行 |
| 📊 **分页查询** | MyBatis-Plus 分页插件，大数据友好 |
| 🖼️ **图片压缩** | 前端压缩 + 后端压缩，节省带宽和存储 |

---

## 功能特性清单

#### 用户系统

| 功能 | 描述 | 状态 |
|------|------|------|
| 邮箱注册 | 发送验证码到邮箱完成注册 | ✅ |
| 邮箱登录 | 使用邮箱 + 密码登录 | ✅ |
| 验证码机制 | 6 位数字验证码，10 分钟有效期 | ✅ |
| JWT 认证 | Token 有效期 24 小时，支持自动刷新 | ✅ |
| 无感登录 | Token 自动刷新，用户无感知 | ✅ |
| 找回密码 | 通过邮箱验证码重置密码 | ✅ |
| 个人资料 | 修改昵称、性别、简介 | ✅ |
| 头像上传 | 支持裁剪、自动压缩 | ✅ |
| 修改密码 | 登录后修改登录密码 | ✅ |
| 退出登录 | 清除 Token，安全退出 | ✅ |

#### 租车服务

| 功能 | 描述 | 状态 |
|------|------|------|
| 车辆列表 | 分页展示所有可用车辆 | ✅ |
| 车辆筛选 | 按价格、状态、类型筛选 | ✅ |
| 车辆搜索 | 关键词搜索车辆名称 | ✅ |
| 车辆详情 | 查看车辆图片、描述、价格、库存 | ✅ |
| 实时库存 | 显示当前可用车辆数量 | ✅ |
| 创建订单 | 选择数量、确认租赁 | ✅ |
| 租赁计费 | 按时计费，自动计算总金额 | ✅ |
| 提前还车 | 支持提前结束订单 | ✅ |
| 订单列表 | 查看我的所有租赁记录 | ✅ |
| 订单详情 | 查看订单时间、金额、状态 | ✅ |
| 订单状态 | 进行中/已完成/已取消 | ✅ |
| 租赁记录导出 | 支持 Excel 导出 | ✅ |

#### 社交聊天

| 功能 | 描述 | 状态 |
|------|------|------|
| 搜索用户 | 通过用户名/邮箱搜索 | ✅ |
| 好友申请 | 发送好友申请，附带消息 | ✅ |
| 好友管理 | 查看好友列表，设置备注 | ✅ |
| 删除好友 | 解除好友关系 | ✅ |
| 实时聊天 | WebSocket 实时消息推送 | ✅ |
| 消息历史 | 查看与好友的聊天记录 | ✅ |
| 离线消息 | 离线消息存储，上线后推送 | ✅ |
| 已读标记 | 显示消息已读/未读状态 | ✅ |
| 在线状态 | 显示好友在线/离线状态 | ✅ |

#### 论坛社区

| 功能 | 描述 | 状态 |
|------|------|------|
| 帖子列表 | 分页展示所有帖子 | ✅ |
| 帖子分类 | 按分类筛选帖子 | ✅ |
| 帖子搜索 | 关键词搜索帖子 | ✅ |
| 创建帖子 | 发布图文混排帖子 | ✅ |
| 多图上传 | 单帖支持多张图片 | ✅ |
| 编辑帖子 | 修改自己的帖子 | ✅ |
| 删除帖子 | 删除自己的帖子 | ✅ |
| 帖子详情 | 查看帖子完整内容和评论 | ✅ |
| 发表评论 | 回复帖子，支持楼中楼 | ✅ |
| 删除评论 | 删除不当评论 | ✅ |
| 点赞功能 | 为喜欢的帖子点赞 | ✅ |
| 收藏功能 | 收藏感兴趣的帖子 | ✅ |
| 我的帖子 | 查看自己发布的帖子 | ✅ |
| 我的收藏 | 查看自己收藏的帖子 | ✅ |
| 帖子审核 | 管理员审核新发帖 | ✅ |

#### 数据统计

| 功能 | 描述 | 状态 |
|------|------|------|
| 今日订单 | 显示今日订单数量 | ✅ |
| 今日营收 | 显示今日营业收入 | ✅ |
| 活跃用户 | 显示今日活跃用户数 | ✅ |
| 车辆使用率 | 显示车辆使用情况 | ✅ |
| 租赁统计 | 按日/周/月统计租赁数据 | ✅ |
| 用户分析 | 新增用户、活跃用户趋势 | ✅ |
| 营收趋势 | 营业收入变化趋势图 | ✅ |
| 热门车辆 | 最受欢迎车辆排行 | ✅ |
| ECharts 图表 | 可视化数据展示 | ✅ |

#### 后台管理

| 功能 | 描述 | 状态 |
|------|------|------|
| 数据看板 | 核心数据概览（订单/用户/营收） | ✅ |
| 车辆管理 | 添加/编辑/删除车辆 | ✅ |
| 车辆导入 | 批量导入车辆（Excel） | ✅ |
| 车辆导出 | 批量导出车辆数据 | ✅ |
| 库存管理 | 调整车辆库存数量 | ✅ |
| 车辆上下架 | 控制车辆是否可租 | ✅ |
| 租赁管理 | 查看所有订单，支持筛选 | ✅ |
| 用户管理 | 查看用户列表，禁用用户 | ✅ |
| 黑名单管理 | 添加/移除黑名单用户 | ✅ |
| 论坛审核 | 审核帖子（通过/拒绝/删除） | ✅ |
| 集市审核 | 审核集市交易 | ✅ |
| 背景图管理 | 上传/排序首页轮播图 | ✅ |
| 登录日志 | 查看用户登录记录（IP/设备） | ✅ |
| 操作日志 | 记录管理员操作 | ✅ |
| 访客日志 | 记录页面访问情况 | ✅ |

### 技术特性

| 特性 | 说明 |
|------|------|
| 前后端分离 | 前端 Vue 3 + 后端 Spring Boot 3 |
| RESTful API | 统一接口规范，返回统一格式 |
| JWT 认证 | 无状态 Token 鉴权，支持自动刷新 |
| 文件上传 | MinIO 对象存储，支持图片压缩 |
| WebSocket | 实时双向通信，支持消息推送 |
| Redis 缓存 | 验证码/会话/热点数据缓存 |
| 数据库连接池 | HikariCP 高性能连接池 |
| 逻辑删除 | 支持数据软删除 |
| 分页查询 | MyBatis-Plus 分页插件 |
| 全局异常处理 | 统一异常捕获与返回 |
| IP 限流 | 基于 AOP 的请求限流 |
| CORS 跨域 | 支持跨域请求 |
| Docker 部署 | 支持容器化一键部署 |
| CI/CD | Jenkins 自动化构建部署 |

### 演示账号

| 角色 | 用户名 | 密码 | 权限说明 |
|------|--------|------|----------|
| 管理员 | admin | admin123 | 全部权限（车辆管理、审核、统计） |
| 普通用户 | user | user123 | 用户权限（租车、聊天、发帖） |

> ⚠️ **安全提醒**：首次部署后请立即修改默认密码！

---

## 环境要求

| 软件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 17 | 17+ | 必须，Spring Boot 3 要求 |
| Maven | 3.8 | 3.9+ | 后端构建工具 |
| Node.js | 16 | 18+ LTS | 前端运行环境 |
| npm | 8 | 9+ | 前端包管理 |
| MySQL | 8.0 | 8.0+ | 主数据库 |
| Redis | 5.0 | 6.x | 缓存/验证码/会话 |
| RabbitMQ | 3.8 | 3.11+ | 消息队列/聊天事件 |
| Docker | 20.10 | 24+ | 容器化部署（可选） |

> 💡 **提示**：
> - 推荐使用 JDK 17（LTS 版本），Node.js 18 LTS 可获得最佳兼容性
> - 如仅需测试基础租车功能，可先只配置 MySQL，Redis/MinIO/邮箱服务可后续补充
> - Windows 用户建议使用 Git Bash 或 WSL2 执行 Linux 命令

---

## 快速开始

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
      password: your_redis_password  # 如有密码

# MinIO 配置（图片上传功能需要）
minio:
  endpoint: http://localhost:9000
  accessKey: your_access_key
  secretKey: your_secret_key
  bucketName: bickdemo

# 邮箱配置（验证码功能需要）
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your_email@qq.com
    password: your_smtp_auth_code
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 4. 启动后端

```bash
cd bickdemo-backend

# 方式 1：Maven 直接运行（支持热重载）
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

#### 5. 启动前端（用户端）

```bash
cd bickdemo-frontend

# 安装依赖（建议使用淘宝镜像）
npm config set registry https://registry.npmmirror.com
npm install

# 启动开发服务器（自动热重载）
npm run dev
```

启动成功后，访问前端地址：http://localhost:5173

#### 6. 启动管理端（可选）

```bash
cd bickdemo-admin

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

启动成功后，访问管理端地址：http://localhost:3000

> 💡 **提示**：
> - 开发模式下，前端会自动将 `/api` 请求代理到后端 `http://localhost:8080`
> - 管理端需要管理员账号（admin/admin123）才能登录

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
REDIS_PASSWORD=your_redis_password
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
| 前端页面 | http://localhost | Nginx 反向代理（用户端） |
| 管理端 | http://localhost:3001 | Nginx 反向代理（管理后台） |
| 后端 API | http://localhost:8080 | Spring Boot |
| MySQL | localhost:3306 | 数据库（建议不暴露外网） |
| MinIO | http://localhost:9001 | 对象存储控制台 |
| RabbitMQ | localhost:15672 | 管理控制台 (guest/guest) |

#### 5. 停止与清理

```bash
# 停止所有服务
docker compose -f script/prod/docker-compose.yml down

# 停止并删除数据卷（谨慎使用，会删除数据）
docker compose -f script/prod/docker-compose.yml down -v
```

---

## 技术架构

### 整体架构图

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
| JWT (jjwt) | 0.11.5 | 无状态 Token 鉴权 |
| MyBatis-Plus | 3.5.5 | ORM 框架 / 分页插件 |
| MySQL Connector | 8.x | MySQL 驱动 |
| Redis | 6.x | 缓存/验证码/会话存储 |
| RabbitMQ | 3.x | 异步消息队列/聊天事件分发 |
| WebSocket | - | 实时双向通信 |
| MinIO | 8.5.7 | 分布式对象存储 |
| Spring Mail | - | 邮件发送服务 |
| HikariCP | - | 高性能数据库连接池 |
| Caffeine | - | 本地缓存 |

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
| @stomp/stompjs | 7.2.0 | WebSocket 消息协议 |
| sockjs-client | 1.6.1 | WebSocket 降级兼容 |

### 认证流程

```
用户登录 ──► /api/auth/login ──► 返回 JWT Token
                │
                ▼
        前端存储 Token (Pinia)
                │
                ▼
        请求拦截器添加 Header
        Authorization: Bearer {token}
                │
                ▼
        JwtAuthenticationFilter 验证
                │
                ▼
        Spring Security 授权 (USER/ADMIN)
```

---

## 项目结构

```
bickdemo/
│
├── bickdemo-backend/                    # Spring Boot 后端模块
│   ├── src/main/java/com/example/bickdemo/
│   │   ├── BickdemoApplication.java     # 启动类
│   │   │
│   │   ├── config/                      # 配置类
│   │   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   │   ├── JwtConfig.java           # JWT 相关配置
│   │   │   ├── WebSocketConfig.java     # WebSocket 配置
│   │   │   ├── RedisConfig.java         # Redis 配置
│   │   │   ├── MinioConfig.java         # MinIO 配置
│   │   │   └── CorsConfig.java          # 跨域配置
│   │   │
│   │   ├── controller/                  # REST API 控制器
│   │   │   ├── AuthController.java      # 认证接口（登录/注册/找回密码）
│   │   │   ├── BicycleController.java   # 自行车接口（列表/详情/管理）
│   │   │   ├── RentalController.java    # 租赁接口（下单/归还/记录）
│   │   │   ├── UserController.java      # 用户接口（资料/头像）
│   │   │   ├── FriendController.java    # 好友接口（添加/删除/列表）
│   │   │   ├── ChatController.java      # 聊天接口（历史/发送）
│   │   │   ├── ForumController.java     # 论坛接口（帖子/评论/点赞）
│   │   │   └── AdminController.java     # 管理接口（统计/审核）
│   │   │
│   │   ├── service/                     # 业务逻辑层
│   │   │   ├── AuthService.java         # 认证服务
│   │   │   ├── BicycleService.java      # 自行车服务
│   │   │   ├── RentalService.java       # 租赁服务
│   │   │   ├── UserService.java         # 用户服务
│   │   │   ├── FriendService.java       # 好友服务
│   │   │   ├── ChatService.java         # 聊天服务
│   │   │   ├── ForumService.java        # 论坛服务
│   │   │   └── StatService.java         # 统计服务
│   │   │
│   │   ├── mapper/                      # MyBatis Mapper
│   │   ├── entity/                      # JPA 实体类
│   │   ├── dto/                         # 数据传输对象
│   │   │   ├── request/                 # 请求 DTO
│   │   │   └── response/                # 响应 DTO
│   │   │
│   │   ├── exception/                   # 异常处理
│   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   └── ApiException.java        # API 异常
│   │   │
│   │   ├── component/                   # 组件
│   │   │   ├── DataInitializer.java     # 数据初始化
│   │   │   └── JwtAuthenticationFilter.java  # JWT 过滤器
│   │   │
│   │   ├── aspect/                      # AOP 切面
│   │   │   └── RateLimitAspect.java     # 限流切面
│   │   │
│   │   └── util/                        # 工具类
│   │       ├── JwtUtil.java             # JWT 工具
│   │       ├── RedisUtil.java           # Redis 工具
│   │       └── MinioUtil.java           # MinIO 工具
│   │
│   ├── src/main/resources/
│   │   ├── application.yml              # 开发环境配置
│   │   ├── application-prod.yml         # 生产环境配置
│   │   └── mapper/                      # MyBatis XML 映射文件
│   │
│   ├── init-db/                         # Docker 初始化脚本
│   │   └── init.sql
│   ├── Dockerfile                       # 后端 Docker 配置
│   └── pom.xml                          # Maven 依赖配置
│
├── bickdemo-frontend/                   # Vue 3 用户端模块
│   ├── src/
│   │   ├── api/                         # API 接口封装
│   │   │   ├── auth.js                  # 认证接口
│   │   │   ├── bicycle.js               # 自行车接口
│   │   │   ├── rental.js                # 租赁接口
│   │   │   ├── user.js                  # 用户接口
│   │   │   ├── friend.js                # 好友接口
│   │   │   ├── chat.js                  # 聊天接口
│   │   │   └── forum.js                 # 论坛接口
│   │   │
│   │   ├── components/                  # 公共组件
│   │   │   ├── Layout/                  # 布局组件
│   │   │   ├── BicycleCard/             # 自行车卡片
│   │   │   ├── ChatBox/                 # 聊天窗口
│   │   │   └── ImageUpload/             # 图片上传
│   │   │
│   │   ├── router/                      # 路由配置
│   │   │   └── index.js
│   │   │
│   │   ├── stores/                      # Pinia 状态管理
│   │   │   ├── user.js                  # 用户状态
│   │   │   ├── auth.js                  # 认证状态
│   │   │   └── app.js                   # 应用状态
│   │   │
│   │   ├── views/                       # 页面组件
│   │   │   ├── Home/                    # 首页
│   │   │   ├── Bicycles/                # 自行车列表
│   │   │   ├── Rentals/                 # 租赁记录
│   │   │   ├── Friends/                 # 好友页面
│   │   │   ├── Forum/                   # 论坛页面
│   │   │   ├── Statistics/              # 统计页面
│   │   │   └── Profile/                 # 个人资料
│   │   │
│   │   ├── utils/                       # 工具函数
│   │   ├── App.vue                      # 根组件
│   │   └── main.js                      # 入口文件
│   │
│   ├── public/                          # 静态资源
│   ├── nginx.conf                       # Nginx 配置
│   ├── Dockerfile                       # 前端 Docker 配置
│   └── package.json                     # 依赖配置
│
├── bickdemo-admin/                      # Vue 3 管理端模块
│   ├── src/
│   │   ├── api/                         # API 接口封装
│   │   │
│   │   ├── layouts/                     # 布局组件
│   │   │   └── AdminLayout.vue          # 管理后台布局
│   │   │
│   │   ├── router/                      # 路由配置
│   │   │   └── index.js
│   │   │
│   │   ├── stores/                      # Pinia 状态管理
│   │   │   └── auth.js                  # 认证状态
│   │   │
│   │   └── views/                       # 管理页面
│   │       ├── Login.vue                # 登录页
│   │       ├── Dashboard.vue            # 数据看板
│   │       ├── Bicycles.vue             # 车辆管理
│   │       ├── Rentals.vue              # 租赁管理
│   │       ├── Users.vue                # 用户管理
│   │       ├── ForumModeration.vue      # 论坛审核
│   │       ├── MarketplaceModeration.vue# 集市审核
│   │       ├── Backgrounds.vue          # 背景图管理
│   │       ├── Blacklist.vue            # 黑名单管理
│   │       ├── LoginLogs.vue            # 登录日志
│   │       ├── VisitorLogs.vue          # 访客日志
│   │       └── OperationLogs.vue        # 操作日志
│   │
│   ├── nginx.conf                       # Nginx 配置
│   ├── Dockerfile                       # 前端 Docker 配置
│   └── package.json                     # 依赖配置
│
├── script/                              # 脚本工具目录
│   ├── dev/                             # 开发环境脚本
│   └── prod/                            # 生产环境脚本与 Docker 编排
│       ├── docker-compose.yml           # Docker Compose 配置
│       └── deploy/                      # Jenkins / 手动部署脚本与文档
│
├── sql/                                 # SQL 初始化脚本
│   └── init.sql                         # 全量初始化脚本
│
├── .env.example                         # 环境变量示例
├── docker-compose.yml                   # Docker Compose 配置
├── Jenkinsfile                          # Jenkins 流水线配置
└── README.md                            # 项目说明文档
```

---

## 功能详解

### 用户系统

**注册登录：**
- 邮箱验证码注册（10 分钟有效期）
- JWT Token 认证（24 小时有效期）
- Token 自动刷新机制
- 忘记密码（邮箱验证 + 重置）

**个人资料：**
- 基本信息修改（昵称、性别、简介）
- 头像裁剪上传（自动压缩）
- 密码修改

### 租车服务

**车辆功能：**
- 车辆列表（分页、筛选、搜索）
- 车辆详情（图片、描述、库存、价格）
- 实时库存显示

**租赁流程：**
1. 选择车辆 → 2. 确认订单 → 3. 扫码开锁（模拟） → 4. 骑行中 → 5. 还车结算

**订单管理：**
- 我的租赁记录（进行中/已完成/已取消）
- 订单详情（时间、金额、状态）
- 提前还车

### 社交聊天

**好友系统：**
- 搜索用户
- 发送好友申请
- 管理好友列表（添加备注、删除好友）

**实时聊天：**
- WebSocket 实时消息推送
- 消息历史记录
- 离线消息存储
- 已读状态标记

### 论坛社区

**帖子功能：**
- 创建帖子（标题、内容、多图）
- 编辑/删除自己的帖子
- 点赞、收藏、评论

**评论系统：**
- 回复帖子
- 回复评论（楼中楼）
- 删除评论

**审核机制：**
- 管理员审核新帖
- 违规内容处理

### 用户系统

**注册与登录**
- 邮箱验证码注册：用户输入邮箱和验证码，系统发送 6 位数字验证码到邮箱
- 验证码有效期：10 分钟，超时自动失效
- 邮箱登录：使用邮箱 + 密码登录系统
- JWT Token：登录成功后返回 Token，有效期 24 小时
- 自动刷新：Token 过期前自动刷新，实现无感登录
- 找回密码：通过邮箱验证码验证身份后重置密码

**个人资料管理**
- 基本信息：修改昵称、性别、个人简介
- 头像上传：支持图片裁剪，自动压缩优化
- 密码修改：登录后修改登录密码
- 账号注销：支持注销账号

### 租车服务

**车辆浏览**
- 车辆列表：分页展示所有可用车辆
- 车辆筛选：按价格区间、车辆状态、类型筛选
- 车辆搜索：支持关键词搜索车辆名称
- 车辆详情：展示车辆图片、描述、价格、实时库存

**租赁流程**
1. 选择车辆：浏览列表或搜索目标车辆
2. 查看库存：确认当前可用数量
3. 创建订单：选择租赁数量，确认订单信息
4. 扫码开锁：模拟扫码开锁（可集成智能锁）
5. 骑行中：订单状态为"进行中"
6. 申请还车：提交还车申请
7. 结算费用：系统自动计算租金

**订单管理**
- 订单列表：查看所有租赁记录，支持分页
- 订单筛选：按状态筛选（进行中/已完成/已取消）
- 订单详情：查看订单时间、金额、车辆信息
- 提前还车：支持提前结束订单，按实际时长计费

### 社交聊天

**好友系统**
- 搜索用户：通过用户名或邮箱搜索其他用户
- 好友申请：发送好友申请，可附带申请消息
- 申请管理：查看收到的好友申请，选择接受/拒绝
- 好友列表：展示所有好友，支持备注名
- 删除好友：解除好友关系
- 在线状态：显示好友在线/离线状态

**实时聊天**
- WebSocket 连接：建立实时双向通信通道
- 消息发送：发送文本消息
- 消息推送：实时接收好友消息
- 消息历史：查看与好友的聊天记录
- 离线消息：离线消息存储，上线后推送
- 已读标记：显示消息已读/未读状态
- 消息类型：支持文本、图片等类型

### 论坛社区

**帖子功能**
- 帖子列表：分页展示所有帖子
- 帖子分类：按分类筛选帖子
- 帖子搜索：关键词搜索帖子标题或内容
- 创建帖子：发布图文混排帖子
- 多图上传：单帖支持多张图片上传
- 编辑帖子：修改自己发布的帖子
- 删除帖子：删除自己发布的帖子
- 帖子详情：查看帖子完整内容、作者信息

**互动功能**
- 发表评论：回复帖子，支持楼中楼回复
- 删除评论：删除不当评论
- 点赞功能：为喜欢的帖子点赞
- 取消点赞：取消已点赞的帖子
- 收藏功能：收藏感兴趣的帖子
- 取消收藏：取消已收藏的帖子
- 我的帖子：查看自己发布的帖子
- 我的收藏：查看自己收藏的帖子

**审核机制**
- 帖子审核：管理员审核新发帖
- 审核状态：待审核/已通过/已拒绝
- 违规处理：管理员可删除违规帖子

### 数据统计

**数据看板**
- 今日订单：显示今日订单数量
- 今日营收：显示今日营业收入
- 活跃用户：显示今日活跃用户数
- 车辆使用率：显示车辆使用情况
- 数据趋势：展示关键指标的日/周/月趋势

**统计图表**
- 租赁统计：按日/周/月统计租赁数据
- 用户分析：新增用户、活跃用户趋势图
- 营收趋势：营业收入变化趋势图
- 热门车辆：最受欢迎车辆排行
- ECharts 可视化：使用 ECharts 展示各类图表

### 后台管理

**数据看板**
- 核心数据概览：订单数、用户数、营收统计
- 趋势图表：数据变化趋势可视化
- 实时数据：实时更新关键指标

**车辆管理**
- 车辆列表：分页展示所有车辆
- 添加车辆：录入新车辆信息
- 编辑车辆：修改车辆信息
- 删除车辆：删除车辆记录
- 批量导入：通过 Excel 批量导入车辆
- 批量导出：导出车辆数据到 Excel
- 库存管理：调整车辆库存数量
- 车辆上下架：控制车辆是否可租

**租赁管理**
- 订单列表：查看所有订单，支持分页
- 订单筛选：按状态、用户、时间筛选
- 订单详情：查看订单完整信息
- 订单导出：导出订单数据到 Excel

**用户管理**
- 用户列表：分页展示所有用户
- 用户搜索：按用户名/邮箱搜索
- 用户详情：查看用户详细信息
- 禁用用户：禁用违规用户账号
- 启用用户：恢复被禁用的账号
- 角色管理：设置用户角色（USER/ADMIN）

**黑名单管理**
- 黑名单列表：查看被拉黑的用户
- 添加黑名单：将用户加入黑名单
- 移除黑名单：将用户从黑名单移除
- 封禁原因：记录封禁原因
- 封禁时长：支持永久封禁或临时封禁

**论坛审核**
- 待审核列表：查看待审核的帖子
- 审核通过：批准帖子发布
- 审核拒绝：拒绝违规帖子
- 删除帖子：删除违规帖子

**集市审核**
- 集市列表：查看集市交易
- 交易审核：审核交易合法性
- 违规处理：处理违规交易

**背景图管理**
- 背景图列表：展示所有背景图
- 上传背景图：上传新的背景图
- 排序管理：调整背景图展示顺序
- 启用/禁用：控制背景图是否显示

**系统日志**
- 登录日志：记录用户登录信息（时间、IP、设备）
- 操作日志：记录管理员关键操作（操作人、动作、结果）
- 访客日志：记录页面访问情况（IP、路径、停留时间）
- 日志查询：支持按条件筛选
- 日志导出：支持导出为 Excel

---

## 页面路由

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
| `/login` | `Login.vue` | 管理员登录 |
| `/dashboard` | `Dashboard.vue` | 数据看板（今日订单、活跃用户、营收统计） |
| `/bicycles` | `Bicycles.vue` | 车辆管理（CRUD、批量操作、库存管理） |
| `/marketplace` | `MarketplaceModeration.vue` | 集市审核 |
| `/rentals` | `Rentals.vue` | 租赁记录（查询、导出） |
| `/forum` | `ForumModeration.vue` | 论坛审核（帖子审核、删除） |
| `/backgrounds` | `Backgrounds.vue` | 背景图管理（上传、排序） |
| `/system/users` | `Users.vue` | 用户管理 |
| `/system/blacklist` | `Blacklist.vue` | 黑名单管理 |
| `/system/login-logs` | `LoginLogs.vue` | 登录日志 |
| `/system/visitor-logs` | `VisitorLogs.vue` | 访客日志 |
| `/system/operation-logs` | `OperationLogs.vue` | 操作日志 |

---

## API 接口

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

#### 认证接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/login | 用户登录 | ❌ |
| POST | /api/auth/register | 用户注册 | ❌ |
| POST | /api/auth/forgot-password | 找回密码 | ❌ |
| POST | /api/auth/send-code | 发送验证码 | ❌ |
| GET | /api/auth/logout | 退出登录 | ✅ |

#### 自行车接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/bicycles | 获取车辆列表 | ✅ |
| GET | /api/bicycles/:id | 获取车辆详情 | ✅ |
| POST | /api/bicycles | 添加车辆 | ✅ (ADMIN) |
| PUT | /api/bicycles/:id | 更新车辆 | ✅ (ADMIN) |
| DELETE | /api/bicycles/:id | 删除车辆 | ✅ (ADMIN) |

#### 租赁接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/rentals | 创建租赁订单 | ✅ |
| POST | /api/rentals/:id/return | 归还车辆 | ✅ |
| GET | /api/rentals/my | 我的租赁记录 | ✅ |
| GET | /api/rentals/:id | 订单详情 | ✅ |
| GET | /api/rentals/admin | 全部订单（管理） | ✅ (ADMIN) |

#### 用户接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/user/profile | 获取个人资料 | ✅ |
| PUT | /api/user/profile | 更新个人资料 | ✅ |
| POST | /api/user/avatar | 上传头像 | ✅ |
| PUT | /api/user/password | 修改密码 | ✅ |

#### 好友接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/friends | 好友列表 | ✅ |
| POST | /api/friends/apply | 发送好友申请 | ✅ |
| POST | /api/friends/accept | 接受好友申请 | ✅ |
| DELETE | /api/friends/:id | 删除好友 | ✅ |
| GET | /api/friends/requests | 好友申请列表 | ✅ |

#### 聊天接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/chat/history/:friendId | 聊天历史 | ✅ |
| GET | /api/chat/friends | 可聊天的好友列表 | ✅ |
| WS | /ws/chat | WebSocket 消息推送 | ✅ |

#### 论坛接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/forum/posts | 帖子列表 | ✅ |
| POST | /api/forum/posts | 创建帖子 | ✅ |
| GET | /api/forum/posts/:id | 帖子详情 | ✅ |
| PUT | /api/forum/posts/:id | 更新帖子 | ✅ |
| DELETE | /api/forum/posts/:id | 删除帖子 | ✅ |
| POST | /api/forum/posts/:id/comment | 发表评论 | ✅ |
| POST | /api/forum/posts/:id/like | 点赞 | ✅ |
| POST | /api/forum/posts/:id/favorite | 收藏 | ✅ |

#### 管理接口

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/statistics | 统计数据 | ✅ (ADMIN) |
| GET | /api/admin/users | 用户列表 | ✅ (ADMIN) |
| PUT | /api/admin/users/:id/status | 修改用户状态 | ✅ (ADMIN) |
| GET | /api/admin/rentals | 全部订单 | ✅ (ADMIN) |
| POST | /api/admin/bicycles/batch-import | 批量导入车辆 | ✅ (ADMIN) |
| GET | /api/admin/logs/login | 登录日志 | ✅ (ADMIN) |
| GET | /api/admin/logs/operation | 操作日志 | ✅ (ADMIN) |

---

## 数据库设计

### 核心数据表

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| user | 用户基础表 | id, username, password, email, role, status |
| user_profile | 用户资料扩展表 | user_id, avatar, nickname, gender, bio |
| bicycle | 自行车表 | id, name, description, price, stock, status, images |
| rental_order | 租赁订单表 | id, user_id, bicycle_id, start_time, end_time, total_amount, status |
| friend | 好友关系表 | id, user_id, friend_id, remark, created_at |
| friend_request | 好友申请表 | id, sender_id, receiver_id, status, message |
| chat_message | 聊天消息表 | id, sender_id, receiver_id, content, type, is_read |
| forum_post | 论坛帖子表 | id, user_id, title, content, images, likes, favorites, status |
| forum_comment | 评论表 | id, post_id, user_id, content, parent_id |
| forum_like | 点赞记录表 | id, post_id, user_id |
| forum_favorite | 收藏记录表 | id, post_id, user_id |
| background_image | 背景图表 | id, url, title, sort_order, is_active |
| email_code | 邮箱验证码表 | id, email, code, type, expires_at, used |
| login_log | 登录日志表 | id, user_id, ip, device, login_time |
| operation_log | 操作日志表 | id, user_id, operation, module, result, created_at |
| visitor_log | 访客日志表 | id, ip, url, method, duration, created_at |
| blacklist | 黑名单表 | id, user_id, reason, expire_at |

### 表关系简述

```
user (1) ──► (N) rental_order
user (1) ──► (1) user_profile
user (M) ◄──► (M) friend (through friend_request)
user (1) ──► (N) chat_message
user (1) ──► (N) forum_post
user (1) ──► (N) forum_comment
bicycle (1) ──► (N) rental_order
forum_post (1) ──► (N) forum_comment
forum_post (1) ──► (N) forum_like
forum_post (1) ──► (N) forum_favorite
```

### 数据库迁移

当项目升级或拉取新代码后，如遇到字段或表不存在的问题，需要执行对应的迁移脚本。

```bash
# 全量初始化（会清空现有数据）
mysql -u root -p bickdemo < sql/init.sql
```

常见字段补充：
```sql
-- 自行车库存字段
ALTER TABLE bicycles ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量（库存）';

-- 租赁数量字段
ALTER TABLE rentals ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '租赁数量';
```

---

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

### 环境变量（后端）

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | localhost | MySQL 主机 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_USERNAME` | root | MySQL 用户名 |
| `MYSQL_PASSWORD` | change-me-db-password | MySQL 密码 |
| `JWT_SECRET` | change-me-jwt-secret... | JWT 签名密钥（建议 32 位+） |
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
| `MAIL_PORT` | 587 | 邮件端口 |
| `MAIL_USERNAME` | (空) | 发件人邮箱 |
| `MAIL_PASSWORD` | (空) | SMTP 授权码 |

---

## 常用命令

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
# 用户端前端
cd bickdemo-frontend
npm install                      # 安装依赖
npm run dev                      # 开发模式 (端口 5173)
npm run build                    # 生产构建
npm run preview                  # 预览构建

# 管理端前端
cd bickdemo-admin
npm install                      # 安装依赖
npm run dev                      # 开发模式 (端口 3000)
npm run build                    # 生产构建
npm run preview                  # 预览构建 (端口 3000)
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

---

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

常见字段补充：
```sql
-- 自行车库存字段
ALTER TABLE bicycles ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量（库存）';

-- 租赁数量字段
ALTER TABLE rentals ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '租赁数量';
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

<details>
<summary>❓ 管理端无法登录，提示权限不足</summary>

**排查步骤：**

1. 确认使用的是管理员账号（默认：admin/admin123）

2. 检查数据库中用户角色
   ```sql
   SELECT id, username, role FROM user WHERE username = 'admin';
   ```

3. 如果角色不是 ADMIN，手动修改
   ```sql
   UPDATE user SET role = 'ADMIN' WHERE username = 'admin';
   ```

4. 清除浏览器缓存后重新登录

</details>

<details>
<summary>❓ 邮箱验证码发送失败</summary>

**排查步骤：**

1. 检查邮箱配置是否正确
   ```yaml
   spring:
     mail:
       host: smtp.qq.com
       port: 587
       username: your_email@qq.com
       password: your_smtp_auth_code  # 注意是授权码，不是密码
   ```

2. 确认已开启 SMTP 服务
   - QQ 邮箱：设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV 服务

3. 检查防火墙是否阻止 587 端口

4. 查看后端日志确认具体错误信息

</details>

---

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

---

## CI/CD 部署

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

---

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

---

## 许可证

本项目采用 [MIT License](LICENSE) 协议开源。

---

<p align="center">Made with ❤️ by BikeShare Team</p>
