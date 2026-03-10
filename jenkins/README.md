# Jenkins Docker 镜像拉取指南

## 问题原因

阿里云服务器访问 Docker Hub 超时，需要配置镜像加速器。

## 解决方案

### 步骤 1：配置 Docker 镜像加速器

在服务器上执行以下命令：

```bash
# 创建/编辑 Docker 配置文件
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.lukapopo.com",
    "https://hub.rat.dev",
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live"
  ]
}
EOF

# 重启 Docker
sudo systemctl daemon-reload
sudo systemctl restart docker

# 验证配置
docker info | grep -A 5 "Registry Mirrors"
```

### 步骤 2：拉取 Jenkins 镜像

```bash
# 清理旧配置（如果有）
docker-compose down

# 重新拉取镜像
cd /opt/bickdemo/jenkins
docker-compose pull

# 启动 Jenkins
docker-compose up -d
```

### 步骤 3：查看日志

```bash
# 查看 Jenkins 启动日志
docker-compose logs -f jenkins
```

## 获取初始密码

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

## 访问 Jenkins

浏览器访问：`http://服务器IP:8081`

## 其他可用的镜像加速器

如果上述加速器不可用，可以尝试：

```json
{
  "registry-mirrors": [
    "https://docker.1panel.live",
    "https://hub.rat.dev",
    "https://docker.lukapopo.com",
    "https://docker.m.daocloud.io",
    "https://huecker.io"
  ]
}
```

## 注意事项

1. 镜像加速器可能会失效，请根据实际情况更换
2. 可以关注 https://github.com/dockermirrors/dockermirrors 获取最新可用加速器
3. 如果所有加速器都不可用，可以考虑使用阿里云容器镜像服务（个人版）
