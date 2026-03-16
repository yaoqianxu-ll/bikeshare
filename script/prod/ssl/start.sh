#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "[ssl] placeholder helper"
echo "1. replace files under $SCRIPT_DIR/ssl/"
echo "2. review nginx-ssl.conf"
echo "3. integrate this config into your production compose or reverse proxy setup"
