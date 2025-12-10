#!/bin/bash

###########################################
# 自动化数据检测平台 - 持久化启动脚本
# 支持systemd服务方式和nohup后台运行
###########################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志目录
LOG_DIR="/root/jc-test/logs"
mkdir -p "$LOG_DIR"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "========================================="
echo "  自动化数据检测平台 - 持久化启动"
echo "========================================="
echo ""

# 检测启动方式：systemd 或 nohup
START_MODE="${1:-auto}"

if [ "$START_MODE" == "systemd" ] || [ "$START_MODE" == "auto" ]; then
    log_info "尝试使用systemd服务启动..."
    
    # 检查是否支持systemd
    if command -v systemctl &> /dev/null; then
        log_info "检测到systemd，使用服务方式启动"
        
        # 创建日志目录
        mkdir -p "$LOG_DIR"
        
        # 1. 停止可能存在的旧服务
        log_info "停止旧服务..."
        systemctl stop detection-frontend 2>/dev/null || true
        systemctl stop detection-backend 2>/dev/null || true
        
        # 等待进程完全停止
        sleep 3
        
        # 2. 确保Docker服务运行
        log_info "检查Docker服务..."
        cd /root/jc-test/docker
        if ! docker-compose ps | grep -q "Up"; then
            log_warn "Docker服务未运行，正在启动..."
            docker-compose up -d
            log_info "等待Docker服务启动(30秒)..."
            sleep 30
        else
            log_info "✓ Docker服务已运行"
        fi
        
        # 3. 检查后端jar包
        log_info "检查后端jar包..."
        cd /root/jc-test/backend
        if [ ! -f "target/detection-platform-1.0.0.jar" ]; then
            log_warn "jar包不存在，开始编译..."
            source /etc/profile 2>/dev/null || true
            mvn clean package -DskipTests
            log_info "✓ 编译完成"
        else
            log_info "✓ jar包已存在"
        fi
        
        # 4. 安装systemd服务文件
        log_info "安装systemd服务..."
        cp /root/jc-test/detection-backend.service /etc/systemd/system/
        cp /root/jc-test/detection-frontend.service /etc/systemd/system/
        
        # 重新加载systemd
        systemctl daemon-reload
        
        # 5. 启动后端服务
        log_info "启动后端服务..."
        systemctl enable detection-backend
        systemctl start detection-backend
        
        # 等待后端启动
        log_info "等待后端启动(最多60秒)..."
        for i in {1..60}; do
            if systemctl is-active --quiet detection-backend && netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
                log_info "✓ 后端服务启动成功(耗时${i}秒)"
                break
            fi
            if [ $i -eq 60 ]; then
                log_error "后端启动超时"
                log_error "查看日志: journalctl -u detection-backend -n 50"
                exit 1
            fi
            sleep 1
        done
        
        # 6. 启动前端服务
        log_info "启动前端服务..."
        systemctl enable detection-frontend
        systemctl start detection-frontend
        
        # 等待前端启动
        log_info "等待前端启动(最多30秒)..."
        for i in {1..30}; do
            if systemctl is-active --quiet detection-frontend && netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
                log_info "✓ 前端服务启动成功(耗时${i}秒)"
                break
            fi
            if [ $i -eq 30 ]; then
                log_error "前端启动超时"
                log_error "查看日志: journalctl -u detection-frontend -n 50"
                exit 1
            fi
            sleep 1
        done
        
        echo ""
        echo "========================================="
        echo "  服务启动成功 (systemd模式)"
        echo "========================================="
        echo ""
        log_info "服务状态:"
        echo "  后端服务: $(systemctl is-active detection-backend)"
        echo "  前端服务: $(systemctl is-active detection-frontend)"
        echo ""
        log_info "管理命令:"
        echo "  查看后端状态: systemctl status detection-backend"
        echo "  查看前端状态: systemctl status detection-frontend"
        echo "  查看后端日志: journalctl -u detection-backend -f"
        echo "  查看前端日志: journalctl -u detection-frontend -f"
        echo "  重启后端: systemctl restart detection-backend"
        echo "  重启前端: systemctl restart detection-frontend"
        echo "  停止服务: systemctl stop detection-backend detection-frontend"
        echo ""
        
    else
        log_warn "系统不支持systemd，使用nohup方式启动"
        START_MODE="nohup"
    fi
fi

