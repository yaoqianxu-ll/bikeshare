#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  ENV_FILE="$SCRIPT_DIR/env.example"
fi

echo "[dev] using env file: $ENV_FILE"
docker compose --env-file "$ENV_FILE" -f "$SCRIPT_DIR/docker-compose.yml" up -d

echo ""
echo "[dev] infrastructure started"
echo "MySQL:      localhost:${MYSQL_PORT:-3306}"
echo "Redis:      localhost:${REDIS_PORT:-6379}"
echo "RabbitMQ:   localhost:${RABBITMQ_PORT:-5672}"
echo "MinIO:      http://localhost:${MINIO_PORT:-9000}"
echo "MinIO UI:   http://localhost:${MINIO_CONSOLE_PORT:-9001}"
echo ""
echo "Then run backend and frontend locally:"
echo "  cd bickdemo-backend && mvn spring-boot:run"
echo "  cd bickdemo-frontend && npm run dev"
