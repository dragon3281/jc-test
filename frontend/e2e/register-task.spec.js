/**
 * 自动化注册任务E2E测试
 * 测试 DES+RSA 双重加密的注册功能
 */

const { test, expect } = require('@playwright/test');

test.describe('自动化注册任务测试', () => {
  let taskId;
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  const apiURL = process.env.API_URL || 'http://localhost:8080';

  test.beforeEach(async ({ page }) => {
    // 登录系统
    await page.goto(`${baseURL}/login`);
    await page.fill('input[type="text"]', 'admin');
    await page.fill('input[type="password"]', 'admin');
    await page.click('button:has-text("登录")');
    
    // 等待登录成功，跳转到首页
    await page.waitForURL(`${baseURL}/dashboard`, { timeout: 5000 });
  });

  test('创建并执行DES+RSA加密注册任务', async ({ page, request }) => {
    console.log('\n========== 测试: 创建DES+RSA加密注册任务 ==========');
    
    // 1. 导航到注册任务页面
    await page.goto(`${baseURL}/business/register`);
    await page.waitForLoadState('networkidle');
    
    // 2. 点击创建任务按钮
    await page.click('button:has-text("新建注册任务")');
    await page.waitForSelector('.el-dialog', { timeout: 5000 });
    
    // 3. 填写任务基本信息
    console.log('填写任务配置...');
    await page.fill('input[placeholder*="任务名称"]', 'E2E测试-WWWTK666自动注册');
    await page.fill('input[placeholder*="网站URL"]', 'https://www.wwwtk666.com');
    await page.fill('input[placeholder*="注册接口"]', '/wps/member/register');
    
    // 选择请求方法为 PUT
    await page.click('label:has-text("PUT")');
    
    // 4. 填写字段映射
    await page.fill('input[placeholder*="用户名字段"]', 'username');
    await page.fill('input[placeholder*="密码字段"]', 'password');
    await page.fill('input[placeholder*="默认密码"]', '133adb');
    
    // 5. 设置账户数量（将在执行配置步骤填写）
    // 6. 配置加密参数 - 选择 DES_RSA
    await page.click('button:has-text("下一步")');
    await page.click('label:has-text("DES+RSA双重加密")');
    
    // 填写加密相关配置
    await page.fill('input[placeholder*="RSA密钥接口"]', '/wps/session/key/rsa');
    await page.fill('input[placeholder*="时间戳参数"]', 't');
    await page.fill('input[placeholder*="加密请求头"]', 'encryption');
    await page.fill('input[placeholder*="数据包装字段"]', 'value');
    await page.click('button:has-text("下一步")');
    await page.fill('input[placeholder*="创建数量"]', '3');
    
    // 7. 填写额外参数（JSON格式）
    const extraParams = {
      "confirmPassword": "133adb",
      "payeeName": "",
      "email": "",
      "qqNum": "",
      "mobileNum": "",
      "captcha": "",
      "verificationCode": "",
      "affiliateCode": "www",
      "paymentPassword": "",
      "line": "",
      "whatsapp": "",
      "facebook": "",
      "wechat": "",
      "idNumber": "",
      "nickname": "",
      "domain": "www-tk999",
      "login": true,
      "registerUrl": "https://www.wwwtk666.com/",
      "registerMethod": "WEB",
      "loginDeviceId": "e6ce5ac9-4b17-4e33-acbd-7350b443f572",
      "headers": {
        "device": "web",
        "language": "BN",
        "merchant": "ck555bdtf3"
      },
      "cookies": {
        "SHELL_deviceId": "772e0b20-91c1-41c5-a522-6f1a9585adbc"
      }
    };
    
    await page.fill('textarea[placeholder*="额外参数"]', JSON.stringify(extraParams, null, 2));
    
    // 8. 提交创建任务
    console.log('提交任务创建请求...');
    await page.click('button:has-text("提交并启动")');
    
    // 等待成功提示
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    console.log('✅ 任务创建成功');
    
    // 9. 等待任务列表更新
    await page.waitForTimeout(1000);
    
    // 10. 获取任务ID（从最新的任务行中提取）
    const taskRow = page.locator('table tbody tr').first();
    await expect(taskRow).toBeVisible({ timeout: 5000 });
    
    // 通过API获取最新任务
    const response = await request.get(`${apiURL}/business/register/list?pageNum=1&pageSize=10`);
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    taskId = data.data.records[0].id;
    console.log(`任务ID: ${taskId}`);
    
    // 11. 启动任务
    console.log('\n========== 启动注册任务 ==========');
    // 已自动启动，无需点击启动按钮
    // 已跳过等待
    
    // 已自动启动，跳过确认
    // 已自动启动，跳过确认
    // 已自动启动，无需额外日志
    
    // 12. 等待任务执行（最多等待30秒）
    console.log('等待任务执行...');
    let taskCompleted = false;
    let attempts = 0;
    const maxAttempts = 30;
    
    while (!taskCompleted && attempts < maxAttempts) {
      await page.waitForTimeout(1000);
      attempts++;
      
      // 检查任务状态
      const statusResponse = await request.get(`${apiURL}/business/register/${taskId}`);
      const statusData = await statusResponse.json();
      const status = statusData.data.status;
      const completedCount = statusData.data.completedCount;
      const successCount = statusData.data.successCount;
      
      console.log(`[${attempts}s] 状态: ${status}, 完成: ${completedCount}/3, 成功: ${successCount}`);
      
      if (status === 3) { // 已完成
        taskCompleted = true;
      }
    }
    
    expect(taskCompleted).toBeTruthy();
    console.log('✅ 任务执行完成');
    
    // 13. 验证注册结果
    console.log('\n========== 验证注册结果 ==========');
    const resultsResponse = await request.get(`${apiURL}/business/register/results/${taskId}`);
    expect(resultsResponse.ok()).toBeTruthy();
    const results = await resultsResponse.json();
    
    console.log('注册结果:');
    console.log(JSON.stringify(results.data, null, 2));
    
    // 验证：至少有一个成功的注册
    const successResults = results.data.filter(r => r.status === 1);
    expect(successResults.length).toBeGreaterThan(0);
    
    // 验证：成功的结果应该包含token
    successResults.forEach((result, index) => {
      console.log(`\n结果 #${index + 1}:`);
      console.log(`  用户名: ${result.username}`);
      console.log(`  密码: ${result.password}`);
      console.log(`  Token: ${result.token}`);
      console.log(`  状态: ${result.status === 1 ? '成功' : '失败'}`);
      console.log(`  消息: ${result.message}`);
      
      expect(result.token).toBeTruthy();
      expect(result.token).toMatch(/^[0-9a-f-]{36}$/); // UUID格式
    });
    
    console.log('\n✅ 所有验证通过！');
  });

  test('脚本上传并测试', async ({ page }) => {
    console.log('\n========== 测试: 脚本上传并测试 ==========');
    await page.goto(`${baseURL}/business/register`);
    await page.click('button:has-text("脚本上传")');
    await page.waitForURL(/\/business\/draft/);
    await page.waitForSelector('.el-dialog:has-text("上传测试脚本")', { timeout: 5000 });

    // 填写上传表单
    await page.fill('input[placeholder*="草稿名称"]', 'E2E脚本-DES_RSA');
    await page.fill('input[placeholder*="https://www.example.com"]', 'https://www.wwwtk666.com');

    // 选择加密类型为 DES+RSA
    await page.click('div.el-form-item:has-text("加密类型") .el-select');
    await page.click('.el-select-dropdown__item:has-text("DES+RSA")');

    const pyScript = "# Test Python Script\ndef encrypt(data):\n    return data\ndef decrypt(data):\n    return data";
    await page.fill('textarea[placeholder*="请粘贴Python脚本内容"]', pyScript);

    // 上传并测试
    await page.click('button:has-text("上传并测试")');

    // 如果弹出保存为模板的确认框，则点击稍后保存
    await page.waitForTimeout(1000);
    const confirmVisible = await page.locator('.el-message-box').isVisible();
    if (confirmVisible) {
      await page.click('button:has-text("稍后保存")');
    }

    // 期待出现信息提示
    await expect(page.locator('.el-message--info')).toBeVisible({ timeout: 5000 });
  });

  test('验证DES加密逻辑', async ({ request }) => {
    console.log('\n========== 测试: 验证DES加密逻辑 ==========');
    
    // 通过API直接测试加密逻辑
    // 这个测试验证后端的DES加密与Python脚本一致
    
    // 创建一个测试任务
    const createResponse = await request.post(`${apiURL}/business/register/create`, {
      data: {
        taskName: 'E2E测试-加密验证',
        websiteUrl: 'https://www.wwwtk666.com',
        registerApi: '/wps/member/register',
        method: 'PUT',
        usernameField: 'username',
        passwordField: 'password',
        defaultPassword: '133adb',
        accountCount: 1,
        encryptionType: 'DES_RSA',
        rsaKeyApi: '/wps/session/key/rsa',
        rsaTsParam: 't',
        encryptionHeader: 'encryption',
        valueFieldName: 'value',
        extraParams: JSON.stringify({
          "confirmPassword": "133adb",
          "payeeName": "",
          "email": "",
          "qqNum": "",
          "mobileNum": "",
          "captcha": "",
          "verificationCode": "",
          "affiliateCode": "www",
          "paymentPassword": "",
          "line": "",
          "whatsapp": "",
          "facebook": "",
          "wechat": "",
          "idNumber": "",
          "nickname": "",
          "domain": "www-tk999",
          "login": true,
          "registerUrl": "https://www.wwwtk666.com/",
          "registerMethod": "WEB",
          "loginDeviceId": "e6ce5ac9-4b17-4e33-acbd-7350b443f572"
        })
      }
    });
    
    expect(createResponse.ok()).toBeTruthy();
    const createData = await createResponse.json();
    const testTaskId = createData.data;
    
    console.log(`创建测试任务ID: ${testTaskId}`);
    
    // 启动任务
    const startResponse = await request.post(`${apiURL}/business/register/start/${testTaskId}`);
    expect(startResponse.ok()).toBeTruthy();
    console.log('✅ 任务已启动，开始检查日志...');
    
    // 等待任务完成
    let completed = false;
    let attempts = 0;
    while (!completed && attempts < 20) {
      await new Promise(resolve => setTimeout(resolve, 1000));
      const statusResp = await request.get(`${apiURL}/business/register/${testTaskId}`);
      const statusData = await statusResp.json();
      if (statusData.data.status === 3) {
        completed = true;
      }
      attempts++;
    }
    
    // 获取结果
    const resultsResp = await request.get(`${apiURL}/business/register/results/${testTaskId}`);
    const results = await resultsResp.json();
    
    console.log('加密验证结果:');
    console.log(JSON.stringify(results.data, null, 2));
    
    // 验证是否成功注册
    const success = results.data.some(r => r.status === 1 && r.token);
    expect(success).toBeTruthy();
    console.log('✅ DES加密逻辑验证通过！');
  });

  test.afterAll(async ({ request }) => {
    // 清理测试数据
    if (taskId) {
      console.log(`\n清理测试任务: ${taskId}`);
      try {
        await request.delete(`${apiURL}/business/register/${taskId}`);
        console.log('✅ 测试数据已清理');
      } catch (error) {
        console.log('⚠️  清理失败，可能需要手动删除');
      }
    }
  });
});
