# 自行车租借系统

一个基于 Spring Boot 3 + Vue 3 的自行车租借系统，使用 MyBatis-Plus 作为数据访问层。

## 技术栈

### 后端
- Spring Boot 3.2.0
- Spring Security + JWT
- MyBatis-Plus 3.5.5
- MySQL 8
- Lombok

### 前端
- Vue 3
- Element Plus
- Pinia
- Vue Router
- Axios

## 项目结构

```
bickdemo/
├── init.sql                     # 数据库初始化脚本
├── README.md                    # 项目说明文档
├── bickdemo-backend/            # 后端项目
│   ├── src/main/java/com/example/bickdemo/
│   │   ├── component/           # 初始化组件
│   │   ├── config/              # 配置类
│   │   ├── controller/          # 控制器
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 实体类
│   │   ├── exception/           # 异常处理
│   │   ├── mapper/              # MyBatis Mapper
│   │   └── service/             # 业务逻辑层
│   └── src/main/resources/
│       ├── application.yml      # 配置文件
│       └── schema.sql           # 数据库脚本
│
└── bickdemo-frontend/           # 前端项目
    ├── src/
    │   ├── api/                 # API 接口
    │   ├── router/              # 路由配置
    │   ├── stores/              # 状态管理
    │   └── views/               # 页面组件
    └── package.json
```

## 快速开始

### 方式一：Docker 部署（推荐）

详见 [DEPLOY-QUICK.md](DEPLOY-QUICK.md)

```bash
# 一键部署
./deploy.sh       # Linux/Mac
deploy.bat        # Windows

# 访问地址
# 前端：http://localhost:80
# 后端：http://localhost:8080
```

### 方式二：本地开发

### 1. 数据库准备

运行数据库初始化脚本：

```bash
mysql -u root -p < init.sql
```

或者手动执行：

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source init.sql
```

数据库配置在 `application.yml` 中修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bickdemo?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2. 后端启动

```bash
cd bickdemo-backend
mvn spring-boot:run
```

后端服务会在 http://localhost:8080 启动

### 3. 前端启动

```bash
cd bickdemo-frontend
npm install
npm run dev
```

前端服务会在 http://localhost:5173 启动

## API 接口

### 认证相关
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/me` - 获取当前用户信息
- `PUT /api/auth/update` - 更新用户信息

### 自行车管理
- `GET /api/bicycles` - 获取所有自行车
- `GET /api/bicycles/available` - 获取可租赁自行车
- `GET /api/bicycles/{id}` - 获取自行车详情
- `POST /api/bicycles` - 添加自行车（管理员）
- `PUT /api/bicycles/{id}` - 更新自行车（管理员）
- `DELETE /api/bicycles/{id}` - 删除自行车（管理员）

### 租赁管理
- `POST /api/rentals` - 创建租赁
- `POST /api/rentals/{id}/end` - 结束租赁
- `POST /api/rentals/{id}/cancel` - 取消租赁
- `GET /api/rentals/my` - 获取我的租赁记录
- `GET /api/rentals` - 获取所有租赁记录（管理员）

### 统计
- `GET /api/statistics` - 获取统计数据

## 功能特性

- 用户注册/登录
- JWT Token 认证
- 角色权限管理（管理员/普通用户）
- 自行车增删改查
- 自行车租赁/归还
- 租赁历史记录
- 统计报表

## 默认账号

系统启动时会自动创建以下账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| user | user123 | 普通用户 |

## 数据库表结构

### users - 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 密码（BCrypt 加密） |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(100) | 手机号 |
| role | VARCHAR(20) | 角色（USER/ADMIN） |
| enabled | TINYINT(1) | 是否启用 |

### bicycles - 自行车表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 自行车名称 |
| type | VARCHAR(20) | 类型（MOUNTAIN/ROAD/CITY/ELECTRIC/TANDEM） |
| status | VARCHAR(20) | 状态（AVAILABLE/RENTED/MAINTENANCE/DISABLED） |
| location | VARCHAR(255) | 位置 |
| price_per_hour | DECIMAL(10,2) | 每小时价格 |

### rentals - 租赁记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| bicycle_id | BIGINT | 自行车 ID |
| start_time | DATETIME | 租赁开始时间 |
| end_time | DATETIME | 租赁结束时间 |
| status | VARCHAR(20) | 状态（ACTIVE/COMPLETED/CANCELLED） |
| total_price | DECIMAL(10,2) | 总价格 |

## License

MIT
