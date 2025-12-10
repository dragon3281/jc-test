#!/bin/bash

###########################################
# 自动化数据检测平台 - 停止服务脚本
###########################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo "========================================="
echo "  停止自动化数据检测平台"
echo "========================================="
echo ""

LOG_DIR="/root/jc-test/logs"

# 检查是否使用systemd
if systemctl is-active --quiet detection-backend 2>/dev/null || systemctl is-active --quiet detection-frontend 2>/dev/null; then
    log_info "检测到systemd服务，停止服务..."
    
    systemctl stop detection-frontend 2>/dev/null || true
    systemctl stop detection-backend 2>/dev/null || true
    
    log_info "✓ systemd服务已停止"
else
    log_info "使用进程方式停止..."
    
    # 从PID文件停止
    if [ -f "$LOG_DIR/frontend.pid" ]; then
        FRONTEND_PID=$(cat "$LOG_DIR/frontend.pid")
        if kill -0 "$FRONTEND_PID" 2>/dev/null; then
            kill "$FRONTEND_PID"
            log_info "✓ 前端进程已停止 (PID: $FRONTEND_PID)"
        fi
        rm -f "$LOG_DIR/frontend.pid"
    fi
    
    if [ -f "$LOG_DIR/backend.pid" ]; then
        BACKEND_PID=$(cat "$LOG_DIR/backend.pid")
        if kill -0 "$BACKEND_PID" 2>/dev/null; then
            kill "$BACKEND_PID"
            log_info "✓ 后端进程已停止 (PID: $BACKEND_PID)"
        fi
        rm -f "$LOG_DIR/backend.pid"
    fi
    
    # 强制清理残留进程
    pkill -f "vite.*3000" 2>/dev/null || true
    pkill -f "detection-platform-1.0.0.jar" 2>/dev/null || true
fi

sleep 2

# 验证
if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
    log_warn "前端端口3000仍在监听"
else
    log_info "✓ 前端已完全停止"
fi

if netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
    log_warn "后端端口8080仍在监听"
else
    log_info "✓ 后端已完全停止"
fi

echo ""
echo "========================================="
echo "  服务已停止"
echo "========================================="
