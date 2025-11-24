#!/bin/bash

echo "========================================="
echo "  自动化注册 API 测试脚本"
echo "========================================="

API_URL="${API_URL:-http://localhost:8080}"

echo ""
echo "[1/5] 检查后端服务..."
if curl -s -f "${API_URL}/actuator/health" > /dev/null 2>&1; then
    echo "✅ 后端服务运行正常"
else
    echo "❌ 后端服务未运行，请先启动后端"
    exit 1
fi

echo ""
echo "[2/5] 创建测试任务..."
TASK_JSON='{
  "taskName": "API测试-WWWTK666",
  "websiteUrl": "https://www.wwwtk666.com",
  "registerApi": "/wps/member/register",
  "method": "PUT",
  "usernameField": "username",
  "passwordField": "password",
  "defaultPassword": "133adb",
  "accountCount": 2,
  "encryptionType": "DES_RSA",
  "rsaKeyApi": "/wps/session/key/rsa",
  "rsaTsParam": "t",
  "encryptionHeader": "encryption",
  "valueFieldName": "value",
  "extraParams": "{\"confirmPassword\":\"133adb\",\"payeeName\":\"\",\"email\":\"\",\"qqNum\":\"\",\"mobileNum\":\"\",\"captcha\":\"\",\"verificationCode\":\"\",\"affiliateCode\":\"www\",\"paymentPassword\":\"\",\"line\":\"\",\"whatsapp\":\"\",\"facebook\":\"\",\"wechat\":\"\",\"idNumber\":\"\",\"nickname\":\"\",\"domain\":\"www-tk999\",\"login\":true,\"registerUrl\":\"https://www.wwwtk666.com/\",\"registerMethod\":\"WEB\",\"loginDeviceId\":\"e6ce5ac9-4b17-4e33-acbd-7350b443f572\"}"
}'

CREATE_RESULT=$(curl -s -X POST "${API_URL}/business/register/create" \
  -H "Content-Type: application/json" \
  -d "${TASK_JSON}")

TASK_ID=$(echo $CREATE_RESULT | grep -o '"data":[0-9]*' | grep -o '[0-9]*')

if [ -z "$TASK_ID" ]; then
    echo "❌ 创建任务失败"
    echo "响应: $CREATE_RESULT"
    exit 1
fi

echo "✅ 任务创建成功，ID: $TASK_ID"

echo ""
echo "[3/5] 启动任务..."
START_RESULT=$(curl -s -X POST "${API_URL}/business/register/start/${TASK_ID}")
echo "启动响应: $START_RESULT"

echo ""
echo "[4/5] 等待任务执行（最多30秒）..."
for i in {1..30}; do
    sleep 1
    STATUS_RESULT=$(curl -s "${API_URL}/business/register/${TASK_ID}")
    STATUS=$(echo $STATUS_RESULT | grep -o '"status":[0-9]*' | grep -o '[0-9]*$')
    COMPLETED=$(echo $STATUS_RESULT | grep -o '"completedCount":[0-9]*' | grep -o '[0-9]*$')
    SUCCESS=$(echo $STATUS_RESULT | grep -o '"successCount":[0-9]*' | grep -o '[0-9]*$')
    
    echo "  [${i}s] 状态: $STATUS, 完成: $COMPLETED/2, 成功: $SUCCESS"
    
    # 状态3表示已完成
    if [ "$STATUS" == "3" ]; then
        echo "✅ 任务执行完成"
        break
    fi
done

echo ""
echo "[5/5] 获取注册结果..."
RESULTS=$(curl -s "${API_URL}/business/register/results/${TASK_ID}")
echo "注册结果:"
echo "$RESULTS" | python3 -m json.tool 2>/dev/null || echo "$RESULTS"

echo ""
echo "========================================="
echo "测试完成！"
echo "========================================="
echo ""
echo "查看详细日志: tail -f logs/detection-platform.log"
echo "清理任务: curl -X DELETE ${API_URL}/business/register/${TASK_ID}"
