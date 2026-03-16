@echo off
setlocal

set SCRIPT_DIR=%~dp0
set ENV_FILE=%SCRIPT_DIR%.env
for %%I in ("%SCRIPT_DIR%..\..") do set ROOT_DIR=%%~fI

if not exist "%ENV_FILE%" (
  set ENV_FILE=%SCRIPT_DIR%env.example
)

echo [prod] using env file: %ENV_FILE%
cd /d "%ROOT_DIR%"
docker compose --env-file "%ENV_FILE%" up -d --build

echo.
echo [prod] services started
echo Frontend: http://localhost
echo Backend:  http://localhost:8080
