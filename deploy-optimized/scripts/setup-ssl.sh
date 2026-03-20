#!/bin/bash
################################################################################
# SSL 证书快速配置脚本
# 功能：复制现有 SSL 证书到部署目录
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"

# 源目录（现有证书位置）
SOURCE_SSL_DIR="${PROJECT_ROOT}/script/prod/ssl/bikeshare.online_nginx"

# 目标目录（优化方案证书位置）
TARGET_SSL_DIR="${SCRIPT_DIR}/nginx/ssl"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查源证书目录是否存在
if [ ! -d "${SOURCE_SSL_DIR}" ]; then
    log_error "源 SSL 证书目录不存在：${SOURCE_SSL_DIR}"
    log_error "请确认证书文件位置"
    exit 1
fi

# 创建目标目录
mkdir -p "${TARGET_SSL_DIR}"

# 复制证书文件
log_info "正在复制 SSL 证书..."
cp -v "${SOURCE_SSL_DIR}"/*.crt "${TARGET_SSL_DIR}/" 2>/dev/null || true
cp -v "${SOURCE_SSL_DIR}"/*.key "${TARGET_SSL_DIR}/" 2>/dev/null || true
cp -v "${SOURCE_SSL_DIR}"/*.pem "${TARGET_SSL_DIR}/" 2>/dev/null || true

# 设置文件权限
log_info "设置文件权限..."
chmod 644 "${TARGET_SSL_DIR}"/*.crt 2>/dev/null || true
chmod 600 "${TARGET_SSL_DIR}"/*.key 2>/dev/null || true
chmod 644 "${TARGET_SSL_DIR}"/*.pem 2>/dev/null || true

# 验证证书文件
log_info "验证证书文件..."
if [ -f "${TARGET_SSL_DIR}/bikeshare.online_bundle.crt" ] && [ -f "${TARGET_SSL_DIR}/bikeshare.online.key" ]; then
    log_info "SSL 证书配置完成！"
    log_info "证书文件位置："
    ls -la "${TARGET_SSL_DIR}/"
else
    log_error "证书文件不完整，请检查："
    log_error "  - bikeshare.online_bundle.crt"
    log_error "  - bikeshare.online.key"
    exit 1
fi
