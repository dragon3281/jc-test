#!/bin/bash

# 简单的状态检查脚本
OUTPUT="/root/jc-test/service-status.txt"

{
    echo "=== 服务状态检查 ==="
    echo "时间: $(date)"
    echo ""
    
    echo "1. 端口检查:"
    netstat -tlnp | grep -E ":(3000|8080)" || echo "未找到监听端口"
    echo ""
    
    echo "2. 进程检查:"
    ps aux | grep -E "[j]ava.*detection|[n]ode.*vite" || echo "未找到相关进程"
    echo ""
    
    echo "3. Docker容器:"
    docker ps --format "{{.Names}}: {{.Status}}"
    echo ""
    
    echo "4. 测试前端(3000):"
    timeout 2 curl -s -I http://127.0.0.1:3000 | head -5 || echo "前端无响应"
    echo ""
    
    echo "5. 测试后端(8080):"
    timeout 2 curl -s http://127.0.0.1:8080/user/test-password?password=test || echo "后端无响应"
    echo ""
    
} > $OUTPUT 2>&1

echo "状态已写入: $OUTPUT"
cat $OUTPUT
