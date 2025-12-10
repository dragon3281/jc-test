#!/bin/bash

# E2E 全量测试脚本 - 带详细日志

set -e

echo "================================"
echo "E2E 代理资源池全量测试"
echo "================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查前端目录
if [ ! -d "frontend" ]; then
    echo -e "${RED}错误: 未找到 frontend 目录${NC}"
    exit 1
fi

cd frontend

# 检查 node_modules
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}未找到 node_modules，开始安装依赖...${NC}"
    npm install
fi

# 检查 Playwright 是否安装
if [ ! -d "node_modules/@playwright" ]; then
    echo -e "${YELLOW}未找到 Playwright，开始安装...${NC}"
    npm install -D @playwright/test
    npx playwright install chromium
fi

# 清理之前的测试结果
echo -e "${BLUE}清理旧的测试结果...${NC}"
rm -rf test-results playwright-report

# 运行测试
echo -e "${GREEN}开始运行 E2E 全量测试...${NC}"
echo ""

# 设置环境变量以启用详细日志
export DEBUG=pw:api
export PWDEBUG=1

# 运行测试，输出详细日志
npx playwright test proxy-pool-full.spec.js \
  --reporter=list,html \
  --output=test-results \
  2>&1 | tee ../e2e-full-test.log

# 检查测试结果
TEST_EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo "================================"
echo "测试执行完成"
echo "================================"
echo ""

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过${NC}"
else
    echo -e "${RED}✗ 部分测试失败 (退出码: $TEST_EXIT_CODE)${NC}"
fi

# 显示测试结果位置
echo ""
echo -e "${BLUE}测试结果保存位置:${NC}"
echo "  - 控制台日志: ../e2e-full-test.log"
echo "  - HTML报告: frontend/playwright-report/index.html"
echo "  - 测试详情: frontend/test-results/"
echo ""

# 尝试打开HTML报告
if [ -f "playwright-report/index.html" ]; then
    echo -e "${YELLOW}提示: 运行以下命令查看HTML报告:${NC}"
    echo "  cd frontend && npx playwright show-report"
    echo ""
fi

exit $TEST_EXIT_CODE
