#!/bin/bash

echo "========================================="
echo "  自动化数据检测平台 - 快速启动脚本"
echo "========================================="
echo ""

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装,请先安装Docker"
    exit 1
fi

# 检查Docker Compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose未安装,请先安装Docker Compose"
    exit 1
fi

echo "✅ Docker环境检查通过"
echo ""

# 启动基础服务
echo "📦 正在启动基础服务(MySQL、Redis、RabbitMQ)..."
cd docker
docker-compose up -d

echo ""
echo "⏳ 等待服务启动(30秒)..."
sleep 30

# 检查服务状态
echo ""
echo "🔍 检查服务状态..."
docker-compose ps

echo ""
echo "========================================="
echo "  基础服务启动完成!"
echo "========================================="
echo ""
echo "📝 服务信息:"
echo "  MySQL:    localhost:3306"
echo "  Redis:    localhost:6379"
echo "  RabbitMQ: localhost:5672"
echo "  RabbitMQ管理界面: http://localhost:15672"
echo "            用户名: admin"
echo "            密码:   admin"
echo ""
echo "📚 后续操作:"
echo "  1. 启动后端: cd backend && mvn spring-boot:run"
echo "  2. 启动前端: cd frontend && npm install && npm run dev"
echo ""
echo "  3. 访问系统: http://localhost:3000"
echo "     默认账号: admin / admin123"
echo ""
echo "========================================="
