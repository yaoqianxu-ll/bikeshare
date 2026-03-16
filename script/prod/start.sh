#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ ! -f "$ENV_FILE" ]; then
  ENV_FILE="$SCRIPT_DIR/env.example"
fi

echo "[prod] using env file: $ENV_FILE"
cd "$ROOT_DIR"
docker compose --env-file "$ENV_FILE" up -d --build

echo ""
echo "[prod] services started"
echo "Frontend: http://localhost"
echo "Backend:  http://localhost:8080"
