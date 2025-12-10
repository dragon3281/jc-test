#!/bin/bash

# 批量设置分组功能测试脚本
# 测试修复后的Integer/Long类型转换问题

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "批量设置分组功能测试"
echo "=========================================="
echo

# 1. 登录获取Token
echo "1. 登录系统..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/user/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "❌ 登录失败！"
  echo "响应: $LOGIN_RESPONSE"
  exit 1
fi

echo "✓ 登录成功，Token: ${TOKEN:0:20}..."
echo

# 2. 创建测试分组
echo "2. 创建测试分组..."
CREATE_GROUP=$(curl -s -X POST "$BASE_URL/proxy/groups?groupName=批量测试分组" \
  -H "Authorization: Bearer $TOKEN")

echo "创建分组响应: $CREATE_GROUP"
echo

# 3. 添加测试节点1
echo "3. 添加测试节点1..."
ADD_NODE1=$(curl -s -X POST "$BASE_URL/proxy/pool" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "poolName": "批量测试节点1",
    "proxyIp": "192.168.100.10",
    "proxyPort": 9001,
    "proxyType": 1,
    "needAuth": 0,
    "description": "批量设置分组测试"
  }')

NODE1_ID=$(echo $ADD_NODE1 | jq -r '.data')
echo "节点1 ID: $NODE1_ID"
echo

# 4. 添加测试节点2
echo "4. 添加测试节点2..."
ADD_NODE2=$(curl -s -X POST "$BASE_URL/proxy/pool" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "poolName": "批量测试节点2",
    "proxyIp": "192.168.100.11",
    "proxyPort": 9002,
    "proxyType": 3,
    "needAuth": 0,
    "description": "批量设置分组测试"
  }')

NODE2_ID=$(echo $ADD_NODE2 | jq -r '.data')
echo "节点2 ID: $NODE2_ID"
echo

# 5. 批量设置分组（关键测试）
echo "5. 批量设置分组（测试Integer/Long转换修复）..."
BATCH_SET_GROUP=$(curl -s -X PUT "$BASE_URL/proxy/pool/batch/group" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"ids\": [$NODE1_ID, $NODE2_ID],
    \"groupName\": \"批量测试分组\"
  }")

echo "批量设置分组响应:"
echo $BATCH_SET_GROUP | jq '.'

# 检查是否成功
SUCCESS=$(echo $BATCH_SET_GROUP | jq -r '.code')
if [ "$SUCCESS" = "200" ]; then
  echo "✓ 批量设置分组成功！"
else
  echo "❌ 批量设置分组失败！"
  echo "错误信息: $(echo $BATCH_SET_GROUP | jq -r '.message')"
fi
echo

# 6. 验证分组设置
echo "6. 验证分组设置..."
LIST_NODES=$(curl -s -X GET "$BASE_URL/proxy/pool/list" \
  -H "Authorization: Bearer $TOKEN")

echo "节点列表:"
echo $LIST_NODES | jq '.data[] | {id, poolName, groupName}' 
echo

# 7. 清理测试数据
echo "7. 清理测试数据..."

# 删除节点1
curl -s -X DELETE "$BASE_URL/proxy/pool/$NODE1_ID" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✓ 删除节点1"

# 删除节点2
curl -s -X DELETE "$BASE_URL/proxy/pool/$NODE2_ID" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✓ 删除节点2"

# 删除测试分组
curl -s -X DELETE "$BASE_URL/proxy/groups?groupName=批量测试分组" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✓ 删除测试分组"

echo
echo "=========================================="
echo "测试完成！"
echo "=========================================="
