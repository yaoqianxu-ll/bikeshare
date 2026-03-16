# Jenkins + Gitea 自动化部署详细教程

## 服务器环境

- **服务器 IP**: your-server-host (腾讯云)
- **Docker**: 已安装并配置 DNS
- **项目**: BikeShare 自行车租赁系统

---

## 第一部分：部署基础设施服务

### 1.1 启动 Gitea (Git 仓库)

```bash
# 进入项目目录
cd /opt/bickdemo

# 启动 Gitea
cd gitea
docker compose up -d

# 查看状态
docker compose ps
```

**访问 Gitea**: `http://your-server-host:3000`

**首次访问配置**:
1. 打开浏览器访问 `http://your-server-host:3000`
2. 设置管理员账号（建议）:
   - 管理员邮箱：`admin@example.com`
   - 管理员用户名：`admin`
   - 管理员密码：`admin123456`
3. 其他配置保持默认，点击"安装"

### 1.2 启动 Jenkins (CI/CD)

```bash
# 进入 jenkins 目录
cd /opt/bickdemo/jenkins

# 启动 Jenkins
docker compose up -d

# 查看日志 (获取初始密码)
docker compose logs jenkins | grep "Please use the following password to proceed to installation"
```

**访问 Jenkins**: `http://your-server-host:8081`

**获取初始密码**:
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

---

## 第二部分：Jenkins 初始化配置

### 2.1 首次登录 Jenkins

1. 访问 `http://your-server-host:8081`
2. 输入初始密码（见上方命令）
3. 选择 **"Install suggested plugins"** (安装推荐插件)
4. 等待插件安装完成
5. 创建管理员账户:
   - 用户名：`admin`
   - 密码：`admin123456`
   - 确认密码：`admin123456`
   - 邮箱：`admin@example.com`
6. 保存并完成

### 2.2 安装必要插件

进入 **系统管理 -> 插件管理 -> 可选插件**，安装:

- ✅ **NodeJS Plugin** - Node.js 环境支持
- ✅ **Gitee Plugin** - Gitea/Gitea 集成
- ✅ **Pipeline Stage View** - 流水线可视化
- ✅ **Blue Ocean** - 现代化 UI (可选)

### 2.3 配置全局工具

进入 **系统管理 -> 全局工具配置**:

#### JDK 配置
```
名称：jdk-17
JAVA_HOME: /opt/java/openjdk
```

#### Maven 配置
```
名称：maven-3.9.6
MAVEN_HOME: /usr/share/maven
```

#### NodeJS 配置
```
名称：node-20
版本：20.x
全局安装包：npm
```

---

## 第三部分：Gitea 配置

### 3.1 创建组织和仓库

1. 登录 Gitea: `http://your-server-host:3000`
2. 点击右上角 **+ -> 新建组织**
   - 组织名称：`bickdemo`
   - 描述：`BikeShare 项目`
3. 在组织下创建仓库:
   - 仓库名称：`bickdemo`
   - 可见性：公开/私有 均可
   - 初始化：不需要，我们将推送现有代码

### 3.2 推送本地代码到 Gitea

```bash
# 在本地电脑上执行 (Windows PowerShell 或 Git Bash)
cd F:\springbootStudy\bickdemo

# 初始化 Git (如果还没有)
git init

# 添加远程仓库 (替换为你的 Gitea 地址)
git remote add gitea http://your-server-host:3000/bickdemo/bickdemo.git

# 或者使用 SSH (需要先配置 SSH key)
# git remote add gitea ssh://git@your-server-host:222/bickdemo/bickdemo.git

# 添加所有文件
git add .

# 创建 .gitignore (如果还没有)
cat > .gitignore <<EOF
node_modules/
target/
*.log
.env
EOF

# 提交
git commit -m "Initial commit"

# 推送
git push -u gitea master
```

### 3.3 配置 Gitea Webhook

1. 进入仓库 -> **设置 -> Webhook**
2. 点击 **添加 Webhook -> Gitea**
3. 配置:
   ```
    Payload URL: http://jenkins:8080/gitee-webhook/
    触发事件：Push 事件
    密钥：(可选，如果设置需要与 Jenkins 一致)
   ```

**注意**: 因为 Jenkins 和 Gitea 在同一 Docker 网络，使用 `jenkins` 作为主机名

---

## 第四部分：Jenkins 流水线配置

### 4.1 创建凭证

进入 **凭证管理 -> 系统 -> 全局凭证 -> 添加凭证**:

#### Gitea 凭证
```
类型：Username with password
用户名：你的 Gitea 用户名
密码：你的 Gitea 密码
ID: gitea-credentials
说明：Gitea 仓库凭证
```

