#!/bin/bash

LOG="/root/jc-test/logs/login-test.log"

{
    echo "========================================"
    echo "登录测试日志"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
    echo ""
    
    echo "1. 测试后端直接访问 (8080端口)"
    echo "----------------------------------------"
    echo "请求: POST http://127.0.0.1:8080/user/login"
    curl -v -X POST http://127.0.0.1:8080/user/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' 2>&1
    echo ""
    echo ""
    
    echo "2. 测试前端代理访问 (3000端口 -> 8080)"
    echo "----------------------------------------"
    echo "请求: POST http://127.0.0.1:3000/api/user/login"
    timeout 10 curl -v -X POST http://127.0.0.1:3000/api/user/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' 2>&1 || echo "请求超时或失败"
    echo ""
    echo ""
    
    echo "3. 查看后端日志中的登录记录"
    echo "----------------------------------------"
    grep -i "user/login\|POST.*login" /root/jc-test/logs/detection-platform.log | tail -10 || echo "无登录日志"
    echo ""
    
    echo "4. 检查Vite代理配置"
    echo "----------------------------------------"
    cat /root/jc-test/frontend/vite.config.js
    echo ""
    
    echo "5. 端口监听状态"
    echo "----------------------------------------"
    netstat -tlnp | grep -E ":(3000|8080)"
    echo ""
    
    echo "========================================"
    echo "测试完成"
    echo "========================================"
} > $LOG 2>&1

echo "测试日志已保存到: $LOG"
cat $LOG
