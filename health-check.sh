#!/bin/bash

###########################################
# 自动化数据检测平台 - 健康监控脚本
# 可以作为cron任务定期执行
###########################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

echo "========================================="
echo "  自动化数据检测平台 - 健康检查"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""

HEALTHY=true

# 1. 检查Docker服务
echo "【1. Docker服务】"
cd /root/jc-test/docker
if docker-compose ps | grep -q "Up"; then
    log_info "MySQL, Redis, RabbitMQ 运行正常"
else
    log_error "Docker服务异常"
    HEALTHY=false
    docker-compose ps
fi
echo ""

# 2. 检查后端服务
echo "【2. 后端服务 (8080)】"
if netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
    log_info "端口监听正常"
    
    # 测试API
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/user/login -X POST \
        -H "Content-Type: application/json" \
        -d '{"username":"test","password":"test"}' | grep -q "200\|401\|400"; then
        log_info "API响应正常"
    else
        log_error "API无响应"
        HEALTHY=false
    fi
else
    log_error "端口未监听"
    HEALTHY=false
fi

# 检查后端进程
if systemctl is-active --quiet detection-backend 2>/dev/null; then
    log_info "systemd服务运行中"
elif pgrep -f "detection-platform-1.0.0.jar" > /dev/null; then
    log_info "进程运行中 (PID: $(pgrep -f 'detection-platform-1.0.0.jar'))"
else
    log_error "后端进程不存在"
    HEALTHY=false
fi
echo ""

# 3. 检查前端服务
echo "【3. 前端服务 (3000)】"
if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
    log_info "端口监听正常"
    
    # 测试前端访问
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 | grep -q "200"; then
        log_info "HTTP响应正常"
    else
        log_warn "HTTP响应异常"
    fi
else
    log_error "端口未监听"
    HEALTHY=false
fi

# 检查前端进程
if systemctl is-active --quiet detection-frontend 2>/dev/null; then
    log_info "systemd服务运行中"
elif pgrep -f "vite.*3000" > /dev/null; then
    log_info "进程运行中 (PID: $(pgrep -f 'vite.*3000'))"
else
    log_error "前端进程不存在"
    HEALTHY=false
fi
echo ""

# 4. WebSocket连接测试
echo "【4. WebSocket连接】"
if netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
    log_info "WebSocket端点可用 (ws://localhost:8080/ws)"
else
    log_error "WebSocket端点不可用"
    HEALTHY=false
fi
echo ""

# 5. 系统资源
echo "【5. 系统资源】"
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
MEM_USAGE=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100}')
DISK_USAGE=$(df -h /root | tail -1 | awk '{print $5}' | sed 's/%//')

echo "  CPU使用率: ${CPU_USAGE}%"
echo "  内存使用率: ${MEM_USAGE}%"
echo "  磁盘使用率: ${DISK_USAGE}%"

if (( $(echo "$CPU_USAGE > 80" | bc -l 2>/dev/null || echo 0) )); then
    log_warn "CPU使用率偏高"
fi
if (( $(echo "$MEM_USAGE > 80" | bc -l 2>/dev/null || echo 0) )); then
    log_warn "内存使用率偏高"
fi
if [ "$DISK_USAGE" -gt 80 ]; then
    log_warn "磁盘使用率偏高"
fi
echo ""

# 6. 日志文件大小
echo "【6. 日志文件】"
LOG_DIR="/root/jc-test/logs"
if [ -d "$LOG_DIR" ]; then
    TOTAL_SIZE=$(du -sh "$LOG_DIR" 2>/dev/null | cut -f1)
    echo "  日志目录大小: $TOTAL_SIZE"
    
    # 检查是否有大文件
    find "$LOG_DIR" -type f -size +100M 2>/dev/null | while read file; do
        SIZE=$(du -h "$file" | cut -f1)
        log_warn "大文件: $file ($SIZE)"
    done
fi
echo ""

# 总结
echo "========================================="
if [ "$HEALTHY" = true ]; then
    log_info "系统健康状态: 正常"
    echo ""
    echo "访问地址:"
    echo "  前端: http://$(hostname -I | awk '{print $1}'):3000"
    echo "  后端: http://$(hostname -I | awk '{print $1}'):8080"
else
    log_error "系统健康状态: 异常"
    echo ""
    echo "建议操作:"
    echo "  1. 查看日志: tail -f $LOG_DIR/*.log"
    echo "  2. 重启服务: bash /root/jc-test/persistent-start.sh"
    echo "  3. 查看systemd状态: systemctl status detection-backend detection-frontend"
fi
echo "========================================="
