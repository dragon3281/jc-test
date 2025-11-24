#!/bin/bash

###############################################################################
# 完整修复脚本 - 处理terminal污染 + 开启日志 + 修复所有问题
###############################################################################

set -e
export LANG=C
export LC_ALL=C

# 输出文件
REPORT="/root/jc-test/fix-report.txt"
LOG_DIR="/root/jc-test/logs"

# 创建日志目录
mkdir -p $LOG_DIR

# 初始化报告
{
    echo "========================================"
    echo "系统修复报告"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
    echo ""
} > $REPORT

log() {
    echo "[$(date '+%H:%M:%S')] $1" | tee -a $REPORT
}

log "开始系统诊断和修复..."

# ============================================
# 步骤1: 诊断terminal污染源
# ============================================
log ""
log "步骤1: 诊断terminal污染源"
log "----------------------------------------"

# 查找所有可疑进程
ps aux | grep -E "curl|wget|sleep" | grep -v grep >> $REPORT 2>&1 || true
log "已记录可疑进程到报告"

# 查找占用关键端口的进程
{
    echo "端口占用情况:"
    netstat -tlnp 2>/dev/null | grep -E ":(3000|8080|3306|6379|5672)" || echo "无端口占用"
} >> $REPORT

# ============================================
# 步骤2: 清理污染源和旧进程
# ============================================
log ""
log "步骤2: 清理污染源和旧进程"
log "----------------------------------------"

# 记录清理前的状态
{
    echo ""
    echo "清理前进程状态:"
    ps aux | grep -E "[n]ode|[j]ava" | wc -l
} >> $REPORT

# 清理所有可能的污染源
killall -9 curl wget 2>/dev/null || true
sleep 2

# 清理旧的应用进程
killall -9 node java 2>/dev/null || true
sleep 3

log "污染源和旧进程已清理"

# 验证清理结果
{
    echo ""
    echo "清理后进程状态:"
    ps aux | grep -E "[n]ode|[j]ava" | wc -l
    echo "应该显示0"
} >> $REPORT

# ============================================
# 步骤3: 验证Docker基础服务
# ============================================
log ""
log "步骤3: 验证Docker基础服务"
log "----------------------------------------"

{
    echo ""
    echo "Docker容器状态:"
    docker ps --format "table {{.Names}}\t{{.Status}}" 2>&1
} >> $REPORT

# 检查MySQL
if docker ps | grep -q detection-mysql; then
    log "✓ MySQL运行正常"
    
    # 验证数据库连接
    {
        echo ""
        echo "数据库连接测试:"
        docker exec detection-mysql mysql -uroot -p123456 -e "SELECT COUNT(*) as user_count FROM detection_platform.t_user;" 2>&1
    } >> $REPORT
else
    log "✗ MySQL未运行，正在启动..."
    cd /root/jc-test/docker && docker-compose up -d detection-mysql >> $REPORT 2>&1
    sleep 15
fi

# 检查Redis
if docker ps | grep -q detection-redis; then
    log "✓ Redis运行正常"
else
    log "✗ Redis未运行，正在启动..."
    cd /root/jc-test/docker && docker-compose up -d detection-redis >> $REPORT 2>&1
    sleep 5
fi

# 检查RabbitMQ
if docker ps | grep -q detection-rabbitmq; then
    log "✓ RabbitMQ运行正常"
else
    log "✗ RabbitMQ未运行，正在启动..."
    cd /root/jc-test/docker && docker-compose up -d detection-rabbitmq >> $REPORT 2>&1
    sleep 10
fi

# ============================================
# 步骤4: 配置并启动后端（开启DEBUG日志）
# ============================================
log ""
log "步骤4: 启动后端服务（8080端口，DEBUG日志）"
log "----------------------------------------"

cd /root/jc-test/backend

# 加载环境变量
source /etc/profile 2>/dev/null || true

# 检查jar包
if [ ! -f "target/detection-platform-1.0.0.jar" ]; then
    log "jar包不存在，开始编译..."
    {
        echo ""
        echo "Maven编译输出:"
        mvn clean package -DskipTests 2>&1
    } >> $REPORT
    
    if [ $? -eq 0 ]; then
        log "✓ 编译成功"
    else
        log "✗ 编译失败，请查看报告"
        exit 1
    fi
else
    log "jar包已存在: $(stat -c%s target/detection-platform-1.0.0.jar) 字节"
fi

# 启动后端，开启详细日志
log "启动后端应用..."

nohup java -jar target/detection-platform-1.0.0.jar \
    --logging.level.root=INFO \
    --logging.level.com.detection.platform=DEBUG \
    --logging.level.org.springframework.security=DEBUG \
    --logging.level.org.springframework.web=DEBUG \
    --logging.file.name=$LOG_DIR/detection-platform.log \
    --logging.file.max-size=100MB \
    --logging.pattern.console='%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n' \
    > $LOG_DIR/backend-startup.log 2>&1 &

BACKEND_PID=$!
log "后端进程已启动，PID: $BACKEND_PID"

