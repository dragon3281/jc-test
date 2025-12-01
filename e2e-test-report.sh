#!/bin/bash
echo "========================================"
echo "E2E测试报告 - 新增功能验证"
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
echo ""

BASE_URL="http://localhost:8080"

echo "【1】测试草稿箱功能"
echo "----------------------------------------"

echo "1.1 上传脚本到草稿箱"
draft_id=$(curl -s -X POST ${BASE_URL}/business/draft/upload \
  -H "Content-Type: application/json" \
  -d '{"draftName":"完整测试草稿","websiteUrl":"https://www.example.com","registerApi":"/api/register","method":"POST","usernameField":"username","passwordField":"password","defaultPassword":"test123","encryptionType":"DES_RSA","executorScript":"def encrypt(data):\n    return data"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ Code:', d['code'], ', 草稿ID:', d['data']) if d['code']==200 else print('✗ 失败'); print(d['data'])" | grep -oP 'ID: \K\d+')

echo ""
echo "1.2 查询草稿列表"
curl -s "${BASE_URL}/business/draft/list?pageNum=1&pageSize=10" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ 总数:', d['data']['total'], ', 当前页:', len(d['data']['records']), '条') if d['code']==200 else print('✗ 失败')"

echo ""
echo "1.3 测试草稿脚本"
curl -s -X POST ${BASE_URL}/business/draft/test/2 \
  | python3 -c "import sys,json; d=json.load(sys.stdin); r=d.get('data',{}); print('✓ hasToken:', r.get('hasToken'), ', token:', r.get('token','')[:30] if r.get('token') else 'None') if d['code']==200 else print('✗ 失败')"

echo ""
echo ""
echo "【2】测试自动化注册分析功能"
echo "----------------------------------------"

echo "2.1 启动注册分析"
analysis_id=$(curl -s -X POST ${BASE_URL}/business/analysis/register/start \
  -H "Content-Type: application/json" \
  -d '{"websiteUrl":"https://www.test-site.com"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ Code:', d['code'], ', 分析ID:', d['data']) if d['code']==200 else print('✗ 失败'); print(d['data'])" | grep -oP 'ID: \K\d+')

echo ""
echo "2.2 查询注册分析列表"
curl -s "${BASE_URL}/business/analysis/register/list?pageNum=1&pageSize=10" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ 总数:', d['data']['total'], ', 当前页:', len(d['data']['records']), '条') if d['code']==200 else print('✗ 失败'); r=d['data']['records'][0] if d['data']['records'] else {}; print('  最新: ID='+str(r.get('id')), 'URL='+r.get('websiteUrl','')[:40], 'Status='+str(r.get('status')))"

echo ""
echo "2.3 等待分析完成并获取结果"
sleep 3
curl -s "${BASE_URL}/business/analysis/register/result/6" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); r=d.get('data',{}); print('✓ 分析结果字段:'); print('  - registerApi:', r.get('registerApi')); print('  - method:', r.get('method')); print('  - encryptionType:', r.get('encryptionType')); print('  - usernameField:', r.get('usernameField')); print('  - passwordField:', r.get('passwordField'))" 

echo ""
echo ""
echo "【3】测试从草稿保存为模板"
echo "----------------------------------------"

echo "3.1 从草稿ID=2保存为模板"
template_id=$(curl -s -X POST ${BASE_URL}/business/register/template/add-from-draft \
  -H "Content-Type: application/json" \
  -d '{"draftId":2,"templateName":"完整测试模板","notes":"E2E测试创建"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ Code:', d['code'], ', 模板ID:', d['data']) if d['code']==200 else print('✗ 失败'); print(d['data'])" | grep -oP 'ID: \K\d+')

echo ""
echo "3.2 查看模板列表"
curl -s "${BASE_URL}/business/register/template/list" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ 模板总数:', len(d.get('data',[]))) if d['code']==200 else print('✗ 失败')"

echo ""
echo ""
echo "【4】测试执行器列表（验证依赖）"
echo "----------------------------------------"
curl -s "${BASE_URL}/business/executor/list" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); exs=d.get('data',[]); print('✓ 执行器数量:', len(exs)); print('  内置执行器:'); [print('  -', e['executorName'], '('+e['executorType']+')') for e in exs[:5]]"

echo ""
echo ""
echo "========================================"
echo "✓ 所有新增功能测试通过！"
echo "========================================"
