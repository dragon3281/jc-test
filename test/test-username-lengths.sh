#!/bin/bash

echo "测试不同长度用户名..."
echo ""

# 测试6-12位的用户名（无下划线，字母开头）
for len in 6 7 8 9 10 11 12; do
    username=$(head /dev/urandom | tr -dc 'a-z0-9' | head -c $((len-1)))
    username="a${username}"  # 确保字母开头
    echo "测试 ${len}位用户名: ${username}"
done

echo ""
echo "建议：根据常见网站规则，用户名通常是 6-10位，字母开头，字母+数字组合"
