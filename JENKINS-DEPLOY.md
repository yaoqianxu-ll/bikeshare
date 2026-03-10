# Jenkins 自动化部署教程

## 目录

- [1. 快速开始](#1-快速开始)
- [2. Jenkins 安装与配置](#2-jenkins-安装与配置)
- [3. 项目配置](#3-项目配置)
- [4. Git Webhook 配置](#4-git-webhook-配置)
- [5. 常见问题](#5-常见问题)

---

## 1. 快速开始

### 1.1 部署 Jenkins

```bash
# 进入 jenkins 目录
cd jenkins

# 启动 Jenkins
docker-compose up -d

# 查看 Jenkins 日志
docker-compose logs -f
```

### 1.2 访问 Jenkins

- **地址**: `http://服务器 IP:8081`
- **初始管理员密码**:
  ```bash
  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
  ```

---

## 2. Jenkins 安装与配置

### 2.1 初始设置

1. **输入管理员密码** (见上方命令)

2. **选择插件安装方式**: 选择 "Install suggested plugins"

3. **创建管理员账户**: 设置用户名、密码、邮箱

4. **配置 Jenkins URL**: 保持默认即可

### 2.2 安装必要插件

进入 **系统管理 -> 插件管理 -> 可选插件**,安装以下插件:

- **NodeJS Plugin** - Node.js 环境支持
- **Gitee Plugin** 或 **GitHub Plugin** - Git 仓库集成
- **Pipeline** - 流水线支持 (通常已安装)
- **Pipeline Stage View** - 流水线可视化

### 2.3 配置全局工具

进入 **系统管理 -> 全局工具配置**:

#### JDK 配置
```
名称：jdk-17
JAVA_HOME: /opt/java/openjdk  (Jenkins 容器内默认路径)
```

#### Maven 配置
```
名称：maven-3.9.6
MAVEN_HOME: /usr/share/maven  (Jenkins 容器内默认路径)
```

#### NodeJS 配置
```
名称：node-20
版本：20.x
全局安装以下包：
  npm
```

### 2.4 配置 Docker 权限

Jenkins 容器需要访问 Docker，已在新建的 `jenkins/docker-compose.yml` 中配置好：

```yaml
privileged: true
user: root
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

---

## 3. 项目配置

### 3.1 创建流水线任务

1. 点击 **新建任务**
2. 输入任务名称：`bickdemo-deploy`
3. 选择 **流水线** 类型
4. 点击 **确定**

### 3.2 配置流水线

#### 3.2.1 源码管理

```
选择：Git
仓库 URL: https://your-repo.com/your-org/bickdemo.git
凭证：添加你的 Git 凭证
分支：*/master 或 */main
```

#### 3.2.2 构建触发器

勾选以下选项:

- **Git hook 触发器** - Git Push 自动触发
- **轮询 SCM** - 每 5 分钟检查：`*/5 * * * *`
- **定时构建** - 每天凌晨 2 点：`0 2 * * *`

#### 3.2.3 流水线配置

**方式一：使用 Jenkinsfile(推荐)**

```
选择：Pipeline script from SCM
脚本路径：Jenkinsfile
```

**方式二：直接在 Jenkins 配置**

```
选择：Pipeline script
```

将 `Jenkinsfile` 内容粘贴到脚本框中。

### 3.3 保存并构建

1. 点击 **保存**
2. 点击 **立即构建**

---

## 4. Git Webhook 配置

### 4.1 Gitee Webhook

1. 进入 Gitee 仓库 -> **管理** -> **WebHooks**

2. **添加 WebHook**:
   ```
   URL: http://服务器 IP:8081/gitee-webhook/
   密码：(可选)
   触发事件：Push 事件
   ```

3. **Jenkins 系统配置**:
   - 进入 **系统管理 -> 系统配置**
   - 找到 **Gitee 配置**
   - 启用 **Gitee Webhook**

### 4.2 GitHub Enterprise Webhook

1. 进入仓库 -> **Settings** -> **Webhooks**

2. **Add webhook**:
   ```
   Payload URL: http://服务器 IP:8081/github-webhook/
   Content type: application/json
   Secret: (可选)
   Trigger: Push events
   ```

---

## 5. 文件说明

```
bickdemo/
├── jenkins/
│   └── docker-compose.yml    # Jenkins Docker 配置
├── Jenkinsfile               # Jenkins 流水线配置
├── deploy-jenkins.sh         # Jenkins 部署脚本
├── docker-compose.yml        # 项目 Docker Compose 配置
└── deploy.sh                 # 原手动部署脚本
```

---

## 6. 常见问题

### Q1: Jenkins 无法使用 Docker 命令

**解决方案**:
```bash
# 确保 Jenkins 容器有 Docker 权限
docker exec jenkins docker --version

# 如果报错，重启 Jenkins 容器
docker-compose restart jenkins
```

### Q2: 构建时 Maven 或 Node.js 不可用

**解决方案**:
- 确保已在 **全局工具配置** 中正确配置工具路径
- 或者在 Jenkinsfile 中使用 Docker 容器构建

### Q3: Git 凭证认证失败

**解决方案**:
1. 进入 **凭证管理**
2. 添加新的凭证
3. 选择 **Username with password**
4. 输入 Git 仓库的用户名和密码/访问令牌

### Q4: 端口冲突

如果 8081 端口被占用，修改 `jenkins/docker-compose.yml`:
```yaml
ports:
  - "8082:8080"  # 改为其他端口
```

### Q5: Windows 系统 Docker 配置

Windows 系统需要额外配置 Docker 命令挂载:

```yaml
# 在 jenkins/docker-compose.yml 中添加
volumes:
  - "C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe:/usr/local/bin/docker"
```

---

## 7. 监控与维护

### 查看构建日志

```bash
# Jenkins 容器日志
docker-compose logs -f jenkins

# 应用日志
docker-compose logs -f app
```

### 备份 Jenkins 数据

```bash
# Jenkins 数据持久化在 jenkins-data 卷中
docker volume ls | grep jenkins
```

### 恢复 Jenkins

```bash
# 从备份恢复
docker-compose up -d  # Jenkins 会自动从卷中加载数据
```

---

## 8. 优化建议

### 8.1 缓存优化

在 Jenkinsfile 中已配置:
- Maven 仓库缓存 (`maven-repo` 卷)
- Node.js 缓存 (`node-cache` 卷)

### 8.2 清理策略

定期清理:
```bash
# 清理悬空镜像
docker image prune -f

# 清理停止的容器
docker container prune -f
```

### 8.3 安全加固

1. **修改默认端口**: 避免使用常见端口
2. **启用 HTTPS**: 配置 SSL 证书
3. **配置访问控制**: 限制 Jenkins 访问 IP
4. **定期更新**: 保持 Jenkins 和插件更新

---

## 9. 快速参考

### 常用命令

```bash
# 启动 Jenkins
cd jenkins && docker-compose up -d

# 停止 Jenkins
docker-compose down

# 查看 Jenkins 状态
docker-compose ps

# 获取管理员密码
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# 重启 Jenkins
docker-compose restart jenkins

# 构建项目 (手动触发)
# 在 Jenkins Web 界面点击 "立即构建"
```

### Cron 表达式参考

```
# 每分钟
* * * * *

# 每 5 分钟
*/5 * * * *

# 每天凌晨 2 点
0 2 * * *

# 每周一上午 9 点
0 9 * * 1

# 每天 9 点到 18 点，每小时
0 9-18 * * *
```

---

## 10. 联系与支持

如有问题，请查看:
- Jenkins 日志：`docker-compose logs jenkins`
- 构建日志：Jenkins Web 界面 -> 任务 -> 构建历史
