#!/bin/bash

###############################################################################
# 端口清理与前端3000端口启动脚本
# 记录详细日志，逐步解决问题
###############################################################################

LOG="/root/jc-test/logs/port-cleanup.log"
mkdir -p /root/jc-test/logs

{
    echo "========================================"
    echo "端口清理日志"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
    echo ""
} > $LOG

log() {
    echo "[$(date '+%H:%M:%S')] $1" | tee -a $LOG
}

log "任务：清理3000-3010端口，确保前端在3000端口启动"
log ""

# 步骤1: 诊断当前端口占用情况
log "步骤1: 诊断当前端口占用情况"
log "----------------------------------------"
{
    echo "当前占用3000-3010端口的进程:"
    netstat -tlnp 2>/dev/null | grep -E ":300[0-9]|:3010"
    echo ""
} | tee -a $LOG

# 获取所有占用端口的PID
PIDS=$(netstat -tlnp 2>/dev/null | grep -E ":300[0-9]|:3010" | awk '{print $7}' | cut -d'/' -f1 | sort -u)

if [ -z "$PIDS" ]; then
    log "✓ 没有进程占用3000-3010端口"
else
    log "发现占用端口的进程: $PIDS"
    {
        echo ""
        echo "进程详细信息:"
        for pid in $PIDS; do
            echo "PID: $pid"
            ps -p $pid -o pid,ppid,user,cmd 2>/dev/null || echo "  进程已退出"
        done
        echo ""
    } | tee -a $LOG
fi

# 步骤2: 尝试正常终止进程
log ""
log "步骤2: 尝试正常终止进程 (SIGTERM)"
log "----------------------------------------"
for pid in $PIDS; do
    if [ -n "$pid" ]; then
        log "  发送SIGTERM到进程 $pid"
        kill -15 $pid 2>/dev/null
    fi
done
sleep 3

# 检查是否还有进程
REMAINING=$(netstat -tlnp 2>/dev/null | grep -E ":300[0-9]|:3010" | awk '{print $7}' | cut -d'/' -f1 | sort -u)
if [ -z "$REMAINING" ]; then
    log "✓ 所有进程已正常终止"
else
    log "⚠ 仍有进程未终止: $REMAINING"
fi

# 步骤3: 强制终止残留进程
log ""
log "步骤3: 强制终止残留进程 (SIGKILL)"
log "----------------------------------------"
REMAINING=$(netstat -tlnp 2>/dev/null | grep -E ":300[0-9]|:3010" | awk '{print $7}' | cut -d'/' -f1 | sort -u)
if [ -n "$REMAINING" ]; then
    for pid in $REMAINING; do
        if [ -n "$pid" ]; then
            log "  发送SIGKILL到进程 $pid"
            kill -9 $pid 2>/dev/null
            if [ $? -eq 0 ]; then
                log "    ✓ 进程 $pid 已强制终止"
            else
                log "    ✗ 无法终止进程 $pid (可能权限不足或进程不存在)"
            fi
        fi
    done
    sleep 2
fi

# 步骤4: 验证端口释放
log ""
log "步骤4: 验证端口释放情况"
log "----------------------------------------"
STILL_OCCUPIED=$(netstat -tlnp 2>/dev/null | grep -E ":300[0-9]|:3010")
if [ -z "$STILL_OCCUPIED" ]; then
    log "✓ 所有端口(3000-3010)已成功释放"
    {
        echo ""
        echo "当前端口状态:"
        netstat -tlnp 2>/dev/null | grep -E ":(3000|8080)" || echo "3000和8080端口都未被占用"
    } | tee -a $LOG
else
    log "✗ 仍有端口被占用:"
    echo "$STILL_OCCUPIED" | tee -a $LOG
    log ""
    log "问题分析: 可能原因包括"
    log "  1. 进程属于其他用户，当前用户无权限终止"
    log "  2. 进程处于僵尸状态"
    log "  3. 系统内核问题"
    log ""
    log "建议解决方案:"
    log "  - 使用root用户执行: sudo kill -9 <PID>"
    log "  - 检查是否有父进程在重启这些进程"
    log "  - 重启系统以彻底清理"
fi

# 步骤5: 启动前端在3000端口
log ""
log "步骤5: 启动前端服务在3000端口"
log "----------------------------------------"

cd /root/jc-test/frontend

# 加载NVM
export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
    \. "$NVM_DIR/nvm.sh"
    log "✓ NVM已加载"
    nvm use 16 >> $LOG 2>&1
    log "✓ 切换到Node.js 16"
else
    log "⚠ NVM未安装，使用系统Node.js"
fi

