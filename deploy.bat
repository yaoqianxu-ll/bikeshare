@echo off
chcp 65001 >nul
echo ==========================================
echo   BikeShare 部署脚本 (Windows)
echo   服务器：60.205.169.251
echo ==========================================

echo.
echo [1/4] 构建后端...
cd bickdemo-backend
call mvn clean package -DskipTests -B
cd ..

echo.
echo [2/4] 构建前端...
cd bickdemo-frontend
call npm run build
cd ..

echo.
echo [3/4] 停止旧容器...
docker-compose down

echo.
echo [4/4] 启动新容器...
docker-compose up -d --build

echo.
echo ==========================================
echo   部署完成！
echo ==========================================
echo.
echo 服务访问地址：
echo   前端：http://60.205.169.251
echo   后端：http://60.205.169.251:8080
echo   MinIO: http://60.205.169.251:9000
echo.
echo 默认账号：
echo   管理员：admin / admin123
echo   用户：user / user123
echo.
echo 查看日志：docker-compose logs -f
echo 停止服务：docker-compose down
echo ==========================================
pause
