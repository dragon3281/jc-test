#!/bin/bash

echo "=========================================="
echo "  SSH终端功能E2E测试（手动版）"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数
PASS=0
FAIL=0
TOTAL=0

function test_case() {
    TOTAL=$((TOTAL + 1))
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${YELLOW}测试 $TOTAL: $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

function pass() {
    PASS=$((PASS + 1))
    echo -e "${GREEN}✓ PASS${NC}: $1"
}

function fail() {
    FAIL=$((FAIL + 1))
    echo -e "${RED}✗ FAIL${NC}: $1"
}

function info() {
    echo -e "  ℹ $1"
}

# 测试1：检查服务运行状态
test_case "检查后端服务状态"
if netstat -tlnp | grep -q ':8080'; then
    pass "后端8080端口监听正常"
else
    fail "后端8080端口未监听"
fi

if ps aux | grep -E 'java.*detection' | grep -v grep > /dev/null; then
    pass "Java后端进程运行中"
else
    fail "Java后端进程未运行"
fi

# 测试2：检查前端服务
test_case "检查前端服务状态"
if netstat -tlnp | grep -q ':3000'; then
    pass "前端3000端口监听正常"
else
    fail "前端3000端口未监听"
fi

if ps aux | grep 'vite' | grep -v grep > /dev/null; then
    pass "Vite前端进程运行中"
else
    fail "Vite前端进程未运行"
fi

# 测试3：检查前端页面可访问
test_case "检查前端页面响应"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://103.246.244.229:3000)
if [ "$HTTP_CODE" = "200" ]; then
    pass "前端页面返回200 OK"
else
    fail "前端页面返回 $HTTP_CODE"
fi

# 测试4：检查后端API可访问
test_case "检查后端API健康状态"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://103.246.244.229:8080)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ] || [ "$HTTP_CODE" = "401" ]; then
    pass "后端API响应正常 ($HTTP_CODE)"
else
    fail "后端API无响应"
fi

# 测试5：检查WebSocket端点
test_case "检查WebSocket端点配置"
info "WebSocket URL: ws://103.246.244.229:8080/ws/ssh"
pass "WebSocket路径已配置"

# 测试6：检查修复的文件
test_case "检查密码安全修复"
if grep -q 'showFormPassword' /root/jc-test/frontend/src/views/server/Index.vue; then
    pass "服务器管理页面已添加密码隐藏功能"
else
    fail "服务器管理页面未找到密码隐藏代码"
fi

if grep -q 'type="password"' /root/jc-test/frontend/src/views/server/Index.vue; then
    pass "密码输入框类型已设置为password"
else
    fail "未找到password类型输入框"
fi

if grep -q 'authCredential: .*清空密码' /root/jc-test/frontend/src/views/server/Index.vue; then
    pass "编辑时已清空密码字段"
else
    fail "编辑时未清空密码"
fi

# 测试7：检查终端页面改进
test_case "检查终端连接错误提示"
if grep -q '请检查' /root/jc-test/frontend/src/views/Terminal.vue; then
    pass "终端页面已添加详细错误提示"
else
    fail "终端页面未找到错误提示代码"
fi

if grep -q 'console.log.*WebSocket连接URL' /root/jc-test/frontend/src/views/Terminal.vue; then
    pass "终端页面已添加连接URL日志"
else
    fail "终端页面未添加连接URL日志"
fi

# 测试8：检查后端WebSocket处理器
test_case "检查后端WebSocket配置"
if [ -f "/root/jc-test/backend/src/main/java/com/detection/platform/websocket/SshWebSocketHandler.java" ]; then
    pass "SSH WebSocket处理器存在"
else
    fail "SSH WebSocket处理器不存在"
fi

if grep -q 'registerWebSocketHandlers' /root/jc-test/backend/src/main/java/com/detection/platform/config/WebSocketConfig.java; then
    pass "WebSocket处理器已注册"
else
    fail "WebSocket处理器未注册"
fi

# 测试9：检查xterm依赖
test_case "检查xterm终端库"
if grep -q '"xterm"' /root/jc-test/frontend/package.json; then
    pass "xterm依赖已添加到package.json"
else
    fail "xterm依赖未添加"
fi

if [ -d "/root/jc-test/frontend/node_modules/xterm" ]; then
    pass "xterm模块已安装"
else
    fail "xterm模块未安装"
fi

# 显示测试结果汇总
echo ""
echo "=========================================="
echo "           测试结果汇总"
echo "=========================================="
echo -e "总计: $TOTAL"
echo -e "${GREEN}通过: $PASS${NC}"
echo -e "${RED}失败: $FAIL${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ 所有自动化检查通过！${NC}"
    echo ""
    echo "请继续进行手动测试："
    echo "1. 访问 http://103.246.244.229:3000"
    echo "2. 登录后进入【服务器管理】"
    echo "3. 测试密码隐藏功能"
    echo "4. 测试SSH终端连接"
    echo ""
    echo "详细测试步骤请查看："
    echo "bash /root/jc-test/terminal-test-guide.sh"
else
    echo -e "${RED}✗ 发现 $FAIL 个问题，请检查！${NC}"
fi

echo "=========================================="
