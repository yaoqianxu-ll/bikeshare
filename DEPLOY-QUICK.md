# BikeShare 快速部署指南

## 服务器信息
- **服务器 IP**: 60.205.169.251
- **前端端口**: 80
- **后端端口**: 8080
- **MinIO 端口**: 9000

## 部署步骤

### 方法一：一键部署脚本（推荐）

**Windows 系统：**
```bash
deploy.bat
```

**Linux/Mac 系统：**
```bash
chmod +x deploy.sh
./deploy.sh
```

### 方法二：手动部署

#### 1. 构建前端
```bash
cd bickdemo-frontend
npm run build
cd ..
```

#### 2. 构建后端
```bash
cd bickdemo-backend
mvn clean package -DskipTests
cd ..
```

#### 3. 启动 Docker 容器
```bash
docker-compose up -d
```

#### 4. 查看日志
```bash
docker-compose logs -f
```

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

## Docker 服务组成

- **mysql**: MySQL 8.0 数据库（容器内部）
- **app**: Spring Boot 后端应用
- **frontend**: Nginx 前端服务

## 常用命令

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

# 停止并删除数据（谨慎使用）
docker-compose down -v
```

## 故障排查

### 1. 后端启动失败
```bash
# 查看后端日志
docker-compose logs -f app

# 检查数据库连接
docker exec -it bickdemo-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"
```

### 2. 前端无法访问
```bash
# 检查 Nginx 配置
docker exec -it bickdemo-frontend nginx -t

# 重启前端容器
docker-compose restart frontend
```

### 3. MinIO 连接问题
确保 MinIO 服务正在运行：
```bash
# 在服务器上检查 MinIO 状态
docker ps | grep minio
```

## 数据持久化

- MySQL 数据卷：`mysql-data`
- 日志文件：`./logs`

## 安全建议

1. 修改默认密码
2. 配置防火墙规则
3. 启用 HTTPS（生产环境）
4. 定期备份数据库