### 4.2 创建流水线任务

1. 点击 **新建任务**
2. 输入名称：`bickdemo-deploy`
3. 选择 **流水线 (Pipeline)**
4. 点击 **确定**

### 4.3 配置流水线

#### 源码管理
```
选择：Git
仓库 URL: http://gitea:3000/bickdemo/bickdemo.git
凭证：选择刚才创建的 gitea-credentials
分支：*/master
```

#### 构建触发器
```
✅ Git hook 触发器
✅ 轮询 SCM: */5 * * * *
```

#### 流水线
```
选择：Pipeline script
```

将 `Jenkinsfile` 内容粘贴进去，或者:

```
选择：Pipeline script from SCM
Git 仓库：同上
脚本路径：Jenkinsfile
```

### 4.4 保存并首次构建

1. 点击 **保存**
2. 点击 **立即构建**
3. 查看控制台输出

---

## 第五部分：验证部署

### 5.1 查看构建状态

在 Jenkins 任务页面查看:
- 🟢 蓝色：成功
- 🔴 红色：失败
- 🟡 灰色：构建中

### 5.2 访问部署的应用

```bash
# 查看运行中的容器
docker ps

# 查看应用日志
docker compose -f script/prod/docker-compose.yml logs -f app
docker compose -f script/prod/docker-compose.yml logs -f frontend
```

**访问地址**:
- 前端：`http://your-server-host`
- 后端 API: `http://your-server-host:8080`
- Jenkins: `http://your-server-host:8081`
- Gitea: `http://your-server-host:3000`

### 5.3 测试自动部署

在本地修改代码并提交推送:

```bash
# 修改代码后
git add .
git commit -m "Test auto deploy"
git push gitea master
```

等待约 1 分钟，Jenkins 会自动触发构建。

---

## 第六部分：常见问题排查

### Q1: Jenkins 无法使用 Docker

```bash
# 检查 Docker 是否可用
docker exec jenkins docker ps

# 如果报错，检查 docker.sock 挂载
docker inspect jenkins | grep docker.sock
```

### Q2: 构建超时或失败

```bash
# 查看 Jenkins 日志
docker compose logs jenkins

# 增加 Maven 构建超时
# 在 Jenkinsfile 中添加 timeout 选项
```

### Q3: Gitea Webhook 不触发

1. 检查 Webhook URL 是否正确
2. 在 Gitea Webhook 页面查看发送记录
3. 在 Jenkins 系统配置中启用 Gitea Webhook

### Q4: 端口冲突

修改对应服务的 `docker-compose.yml`:
```yaml
ports:
  - "8082:8080"  # 改为其他可用端口
```

### Q5: 构建缓存过大

```bash
# 清理 Jenkins 缓存
docker volume rm jenkins_maven-repo
docker volume rm jenkins_node-cache

# 清理 Docker 悬空镜像
docker image prune -f
```

---

## 第七部分：优化建议

### 7.1 配置 HTTPS

使用 Nginx 反向代理配置 SSL:

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:80;
    }
}
```

### 7.2 配置邮件通知

在 Jenkins 系统配置中添加 SMTP 服务器，构建失败时发送邮件通知。

### 7.3 配置 SSH Key

```bash
# 在服务器上生成 SSH key
ssh-keygen -t rsa -b 4096

# 添加到 Gitea
cat ~/.ssh/id_rsa.pub
# 复制到 Gitea -> 设置 -> SSH Keys
```

---

## 快速参考命令

```bash
# 启动所有服务
cd /opt/bickdemo
docker compose -f gitea/docker-compose.yml up -d
docker compose -f jenkins/docker-compose.yml up -d
docker compose -f script/prod/docker-compose.yml up -d

# 查看服务状态
docker compose -f script/prod/docker-compose.yml ps
docker compose -f gitea/docker-compose.yml ps
docker compose -f jenkins/docker-compose.yml ps

# 查看日志
docker compose -f script/prod/docker-compose.yml logs -f
docker compose -f jenkins/docker-compose.yml logs -f jenkins

# 重启服务
docker compose -f script/prod/docker-compose.yml restart
docker compose -f jenkins/docker-compose.yml restart jenkins

# 进入容器
docker exec -it jenkins bash
docker exec -it gitea bash
```

---

## 总结

通过以上步骤，你已经完成了:

1. ✅ Gitea Git 仓库部署
2. ✅ Jenkins CI/CD 平台部署
3. ✅ 自动化流水线配置
4. ✅ Webhook 自动触发配置
5. ✅ 推送代码自动部署

现在，每次推送到 Gitea 的代码都会自动构建并部署到服务器！

