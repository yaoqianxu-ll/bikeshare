# 快速入门指南

> **完整部署指南**: 查看 [DEPLOY.md](./DEPLOY.md)

> 5 分钟快速部署 BikeShare 系统

## 前提条件

- 服务器：Linux (Ubuntu 20.04+ / CentOS 7+)
- Docker 20.10+ 和 Docker Compose 2.0+
- 域名已解析到服务器：`bikeshare.online`, `admin.bikeshare.online`, `minio.bikeshare.online`
- SSL 证书文件（已存在于 `script/prod/ssl/` 目录）

## 步骤 1: 准备环境 (2 分钟)

```bash
# 1. 进入项目目录
cd /opt/bickdemo/deploy-optimized

# 2. 配置环境变量
cp .env.example .env
vi .env  # 修改密码和密钥

# 3. 复制现有 SSL 证书（如果已有）
cp ../script/prod/ssl/bikeshare.online_nginx/* nginx/ssl/
```

## 步骤 2: 配置 Jenkins 自动部署 (2 分钟)

```bash
# 在现有 Jenkins 中创建新任务
# 1. 新建任务 -> 流水线
# 2. 源码管理：Git (你的 Gitea 仓库)
# 3. 脚本路径：deploy-optimized/jenkins/Jenkinsfile
# 4. 点击"立即构建"
```

**或者手动部署（首次测试用）：**

```bash
# 手动部署（仅首次测试使用）
bash scripts/deploy.sh --backup --build
```

## 步骤 3: 验证部署 (1 分钟)

```bash
# 查看容器状态
docker-compose ps

# 测试访问
curl -I https://bikeshare.online
```

## 完成！

访问以下地址：

| 服务 | 地址 |
|------|------|
| 用户端 | https://bikeshare.online |
| 管理端 | https://admin.bikeshare.online |
| MinIO 控制台 | https://minio.bikeshare.online/console/ |

### 默认账号

- 管理员：`admin` / `admin123`
- 用户：`user` / `user123`

---

## 常用命令

```bash
# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 备份数据
bash scripts/backup.sh

# 停止服务
docker-compose down
```

## 下一步

- 查看详细文档：[DEPLOYMENT.md](./DEPLOYMENT.md)
- 配置 Jenkins 自动化部署：[jenkins/README.md](./jenkins/README.md)
- 设置定时备份：[scripts/backup.sh](./scripts/backup.sh)
