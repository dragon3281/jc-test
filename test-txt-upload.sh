#!/bin/bash

# 数据中心TXT上传E2E测试脚本

API_BASE="http://localhost:8080"
LOG_FILE="/tmp/txt-upload-test.log"

echo "========================================" | tee $LOG_FILE
echo "  数据中心TXT上传E2E测试" | tee -a $LOG_FILE
echo "========================================" | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 1. 登录获取Token
echo "Step 1: 登录获取Token..." | tee -a $LOG_FILE
LOGIN_RESPONSE=$(curl -s -X POST "${API_BASE}/user/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

echo "登录响应: $LOGIN_RESPONSE" | tee -a $LOG_FILE

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['data']['token'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "✗ 登录失败，无法获取Token" | tee -a $LOG_FILE
  exit 1
fi

echo "✓ 登录成功，Token: ${TOKEN:0:20}..." | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 2. 创建测试TXT文件
echo "Step 2: 创建测试TXT文件..." | tee -a $LOG_FILE
cat > /tmp/test-upload.txt << 'EOF'
13800138000
13900139000
15012345678
18612345678
19912345678
EOF

echo "测试文件内容：" | tee -a $LOG_FILE
cat /tmp/test-upload.txt | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 3. 测试上传TXT文件
echo "Step 3: 上传TXT文件..." | tee -a $LOG_FILE
UPLOAD_RESPONSE=$(curl -s -X POST "${API_BASE}/data/base/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/test-upload.txt" \
  -F "country=中国" \
  -F "dataType=手机号")

echo "上传响应: $UPLOAD_RESPONSE" | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 检查响应
if echo "$UPLOAD_RESPONSE" | grep -q '"code":200'; then
  COUNT=$(echo $UPLOAD_RESPONSE | grep -o '"data":[0-9]*' | cut -d':' -f2)
  echo "✓ 上传成功，导入 $COUNT 条数据" | tee -a $LOG_FILE
elif echo "$UPLOAD_RESPONSE" | grep -q "Excel"; then
  echo "✗ 错误：仍然提示Excel格式" | tee -a $LOG_FILE
  echo "完整错误信息：" | tee -a $LOG_FILE
  echo "$UPLOAD_RESPONSE" | tee -a $LOG_FILE
  exit 1
elif echo "$UPLOAD_RESPONSE" | grep -q '"code":'; then
  CODE=$(echo $UPLOAD_RESPONSE | grep -o '"code":[0-9]*' | cut -d':' -f2)
  MESSAGE=$(echo $UPLOAD_RESPONSE | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
  echo "✗ 上传失败 (code: $CODE)" | tee -a $LOG_FILE
  echo "错误信息: $MESSAGE" | tee -a $LOG_FILE
  exit 1
else
  echo "✗ 未知响应格式" | tee -a $LOG_FILE
  echo "$UPLOAD_RESPONSE" | tee -a $LOG_FILE
  exit 1
fi

# 4. 查询数据验证
echo "" | tee -a $LOG_FILE
echo "Step 4: 查询数据验证..." | tee -a $LOG_FILE
QUERY_RESPONSE=$(curl -s -X GET "${API_BASE}/data/base/page?current=1&size=10" \
  -H "Authorization: Bearer $TOKEN")

echo "查询响应: $QUERY_RESPONSE" | tee -a $LOG_FILE

if echo "$QUERY_RESPONSE" | grep -q '13800138000'; then
  echo "✓ 数据验证成功，找到上传的号码" | tee -a $LOG_FILE
else
  echo "✗ 数据验证失败，未找到上传的号码" | tee -a $LOG_FILE
fi

echo "" | tee -a $LOG_FILE
echo "========================================" | tee -a $LOG_FILE
echo "  测试完成！" | tee -a $LOG_FILE
echo "========================================" | tee -a $LOG_FILE
echo "详细日志: $LOG_FILE" | tee -a $LOG_FILE
