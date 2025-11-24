#!/bin/bash

###############################################################################
# 自动化数据检测平台 - 服务重启脚本
# 功能：清理环境、重启所有服务、开启详细日志
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 日志目录
LOG_DIR="/root/jc-test/logs"
DEPLOY_LOG="$LOG_DIR/deployment.log"

# 创建日志目录
mkdir -p $LOG_DIR

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1" | tee -a $DEPLOY_LOG
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a $DEPLOY_LOG
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a $DEPLOY_LOG
}

# 初始化日志
echo "========================================" > $DEPLOY_LOG
echo "服务重启日志" >> $DEPLOY_LOG
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')" >> $DEPLOY_LOG
echo "========================================" >> $DEPLOY_LOG

log_info "开始服务重启流程..."

# 1. 清理旧进程
log_info "步骤1: 清理旧进程"
pkill -9 node 2>/dev/null || log_warn "没有node进程需要清理"
pkill -9 java 2>/dev/null || log_warn "没有java进程需要清理"
sleep 3
log_info "进程清理完成"

# 2. 检查Docker服务
log_info "步骤2: 检查Docker服务"
if ! docker ps >/dev/null 2>&1; then
    log_error "Docker服务未运行"
    exit 1
fi

# 检查MySQL
if docker ps | grep -q detection-mysql; then
    log_info "✓ MySQL运行正常"
else
    log_warn "MySQL未运行，尝试启动..."
    cd /root/jc-test/docker && docker-compose up -d detection-mysql
    sleep 10
fi

# 检查Redis
if docker ps | grep -q detection-redis; then
    log_info "✓ Redis运行正常"
else
    log_warn "Redis未运行，尝试启动..."
    cd /root/jc-test/docker && docker-compose up -d detection-redis
    sleep 5
fi

# 检查RabbitMQ
if docker ps | grep -q detection-rabbitmq; then
    log_info "✓ RabbitMQ运行正常"
else
    log_warn "RabbitMQ未运行，尝试启动..."
    cd /root/jc-test/docker && docker-compose up -d detection-rabbitmq
    sleep 10
fi

# 3. 启动后端服务
log_info "步骤3: 启动后端服务 (端口8080)"
cd /root/jc-test/backend

# 确保环境变量已加载
source /etc/profile 2>/dev/null || true

# 检查jar包是否存在
if [ ! -f "target/detection-platform-1.0.0.jar" ]; then
    log_warn "jar包不存在，开始编译..."
    mvn clean package -DskipTests >> $DEPLOY_LOG 2>&1
    if [ $? -ne 0 ]; then
        log_error "编译失败，请查看日志"
        exit 1
    fi
    log_info "编译成功"
fi

# 启动后端
log_info "启动后端服务..."
nohup java -jar target/detection-platform-1.0.0.jar \
    --logging.file.name=$LOG_DIR/detection-platform.log \
    --logging.level.root=INFO \
    --logging.level.com.detection.platform=DEBUG \
    > $LOG_DIR/backend-startup.log 2>&1 &

BACKEND_PID=$!
log_info "后端进程PID: $BACKEND_PID"

# 等待后端启动
log_info "等待后端启动(最多60秒)..."
for i in {1..60}; do
    if netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
        log_info "✓ 后端服务启动成功 (耗时${i}秒)"
        break
    fi
    if [ $i -eq 60 ]; then
        log_error "后端启动超时"
        tail -50 $LOG_DIR/backend-startup.log
        exit 1
    fi
    sleep 1
done

# 4. 启动前端服务
log_info "步骤4: 启动前端服务 (端口3000)"
cd /root/jc-test/frontend

# 加载NVM
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
nvm use 16 >> $DEPLOY_LOG 2>&1 || log_warn "NVM切换失败，使用系统Node.js"

# 检查依赖
if [ ! -d "node_modules" ]; then
    log_warn "node_modules不存在，开始安装依赖..."
    npm install >> $DEPLOY_LOG 2>&1
    log_info "依赖安装完成"
fi

# 启动前端
log_info "启动前端服务..."
nohup npm run dev > $LOG_DIR/frontend-startup.log 2>&1 &
FRONTEND_PID=$!
log_info "前端进程PID: $FRONTEND_PID"

# 等待前端启动
log_info "等待前端启动(最多30秒)..."
for i in {1..30}; do
    if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
        log_info "✓ 前端服务启动成功 (耗时${i}秒)"
        break
    fi
    if [ $i -eq 30 ]; then
        log_error "前端启动超时"
        tail -50 $LOG_DIR/frontend-startup.log
        exit 1
    fi
    sleep 1
done

# 5. 验证服务
log_info "步骤5: 验证服务"

# 测试前端
log_info "测试前端访问..."
if timeout 3 curl -I http://127.0.0.1:3000 >/dev/null 2>&1; then
    log_info "✓ 前端访问正常"
else
    log_warn "前端访问失败，请手动检查"
fi

# 测试后端
log_info "测试后端API..."
if timeout 3 curl -I http://127.0.0.1:8080/user/test-password?password=test >/dev/null 2>&1; then
    log_info "✓ 后端API访问正常"
else
    log_warn "后端API访问失败，请手动检查"
fi

# 6. 输出摘要
echo ""
echo "========================================"
echo "服务启动完成"
echo "========================================"
echo ""
echo "📊 服务状态:"
echo "  - 前端: http://127.0.0.1:3000"
echo "  - 后端: http://127.0.0.1:8080"
echo "  - MySQL: localhost:3306"
echo "  - Redis: localhost:6379"
echo "  - RabbitMQ: http://localhost:15672 (admin/admin)"
echo ""
echo "📝 日志文件:"
echo "  - 部署日志: $DEPLOY_LOG"
echo "  - 后端日志: $LOG_DIR/detection-platform.log"
echo "  - 后端启动: $LOG_DIR/backend-startup.log"
echo "  - 前端启动: $LOG_DIR/frontend-startup.log"
echo ""
echo "🔐 登录账号:"
echo "  - 用户名: admin"
echo "  - 密码: admin123"
echo ""
echo "========================================"
echo ""

log_info "服务重启流程完成！"