if [ "$START_MODE" == "nohup" ]; then
    log_info "使用nohup后台方式启动..."
    
    # 1. 停止旧进程
    log_info "停止旧进程..."
    pkill -f "detection-platform-1.0.0.jar" 2>/dev/null || true
    pkill -f "vite.*3000" 2>/dev/null || true
    sleep 3
    
    # 2. 确保Docker服务运行
    log_info "检查Docker服务..."
    cd /root/jc-test/docker
    if ! docker-compose ps | grep -q "Up"; then
        log_warn "Docker服务未运行，正在启动..."
        docker-compose up -d
        log_info "等待Docker服务启动(30秒)..."
        sleep 30
    else
        log_info "✓ Docker服务已运行"
    fi
    
    # 3. 启动后端
    log_info "启动后端服务..."
    cd /root/jc-test/backend
    
    source /etc/profile 2>/dev/null || true
    
    if [ ! -f "target/detection-platform-1.0.0.jar" ]; then
        log_warn "jar包不存在，开始编译..."
        mvn clean package -DskipTests
        log_info "✓ 编译完成"
    fi
    
    nohup java -jar \
        -Xms512m \
        -Xmx2048m \
        -XX:+UseG1GC \
        target/detection-platform-1.0.0.jar \
        --logging.file.name="$LOG_DIR/detection-platform.log" \
        --logging.level.root=INFO \
        --logging.level.com.detection.platform=DEBUG \
        > "$LOG_DIR/backend-startup.log" 2>&1 &
    
    BACKEND_PID=$!
    log_info "后端进程PID: $BACKEND_PID"
    echo $BACKEND_PID > "$LOG_DIR/backend.pid"
    
    # 等待后端启动
    log_info "等待后端启动(最多60秒)..."
    for i in {1..60}; do
        if netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
            log_info "✓ 后端服务启动成功(耗时${i}秒)"
            break
        fi
        if [ $i -eq 60 ]; then
            log_error "后端启动超时"
            log_error "查看日志: tail -50 $LOG_DIR/backend-startup.log"
            exit 1
        fi
        sleep 1
    done
    
    # 4. 启动前端
    log_info "启动前端服务..."
    cd /root/jc-test/frontend
    
    # 加载NVM
    export NVM_DIR="$HOME/.nvm"
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
    nvm use 16 2>/dev/null || log_warn "NVM切换失败，使用系统Node.js"
    
    # 检查依赖
    if [ ! -d "node_modules" ]; then
        log_warn "node_modules不存在，安装依赖..."
        npm install
        log_info "✓ 依赖安装完成"
    fi
    
    nohup npm run dev > "$LOG_DIR/frontend-startup.log" 2>&1 &
    FRONTEND_PID=$!
    log_info "前端进程PID: $FRONTEND_PID"
    echo $FRONTEND_PID > "$LOG_DIR/frontend.pid"
    
    # 等待前端启动
    log_info "等待前端启动(最多30秒)..."
    for i in {1..30}; do
        if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
            log_info "✓ 前端服务启动成功(耗时${i}秒)"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "前端启动超时"
            log_error "查看日志: tail -50 $LOG_DIR/frontend-startup.log"
            exit 1
        fi
        sleep 1
    done
    
    echo ""
    echo "========================================="
    echo "  服务启动成功 (nohup模式)"
    echo "========================================="
    echo ""
    log_info "进程信息:"
    echo "  后端PID: $BACKEND_PID"
    echo "  前端PID: $FRONTEND_PID"
    echo ""
    log_info "管理命令:"
    echo "  查看后端日志: tail -f $LOG_DIR/backend-startup.log"
    echo "  查看前端日志: tail -f $LOG_DIR/frontend-startup.log"
    echo "  查看应用日志: tail -f $LOG_DIR/detection-platform.log"
    echo "  停止后端: kill \$(cat $LOG_DIR/backend.pid)"
    echo "  停止前端: kill \$(cat $LOG_DIR/frontend.pid)"
    echo ""
fi

# 验证服务
echo "========================================="
echo "  服务验证"
echo "========================================="
echo ""

log_info "端口监听状态:"
netstat -tlnp | grep -E ":(3000|8080)" || log_warn "端口未监听"

echo ""
log_info "访问地址:"
echo "  前端: http://$(hostname -I | awk '{print $1}'):3000"
echo "  后端: http://$(hostname -I | awk '{print $1}'):8080"
echo "  默认账号: admin / admin123"
echo ""

log_info "持久化说明:"
if [ "$START_MODE" == "systemd" ] || systemctl is-active --quiet detection-backend 2>/dev/null; then
    echo "  ✓ 使用systemd服务，开机自启，进程守护"
    echo "  ✓ 异常退出自动重启"
else
    echo "  ✓ 使用nohup后台运行"
    echo "  ⚠ 需要手动创建开机启动脚本"
fi

echo ""
echo "========================================="
