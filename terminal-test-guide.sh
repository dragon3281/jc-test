#!/bin/bash

echo "======================================"
echo "SSH终端功能手动测试指南"
echo "======================================"
echo ""

echo "修复内容："
echo "1. ✓ 编辑服务器对话框密码输入框改为password类型"
echo "2. ✓ 添加眼睛图标切换显示/隐藏密码"
echo "3. ✓ 编辑服务器时清空密码，要求重新输入"
echo "4. ✓ WebSocket连接增加详细错误提示"
echo "5. ✓ 终端连接失败时显示诊断信息"
echo ""

echo "测试步骤："
echo ""
echo "【测试1：密码安全性】"
echo "1. 访问 http://103.246.244.229:3000"
echo "2. 登录：用户名 admin，密码 admin123"
echo "3. 进入【服务器管理】"
echo "4. 点击【添加服务器】按钮"
echo "   - 检查凭证输入框是否为密码类型（显示为••••••）"
echo "   - 检查是否有眼睛图标可以切换显示"
echo "5. 点击【编辑】按钮编辑现有服务器"
echo "   - 检查凭证输入框是否已清空"
echo "   - 检查是否为密码类型"
echo "   - 需要重新输入密码才能保存"
echo ""

echo "【测试2：列表密码显示】"
echo "1. 在服务器列表中查看【认证凭证】列"
echo "   - 默认应显示为 ••••••••"
echo "   - 点击眼睛图标可以切换显示/隐藏"
echo ""

echo "【测试3：SSH终端连接】"
echo "前置条件：确保至少有一台在线的Linux服务器"
echo ""
echo "1. 在服务器列表中找到状态为【在线】的服务器"
echo "2. 点击【终端】按钮"
echo "3. 新窗口应该打开终端页面"
echo "4. 观察连接过程："
echo "   - 应显示：正在连接到: ws://103.246.244.229:8080/ws/ssh?serverId=xxx"
echo "   - 如果连接成功：显示绿色【已连接】标签，终端可以输入命令"
echo "   - 如果连接失败：显示详细错误信息和诊断建议"
echo ""

echo "【预期结果】"
echo "✓ 密码输入框默认为password类型（显示••••••）"
echo "✓ 可以通过眼睛图标切换显示/隐藏"
echo "✓ 编辑服务器时不显示原密码"
echo "✓ 终端可以正常打开并连接"
echo "✓ 连接失败时有清晰的错误提示"
echo ""

echo "【WebSocket连接诊断】"
echo "如果终端连接失败，请检查："
echo ""
echo "1. 后端服务状态："
netstat -tlnp | grep 8080 && echo "   ✓ 后端8080端口监听正常" || echo "   ✗ 后端8080端口未监听"
echo ""

echo "2. 服务器SSH配置："
echo "   - 服务器IP地址是否正确"
echo "   - SSH端口是否正确（默认22）"
echo "   - SSH密码/密钥是否正确"
echo ""

echo "3. 服务进程状态："
ps aux | grep -E 'java.*detection' | grep -v grep > /dev/null
if [ $? -eq 0 ]; then
    echo "   ✓ Java后端服务运行中"
else
    echo "   ✗ Java后端服务未运行"
fi

ps aux | grep -E 'vite' | grep -v grep > /dev/null
if [ $? -eq 0 ]; then
    echo "   ✓ 前端服务运行中"
else
    echo "   ✗ 前端服务未运行"
fi
echo ""

echo "4. 测试WebSocket端点："
echo "   在浏览器开发者工具Console中执行："
echo "   const ws = new WebSocket('ws://103.246.244.229:8080/ws/ssh?serverId=1')"
echo "   ws.onopen = () => console.log('连接成功')"
echo "   ws.onerror = (e) => console.error('连接失败', e)"
echo ""

echo "======================================"
echo "访问地址："
echo "前端：http://103.246.244.229:3000"
echo "后端：http://103.246.244.229:8080"
echo "======================================"
