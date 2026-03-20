# 部署流程总结

## 完整部署步骤

### 第一步：准备环境 (5 分钟)

```bash
# 1. 进入项目目录
cd /opt/bickdemo/deploy-optimized

# 2. 复制环境变量
cp .env.example .env

# 3. 编辑配置
vi .env
```

**必须修改的配置：**

```ini
# MySQL 配置
MYSQL_ROOT_PASSWORD=你的强密码
MYSQL_DATABASE=bikeshare
MYSQL_PASSWORD=你的强密码

# Redis 配置
REDIS_PASSWORD=你的强密码
APP_REDIS_KEY_PREFIX=bikeshare:prod:

# RabbitMQ 配置
RABBITMQ_USERNAME=bikeshare
RABBITMQ_PASSWORD=你的强密码

# MinIO 配置
MINIO_ROOT_USER=bikeshare-admin
MINIO_ROOT_PASSWORD=你的强密码

# JWT 配置
JWT_SECRET=你的 JWT 密钥（至少 32 字符）

# 邮件配置
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=你的 SMTP 授权码
```

---

### 第二步：配置 SSL 证书 (1 分钟)

**使用现有证书（推荐）：**

```bash
# 方式 1: 使用脚本自动复制
bash deploy-optimized/scripts/setup-ssl.sh

# 方式 2: 手动复制
cp ../script/prod/ssl/bikeshare.online_nginx/* nginx/ssl/
```

**证书文件说明：**
- `bikeshare.online_bundle.crt` - 证书链
- `bikeshare.online.key` - 私钥

---

### 第三步：启动 Jenkins (2 分钟)

```bash
# 进入 Jenkins 目录
cd jenkins

# 复制环境变量
cp .env.example .env

# 编辑 Git 仓库配置
vi .env
```

修改 Git 仓库信息：

```ini
GIT_REPO_URL=http://你的 Gitea 地址:3000/用户名/bickdemo.git
GIT_USERNAME=你的 Git 用户名
GIT_PASSWORD=你的 Git 密码/Token
```

启动 Jenkins：

```bash
docker-compose up -d

# 获取初始管理员密码
docker exec bikeshare-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

---

### 第四步：配置 Jenkins 流水线 (5 分钟)

1. **访问 Jenkins**: `http://服务器 IP:8081`

2. **初始设置**:
   - 输入初始管理员密码
   - 选择 "Install suggested plugins"
   - 创建管理员账户

3. **安装插件** (系统管理 -> 插件管理):
   - NodeJS Plugin
   - Gitee Plugin (或 GitHub Plugin)
   - Pipeline Stage View

4. **配置全局工具** (系统管理 -> 全局工具配置):
   - JDK: `JAVA_HOME=/opt/java/openjdk`
   - Maven: `MAVEN_HOME=/usr/share/maven`
   - NodeJS: 版本 20.x

5. **创建流水线任务**:
   - 新建任务 -> 流水线
   - 任务名称：`bikeshare-optimized`
   - 源码管理：Git -> 配置 Gitea 仓库
   - 脚本路径：`deploy-optimized/jenkins/Jenkinsfile`

6. **配置 Webhook** (在 Gitea 中):
   - 仓库 -> 设置 -> Webhooks
   - URL: `http://服务器 IP:8081/gitee-webhook/`
   - 触发事件：Push 事件

---

### 第五步：开始构建 (10-15 分钟)

1. **首次构建**:
   - 在 Jenkins 任务中点击 "立即构建"
   - 等待构建完成（首次构建需要下载依赖，时间较长）

2. **查看进度**:
   - 点击任务 -> 构建历史 -> 查看控制台输出

3. **构建完成后验证**:
   ```bash
   # 查看容器状态
   docker-compose ps

   # 测试访问
   curl -I https://bikeshare.online
   curl -I https://admin.bikeshare.online
   ```

---

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Jenkins | `http://服务器 IP:8081` | CI/CD 管理 |
| 用户端 | `https://bikeshare.online` | 用户访问 |
| 管理端 | `https://admin.bikeshare.online` | 后台管理 |
| MinIO | `https://minio.bikeshare.online/console/` | 对象存储管理 |

---

## 默认账号

| 服务 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 用户 | user | user123 |

---

## 常用命令

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启单个服务
docker-compose restart app

# 停止所有服务
docker-compose down

# 手动备份
bash scripts/backup.sh

# 进入 MySQL
docker exec -it bikeshare-mysql mysql -uroot -p
```

---

## 后续自动化

配置完成后：

1. **代码推送自动构建** - Push 到 Gitea 自动触发 Jenkins 构建
2. **定时构建** - 每天凌晨 2 点自动检查更新
3. **自动备份** - 每天凌晨 3 点自动备份数据

---

## 故障排查

### 容器启动失败

```bash
# 查看容器日志
docker-compose logs <服务名>

# 查看容器状态
docker-compose ps

# 重启服务
docker-compose restart
```

### Jenkins 构建失败

1. 查看构建控制台输出
2. 检查 Git 凭证配置
3. 检查 Docker 权限：`docker exec bikeshare-jenkins docker ps`

### HTTPS 无法访问

1. 检查证书文件是否存在
2. 检查 Nginx 配置：`docker exec bikeshare-nginx nginx -t`
3. 检查域名解析：`ping bikeshare.online`
