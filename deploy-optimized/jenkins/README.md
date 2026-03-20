# Jenkins CI/CD 配置指南

## 完整部署流程

### 第一阶段：启动 Jenkins

#### 1. 启动 Jenkins 容器

```bash
# 进入 Jenkins 目录
cd /opt/bickdemo/deploy-optimized/jenkins

# 复制环境变量文件
cp .env.example .env

# 编辑配置（可选，配置 Git 仓库信息）
vi .env

# 启动 Jenkins
docker-compose up -d

# 查看启动日志
docker-compose logs -f

# 获取初始管理员密码
docker exec bikeshare-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

**输出示例：**
```
a1b2c3d4e5f6g7h8i9j0
```
记下这个密码，首次登录需要用到。

#### 2. 访问 Jenkins Web 界面

打开浏览器访问：`http://服务器 IP:8081`

---

### 第二阶段：Jenkins 初始配置

#### 3. 首次登录

1. **输入管理员密码**
   - 粘贴刚才获取的密码
   - 点击 "继续"

2. **选择插件安装方式**
   - 选择 **"Install suggested plugins"**（安装推荐插件）
   - 等待插件安装完成（约 2-5 分钟）

3. **创建管理员账户**
   - 用户名：`admin`
   - 密码：你的强密码
   - 姓名：`Administrator`
   - 邮箱：你的邮箱地址
   - 点击 "保存并继续"

4. **配置 Jenkins URL**
   - 保持默认即可（`http://服务器 IP:8081`）
   - 点击 "保存并完成"

---

### 第三阶段：安装必要插件

#### 4. 安装额外插件

1. 进入 **系统管理 -> 插件管理 -> 可选插件**

2. **搜索并安装以下插件**（勾选后点击"直接安装"）：

| 插件名称 | 用途 | 必装 |
|---------|------|------|
| NodeJS Plugin | Node.js 环境支持 | ✅ |
| Gitee Plugin | Gitea/Gitee 仓库集成 | ✅ |
| Pipeline Stage View | 流水线可视化 | ✅ |
| Blue Ocean | 现代化 Jenkins 界面 | 可选 |
| Email Extension | 邮件通知 | 可选 |

3. 等待安装完成，可能需要重启 Jenkins

---

### 第四阶段：配置全局工具

#### 5. 配置 JDK、Maven、Node.js

进入 **系统管理 -> 全局工具配置**

#### 5.1 JDK 配置

展开 **"JDK 安装"**，点击 **"添加 JDK"**：

```
名称：jdk-17
□ 自动安装（取消勾选，使用容器内 JDK）
JAVA_HOME: /opt/java/openjdk
```

#### 5.2 Maven 配置

展开 **"Maven 安装"**，点击 **"添加 Maven"**：

```
名称：maven-3.9.6
□ 自动安装（取消勾选，使用容器内 Maven）
MAVEN_HOME: /usr/share/maven
```

#### 5.3 Node.js 配置

展开 **"NodeJS 安装"**，点击 **"添加 NodeJS"**：

```
名称：node-20
版本：20.x
全局安装以下包：
  npm
```

点击 **"保存"** 完成配置。

---

### 第五阶段：配置 Git 凭证

#### 6. 添加 Git 仓库凭证

1. 进入 **系统管理 -> 凭证管理**

2. 点击 **"全局"** 凭证域

3. 点击 **"添加凭证"**

4. 填写凭证信息：

```
种类：Username with password
用户名：你的 Gitea 用户名
密码：你的 Gitea 密码/Token
ID: gitea-credentials
描述：BikeShare Gitea 仓库凭证
```

5. 点击 **"确定"** 保存

---

### 第六阶段：创建流水线任务

#### 7. 创建 Bikeshare 部署任务

1. **新建任务**
   - 点击左侧 **"新建任务"**
   - 输入任务名称：`bikeshare-optimized`
   - 选择 **"流水线"** 类型
   - 点击 **"确定"**

2. **配置 General 选项**
   ```
   描述：BikeShare 项目自动化构建部署
   丢弃旧的构建：✓ 丢弃旧的构建
   最大构建保留：10
   ```

3. **配置源码管理**
   ```
   源码管理：Git
   仓库 URL: http://你的 Gitea 地址:3000/用户名/bickdemo.git
   凭证：选择刚才添加的 gitea-credentials
   分支：*/main 或 */master
   ```

4. **配置构建触发器**
   ```
   ✓ 轮询 SCM: */5 * * * *  (每 5 分钟检查一次)
   ✓ 定时构建：0 2 * * *  (每天凌晨 2 点)
   ```

5. **配置 Pipeline**
   ```
   类型：Pipeline script from SCM
   SCM: Git
   仓库 URL: 同上
   凭证：同上
   分支：*/main
   脚本路径：deploy-optimized/jenkins/Jenkinsfile
   ```

6. 点击 **"保存"**

---

### 第七阶段：配置 Webhook（可选）

#### 8. 在 Gitea 中配置 Webhook

1. 访问你的 Gitea 仓库页面

2. 进入 **仓库 -> 设置 -> Webhooks**

3. 点击 **"添加 Webhook"** -> **"Gitee"**

4. 填写配置：
   ```
   URL: http://服务器 IP:8081/gitee-webhook/
   密钥：(可选，留空)
   触发事件：✓ Push 事件
   ```

5. 点击 **"添加 Webhook"**

6. 测试 Webhook：点击 Webhook 列表中的 Webhook -> 点击 **"发送假事件"**

---

### 第八阶段：开始构建

#### 9. 首次构建

1. 回到 Jenkins 任务页面