# 检查node和npm版本
{
    echo ""
    echo "Node.js版本:"
    node -v
    echo "npm版本:"
    npm -v
    echo ""
} | tee -a $LOG

# 检查3000端口是否已释放
if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
    log "✗ 3000端口仍被占用，无法启动前端"
    log "  占用情况:"
    netstat -tlnp 2>/dev/null | grep ":3000" | tee -a $LOG
    log ""
    log "  尝试最后一次强制清理..."
    PID_3000=$(netstat -tlnp 2>/dev/null | grep ":3000" | awk '{print $7}' | cut -d'/' -f1)
    if [ -n "$PID_3000" ]; then
        kill -9 $PID_3000 2>/dev/null
        sleep 3
    fi
fi

# 再次检查
if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
    log "✗ 无法清理3000端口，前端将尝试其他端口"
    # 不使用--strictPort，让vite自动选择可用端口
    nohup npm run dev > /root/jc-test/logs/frontend-auto.log 2>&1 &
    FRONTEND_PID=$!
    log "  前端进程已启动 (PID: $FRONTEND_PID)，将使用自动选择的端口"
else
    log "✓ 3000端口已释放，启动前端..."
    # 使用--strictPort确保只在3000端口启动
    nohup npm run dev -- --port 3000 --strictPort > /root/jc-test/logs/frontend-3000-strict.log 2>&1 &
    FRONTEND_PID=$!
    log "  前端进程已启动 (PID: $FRONTEND_PID)，严格使用3000端口"
fi

# 步骤6: 等待前端启动并验证
log ""
log "步骤6: 等待前端启动并验证"
log "----------------------------------------"
log "等待最多30秒..."

SUCCESS=false
for i in {1..30}; do
    if netstat -tlnp 2>/dev/null | grep -q "127.0.0.1:3000.*LISTEN"; then
        log "✓ 前端在3000端口启动成功 (耗时 ${i} 秒)"
        SUCCESS=true
        
        # 测试HTTP响应
        sleep 2
        if timeout 5 curl -s -I http://127.0.0.1:3000 > /tmp/fe-test.txt 2>&1; then
            HTTP_STATUS=$(head -1 /tmp/fe-test.txt)
            log "  HTTP响应: $HTTP_STATUS"
            if echo "$HTTP_STATUS" | grep -q "200"; then
                log "  ✓ 前端响应正常"
            else
                log "  ⚠ 前端返回非200状态"
            fi
        else
            log "  ⚠ 前端HTTP请求超时或失败"
        fi
        break
    fi
    sleep 1
done

if [ "$SUCCESS" = false ]; then
    log "✗ 前端未在3000端口启动"
    log "  检查实际启动端口..."
    ACTUAL_PORT=$(netstat -tlnp 2>/dev/null | grep "$FRONTEND_PID" | grep "127.0.0.1" | awk '{print $4}' | cut -d':' -f2)
    if [ -n "$ACTUAL_PORT" ]; then
        log "  前端实际启动在端口: $ACTUAL_PORT"
        log "  查看启动日志以了解原因:"
        if [ -f "/root/jc-test/logs/frontend-3000-strict.log" ]; then
            tail -20 /root/jc-test/logs/frontend-3000-strict.log | tee -a $LOG
        elif [ -f "/root/jc-test/logs/frontend-auto.log" ]; then
            tail -20 /root/jc-test/logs/frontend-auto.log | tee -a $LOG
        fi
    else
        log "  ✗ 前端进程可能启动失败"
    fi
fi

# 步骤7: 生成最终报告
log ""
log "========================================"
log "最终状态报告"
log "========================================"

{
    echo ""
    echo "端口占用情况:"
    netstat -tlnp 2>/dev/null | grep -E ":(3000|8080)" || echo "  3000和8080端口未被占用"
    echo ""
    echo "前端进程:"
    ps aux | grep -E "[n]ode.*vite|[n]pm.*dev" || echo "  无前端进程运行"
    echo ""
    echo "后端进程:"
    ps aux | grep -E "[j]ava.*detection-platform" || echo "  无后端进程运行"
    echo ""
} | tee -a $LOG

log ""
log "📝 完整日志已保存到: $LOG"
log ""

if [ "$SUCCESS" = true ]; then
    log "✅ 任务完成: 前端已在3000端口成功启动"
    log ""
    log "🌐 访问地址:"
    log "  - 前端: http://127.0.0.1:3000"
    log "  - 后端: http://127.0.0.1:8080"
    log ""
    log "🔐 登录信息:"
    log "  - 用户名: admin"
    log "  - 密码: admin123"
else
    log "⚠ 任务未完全成功: 前端未能在3000端口启动"
    log "  请查看日志文件了解详细信息"
fi

exit 0
