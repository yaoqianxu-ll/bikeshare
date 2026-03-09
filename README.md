# 自行车租赁系统 (BikeShare)

一个基于 **Spring Boot 3 + Vue 3** 的全栈自行车租赁系统，支持完整的租赁业务流程、后台管理和数据统计分析。

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![Vue](https://img.shields.io/badge/Vue-3.4.0-blue)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.5-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [本地部署教程](#-本地部署教程)
- [Docker 部署教程](#-docker-部署教程)
- [API 接口文档](#-api-接口文档)
- [数据库设计](#-数据库设计)
- [常见问题](#-常见问题)

## ✨ 功能特性

### 用户端功能
- 📱 用户注册/登录（JWT 认证）
- 🚴 自行车浏览与搜索
- 🔑 扫码/在线租赁自行车
- 📝 租赁记录查询
- 👤 个人中心（信息修改、密码修改）
- 🖼️ 头像上传（支持图片压缩）

### 管理端功能
- 🔐 管理员权限控制
- 🚲 自行车管理（增删改查）
- 📊 数据统计分析
- 👥 用户管理
- 📋 租赁订单管理
- 🖼️ 背景图管理

### 系统特性
- 🔒 Spring Security + JWT 双重认证
- 💾 MyBatis-Plus 高效数据访问
- 🗄️ MySQL 8.0 数据存储
- 📦 MinIO 对象存储（图片/文件）
- 🎨 Element Plus 现代化 UI
- 📱 响应式设计，支持移动端

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.x | 安全认证 |
| JWT | - | Token 认证 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| MinIO | - | 对象存储 |
| Lombok | - | 简化代码 |
| HikariCP | - | 数据库连接池 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 渐进式框架 |
| Vue Router | 4.2.5 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Element Plus | 2.5.0 | UI 组件库 |
| Axios | 1.6.2 | HTTP 客户端 |
| ECharts | 6.0.0 | 数据可视化 |
| Vite | 5.0.8 | 构建工具 |
| Sass | - | CSS 预处理器 |

## 📁 项目结构

```
bickdemo/
├── 📄 README.md                 # 项目说明文档
├── 📄 DEPLOY.md                 # 服务器部署指南
├── 📄 DOCKER-DEPLOY.md          # Docker 部署指南
├── 📄 init.sql                  # 数据库初始化脚本
├── 📄 docker-compose.yml        # Docker 编排配置
├── 📄 deploy.bat                # Windows 一键部署脚本
├── 📄 deploy.sh                 # Linux/Mac 一键部署脚本
│
├── 📂 bickdemo-backend/         # 后端项目
│   ├── 📂 src/main/java/com/example/bickdemo/
│   │   ├── 📂 component/        # 初始化组件
│   │   │   ├── DataInitializer.java   # 数据初始化
│   │   │   └── MinioInitializer.java  # MinIO 初始化
│   │   ├── 📂 config/           # 配置类
│   │   │   ├── SecurityConfig.java    # 安全配置
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── ApplicationConfig.java
│   │   │   └── MinioConfig.java
│   │   ├── 📂 controller/       # 控制器层
│   │   │   ├── AuthController.java    # 认证接口
│   │   │   ├── BicycleController.java # 自行车接口
│   │   │   ├── RentalController.java  # 租赁接口
│   │   │   ├── StatisticsController.java
│   │   │   └── FileUploadController.java
│   │   ├── 📂 dto/              # 数据传输对象
│   │   ├── 📂 entity/           # 实体类
│   │   ├── 📂 exception/        # 异常处理
│   │   ├── 📂 mapper/           # MyBatis Mapper
│   │   └── 📂 service/          # 业务逻辑层
│   ├── 📂 src/main/resources/
│   │   ├── application.yml      # 配置文件
│   │   ├── application-prod.yml # 生产环境配置
│   │   └── schema.sql           # 数据库脚本
│   ├── 📄 pom.xml               # Maven 配置
│   └── 📄 Dockerfile            # Docker 镜像配置
│
└── 📂 bickdemo-frontend/        # 前端项目
    ├── 📂 public/               # 静态资源
    ├── 📂 src/
    │   ├── 📂 api/              # API 接口封装
    │   ├── 📂 assets/           # 资源文件
    │   ├── 📂 components/       # 公共组件
    │   ├── 📂 router/           # 路由配置
    │   ├── 📂 stores/           # Pinia 状态管理
    │   ├── 📂 styles/           # 全局样式
    │   ├── 📂 views/            # 页面组件
    │   │   ├── Login.vue        # 登录页
    │   │   ├── Register.vue     # 注册页
    │   │   ├── BicycleList.vue  # 自行车列表
    │   │   ├── MyRentals.vue    # 我的租赁
    │   │   ├── Admin.vue        # 管理后台
    │   │   ├── Statistics.vue   # 统计页面
    │   │   └── Profile.vue      # 个人中心
    │   ├── App.vue              # 根组件
    │   └── main.js              # 入口文件
    ├── 📄 package.json          # 依赖配置
    ├── 📄 vite.config.js        # Vite 配置
    └── 📄 Dockerfile            # Docker 镜像配置
```

## 💻 本地部署教程

### 环境要求

| 软件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| JDK | 17 | 17+ |
| Node.js | 16 | 18+ |
| Maven | 3.6 | 3.8+ |
| MySQL | 8.0 | 8.0+ |
| Git | - | 最新版 |

### 第一步：安装 MySQL 数据库

**Windows 用户：**
1. 访问 [MySQL 官网](https://dev.mysql.com/downloads/mysql/) 下载安装包
2. 或使用 [MySQL Installer](https://dev.mysql.com/downloads/installer/) 一键安装
3. 安装时记住 root 密码

**macOS 用户：**
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

**Linux 用户：**
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install mysql-server-8.0

# CentOS/RHEL
sudo yum install mysql-server
sudo systemctl start mysqld
```

### 第二步：创建数据库并导入数据

方法一：使用命令行
```bash
# 登录 MySQL
mysql -u root -p

# 执行以下 SQL
CREATE DATABASE IF NOT EXISTS bickdemo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bickdemo;
source init.sql;
```

方法二：使用客户端工具（Navicat、DBeaver 等）
1. 创建名为 `bickdemo` 的数据库
2. 字符集选择 `utf8mb4`
3. 运行 `init.sql` 脚本

### 第三步：配置后端

1. 编辑配置文件 `bickdemo-backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bickdemo?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true
    username: root          # 修改为你的数据库用户名
    password: 你的密码       # 修改为你的数据库密码
```

2. 如果不需要 MinIO 对象存储，可以暂时注释掉相关配置

### 第四步：启动后端服务

```bash
# 进入后端目录
cd bickdemo-backend

# 使用 Maven 启动（需要联网下载依赖）
mvn spring-boot:run

# 或者先打包再运行
mvn clean package -DskipTests
java -jar target/bickdemo-0.0.1-SNAPSHOT.jar
```

启动成功后会看到类似日志：
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

BickdemoApplication Started in 5.123 seconds
Tomcat started on port(s): 8080 (http)
```

访问 `http://localhost:8080/actuator/health` 验证后端是否启动成功。

### 第五步：启动前端服务

```bash
# 进入前端目录
cd bickdemo-frontend

# 安装依赖（首次需要，耐心等待）
npm install

# 如果npm安装慢，可以使用国内镜像
npm config set registry https://registry.npmmirror.com
npm install

# 启动开发服务器
npm run dev
```

启动成功后会看到：
```
  VITE v5.0.8  ready in 500 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

### 第六步：访问系统

打开浏览器访问：

- **前端开发地址**: http://localhost:5173
- **后端 API 地址**: http://localhost:8080/api

### 默认账号

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 全部权限 |
| 普通用户 | user | user123 | 基础租赁功能 |

## 🐳 Docker 部署教程

### 方式一：一键部署（推荐）

**Windows:**
```bash
deploy.bat
```

**Linux/Mac:**
```bash
chmod +x deploy.sh
./deploy.sh
```

### 方式二：手动部署

```bash
# 1. 构建前端
cd bickdemo-frontend
npm install
npm run build
cd ..

# 2. 构建后端
cd bickdemo-backend
mvn clean package -DskipTests
cd ..

# 3. 启动 Docker 容器
docker-compose up -d

# 4. 查看日志
docker-compose logs -f
```

### Docker 服务组成

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | bickdemo-mysql | 3306 | 数据库 |
| Backend | bickdemo-app | 8080 | Spring Boot 后端 |
| Frontend | bickdemo-frontend | 80 | Nginx 前端 |

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost |
| 后端 API | http://localhost:8080/api |

### 常用 Docker 命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f app
docker-compose logs -f frontend

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 重新构建
docker-compose build --no-cache
```

## 📖 API 接口文档

### 认证接口

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | /api/auth/register | 用户注册 | ❌ |
| POST | /api/auth/login | 用户登录 | ❌ |
| GET | /api/auth/me | 获取当前用户 | ✅ |
| PUT | /api/auth/update | 更新用户信息 | ✅ |
| PUT | /api/auth/change-password | 修改密码 | ✅ |

### 自行车接口

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| GET | /api/bicycles | 获取所有自行车 | ❌ |
| GET | /api/bicycles/available | 获取可租赁自行车 | ❌ |
| GET | /api/bicycles/{id} | 获取自行车详情 | ❌ |
| POST | /api/bicycles | 添加自行车 | ✅ 管理员 |
| PUT | /api/bicycles/{id} | 更新自行车 | ✅ 管理员 |
| DELETE | /api/bicycles/{id} | 删除自行车 | ✅ 管理员 |

### 租赁接口

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | /api/rentals | 创建租赁 | ✅ |
| POST | /api/rentals/{id}/end | 结束租赁 | ✅ |
| POST | /api/rentals/{id}/cancel | 取消租赁 | ✅ |
| GET | /api/rentals/my | 获取我的租赁记录 | ✅ |
| GET | /api/rentals | 获取所有租赁记录 | ✅ 管理员 |

### 统计接口

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| GET | /api/statistics | 获取统计数据 | ✅ 管理员 |
| GET | /api/statistics/revenue | 收益统计 | ✅ 管理员 |

### 文件上传接口

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | /api/upload | 上传图片 | ✅ |
| POST | /api/upload/background | 上传背景图 | ✅ 管理员 |

### 请求示例

**用户登录：**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN"
    }
  }
}
```

**携带 Token 请求：**
```bash
curl -X GET http://localhost:8080/api/bicycles \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 🗄️ 数据库设计

### users - 用户表

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    enabled TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | 密码（BCrypt 加密） |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像 URL |
| role | VARCHAR(20) | 角色（USER/ADMIN） |
| enabled | TINYINT(1) | 是否启用 |

### bicycles - 自行车表

```sql
CREATE TABLE bicycles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    location VARCHAR(255),
    price_per_hour DECIMAL(10,2) NOT NULL,
    image VARCHAR(255),
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 自行车名称 |
| type | VARCHAR(20) | 类型（MOUNTAIN/ROAD/CITY/ELECTRIC/TANDEM） |
| status | VARCHAR(20) | 状态（AVAILABLE/RENTED/MAINTENANCE/DISABLED） |
| location | VARCHAR(255) | 停放位置 |
| price_per_hour | DECIMAL(10,2) | 每小时价格（元） |
| image | VARCHAR(255) | 图片 URL |
| description | TEXT | 描述 |

### rentals - 租赁记录表

```sql
CREATE TABLE rentals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bicycle_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    total_price DECIMAL(10,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (bicycle_id) REFERENCES bicycles(id)
);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID（外键） |
| bicycle_id | BIGINT | 自行车 ID（外键） |
| start_time | DATETIME | 租赁开始时间 |
| end_time | DATETIME | 租赁结束时间 |
| status | VARCHAR(20) | 状态（ACTIVE/COMPLETED/CANCELLED） |
| total_price | DECIMAL(10,2) | 总价格 |

## ❓ 常见问题

### 1. 后端启动失败：找不到或无法加载主类

**解决方案：**
```bash
# 清理 Maven 缓存
mvn clean

# 重新编译
mvn clean package -DskipTests

# 确保使用 JDK 17
java -version
```

### 2. 数据库连接失败

**错误信息：** `Communications link failure`

**解决方案：**
1. 确认 MySQL 服务已启动
2. 检查配置文件中的数据库用户名密码
3. 确认数据库 `bickdemo` 已创建
4. 检查防火墙设置

### 3. 前端 npm install 失败

**解决方案：**
```bash
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 删除 node_modules 重新安装
rm -rf node_modules package-lock.json
npm install
```

### 4. 跨域问题

开发时前端需要配置代理：

```javascript
// vite.config.js
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

### 5. JWT Token 过期

Token 默认有效期为 24 小时，过期后需要重新登录。

可以在配置文件中修改：
```yaml
jwt:
  expiration: 86400000  # 毫秒单位
```

### 6. MinIO 连接失败

如果不需要图片上传功能，可以暂时注释掉 MinIO 相关配置。

需要 MinIO 时：
```bash
# 启动 MinIO
docker run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=password" \
  minio/minio server /data --console-address ":9001"
```

## 📝 开发指南

### 后端开发

```bash
# 运行后端
cd bickdemo-backend
mvn spring-boot:run

# 运行测试
mvn test

# 打包
mvn clean package
```

### 前端开发

```bash
# 安装依赖
cd bickdemo-frontend
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 🔒 安全建议

1. **生产环境修改默认密码** - 数据库、JWT Secret、MinIO 密钥
2. **启用 HTTPS** - 使用 Nginx 反向代理配置 SSL
3. **定期备份数据** - 尤其是 MySQL 数据库
4. **限制 API 访问频率** - 防止恶意请求
5. **日志脱敏** - 不记录敏感信息

## 📄 License

MIT License

## 👥 联系方式

如有问题，请提交 Issue 或联系开发者。