# 等待后端启动
log "等待后端启动（最多60秒）..."
for i in {1..60}; do
    if netstat -tlnp 2>/dev/null | grep -q ":8080.*LISTEN"; then
        log "✓ 后端服务启动成功（耗时 ${i} 秒）"
        {
            echo ""
            echo "后端启动日志（最后20行）:"
            tail -20 $LOG_DIR/backend-startup.log 2>&1
        } >> $REPORT
        break
    fi
    
    if [ $i -eq 60 ]; then
        log "✗ 后端启动超时"
        {
            echo ""
            echo "后端启动失败日志:"
            tail -50 $LOG_DIR/backend-startup.log 2>&1
        } >> $REPORT
        exit 1
    fi
    sleep 1
done

# ============================================
# 步骤5: 启动前端服务
# ============================================
log ""
log "步骤5: 启动前端服务（3000端口）"
log "----------------------------------------"

cd /root/jc-test/frontend

# 加载NVM
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" 2>/dev/null || true

# 使用Node 16
nvm use 16 >> $REPORT 2>&1 || log "NVM不可用，使用系统Node.js"

# 检查依赖
if [ ! -d "node_modules" ]; then
    log "安装前端依赖..."
    {
        echo ""
        echo "npm install 输出:"
        npm install 2>&1
    } >> $REPORT
fi

# 启动前端
log "启动前端应用..."
nohup npm run dev > $LOG_DIR/frontend-startup.log 2>&1 &
FRONTEND_PID=$!
log "前端进程已启动，PID: $FRONTEND_PID"

# 等待前端启动
log "等待前端启动（最多30秒）..."
for i in {1..30}; do
    if netstat -tlnp 2>/dev/null | grep -q ":3000.*LISTEN"; then
        log "✓ 前端服务启动成功（耗时 ${i} 秒）"
        {
            echo ""
            echo "前端启动日志（最后10行）:"
            tail -10 $LOG_DIR/frontend-startup.log 2>&1
        } >> $REPORT
        break
    fi
    
    if [ $i -eq 30 ]; then
        log "✗ 前端启动超时"
        {
            echo ""
            echo "前端启动失败日志:"
            tail -30 $LOG_DIR/frontend-startup.log 2>&1
        } >> $REPORT
        exit 1
    fi
    sleep 1
done

# ============================================
# 步骤6: 服务验证和测试
# ============================================
log ""
log "步骤6: 服务验证和测试"
log "----------------------------------------"

# 端口检查
{
    echo ""
    echo "最终端口状态:"
    netstat -tlnp 2>/dev/null | grep -E ":(3000|8080)" || echo "端口未监听"
} >> $REPORT

# 测试前端
log "测试前端访问（3000端口）..."
if timeout 5 curl -s -I http://127.0.0.1:3000 > /tmp/frontend-test.txt 2>&1; then
    log "✓ 前端响应正常"
    {
        echo ""
        echo "前端响应头:"
        cat /tmp/frontend-test.txt
    } >> $REPORT
else
    log "✗ 前端无响应"
fi

# 测试后端API
log "测试后端API（8080端口）..."
if timeout 5 curl -s http://127.0.0.1:8080/user/test-password?password=test123 > /tmp/backend-test.txt 2>&1; then
    log "✓ 后端API响应正常"
    {
        echo ""
        echo "后端API响应:"
        cat /tmp/backend-test.txt
    } >> $REPORT
else
    log "✗ 后端API无响应"
fi

# 测试登录接口
log "测试登录接口..."
curl -s -X POST http://127.0.0.1:8080/user/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' \
    > /tmp/login-test.txt 2>&1

if grep -q "code" /tmp/login-test.txt; then
    log "✓ 登录接口响应正常"
    {
        echo ""
        echo "登录接口响应:"
        cat /tmp/login-test.txt
    } >> $REPORT
else
    log "⚠ 登录接口响应异常，请查看后端日志"
fi

# ============================================
# 步骤7: 生成摘要和日志指引
# ============================================
{
    echo ""
    echo "========================================"
    echo "修复完成摘要"
    echo "========================================"
    echo ""
    echo "📊 服务状态:"
    echo "  - 前端: http://127.0.0.1:3000"
    echo "  - 后端: http://127.0.0.1:8080"
    echo ""
    echo "🔐 登录信息:"
    echo "  - 用户名: admin"
    echo "  - 密码: admin123"
    echo ""
    echo "📝 日志文件位置:"
    echo "  - 修复报告: /root/jc-test/fix-report.txt"
    echo "  - 后端应用日志(DEBUG): /root/jc-test/logs/detection-platform.log"
    echo "  - 后端启动日志: /root/jc-test/logs/backend-startup.log"
    echo "  - 前端启动日志: /root/jc-test/logs/frontend-startup.log"
    echo ""
    echo "🔍 查看日志命令:"
    echo "  - 实时查看后端日志: tail -f /root/jc-test/logs/detection-platform.log"
    echo "  - 查看登录日志: grep -i login /root/jc-test/logs/detection-platform.log"
    echo "  - 查看错误日志: grep -i error /root/jc-test/logs/detection-platform.log"
    echo ""
    echo "🎯 下一步:"
    echo "  1. 查看完整报告: cat /root/jc-test/fix-report.txt"
    echo "  2. 访问前端界面测试登录功能"
    echo "  3. 如有问题，查看对应的日志文件"
    echo ""
    echo "========================================"
} >> $REPORT

log ""
log "✅ 所有步骤执行完成！"
log "📄 完整报告已保存到: /root/jc-test/fix-report.txt"

# 输出报告路径，确保能看到
echo ""
echo "========================================" 
echo "报告已生成: /root/jc-test/fix-report.txt"
echo "========================================" 

exit 0
