#!/bin/bash

###############################################################################
# 安全重启脚本 - 清理僵尸进程并重新启动服务
# 避免端口占用和进程残留问题
###############################################################################

set -e

LOG_DIR="/root/jc-test/logs"
LOG_FILE="$LOG_DIR/restart-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "$LOG_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "========================================"
log "开始安全重启服务"
log "========================================"
log ""

# 步骤1: 停止所有相关服务
log "步骤1: 停止所有服务"
log "----------------------------------------"

# 停止前端 (所有node/npm进程)
log "停止前端进程..."
pkill -9 -f "node.*vite" 2>/dev/null || true
pkill -9 -f "npm.*dev" 2>/dev/null || true
killall -9 node 2>/dev/null || true
killall -9 npm 2>/dev/null || true
sleep 2

# 停止后端 (java进程)
log "停止后端进程..."
pkill -9 -f "java.*detection-platform" 2>/dev/null || true
sleep 2

# 清理可能的僵尸curl进程
log "清理僵尸进程..."
pkill -9 curl 2>/dev/null || true
pkill -9 -f curl 2>/dev/null || true

sleep 3

# 步骤2: 验证进程已清理
log ""
log "步骤2: 验证进程清理"
log "----------------------------------------"

NODE_COUNT=$(ps aux | grep -E "[n]ode|[n]pm" | wc -l)
JAVA_COUNT=$(ps aux | grep "[j]ava.*detection" | wc -l)
CURL_COUNT=$(ps aux | grep "[c]url" | wc -l)

log "剩余node/npm进程: $NODE_COUNT"
log "剩余java进程: $JAVA_COUNT"
log "剩余curl进程: $CURL_COUNT"

if [ "$NODE_COUNT" -gt 0 ] || [ "$JAVA_COUNT" -gt 0 ]; then
    log "警告: 仍有进程未清理，尝试强制清理..."
    sleep 2
    killall -9 node npm java 2>/dev/null || true
    sleep 3
fi

# 步骤3: 验证端口释放
log ""
log "步骤3: 验证端口释放"
log "----------------------------------------"

for port in 3000 3001 3002 3003 8080; do
    if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
        log "警告: 端口 $port 仍被占用"
        PID=$(netstat -tlnp 2>/dev/null | grep ":$port " | awk '{print $7}' | cut -d'/' -f1)
        if [ -n "$PID" ]; then
            log "  强制终止进程 $PID"
            kill -9 "$PID" 2>/dev/null || true
        fi
    else
        log "✓ 端口 $port 已释放"
    fi
done

sleep 3

# 步骤4: 启动后端
log ""
log "步骤4: 启动后端服务"
log "----------------------------------------"

cd /root/jc-test/backend

if [ ! -f "target/detection-platform-1.0.0.jar" ]; then
    log "错误: 后端jar文件不存在"
    exit 1
fi

nohup java -jar target/detection-platform-1.0.0.jar > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
log "后端已启动 (PID: $BACKEND_PID)"

# 等待后端启动
log "等待后端启动..."
for i in {1..30}; do
    if netstat -tlnp 2>/dev/null | grep -q ":8080 "; then
        log "✓ 后端在8080端口启动成功 (耗时 ${i} 秒)"
        break
    fi
    sleep 1
done

if ! netstat -tlnp 2>/dev/null | grep -q ":8080 "; then
    log "错误: 后端启动失败"
    log "查看日志: tail -50 $LOG_DIR/backend.log"
    exit 1
fi

# 步骤5: 启动前端 (使用严格端口模式)
log ""
log "步骤5: 启动前端服务"
log "----------------------------------------"

cd /root/jc-test/frontend

# 加载NVM
export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
    \. "$NVM_DIR/nvm.sh"
    nvm use 16 >/dev/null 2>&1
    log "✓ 使用 Node.js $(node -v)"
else
    log "错误: NVM未安装"
    exit 1
fi

# 启动前端 (严格要求3000端口)
log "启动前端 (端口:3000, host:0.0.0.0)..."
nohup npm run dev -- --port 3000 --host 0.0.0.0 --strictPort > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
log "前端已启动 (PID: $FRONTEND_PID)"

# 等待前端启动
log "等待前端启动..."
SUCCESS=false
for i in {1..30}; do
    if netstat -tlnp 2>/dev/null | grep -q "0.0.0.0:3000 "; then
        log "✓ 前端在3000端口启动成功 (耗时 ${i} 秒)"
        SUCCESS=true
        break
    fi
    sleep 1
done

if [ "$SUCCESS" = false ]; then
    log "错误: 前端未能在3000端口启动"
    log "查看日志: tail -50 $LOG_DIR/frontend.log"
    
    # 检查是否启动在其他端口
    ACTUAL_PORT=$(netstat -tlnp 2>/dev/null | grep "$FRONTEND_PID" | grep "0.0.0.0" | awk '{print $4}' | cut -d':' -f2)
    if [ -n "$ACTUAL_PORT" ]; then
        log "前端实际启动在端口: $ACTUAL_PORT"
    fi
    exit 1
fi

# 步骤6: 验证服务
log ""
log "步骤6: 验证服务状态"
log "----------------------------------------"

# 测试后端
if timeout 5 curl -s http://localhost:8080/user/test-password >/dev/null 2>&1; then
    log "✓ 后端API响应正常"
else
    log "⚠ 后端API未响应 (可能还在初始化)"
fi

# 测试前端
if timeout 5 curl -s http://localhost:3000 >/dev/null 2>&1; then
    log "✓ 前端页面响应正常"
else
    log "⚠ 前端页面未响应"
fi

# 步骤7: 显示最终状态
log ""
log "========================================"
log "服务重启完成"
log "========================================"
log ""
log "服务状态:"
log "  后端: http://103.246.244.229:8080 (PID: $BACKEND_PID)"
log "  前端: http://103.246.244.229:3000 (PID: $FRONTEND_PID)"
log ""
log "端口监听:"
netstat -tlnp 2>/dev/null | grep -E ":(3000|8080)" | tee -a "$LOG_FILE"
log ""
log "日志文件:"
log "  重启日志: $LOG_FILE"
log "  后端日志: $LOG_DIR/backend.log"
log "  前端日志: $LOG_DIR/frontend.log"
log ""
log "登录信息:"
log "  用户名: admin"
log "  密码: admin123"
log ""
log "========================================"

exit 0
