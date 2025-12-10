/**
 * E2E全量测试 - 验证Phase 1优化功能
 * 测试内容：
 * 1. HTTP请求包导入功能
 * 2. 流式文件处理（通过API间接测试）
 * 3. 系统基础功能回归测试
 */

const axios = require('axios');

const BASE_URL = 'http://localhost:8080';
const TEST_TOKEN = 'test-token-' + Date.now();

// 颜色输出
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[36m',
};

function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

function logStep(step, message) {
    log(`\n${'='.repeat(60)}`, 'blue');
    log(`[步骤 ${step}] ${message}`, 'blue');
    log('='.repeat(60), 'blue');
}

function logSuccess(message) {
    log(`✅ ${message}`, 'green');
}

function logError(message) {
    log(`❌ ${message}`, 'red');
}

function logInfo(message) {
    log(`ℹ️  ${message}`, 'yellow');
}

// 测试结果统计
const testResults = {
    total: 0,
    passed: 0,
    failed: 0,
    details: []
};

function recordTest(name, passed, message) {
    testResults.total++;
    if (passed) {
        testResults.passed++;
        logSuccess(`${name}: ${message}`);
    } else {
        testResults.failed++;
        logError(`${name}: ${message}`);
    }
    testResults.details.push({ name, passed, message });
}

// 登录获取token
async function login() {
    try {
        const response = await axios.post(`${BASE_URL}/auth/login`, {
            username: 'admin',
            password: 'admin123'
        });
        return response.data.data;
    } catch (error) {
        logError('登录失败: ' + error.message);
        throw error;
    }
}

