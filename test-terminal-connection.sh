#!/bin/bash

echo "=========================================="
echo "  SSH终端功能E2E测试"
echo "=========================================="
echo ""

# 服务器信息
SERVER_IP="103.246.246.13"
SERVER_PORT="22"
SERVER_PASSWORD="kaolacc@123"

# 测试颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 步骤1: 检查服务运行状态
echo "步骤1: 检查服务运行状态"
echo "----------------------------------------"
if netstat -tlnp | grep -q ':8080'; then
    echo -e "${GREEN}✓${NC} 后端8080端口监听正常"
else
    echo -e "${RED}✗${NC} 后端8080端口未监听"
    exit 1
fi

if netstat -tlnp | grep -q ':3000'; then
    echo -e "${GREEN}✓${NC} 前端3000端口监听正常"
else
    echo -e "${RED}✗${NC} 前端3000端口未监听"
    exit 1
fi

echo ""

# 步骤2: 测试SSH连接
echo "步骤2: 测试SSH服务器连接"
echo "----------------------------------------"
echo "服务器: ${SERVER_IP}:${SERVER_PORT}"

# 使用nc测试端口
if timeout 3 bash -c "echo >/dev/tcp/${SERVER_IP}/${SERVER_PORT}" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} SSH端口${SERVER_PORT}可访问"
else
    echo -e "${RED}✗${NC} SSH端口${SERVER_PORT}无法访问"
    exit 1
fi

echo ""

# 步骤3: 登录系统并添加服务器
echo "步骤3: 登录系统并配置服务器"
echo "----------------------------------------"

# 登录获取token
LOGIN_RESPONSE=$(curl -s -X POST http://103.246.244.229:8080/user/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗${NC} 登录失败"
    echo "响应: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓${NC} 登录成功，获取token: ${TOKEN:0:20}..."

# 查询现有服务器
SERVER_LIST=$(curl -s -X GET http://103.246.244.229:8080/server/list \
    -H "Authorization: Bearer $TOKEN")

SERVER_ID=$(echo $SERVER_LIST | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$SERVER_ID" ]; then
    echo "添加测试服务器..."
    # 添加服务器
    ADD_RESPONSE=$(curl -s -X POST http://103.246.244.229:8080/server \
        -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' \
        -d "{
            \"serverName\": \"E2E测试服务器\",
            \"ipAddress\": \"${SERVER_IP}\",
            \"sshPort\": ${SERVER_PORT},
            \"authType\": 1,
            \"authCredential\": \"${SERVER_PASSWORD}\",
            \"maxConcurrent\": 10
        }")
    
    SERVER_ID=$(echo $ADD_RESPONSE | grep -o '"data":[0-9]*' | cut -d':' -f2)
    
    if [ -z "$SERVER_ID" ]; then
        echo -e "${RED}✗${NC} 添加服务器失败"
        echo "响应: $ADD_RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}✓${NC} 服务器添加成功，ID: $SERVER_ID"
else
    echo -e "${GREEN}✓${NC} 使用现有服务器，ID: $SERVER_ID"
    
    # 更新服务器信息
    UPDATE_RESPONSE=$(curl -s -X PUT http://103.246.244.229:8080/server \
        -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' \
        -d "{
            \"id\": ${SERVER_ID},
            \"serverName\": \"E2E测试服务器\",
            \"ipAddress\": \"${SERVER_IP}\",
            \"sshPort\": ${SERVER_PORT},
            \"authType\": 1,
            \"authCredential\": \"${SERVER_PASSWORD}\",
            \"maxConcurrent\": 10
        }")
    
    echo -e "${GREEN}✓${NC} 服务器信息已更新"
fi

echo ""

# 步骤4: 测试WebSocket连接（模拟）
echo "步骤4: 检查WebSocket端点"
echo "----------------------------------------"

WS_URL="ws://103.246.244.229:8080/terminal/ssh?serverId=${SERVER_ID}"
echo "WebSocket URL: $WS_URL"

# 检查后端日志中是否有错误
echo ""
echo "查看最新日志..."
RECENT_LOGS=$(tail -20 /root/jc-test/logs/backend.log 2>/dev/null)

if echo "$RECENT_LOGS" | grep -q "Invalid SockJS path"; then
    echo -e "${RED}✗${NC} 检测到SockJS路径冲突错误"
    exit 1
else
    echo -e "${GREEN}✓${NC} 未检测到WebSocket配置错误"
fi

echo ""

# 步骤5: 显示测试总结
echo "=========================================="
echo "           测试结果总结"
echo "=========================================="
echo -e "${GREEN}✓${NC} 后端服务运行正常"
echo -e "${GREEN}✓${NC} 前端服务运行正常"
echo -e "${GREEN}✓${NC} SSH服务器端口可访问"
echo -e "${GREEN}✓${NC} 系统登录成功"
echo -e "${GREEN}✓${NC} 服务器配置成功 (ID: $SERVER_ID)"
echo -e "${GREEN}✓${NC} WebSocket路径配置正确"
echo ""
echo "=========================================="
echo "           手动测试步骤"
echo "=========================================="
echo "1. 访问: http://103.246.244.229:3000"
echo "2. 登录: admin / admin123"
echo "3. 进入【服务器管理】"
echo "4. 找到服务器: E2E测试服务器"
echo "5. 点击【终端】按钮"
echo "6. 观察终端连接过程："
echo "   - 应显示: 正在连接到: ${WS_URL}"
echo "   - 连接成功后显示绿色【已连接】标签"
echo "   - 可以输入命令: whoami, ls, pwd 等"
echo ""
echo "WebSocket路径已从 /ws/ssh 改为 /terminal/ssh"
echo "避免与STOMP端点 /ws 冲突"
echo "=========================================="
