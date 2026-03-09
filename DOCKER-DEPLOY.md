# BikeShare Docker 部署说明

## 服务器信息
- **服务器 IP**: 60.205.169.251
- **前端端口**: 80
- **后端端口**: 8080
- **MinIO**: 已在服务器上运行 (端口 9000)

## 部署方式

### 步骤 1：上传项目到服务器

将项目文件上传到服务器，或使用 git 克隆：

```bash
# 在服务器上执行
git clone <你的仓库地址>
cd bickdemo
```

或者直接上传本地的构建产物：

```bash
# 在本地执行
scp -r bickdemo-frontend/dist root@60.205.169.251:/path/to/deploy/
scp bickdemo-backend/target/*.jar root@60.205.169.251:/path/to/deploy/
```

### 步骤 2：在服务器上构建和启动

#### 选项 A：在服务器上完整构建（推荐）

```bash
# SSH 登录服务器
ssh root@60.205.169.251

# 进入项目目录
cd /path/to/bickdemo

# 构建后端
cd bickdemo-backend
mvn clean package -DskipTests

# 返回项目根目录
cd ..

# 构建前端
cd bickdemo-frontend
npm install
npm run build
cd ..

# 启动 Docker 容器
docker-compose up -d --build
```

#### 选项 B：本地构建后上传

```bash
# 在本地构建
cd bickdemo-frontend
npm run build
cd ..

# 上传到服务器
scp -r bickdemo-frontend/dist root@60.205.169.251:/opt/bickdemo/frontend/
scp bickdemo-backend/Dockerfile root@60.205.169.251:/opt/bickdemo/backend/
scp bickdemo-backend/pom.xml root@60.205.169.251:/opt/bickdemo/backend/
scp -r bickdemo-backend/src root@60.205.169.251:/opt/bickdemo/backend/

# 在服务器上构建后端
ssh root@60.205.169.251
cd /opt/bickdemo/backend
mvn clean package -DskipTests
cd ..

# 启动 Docker
docker-compose up -d --build
```

### 步骤 3：验证服务

```bash
# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 测试后端
curl http://localhost:8080/api/bicycles

# 测试前端
curl http://localhost
```

## Docker Compose 配置说明

当前配置包含 3 个服务：

1. **mysql** - MySQL 8.0 数据库（容器内）
   - 数据持久化到 `mysql-data` 卷
   - 自动执行 init.sql 初始化

2. **app** - Spring Boot 后端
   - 端口：8080
   - 连接 MySQL 容器
   - 连接外部 MinIO (60.205.169.251:9000)

3. **frontend** - Nginx 前端
   - 端口：80
   - 代理 API 请求到后端

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://60.205.169.251 | 用户界面 |
| 后端 API | http://60.205.169.251:8080/api | REST API |
| MinIO 控制台 | http://60.205.169.251:9000 | 对象存储管理 |

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | user | user123 |

## 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f app
docker-compose logs -f frontend
docker-compose logs -f mysql

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 停止并删除数据（谨慎使用！）
docker-compose down -v

# 重新构建
docker-compose build --no-cache

# 进入容器
docker exec -it bickdemo-app sh
docker exec -it bickdemo-mysql mysql -uroot -proot123456
```

## 故障排查

### 1. 后端启动失败

```bash
# 查看后端日志
docker-compose logs app

# 检查数据库连接
docker exec -it bickdemo-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"
```

### 2. 前端无法访问

```bash
# 检查 Nginx 配置
docker exec -it bickdemo-frontend nginx -t

# 查看前端日志
docker-compose logs frontend
```

### 3. MinIO 连接失败

确保 MinIO 正在运行：
```bash
docker ps | grep minio
```

检查网络连通性：
```bash
docker exec -it bickdemo-app wget -qO- http://60.205.169.251:9000/minio/health/live
```

### 4. 数据库问题

```bash
# 进入 MySQL 容器
docker exec -it bickdemo-mysql mysql -uroot -proot123456

# 检查数据库
USE bickdemo;
SHOW TABLES;
```

## 数据备份

```bash
# 备份数据库
docker exec bickdemo-mysql mysqldump -uroot -proot123456 bickdemo > backup-$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i bickdemo-mysql mysql -uroot -proot123456 bickdemo < backup-20260309.sql
```

## 安全建议

1. **修改默认密码** - 修改 docker-compose.yml 中的数据库密码和 JWT 密钥
2. **配置防火墙** - 只开放必要端口（80, 443）
3. **启用 HTTPS** - 生产环境建议配置 SSL 证书
4. **定期备份** - 定期备份 MySQL 数据
5. **限制 MinIO 访问** - 配置 MinIO 访问策略
