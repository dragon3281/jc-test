#!/bin/bash

echo "========================================="
echo "前端访问诊断测试"
echo "========================================="
echo ""

# 测试1: 检查端口监听
echo "【测试1】检查3000端口监听状态"
if netstat -tlnp | grep -q ":3000"; then
    echo "✓ 3000端口正在监听"
    netstat -tlnp | grep ":3000"
else
    echo "✗ 3000端口未监听"
    exit 1
fi
echo ""

# 测试2: 检查进程状态
echo "【测试2】检查Vite进程"
if ps aux | grep -q "node.*vite"; then
    echo "✓ Vite进程运行中"
    ps aux | grep "node.*vite" | grep -v grep
else
    echo "✗ Vite进程未运行"
    exit 1
fi
echo ""

# 测试3: 检查前端日志
echo "【测试3】检查前端启动日志"
if [ -f /tmp/frontend-3000.log ]; then
    echo "=== 前端日志 ==="
    cat /tmp/frontend-3000.log
    echo ""
else
    echo "✗ 日志文件不存在"
fi

# 测试4: telnet测试端口连通性
echo "【测试4】测试端口连通性"
timeout 2 bash -c 'cat < /dev/null > /dev/tcp/127.0.0.1/3000' 2>&1
if [ $? -eq 0 ]; then
    echo "✓ 端口可连接"
else
    echo "✗ 端口连接失败"
fi
echo ""

# 测试5: wget测试HTTP响应
echo "【测试5】wget测试HTTP响应"
timeout 5 wget -O- http://127.0.0.1:3000 2>&1 | head -30
echo ""

# 测试6: 检查防火墙
echo "【测试6】检查防火墙状态"
systemctl status firewalld 2>&1 | grep -i active || echo "防火墙服务未运行"
echo ""

# 测试7: 检查Vite配置
echo "【测试7】检查Vite配置"
if [ -f /root/jc-test/frontend/vite.config.js ]; then
    echo "=== vite.config.js ==="
    cat /root/jc-test/frontend/vite.config.js
fi
echo ""

echo "========================================="
echo "诊断完成"
echo "========================================="
