#!/bin/bash

###############################################################################
# 全局E2E测试脚本
# 测试所有子菜单功能
###############################################################################

BASE_URL="http://103.246.244.229:3000"
LOG_FILE="/root/jc-test/logs/e2e-complete-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "$(dirname "$LOG_FILE")"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

test_api() {
    local name="$1"
    local method="$2"
    local url="$3"
    local data="$4"
    
    log "测试: $name"
    log "  请求: $method $url"
    
    if [ "$method" = "POST" ]; then
        RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
            -X POST "$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data")
    else
        RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
            -H "Authorization: Bearer $TOKEN" \
            "$url")
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$RESPONSE" | grep -v "HTTP_CODE")
    
    log "  状态码: $HTTP_CODE"
    
    if [ "$HTTP_CODE" = "200" ]; then
        log "  结果: ✓ 成功"
        echo "$BODY" | head -3 | while read line; do
            log "    $line"
        done
        return 0
    else
        log "  结果: ✗ 失败"
        log "  响应: $BODY"
        return 1
    fi
}

{
    log "========================================"
    log "全局E2E测试开始"
    log "========================================"
    log ""
    
    # 步骤1: 登录获取Token
    log "步骤1: 用户登录"
    log "----------------------------------------"
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
        -X POST "$BASE_URL/api/user/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$HTTP_CODE" != "200" ]; then
        log "✗ 登录失败，HTTP状态码: $HTTP_CODE"
        log "响应: $(echo "$RESPONSE" | grep -v HTTP_CODE)"
        exit 1
    fi
    
    TOKEN=$(echo "$RESPONSE" | grep -v HTTP_CODE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$TOKEN" ]; then
        log "✗ 未能获取Token"
        exit 1
    fi
    
    log "✓ 登录成功"
    log "Token: ${TOKEN:0:50}..."
    log ""
    
    # 测试计数
    TOTAL=0
    SUCCESS=0
    FAILED=0
    
    # 步骤2: 数据中心测试
    log "步骤2: 数据中心模块测试"
    log "========================================"
    log ""
    
    # 2.1 基础数据
    log "2.1 基础数据管理"
    ((TOTAL++))
    test_api "获取基础数据列表" "GET" "$BASE_URL/api/data/base/page?current=1&size=10" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 2.2 历史检测结果
    log "2.2 历史检测结果"
    ((TOTAL++))
    test_api "获取历史结果" "GET" "$BASE_URL/api/data/result/page?current=1&size=10" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    ((TOTAL++))
    test_api "统计数据" "GET" "$BASE_URL/api/data/result/statistics" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 2.3 最新检测结果
    log "2.3 最新检测结果"
    ((TOTAL++))
    test_api "获取最新结果" "GET" "$BASE_URL/api/data/result/page?current=1&size=10&latest=true" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 步骤3: 检测任务测试
    log "步骤3: 检测任务模块测试"
    log "========================================"
    log ""
    
    # 3.1 任务列表
    log "3.1 任务列表"
    ((TOTAL++))
    test_api "获取任务列表" "GET" "$BASE_URL/api/task/page?current=1&size=10" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 3.2 已完成任务
    log "3.2 已完成任务"
    ((TOTAL++))
    test_api "获取已完成任务" "GET" "$BASE_URL/api/task/page?current=1&size=10&taskStatus=4" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 3.3 创建任务依赖数据
    log "3.3 创建任务依赖数据"
    ((TOTAL++))
    test_api "获取模板列表" "GET" "$BASE_URL/api/template/list" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    ((TOTAL++))
    test_api "获取代理池列表" "GET" "$BASE_URL/api/proxy/pool/list" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    ((TOTAL++))
    test_api "获取服务器列表" "GET" "$BASE_URL/api/server/list" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 步骤4: 业务中心测试
    log "步骤4: 业务中心模块测试"
    log "========================================"
    log ""
    
    # 4.1 POST模板
    log "4.1 POST模板管理"
    ((TOTAL++))
    test_api "获取模板分页" "GET" "$BASE_URL/api/template/page?current=1&size=10" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 4.2 注册管理
    log "4.2 注册管理"
    ((TOTAL++))
    test_api "获取注册任务列表" "GET" "$BASE_URL/api/business/register/list" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 4.3 分析管理
    log "4.3 网站分析"
    ((TOTAL++))
    test_api "获取分析列表" "GET" "$BASE_URL/api/business/analysis/list" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 步骤5: 代理管理测试
    log "步骤5: 代理管理模块测试"
    log "========================================"
    log ""
    
    ((TOTAL++))
    test_api "获取代理池分页" "GET" "$BASE_URL/api/proxy/pool/page?current=1&size=10" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 步骤6: 服务器管理测试
    log "步骤6: 服务器管理模块测试"
    log "========================================"
    log ""
    
    ((TOTAL++))
    test_api "获取服务器分页" "GET" "$BASE_URL/api/server/page?current=1&size=10" && ((SUCCESS++)) || ((FAILED++))
    log ""
    
    # 测试总结
    log "========================================"
    log "测试总结"
    log "========================================"
    log "总测试数: $TOTAL"
    log "成功: $SUCCESS ($(awk "BEGIN {printf \"%.1f\", $SUCCESS*100/$TOTAL}")%)"
    log "失败: $FAILED ($(awk "BEGIN {printf \"%.1f\", $FAILED*100/$TOTAL}")%)"
    log ""
    
    if [ $FAILED -eq 0 ]; then
        log "✓ 所有测试通过！"
        exit 0
    else
        log "✗ 有 $FAILED 个测试失败，请检查日志"
        exit 1
    fi
    
} 2>&1 | tee -a "$LOG_FILE"
