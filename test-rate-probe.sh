#!/bin/bash
# 速率探测测试脚本

echo "======================================"
echo "   POST模板速率探测功能测试"
echo "======================================"
echo ""

# 配置
TEMPLATE_ID=1
API_URL="http://localhost:8080/template/detect/probe"

# 测试数据（使用真实token和手机号）
read -r -d '' REQUEST_BODY << 'EOF'
{
  "templateId": 1,
  "tokens": [
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MzM0Nzk5ODgsImp0aSI6IjkzMDQ4MTY1NiIsImlhdCI6MTczMzQ3MjE4OCwiaXNzIjoicHB2aXAyIiwibmJmIjoxNzMzNDcyMTg4LCJ1c2VyX2lkIjo5MzA0ODE2NTZ9.nFKJqhC4e-1xtCcWXlSV98pUJmLMkQwU6qA3bTfXYBs",
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MzM0Nzk5OTQsImp0aSI6IjkzMDQ4MTY1NyIsImlhdCI6MTczMzQ3MjE5NCwiaXNzIjoicHB2aXAyIiwibmJmIjoxNzMzNDcyMTk0LCJ1c2VyX2lkIjo5MzA0ODE2NTd9.qA2vGiqZQPPWjAEKN4EYuE1wXwqR-yMYq0v4OvS7pV8"
  ],
  "testPhones": [
    "09876543210",
    "09876543211",
    "09876543212",
    "09876543213",
    "09876543214",
    "09876543215",
    "09876543216",
    "09876543217",
    "09876543218",
    "09876543219"
  ],
  "autoApply": false
}
EOF

echo "📡 正在向网站发送速率探测请求..."
echo "🔍 测试手机号数量: 10个"
echo "🎫 Token数量: 2个"
echo ""

# 发送请求
RESPONSE=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY")

# 解析响应
if [ $? -eq 0 ]; then
    echo "✅ 探测完成！"
    echo ""
    echo "======================================"
    echo "        探测结果"
    echo "======================================"
    echo ""
    
    # 提取关键信息
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    
    echo ""
    echo "======================================"
    echo "💡 使用建议："
    echo "   1. 查看 'optimalConcurrency' 了解最优并发数"
    echo "   2. 查看 'rateLimitDetected' 确认是否有限流"
    echo "   3. 查看 'estimatedRate' 了解预估速率"
    echo "   4. 查看 'estimatedTimeFor10k' 了解处理1万号码所需时间"
    echo "   5. 如果满意，可设置 'autoApply: true' 自动应用配置"
    echo "======================================"
else
    echo "❌ 请求失败，请检查后端服务是否正常运行"
fi
