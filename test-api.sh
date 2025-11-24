#!/bin/bash

###############################################################################
# 自动化数据检测平台 - 快速测试脚本
# 用途: 验证前后端接口对接和基础功能
###############################################################################

echo "=========================================="
echo "自动化数据检测平台 - 快速测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# API基础地址
API_BASE="http://localhost:8080/api"
FRONTEND_BASE="http://localhost:3000"

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "测试 $TOTAL_TESTS: $name ... "
    
    if [ "$method" == "GET" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" "$API_BASE$endpoint")
    elif [ "$method" == "POST" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$API_BASE$endpoint")
    fi
    
    if [ "$response" == "200" ] || [ "$response" == "401" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $response)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ 失败${NC} (HTTP $response)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo "1. 检查服务启动状态"
echo "-------------------------------------------"

# 检查后端服务
echo -n "后端服务 (8080端口) ... "
if curl -s -o /dev/null -w "%{http_code}" "$API_BASE/user/login" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 未启动${NC}"
    echo ""
    echo -e "${YELLOW}提示: 请先启动后端服务${NC}"
    echo "命令: cd backend && mvn spring-boot:run"
    exit 1
fi

# 检查前端服务
echo -n "前端服务 (3000端口) ... "
if curl -s -o /dev/null "$FRONTEND_BASE" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${YELLOW}⚠ 未启动${NC}"
    echo "提示: 前端服务未启动,仅测试后端接口"
fi

echo ""
echo "2. 测试用户认证接口"
echo "-------------------------------------------"

# 用户登录
test_endpoint "用户登录" "POST" "/user/login" '{"username":"admin","password":"admin123"}'

echo ""
echo "3. 测试服务器管理接口"
echo "-------------------------------------------"

test_endpoint "服务器列表(分页)" "GET" "/server/page?current=1&size=10"
test_endpoint "服务器列表(全部)" "GET" "/server/list"

echo ""
echo "4. 测试代理资源池接口"
echo "-------------------------------------------"

test_endpoint "代理池列表(分页)" "GET" "/proxy/pool/page?current=1&size=10"
test_endpoint "代理池列表(全部)" "GET" "/proxy/pool/list"
test_endpoint "代理节点列表" "GET" "/proxy/node/page?current=1&size=10"

echo ""
echo "5. 测试数据中心接口"
echo "-------------------------------------------"

test_endpoint "基础数据列表" "GET" "/data/base/page?current=1&size=10"
test_endpoint "检测结果列表" "GET" "/data/result/page?current=1&size=10"

echo ""
echo "6. 测试业务中心接口"
echo "-------------------------------------------"

test_endpoint "网站分析列表" "GET" "/business/analysis/list?pageNum=1&pageSize=10"
test_endpoint "注册任务列表" "GET" "/business/register/list?pageNum=1&pageSize=10"
test_endpoint "POST模板列表(分页)" "GET" "/template/page?current=1&size=10"
test_endpoint "POST模板列表(全部)" "GET" "/template/list"

echo ""
echo "7. 测试检测任务接口"
echo "-------------------------------------------"

test_endpoint "检测任务列表(分页)" "GET" "/task/page?current=1&size=10"
test_endpoint "检测任务列表(全部)" "GET" "/task/list"

echo ""
echo "=========================================="
echo "测试结果汇总"
echo "=========================================="
echo -e "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ 所有接口测试通过!${NC}"
    echo ""
    echo "下一步操作:"
    echo "1. 访问前端: http://localhost:3000"
    echo "2. 使用账号: admin / admin123"
    echo "3. 开始体验系统功能"
    exit 0
else
    echo ""
    echo -e "${RED}✗ 部分接口测试失败${NC}"
    echo ""
    echo "排查建议:"
    echo "1. 检查数据库是否已初始化 (sql/init.sql)"
    echo "2. 检查MySQL/Redis/RabbitMQ是否正常运行"
    echo "3. 检查后端日志: logs/detection-platform.log"
    exit 1
fi
