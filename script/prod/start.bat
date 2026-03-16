@echo off
setlocal

set SCRIPT_DIR=%~dp0
set ENV_FILE=%SCRIPT_DIR%.env

if not exist "%ENV_FILE%" (
  set ENV_FILE=%SCRIPT_DIR%env.example
)

echo [prod] using env file: %ENV_FILE%
docker compose --env-file "%ENV_FILE%" -f "%SCRIPT_DIR%docker-compose.yml" up -d --build

echo.
echo [prod] services started
echo Frontend: http://localhost
echo Backend:  http://localhost:8080