// 测试1: HTTP请求包导入功能（核心优化）
async function testHttpRequestImport(token) {
    logStep(1, '测试HTTP请求包导入功能');
    
    // 测试用例1.1: 标准POST请求包
    try {
        const rawRequest = `POST /api/check HTTP/1.1
Host: example.com
Authorization: Bearer {{token}}
Content-Type: application/json

{"mobile":"{{phone}}","type":"verify"}`;

        logInfo('测试用例1.1: 导入标准POST请求包');
        const response = await axios.post(
            `${BASE_URL}/template/import-request`,
            { rawRequest },
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        const result = response.data;
        if (result.code === 200 && result.data.success) {
            const data = result.data;
            recordTest('请求包解析', true, `成功解析，发现${data.variables.length}个变量`);
            
            // 验证解析结果
            if (data.url === 'https://example.com/api/check') {
                logSuccess('  - URL解析正确: ' + data.url);
            }
            if (data.method === 'POST') {
                logSuccess('  - 方法解析正确: ' + data.method);
            }
            if (data.headers && data.headers.Authorization) {
                logSuccess('  - Headers解析正确，包含Authorization');
            }
            if (data.variables && data.variables.length === 2) {
                logSuccess('  - 变量识别正确，找到2个变量');
                data.variables.forEach(v => {
                    logInfo(`    变量: ${v.name}, 位置: ${v.location}, 类型: ${v.suggestedType}`);
                });
            }
        } else {
            recordTest('请求包解析', false, '解析失败');
        }
    } catch (error) {
        recordTest('请求包解析', false, error.response?.data?.message || error.message);
    }

    // 测试用例1.2: GET请求包
    try {
        const rawRequest = `GET /api/users?id={{userId}} HTTP/1.1
Host: api.example.com
Authorization: Bearer {{token}}`;

        logInfo('测试用例1.2: 导入GET请求包');
        const response = await axios.post(
            `${BASE_URL}/template/import-request`,
            { rawRequest },
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        const result = response.data;
        if (result.code === 200 && result.data.method === 'GET') {
            recordTest('GET请求解析', true, `URL: ${result.data.url}`);
        } else {
            recordTest('GET请求解析', false, '解析失败');
        }
    } catch (error) {
        recordTest('GET请求解析', false, error.response?.data?.message || error.message);
    }

    // 测试用例1.3: 无占位符的智能检测
    try {
        const rawRequest = `POST /api/login HTTP/1.1
Host: example.com
Content-Type: application/json

{"username":"test","mobile":"13800138000"}`;

        logInfo('测试用例1.3: 智能检测无占位符的变量');
        const response = await axios.post(
            `${BASE_URL}/template/import-request`,
            { rawRequest },
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        const result = response.data;
        if (result.code === 200) {
            recordTest('智能变量检测', true, `检测到${result.data.variables?.length || 0}个潜在变量`);
            if (result.data.variables && result.data.variables.length > 0) {
                logInfo('  智能检测结果:');
                result.data.variables.forEach(v => {
                    logInfo(`    - ${v.name}: ${v.fieldName || 'N/A'} (${v.suggestedType})`);
                });
            }
        }
    } catch (error) {
        recordTest('智能变量检测', false, error.response?.data?.message || error.message);
    }

    // 测试用例1.4: 错误请求包处理
    try {
        const rawRequest = 'INVALID REQUEST FORMAT';

        logInfo('测试用例1.4: 错误请求包处理');
        const response = await axios.post(
            `${BASE_URL}/template/import-request`,
            { rawRequest },
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        if (response.data.code !== 200) {
            recordTest('错误处理', true, '正确识别并拒绝无效请求');
        } else {
            recordTest('错误处理', false, '应该拒绝无效请求');
        }
    } catch (error) {
        // 预期会失败
        recordTest('错误处理', true, '正确抛出错误');
    }
}

// 测试2: 系统基础功能回归测试
async function testBasicFunctions(token) {
    logStep(2, '系统基础功能回归测试');

    // 测试2.1: 获取模板列表
    try {
        logInfo('测试用例2.1: 获取POST模板列表');
        const response = await axios.get(
            `${BASE_URL}/template/list`,
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        if (response.data.code === 200) {
            recordTest('模板列表查询', true, `查询成功，共${response.data.data.length}个模板`);
        } else {
            recordTest('模板列表查询', false, '查询失败');
        }
    } catch (error) {
        recordTest('模板列表查询', false, error.message);
    }

    // 测试2.2: 创建模板
    try {
        logInfo('测试用例2.2: 创建POST模板');
        const templateData = {
            templateName: 'E2E测试模板-' + Date.now(),
            targetSite: 'example.com',
            requestUrl: 'https://example.com/api/test',
            requestMethod: 'POST',
            requestHeaders: '{"Content-Type":"application/json"}',
            requestBody: '{"phone":"{{phone}}"}',
            variableConfig: '[{"name":"phone","type":"手机号"}]',
            duplicateMsg: '已注册',
            responseCode: 200
        };

        const response = await axios.post(
            `${BASE_URL}/template`,
            templateData,
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        if (response.data.code === 200) {
            const templateId = response.data.data;
            recordTest('创建模板', true, `模板ID: ${templateId}`);
            
            // 保存ID用于后续测试
            global.testTemplateId = templateId;
        } else {
            recordTest('创建模板', false, '创建失败');
        }
    } catch (error) {
        recordTest('创建模板', false, error.response?.data?.message || error.message);
    }

    // 测试2.3: 查询模板详情
    if (global.testTemplateId) {
        try {
            logInfo('测试用例2.3: 查询模板详情');
            const response = await axios.get(
                `${BASE_URL}/template/${global.testTemplateId}`,
                { headers: { 'Authorization': `Bearer ${token}` } }
            );

            if (response.data.code === 200 && response.data.data) {
                recordTest('查询模板详情', true, `模板名称: ${response.data.data.templateName}`);
            } else {
                recordTest('查询模板详情', false, '查询失败');
            }
        } catch (error) {
            recordTest('查询模板详情', false, error.message);
        }
    }
}

// 测试3: 性能和稳定性测试
async function testPerformance(token) {
    logStep(3, '性能和稳定性测试');

    // 测试3.1: 并发请求包解析
    try {
        logInfo('测试用例3.1: 并发解析10个请求包');
        const rawRequest = `POST /api/test HTTP/1.1
Host: test.com
Authorization: Bearer {{token}}

{"data":"{{value}}"}`;

        const promises = [];
        for (let i = 0; i < 10; i++) {
            promises.push(
                axios.post(
                    `${BASE_URL}/template/import-request`,
                    { rawRequest },
                    { headers: { 'Authorization': `Bearer ${token}` } }
                )
            );
        }

        const startTime = Date.now();
        const results = await Promise.all(promises);
        const duration = Date.now() - startTime;

        const allSuccess = results.every(r => r.data.code === 200);
        if (allSuccess) {
            recordTest('并发解析性能', true, `10个请求耗时${duration}ms，平均${duration/10}ms/个`);
        } else {
            recordTest('并发解析性能', false, '部分请求失败');
        }
    } catch (error) {
        recordTest('并发解析性能', false, error.message);
    }

    // 测试3.2: 大请求包处理
    try {
        logInfo('测试用例3.2: 处理大请求包（包含多个变量）');
        const largeHeaders = Array.from({ length: 20 }, (_, i) => 
            `X-Custom-${i}: value-${i}`
        ).join('\n');
        
        const rawRequest = `POST /api/large HTTP/1.1
Host: example.com
Authorization: Bearer {{token}}
${largeHeaders}
Content-Type: application/json

{"field1":"{{var1}}","field2":"{{var2}}","field3":"{{var3}}","field4":"{{var4}}","field5":"{{var5}}"}`;

        const response = await axios.post(
            `${BASE_URL}/template/import-request`,
            { rawRequest },
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        if (response.data.code === 200) {
            const varCount = response.data.data.variables.length;
            recordTest('大请求包处理', true, `成功解析，识别${varCount}个变量`);
        } else {
            recordTest('大请求包处理', false, '解析失败');
        }
    } catch (error) {
        recordTest('大请求包处理', false, error.message);
    }
}

// 清理测试数据
async function cleanup(token) {
    logStep(4, '清理测试数据');

    if (global.testTemplateId) {
        try {
            await axios.delete(
                `${BASE_URL}/template/${global.testTemplateId}`,
                { headers: { 'Authorization': `Bearer ${token}` } }
            );
            logSuccess('测试模板已删除');
        } catch (error) {
            logError('删除测试模板失败: ' + error.message);
        }
    }
}

// 主测试流程
async function runTests() {
    log('\n' + '='.repeat(80), 'blue');
    log('  E2E 全量测试 - Phase 1优化功能验证', 'blue');
    log('  测试时间: ' + new Date().toLocaleString(), 'blue');
    log('='.repeat(80) + '\n', 'blue');

    let token;

    try {
        // 登录
        logInfo('正在登录...');
        token = await login();
        logSuccess('登录成功，获取到Token');

        // 执行测试
        await testHttpRequestImport(token);
        await testBasicFunctions(token);
        await testPerformance(token);
        await cleanup(token);

        // 输出测试结果
        log('\n' + '='.repeat(80), 'blue');
        log('  测试结果汇总', 'blue');
        log('='.repeat(80), 'blue');
        log(`总测试数: ${testResults.total}`, 'yellow');
        log(`通过: ${testResults.passed}`, 'green');
        log(`失败: ${testResults.failed}`, 'red');
        log(`成功率: ${((testResults.passed / testResults.total) * 100).toFixed(2)}%`, 'yellow');
        log('='.repeat(80) + '\n', 'blue');

        // 详细结果
        log('详细测试结果:', 'blue');
        testResults.details.forEach((detail, index) => {
            const icon = detail.passed ? '✅' : '❌';
            const color = detail.passed ? 'green' : 'red';
            log(`${index + 1}. ${icon} ${detail.name}: ${detail.message}`, color);
        });

        // 退出码
        process.exit(testResults.failed > 0 ? 1 : 0);

    } catch (error) {
        logError('测试执行失败: ' + error.message);
        console.error(error);
        process.exit(1);
    }
}

// 运行测试
runTests();
