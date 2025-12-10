#!/bin/bash

# =======================================================================
# 分组管理功能 E2E 完整测试脚本
# 测试场景：创建分组 → 批量加入节点 → 按分组筛选 → 重命名分组 → 删除分组
# =======================================================================

set -e

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)
TEST_GROUP="E2E测试分组_${TIMESTAMP}"
TEST_GROUP_RENAMED="E2E重命名分组_${TIMESTAMP}"

echo "========================================"
echo "分组管理功能 E2E 全量测试"
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
echo ""

# 步骤0: 登录获取Token
echo "【步骤0】登录获取Token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/user/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token // empty')

if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败，无法获取Token"
  echo "响应: $LOGIN_RESPONSE"
  exit 1
else
  echo "✓ Token获取成功"
fi
echo ""

# 步骤1: 查看初始分组列表
echo "【步骤1】查看初始分组列表..."
INITIAL_GROUPS=$(curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN")
echo "初始分组列表:"
echo $INITIAL_GROUPS | jq '.'
echo ""
sleep 1

# 步骤2: 创建测试节点1
echo "【步骤2】创建测试节点1..."
NODE1_RESPONSE=$(curl -s -X POST "$BASE_URL/proxy/pool" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "poolName": "E2E测试节点1_'$TIMESTAMP'",
    "proxyIp": "192.168.100.10",
    "proxyPort": 9001,
    "proxyType": 1,
    "needAuth": 0,
    "country": "中国",
    "description": "E2E分组测试节点"
  }')

NODE1_ID=$(echo $NODE1_RESPONSE | jq -r '.data // empty')
if [ -z "$NODE1_ID" ]; then
  echo "❌ 创建节点1失败"
  echo $NODE1_RESPONSE | jq '.'
  exit 1
else
  echo "✓ 节点1创建成功, ID: $NODE1_ID"
fi
echo ""
sleep 1

# 步骤3: 创建测试节点2
echo "【步骤3】创建测试节点2..."
NODE2_RESPONSE=$(curl -s -X POST "$BASE_URL/proxy/pool" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "poolName": "E2E测试节点2_'$TIMESTAMP'",
    "proxyIp": "192.168.100.11",
    "proxyPort": 9002,
    "proxyType": 3,
    "needAuth": 0,
    "country": "美国",
    "description": "E2E分组测试节点"
  }')

NODE2_ID=$(echo $NODE2_RESPONSE | jq -r '.data // empty')
if [ -z "$NODE2_ID" ]; then
  echo "❌ 创建节点2失败"
  echo $NODE2_RESPONSE | jq '.'
  exit 1
else
  echo "✓ 节点2创建成功, ID: $NODE2_ID"
fi
echo ""
sleep 1

# 步骤4: 批量设置分组（创建新分组并加入节点）
echo "【步骤4】批量设置分组: 将2个节点加入分组 \"$TEST_GROUP\"..."
BATCH_SET_RESPONSE=$(curl -s -X PUT "$BASE_URL/proxy/pool/batch/group" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ids": ['$NODE1_ID', '$NODE2_ID'],
    "groupName": "'$TEST_GROUP'"
  }')

echo "批量设置分组响应:"
echo $BATCH_SET_RESPONSE | jq '.'

if [ "$(echo $BATCH_SET_RESPONSE | jq -r '.code')" = "200" ]; then
  echo "✓ 批量设置分组成功"
else
  echo "❌ 批量设置分组失败"
  exit 1
fi
echo ""
sleep 1

# 步骤5: 验证分组已创建并显示节点数量
echo "【步骤5】验证分组已创建（查看分组详情）..."
GROUPS_DETAIL=$(curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN")

echo "当前分组列表:"
echo $GROUPS_DETAIL | jq '.'

GROUP_EXISTS=$(echo $GROUPS_DETAIL | jq -r '.data[] | select(.groupName == "'$TEST_GROUP'") | .groupName')
NODE_COUNT=$(echo $GROUPS_DETAIL | jq -r '.data[] | select(.groupName == "'$TEST_GROUP'") | .nodeCount')

if [ "$GROUP_EXISTS" = "$TEST_GROUP" ] && [ "$NODE_COUNT" = "2" ]; then
  echo "✓ 分组 \"$TEST_GROUP\" 存在，节点数量: $NODE_COUNT"
else
  echo "❌ 分组验证失败"
  exit 1
fi
echo ""
sleep 1

# 步骤6: 按分组筛选节点
echo "【步骤6】按分组筛选节点..."
# URL编码分组名
ENCODED_GROUP=$(echo -n "$TEST_GROUP" | jq -sRr @uri)
FILTER_RESPONSE=$(curl -s -X GET "$BASE_URL/proxy/pool/list?groupName=$ENCODED_GROUP" \
  -H "Authorization: Bearer $TOKEN")

echo "分组筛选结果:"
echo $FILTER_RESPONSE | jq '.'

FILTERED_COUNT=$(echo $FILTER_RESPONSE | jq '.data | length')
if [ "$FILTERED_COUNT" = "2" ]; then
  echo "✓ 按分组筛选成功，找到 $FILTERED_COUNT 个节点"
else
  echo "❌ 按分组筛选失败，期望2个节点，实际 $FILTERED_COUNT 个"
  exit 1
fi
echo ""
sleep 1

# 步骤7: 重命名分组
echo "【步骤7】重命名分组: \"$TEST_GROUP\" → \"$TEST_GROUP_RENAMED\"..."
# URL编码参数
ENCODED_OLD=$(echo -n "$TEST_GROUP" | jq -sRr @uri)
ENCODED_NEW=$(echo -n "$TEST_GROUP_RENAMED" | jq -sRr @uri)
RENAME_RESPONSE=$(curl -s -X PUT "$BASE_URL/proxy/groups/rename?oldName=$ENCODED_OLD&newName=$ENCODED_NEW" \
  -H "Authorization: Bearer $TOKEN")

echo "重命名响应:"
echo $RENAME_RESPONSE | jq '.'

if [ "$(echo $RENAME_RESPONSE | jq -r '.code')" = "200" ]; then
  echo "✓ 分组重命名成功"
else
  echo "❌ 分组重命名失败"
  exit 1
fi
echo ""
sleep 1

# 步骤8: 验证重命名后的分组
echo "【步骤8】验证重命名后的分组..."
GROUPS_AFTER_RENAME=$(curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN")

echo "重命名后的分组列表:"
echo $GROUPS_AFTER_RENAME | jq '.'

NEW_GROUP_EXISTS=$(echo $GROUPS_AFTER_RENAME | jq -r '.data[] | select(.groupName == "'$TEST_GROUP_RENAMED'") | .groupName')
OLD_GROUP_EXISTS=$(echo $GROUPS_AFTER_RENAME | jq -r '.data[] | select(.groupName == "'$TEST_GROUP'") | .groupName')

if [ "$NEW_GROUP_EXISTS" = "$TEST_GROUP_RENAMED" ] && [ -z "$OLD_GROUP_EXISTS" ]; then
  echo "✓ 分组重命名验证成功"
else
  echo "❌ 分组重命名验证失败"
  exit 1
fi
echo ""
sleep 1

# 步骤9: 验证节点的分组名称已更新
echo "【步骤9】验证节点的分组名称已更新..."
NODE1_INFO=$(curl -s -X GET "$BASE_URL/proxy/pool/list" \
  -H "Authorization: Bearer $TOKEN" | jq '.data[] | select(.id == '$NODE1_ID')')

NODE1_GROUP=$(echo $NODE1_INFO | jq -r '.groupName')
if [ "$NODE1_GROUP" = "$TEST_GROUP_RENAMED" ]; then
  echo "✓ 节点1的分组名称已更新为: $NODE1_GROUP"
else
  echo "❌ 节点1的分组名称未正确更新，当前: $NODE1_GROUP"
  exit 1
fi
echo ""
sleep 1

# 步骤10: 删除分组（应该清空节点的分组字段）
echo "【步骤10】删除分组 \"$TEST_GROUP_RENAMED\"..."
ENCODED_DELETE=$(echo -n "$TEST_GROUP_RENAMED" | jq -sRr @uri)
DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/proxy/groups?groupName=$ENCODED_DELETE" \
  -H "Authorization: Bearer $TOKEN")

echo "删除分组响应:"
echo $DELETE_RESPONSE | jq '.'

if [ "$(echo $DELETE_RESPONSE | jq -r '.code')" = "200" ]; then
  echo "✓ 分组删除成功"
else
  echo "❌ 分组删除失败"
  exit 1
fi
echo ""
sleep 1

# 步骤11: 验证分组已删除
echo "【步骤11】验证分组已从列表中删除..."
GROUPS_AFTER_DELETE=$(curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN")

echo "删除后的分组列表:"
echo $GROUPS_AFTER_DELETE | jq '.'

DELETED_GROUP_EXISTS=$(echo $GROUPS_AFTER_DELETE | jq -r '.data[] | select(.groupName == "'$TEST_GROUP_RENAMED'") | .groupName')
if [ -z "$DELETED_GROUP_EXISTS" ]; then
  echo "✓ 分组已从列表中删除"
else
  echo "❌ 分组仍然存在于列表中"
  exit 1
fi
echo ""
sleep 1

# 步骤12: 验证节点的分组已被清空
echo "【步骤12】验证节点的分组已被清空..."
NODE1_AFTER_DELETE=$(curl -s -X GET "$BASE_URL/proxy/pool/list" \
  -H "Authorization: Bearer $TOKEN" | jq '.data[] | select(.id == '$NODE1_ID')')

NODE1_GROUP_AFTER=$(echo $NODE1_AFTER_DELETE | jq -r '.groupName // empty')
if [ -z "$NODE1_GROUP_AFTER" ]; then
  echo "✓ 节点1的分组已被清空"
else
  echo "❌ 节点1的分组未被清空，当前: $NODE1_GROUP_AFTER"
  exit 1
fi
echo ""
sleep 1

# 步骤13: 清理测试数据
echo "【步骤13】清理测试数据..."
echo "删除测试节点1..."
curl -s -X DELETE "$BASE_URL/proxy/pool/$NODE1_ID" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

echo "删除测试节点2..."
curl -s -X DELETE "$BASE_URL/proxy/pool/$NODE2_ID" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

echo "✓ 测试数据清理完成"
echo ""

# 测试总结
echo "========================================"
echo "✓✓✓ 所有测试通过！ ✓✓✓"
echo "========================================"
echo ""
echo "测试覆盖场景："
echo "  1. 创建测试节点"
echo "  2. 批量设置分组（自动创建新分组）"
echo "  3. 验证分组列表及节点数量统计"
echo "  4. 按分组筛选节点"
echo "  5. 重命名分组（自动更新所有节点）"
echo "  6. 删除分组（自动清空节点分组字段）"
echo "  7. 清理测试数据"
echo ""
echo "分组功能完全符合基于标签的设计要求！"
echo "========================================"
