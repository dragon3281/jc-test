#!/bin/bash

###############################################################################
# 前端修复脚本 - 强制清理并在3000端口启动
###############################################################################

LOG_FILE="/root/jc-test/logs/frontend-fix.log"

log() {
    echo "[$(date '+%H:%M:%S')] $1" | tee -a $LOG_FILE
}

{
    echo "========================================"
    echo "前端修复日志"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
} > $LOG_FILE

log "开始修复前端..."

# 1. 强制清理3000-3009端口
log "步骤1: 清理3000-3009端口上的所有进程"
for port in {3000..3009}; do
    PID=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$PID" ]; then
        log "  清理端口 $port 上的进程 $PID"
        kill -9 $PID 2>/dev/null || true
    fi
done
sleep 3

# 2. 验证3000端口已释放
log "步骤2: 验证3000端口状态"
if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
    log "  ✗ 3000端口仍被占用"
    # 再次尝试强制清理
    fuser -k 3000/tcp 2>/dev/null || true
    sleep 2
fi

if ! netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
    log "  ✓ 3000端口已释放"
else
    log "  ✗ 无法释放3000端口，将使用其他端口"
fi

# 3. 启动前端
log "步骤3: 启动前端服务"
cd /root/jc-test/frontend

# 加载NVM
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" 2>/dev/null || true
nvm use 16 >> $LOG_FILE 2>&1 || log "  使用系统Node.js"

# 强制指定3000端口启动
log "  启动Vite on port 3000..."
nohup npm run dev -- --port 3000 --strictPort > /root/jc-test/logs/frontend-new.log 2>&1 &
FRONTEND_PID=$!
log "  前端进程PID: $FRONTEND_PID"

# 4. 等待启动
log "步骤4: 等待前端启动（最多30秒）..."
for i in {1..30}; do
    if netstat -tlnp 2>/dev/null | grep -q "127.0.0.1:3000.*LISTEN"; then
        log "  ✓ 前端在3000端口启动成功（耗时 ${i} 秒）"
        
        # 测试访问
        sleep 2
        if timeout 3 curl -s -I http://127.0.0.1:3000 >/dev/null 2>&1; then
            log "  ✓ 前端响应正常"
        else
            log "  ⚠ 前端可能还在初始化中"
        fi
        
        break
    fi
    
    if [ $i -eq 30 ]; then
        log "  ✗ 前端启动超时，查看日志:"
        tail -20 /root/jc-test/logs/frontend-new.log | tee -a $LOG_FILE
        exit 1
    fi
    sleep 1
done

# 5. 输出结果
{
    echo ""
    echo "========================================"
    echo "前端修复完成"
    echo "========================================"
    echo ""
    echo "✅ 前端地址: http://127.0.0.1:3000"
    echo "✅ 后端地址: http://127.0.0.1:8080"
    echo ""
    echo "🔐 登录信息:"
    echo "  - 用户名: admin"
    echo "  - 密码: admin123"
    echo ""
    echo "📝 日志文件:"
    echo "  - 前端启动: /root/jc-test/logs/frontend-new.log"
    echo "  - 后端日志: /root/jc-test/logs/detection-platform.log"
    echo ""
} | tee -a $LOG_FILE

log "✅ 前端修复完成！"

exit 0
