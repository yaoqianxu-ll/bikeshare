# 把子域名证书复制到 ssl 根目录
cp /var/jenkins_home/workspace/bike-deploy/ssl/admin.bikeshare.online_nginx/admin.bikeshare.online_bundle.crt /var/jenkins_home/workspace/bike-deploy/ssl/
cp /var/jenkins_home/workspace/bike-deploy/ssl/admin.bikeshare.online_nginx/admin.bikeshare.online.key /var/jenkins_home/workspace/bike-deploy/ssl/
cp /var/jenkins_home/workspace/bike-deploy/ssl/minio.bikeshare.online_nginx/minio.bikeshare.online_bundle.crt /var/jenkins_home/workspace/bike-deploy/ssl/
cp /var/jenkins_home/workspace/bike-deploy/ssl/minio.bikeshare.online_nginx/minio.bikeshare.online.key /var/jenkins_home/workspace/bike-deploy/ssl/

# 重启容器
docker restart bike-deploy-frontend-1 bike-deploy-admin-1
