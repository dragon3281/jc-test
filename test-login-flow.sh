#!/bin/bash

LOG="/root/jc-test/logs/login-flow-test.log"

{
    echo "========================================"
    echo "登录流程测试"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
    echo ""
    
    echo "测试1: 前端访问测试"
    echo "----------------------------------------"
    echo "GET http://103.246.244.229:3000"
    curl -I http://103.246.244.229:3000 2>&1 | head -10
    echo ""
    
    echo "测试2: 直接调用后端登录API"
    echo "----------------------------------------"
    echo "POST http://103.246.244.229:8080/user/login"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://103.246.244.229:8080/user/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$RESPONSE" | grep -v "HTTP_CODE")
    
    echo "HTTP状态码: $HTTP_CODE"
    echo "响应体: $BODY"
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 后端登录API正常"
        TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$TOKEN" ]; then
            echo "✓ 获得Token: ${TOKEN:0:50}..."
        fi
    else
        echo "✗ 后端登录API失败"
    fi
    echo ""
    
    echo "测试3: 通过前端代理调用登录API"
    echo "----------------------------------------"
    echo "POST http://103.246.244.229:3000/api/user/login"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://103.246.244.229:3000/api/user/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$RESPONSE" | grep -v "HTTP_CODE")
    
    echo "HTTP状态码: $HTTP_CODE"
    echo "响应体: $BODY"
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 前端代理登录API正常"
        TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$TOKEN" ]; then
            echo "✓ 获得Token: ${TOKEN:0:50}..."
            
            echo ""
            echo "测试4: 使用Token访问需要认证的接口"
            echo "----------------------------------------"
            echo "GET http://103.246.244.229:8080/user/info (with token)"
            USER_INFO=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
                -H "Authorization: Bearer $TOKEN" \
                http://103.246.244.229:8080/user/info)
            
            INFO_CODE=$(echo "$USER_INFO" | grep "HTTP_CODE" | cut -d: -f2)
            INFO_BODY=$(echo "$USER_INFO" | grep -v "HTTP_CODE")
            
            echo "HTTP状态码: $INFO_CODE"
            echo "响应体: $INFO_BODY"
            
            if [ "$INFO_CODE" = "200" ]; then
                echo "✓ Token验证通过"
            elif [ "$INFO_CODE" = "401" ]; then
                echo "✗ Token验证失败 (401)"
            else
                echo "⚠ 接口可能不存在或其他错误"
            fi
        fi
    elif [ "$HTTP_CODE" = "401" ]; then
        echo "✗ 返回401错误"
        echo "  原因分析:"
        echo "  1. 检查是否URL路径错误导致请求被JWT过滤器拦截"
        echo "  2. 检查SecurityConfig中的公开路径配置"
        echo "  3. 查看后端日志: tail -50 /root/jc-test/logs/detection-platform.log"
    else
        echo "✗ 前端代理登录API失败"
    fi
    echo ""
    
    echo "测试5: 检查Vite代理日志"
    echo "----------------------------------------"
    if [ -f "/root/jc-test/logs/frontend-running.log" ]; then
        echo "最近的代理日志:"
        tail -30 /root/jc-test/logs/frontend-running.log | grep -i "proxy\|error" || echo "无代理相关日志"
    else
        echo "前端日志文件不存在"
    fi
    echo ""
    
    echo "========================================"
    echo "测试完成"
    echo "========================================"
} | tee $LOG

echo ""
echo "完整日志已保存到: $LOG"
