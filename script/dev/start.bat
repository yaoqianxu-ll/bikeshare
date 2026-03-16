@echo off
setlocal

set SCRIPT_DIR=%~dp0
set ENV_FILE=%SCRIPT_DIR%.env

if not exist "%ENV_FILE%" (
  set ENV_FILE=%SCRIPT_DIR%env.example
)

echo [dev] using env file: %ENV_FILE%
docker compose --env-file "%ENV_FILE%" -f "%SCRIPT_DIR%docker-compose.yml" up -d

echo.
echo [dev] infrastructure started
echo MySQL:      localhost:3306
echo Redis:      localhost:6379
echo RabbitMQ:   localhost:5672
echo MinIO:      http://localhost:9000
echo MinIO UI:   http://localhost:9001
echo.
echo Then run backend and frontend locally:
echo   cd bickdemo-backend ^&^& mvn spring-boot:run
echo   cd bickdemo-frontend ^&^& npm run dev