2. 点击 **"立即构建"**

3. **查看构建进度**：
   - 点击任务 -> 构建历史 -> 点击构建编号 -> 查看控制台输出

4. **构建阶段说明**：

| 阶段 | 说明 | 预计时间 |
|------|------|---------|
| Checkout | 拉取代码 | 30 秒 |
| Prepare | 准备环境 | 10 秒 |
| Backup Data | 备份数据 | 30 秒 |
| Stop Containers | 停止容器 | 10 秒 |
| Build Backend | 构建后端 | 2-5 分钟 |
| Build Frontend | 构建前端 | 1-2 分钟 |
| Build Admin | 构建管理端 | 1-2 分钟 |
| Build Images | 构建镜像 | 2-3 分钟 |
| Deploy | 部署应用 | 30 秒 |
| Health Check | 健康检查 | 30 秒 |
| Cleanup | 清理缓存 | 30 秒 |

**总预计时间：10-15 分钟（首次构建）**

5. **构建成功标志**：
   - 状态显示绿色 ✓
   - 输出 "✅ 部署成功！"

---

## 构建参数说明

点击 **"立即构建"** 时可以选择参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `SKIP_BUILD` | Boolean | false | 跳过构建，仅部署现有镜像（用于快速重启） |
| `CLEAN_WORKSPACE` | Boolean | false | 构建前清空工作区（依赖出问题时使用） |
| `BACKUP_BEFORE_DEPLOY` | Boolean | true | 部署前先备份数据 |
| `DEPLOY_TARGET` | Choice | all | 部署目标：all/frontend/admin/backend |

---

## 构建流程图

```
┌─────────────────────────────────────────────────────────────┐
│                     Jenkins 构建流程                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Checkout ──────→ 拉取 Git 代码                          │
│         ↓                                                   │
│  2. Prepare ───────→ 检查 Docker 环境                       │
│         ↓                                                   │
│  3. Backup Data ───→ 备份 MySQL 数据（可选）                │
│         ↓                                                   │
│  4. Stop Containers → 停止现有容器                          │
│         ↓                                                   │
│  5. Build Backend ──→ Maven 构建 Spring Boot               │
│         ↓                                                   │
│  6. Build Frontend ─→ npm build Vue3 用户端                │
│         ↓                                                   │
│  7. Build Admin ────→ npm build Vue3 管理端                │
│         ↓                                                   │
│  8. Build Images ───→ docker-compose build                 │
│         ↓                                                   │
│  9. Deploy ─────────→ docker-compose up -d                 │
│         ↓                                                   │
│  10. Health Check ──→ 检查服务健康状态                      │
│         ↓                                                   │
│  11. Cleanup ───────→ 清理 Docker 缓存                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 常用命令

```bash
# 启动 Jenkins
cd deploy-optimized/jenkins && docker-compose up -d

# 停止 Jenkins
cd deploy-optimized/jenkins && docker-compose down

# 查看 Jenkins 日志
docker-compose logs -f bikeshare-jenkins

# 获取管理员密码
docker exec bikeshare-jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# 重启 Jenkins
docker-compose restart bikeshare-jenkins

# 查看 Jenkins 容器状态
docker ps | grep bikeshare-jenkins
```

---

## 常见问题

### Q1: Docker 命令不可用

**症状**: 构建时提示 `docker: command not found`

**解决**:
确保 Jenkins 容器有 Docker 权限：

```yaml
# jenkins/docker-compose.yml
privileged: true
user: root
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

### Q2: 构建超时

**症状**: 构建在某个阶段卡住

**解决**:
增加超时时间（在 Jenkinsfile 中）：

```groovy
options {
    timeout(time: 60, unit: 'MINUTES')
}
```

### Q3: Git 凭证失败

**症状**: `Authentication failed` 错误

**解决**:
1. 系统管理 -> 凭证管理
2. 添加凭证 -> Username with password
3. 输入 Git 仓库用户名和密码/访问令牌

### Q4: 内存不足

**症状**: 容器被 OOM Kill

**解决**:
调整 Jenkins 容器内存限制：

```bash
# 编辑 docker-compose.yml，添加内存限制
deploy:
  resources:
    limits:
      memory: 2G
```

### Q5: 构建成功但服务无法访问

**解决**:
1. 检查容器状态：`docker-compose ps`
2. 查看服务日志：`docker-compose logs app`
3. 检查端口冲突：`netstat -tlnp | grep :8080`

---

## 备份与恢复

### 备份 Jenkins

```bash
# Jenkins 数据持久化在 volumes/jenkins 目录
# 备份整个 volumes 目录即可
tar -czf jenkins-backup.tar.gz volumes/jenkins
```

### 恢复 Jenkins

```bash
# 停止 Jenkins
docker-compose down

# 恢复数据
tar -xzf jenkins-backup.tar.gz -C volumes/

# 重启 Jenkins
docker-compose up -d
```

---

## 访问地址汇总

| 服务 | 地址 | 说明 |
|------|------|------|
| Jenkins | `http://服务器 IP:8081` | CI/CD 管理 |
| 用户端 | `https://bikeshare.online` | 用户访问 |
| 管理端 | `https://admin.bikeshare.online` | 后台管理 |
| MinIO 控制台 | `https://minio.bikeshare.online/console/` | 对象存储管理 |

---

## 参考链接

- [Jenkins 官方文档](https://www.jenkins.io/doc/)
- [Jenkins Pipeline 语法](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [NodeJS Plugin](https://plugins.jenkins.io/nodejs/)
- [Gitee Plugin](https://plugins.jenkins.io/gitee/)
