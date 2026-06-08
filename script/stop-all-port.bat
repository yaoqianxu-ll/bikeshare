@echo off
powershell -Command "Get-NetTCPConnection -LocalPort 5173,3000,8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }"
