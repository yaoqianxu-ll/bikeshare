# Gitea 部署指南

## 1. 部署 Gitea

```bash
# 创建目录
mkdir -p /opt/gitea
cd /opt/gitea

# 下载 docker-compose.yml
# (从项目根目录复制 gitea/docker-compose.yml 到这里)

# 启动 Gitea
docker-compose up -d

# 查看日志
docker-compose logs -f
```

## 2. 初始化 Gitea

浏览器访问：`http://60.205.169.251:3000`

### 数据库设置
```
数据库类型：MySQL
主机：mysql:3306
数据库名称：gitea
用户名：gitea
密码：gitea123456
```

### 管理员账户
```
管理员用户名：admin
管理员密码：admin123
管理员邮箱：admin@example.com
```

### 其他设置
```
Gitea 基础 URL：http://60.205.169.251:3000
SSH 端口：222
```

## 3. 创建仓库

1. 登录 Gitea
2. 点击右上角 **+** → **新建仓库**
3. 仓库名称：`bikelease`
4. 初始化仓库（勾选）
5. 创建

## 4. 推送代码

```bash
# 添加 Gitea 远程仓库
cd F:\springbootStudy\bickdemo
git remote add gitea http://60.205.169.251:3000/loopeasen/bikelease.git

# 推送代码
git push gitea master
```

## 5. Jenkins 配置 Gitea

### 安装 Gitea 插件

1. Jenkins → 系统管理 → 插件管理 → 可选插件
2. 搜索 `Gitea`
3. 安装 **Gitea plugin**

### 配置 Gitea 连接

1. 系统管理 → 系统配置
2. 找到 **Gitea** 部分
3. 添加 Gitea 连接：
   ```
   名称：gitea
   URL: http://60.205.169.251:3000
   凭证：添加 Gitea 账号密码
   ```

### 配置任务

1. 任务配置 → 源码管理
2. 选择 **Git**
3. 仓库 URL: `http://60.205.169.251:3000/loopeasen/bikelease.git`
4. 凭证：Gitea 账号密码

### 配置 Webhook

1. Gitea 仓库 → 设置 → Webhook
2. 添加 Webhook:
   ```
   URL: http://60.205.169.251:8081/gitea-webhook/
   触发事件：Push 事件
   ```
