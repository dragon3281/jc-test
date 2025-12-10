#!/bin/bash

# 分组管理功能 E2E 测试脚本
# 测试完整的 CRUD 操作

BASE_URL="http://localhost:8080"

# 获取 Token
echo "正在登录获取 Token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/user/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

echo "Login response: $LOGIN_RESPONSE"

# 提取 Token
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "登录失败，无法获取 Token"
  echo "使用默认 Token 进行测试..."
  # 使用localStorage中的token(需要手动更新)
  TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTczMzc4NzM0NX0.q3CtQkBK_rnwWWxG6SQ0jd7hzSuSZXb_Kzz2Sp-EZyk"
else
  echo "Token 获取成功: $TOKEN"
fi
echo ""

echo "=========================================="
echo "分组管理功能 E2E 测试"
echo "=========================================="
echo ""

# 1. 获取分组详情列表
echo "1. 测试：获取分组详情列表（带节点数量）"
curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 2. 创建新分组
echo "2. 测试：创建新分组 'E2E测试分组'"
curl -s -X POST "$BASE_URL/proxy/groups?groupName=E2E测试分组" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 3. 再次获取分组列表，验证创建成功
echo "3. 验证：分组已创建（在简单列表中）"
curl -s -X GET "$BASE_URL/proxy/groups" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 4. 添加代理节点到该分组
echo "4. 测试：添加代理节点到 'E2E测试分组'"
curl -s -X POST "$BASE_URL/proxy/pool" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "poolName": "E2E测试节点",
    "proxyIp": "192.168.1.100",
    "proxyPort": 8888,
    "proxyType": 1,
    "needAuth": 0,
    "groupName": "E2E测试分组",
    "country": "中国",
    "description": "E2E测试用节点"
  }' | jq '.'
echo ""
sleep 1

# 5. 再次查看分组详情，验证节点数量
echo "5. 验证：分组下有1个节点"
curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 6. 重命名分组
echo "6. 测试：重命名分组 'E2E测试分组' → 'E2E重命名分组'"
curl -s -X PUT "$BASE_URL/proxy/groups/rename?oldName=E2E测试分组&newName=E2E重命名分组" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 7. 验证重命名后的分组列表
echo "7. 验证：分组已重命名"
curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 8. 验证节点的分组名称已更新
echo "8. 验证：节点的分组名称已更新"
curl -s -X GET "$BASE_URL/proxy/pool/list" \
  -H "Authorization: Bearer $TOKEN" | jq '.data[] | select(.poolName == "E2E测试节点")'
echo ""
sleep 1

# 9. 尝试删除有节点的分组（应该成功，但会清空节点的分组）
echo "9. 测试：删除有节点的分组 'E2E重命名分组'"
curl -s -X DELETE "$BASE_URL/proxy/groups?groupName=E2E重命名分组" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 10. 验证分组已删除
echo "10. 验证：分组已删除"
curl -s -X GET "$BASE_URL/proxy/groups/detail" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""
sleep 1

# 11. 验证节点的分组已被清空
echo "11. 验证：节点的分组已被清空"
curl -s -X GET "$BASE_URL/proxy/pool/list" \
  -H "Authorization: Bearer $TOKEN" | jq '.data[] | select(.poolName == "E2E测试节点")'
echo ""
sleep 1

# 12. 清理：删除测试节点
echo "12. 清理：删除测试节点"
NODE_ID=$(curl -s -X GET "$BASE_URL/proxy/pool/list" -H "Authorization: Bearer $TOKEN" | jq -r '.data[] | select(.poolName == "E2E测试节点") | .id')
if [ -n "$NODE_ID" ]; then
  curl -s -X DELETE "$BASE_URL/proxy/pool/$NODE_ID" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  echo "节点ID: $NODE_ID 已删除"
else
  echo "未找到测试节点"
fi
echo ""

echo "=========================================="
echo "E2E 测试完成!"
echo "=========================================="
