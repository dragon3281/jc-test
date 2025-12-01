#!/bin/bash

# 登录获取token
echo "1. 登录获取token..."
LOGIN_RESP=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESP | grep -o '"data":"[^"]*"' | cut -d'"' -f4)
echo "Token: $TOKEN"

if [ -z "$TOKEN" ]; then
  echo "登录失败"
  exit 1
fi

# 创建测试任务
echo -e "\n2. 创建测试任务..."
CREATE_RESP=$(curl -s -X POST http://localhost:8080/api/business/register/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "taskName": "测试任务_删除测试",
    "websiteUrl": "https://test.example.com",
    "registerApi": "/api/register",
    "method": "POST",
    "usernameField": "username",
    "passwordField": "password",
    "defaultPassword": "test123",
    "accountCount": 1,
    "extraParams": "{}",
    "needToken": false,
    "encryptionType": "NONE",
    "useProxy": false,
    "concurrency": 1,
    "autoRetry": false,
    "retryTimes": 0
  }')

echo "创建响应: $CREATE_RESP"
TASK_ID=$(echo $CREATE_RESP | grep -o '"data":[0-9]*' | cut -d':' -f2)
echo "任务ID: $TASK_ID"

if [ -z "$TASK_ID" ]; then
  echo "创建任务失败"
  exit 1
fi

# 尝试删除任务
echo -e "\n3. 删除任务..."
DELETE_RESP=$(curl -s -X DELETE http://localhost:8080/api/business/register/$TASK_ID \
  -H "Authorization: Bearer $TOKEN")

echo "删除响应: $DELETE_RESP"

# 验证删除
echo -e "\n4. 验证任务是否已删除..."
GET_RESP=$(curl -s -X GET http://localhost:8080/api/business/register/$TASK_ID \
  -H "Authorization: Bearer $TOKEN")

echo "查询响应: $GET_RESP"
