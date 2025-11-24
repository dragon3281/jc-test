#!/bin/bash

###############################################################################
# 前端全局E2E测试脚本
# 测试所有主要功能页面的API调用
###############################################################################

LOG="/root/jc-test/logs/e2e-full-test.log"
BASE_URL="http://103.246.244.229:3000"
API_URL="http://103.246.244.229:8080"

{
    echo "========================================"
    echo "前端全局 E2E 测试"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
    echo ""
    
    # 测试1: 登录获取Token
    echo "测试1: 登录功能"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST $BASE_URL/api/user/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$RESPONSE" | grep -v "HTTP_CODE")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 登录成功 (200)"
        TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        echo "✓ Token: ${TOKEN:0:50}..."
    else
        echo "✗ 登录失败 ($HTTP_CODE)"
        echo "$BODY"
        exit 1
    fi
    echo ""
    
    # 测试2: 数据中心 - 基础数据
    echo "测试2: 数据中心 - 基础数据"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/data/base/page?current=1&size=20")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$RESPONSE" | grep -v "HTTP_CODE")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 基础数据API正常 (200)"
        echo "  响应: ${BODY:0:100}..."
    else
        echo "✗ 基础数据API失败 ($HTTP_CODE)"
        echo "  响应: $BODY"
    fi
    echo ""
    
    # 测试3: 数据中心 - 检测结果（最新）
    echo "测试3: 数据中心 - 检测结果（最新）"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/data/result/page?current=1&size=20&latest=true")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 检测结果（最新）API正常 (200)"
    else
        echo "✗ 检测结果（最新）API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试4: 数据中心 - 检测结果（历史）
    echo "测试4: 数据中心 - 检测结果（历史）"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/data/result/page?current=1&size=20")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 检测结果（历史）API正常 (200)"
    else
        echo "✗ 检测结果（历史）API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试5: 数据统计
    echo "测试5: 数据中心 - 统计信息"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/data/result/statistics")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 统计API正常 (200)"
    else
        echo "✗ 统计API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试6: 任务管理 - 任务列表
    echo "测试6: 任务管理 - 任务列表"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/task/page?current=1&size=20")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 任务列表API正常 (200)"
    else
        echo "✗ 任务列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试7: 任务管理 - 已完成任务
    echo "测试7: 任务管理 - 已完成任务"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/task/page?current=1&size=20&status=3")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 已完成任务API正常 (200)"
    else
        echo "✗ 已完成任务API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试8: 业务管理 - 模板列表
    echo "测试8: 业务管理 - POST模板"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/template/page?current=1&size=20")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 模板列表API正常 (200)"
    else
        echo "✗ 模板列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试9: 业务管理 - 注册列表
    echo "测试9: 业务管理 - 注册列表"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/business/register/list?current=1&size=20")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 注册列表API正常 (200)"
    else
        echo "✗ 注册列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试10: 业务管理 - 分析列表
    echo "测试10: 业务管理 - 分析列表"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/business/analysis/list?current=1&size=20")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 分析列表API正常 (200)"
    else
        echo "✗ 分析列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试11: 获取代理池列表（多个页面使用）
    echo "测试11: 资源管理 - 代理池列表"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/proxy/pool/list")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 代理池列表API正常 (200)"
    else
        echo "✗ 代理池列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试12: 获取服务器列表
    echo "测试12: 资源管理 - 服务器列表"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/server/list")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 服务器列表API正常 (200)"
    else
        echo "✗ 服务器列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    # 测试13: 获取模板列表（创建任务页面）
    echo "测试13: 创建任务 - 模板列表"
    echo "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/template/list")
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✓ 模板列表API正常 (200)"
    else
        echo "✗ 模板列表API失败 ($HTTP_CODE)"
    fi
    echo ""
    
    echo "========================================"
    echo "测试总结"
    echo "========================================"
    echo ""
    echo "所有测试完成！"
    echo ""
    echo "如果所有测试显示 ✓，说明前端API调用已全部修复"
    echo "如果有任何 ✗，请查看具体错误信息"
    echo ""
    echo "完整日志: $LOG"
    echo ""
    
} | tee $LOG

echo "测试完成！"
