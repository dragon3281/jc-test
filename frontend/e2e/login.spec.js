import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

// 测试日志文件
const LOG_FILE = path.join(process.cwd(), 'e2e-test.log');

function log(message) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] ${message}\n`;
  fs.appendFileSync(LOG_FILE, logMessage);
  console.log(message);
}

test.describe('登录功能测试', () => {
  
  test.beforeAll(() => {
    // 清空日志文件
    fs.writeFileSync(LOG_FILE, '========================================\n');
    fs.appendFileSync(LOG_FILE, '登录功能 E2E 测试日志\n');
    fs.appendFileSync(LOG_FILE, `时间: ${new Date().toLocaleString('zh-CN')}\n`);
    fs.appendFileSync(LOG_FILE, '========================================\n\n');
  });

  test('应该能够成功登录', async ({ page }) => {
    log('测试开始: 登录功能');
    
    // 监听所有请求
    page.on('request', request => {
      if (request.url().includes('/api/')) {
        log(`📤 请求: ${request.method()} ${request.url()}`);
        if (request.postData()) {
          log(`   Body: ${request.postData()}`);
        }
      }
    });

    // 监听所有响应
    page.on('response', async response => {
      if (response.url().includes('/api/')) {
        const status = response.status();
        log(`📥 响应: ${status} ${response.url()}`);
        try {
          const body = await response.text();
          log(`   Body: ${body.substring(0, 200)}`);
        } catch (e) {
          log(`   无法读取响应体`);
        }
      }
    });

    // 监听控制台
    page.on('console', msg => {
      log(`🖥️  Console [${msg.type()}]: ${msg.text()}`);
    });

    // 监听页面错误
    page.on('pageerror', error => {
      log(`❌ 页面错误: ${error.message}`);
    });

    // 1. 访问登录页
    log('\n步骤1: 访问登录页面');
    await page.goto('http://103.246.244.229:3000/login', { 
      waitUntil: 'networkidle',
      timeout: 30000 
    });
    
    log(`✓ 页面加载完成: ${page.url()}`);
    
    // 截图
    await page.screenshot({ path: 'e2e/screenshots/01-login-page.png' });

    // 2. 等待登录表单加载
    log('\n步骤2: 等待登录表单');
    try {
      await page.waitForSelector('input[type="text"], input[placeholder*="用户名"]', { 
        timeout: 10000 
      });
      log('✓ 找到用户名输入框');
    } catch (e) {
      log(`❌ 未找到用户名输入框: ${e.message}`);
      const html = await page.content();
      log(`页面HTML长度: ${html.length}`);
      throw e;
    }

    // 3. 输入用户名和密码
    log('\n步骤3: 输入登录信息');
    
    // 查找用户名输入框
    const usernameInput = await page.locator('input[type="text"], input[placeholder*="用户名"]').first();
    await usernameInput.fill('admin');
    log('✓ 已输入用户名: admin');

    // 查找密码输入框
    const passwordInput = await page.locator('input[type="password"], input[placeholder*="密码"]').first();
    await passwordInput.fill('admin123');
    log('✓ 已输入密码');

    await page.screenshot({ path: 'e2e/screenshots/02-filled-form.png' });

    // 4. 点击登录按钮
    log('\n步骤4: 点击登录按钮');
    const loginButton = await page.locator('button:has-text("登录"), button[type="submit"]').first();
    
    // 等待登录请求和响应
    const responsePromise = page.waitForResponse(
      response => response.url().includes('/user/login'),
      { timeout: 10000 }
    );
    
    await loginButton.click();
    log('✓ 已点击登录按钮');

    // 5. 等待登录响应
    log('\n步骤5: 等待登录响应');
    try {
      const response = await responsePromise;
      const status = response.status();
      const body = await response.json();
      
      log(`响应状态码: ${status}`);
      log(`响应数据: ${JSON.stringify(body, null, 2)}`);
      
      if (status === 200 && body.code === 200) {
        log('✓ 登录成功');
        expect(body.data.token).toBeTruthy();
        log(`✓ 获得Token: ${body.data.token.substring(0, 50)}...`);
      } else if (status === 401) {
        log(`❌ 登录失败: 401 未授权`);
        log(`错误信息: ${body.message || '无'}`);
        throw new Error(`登录返回401: ${JSON.stringify(body)}`);
      } else {
        log(`❌ 登录失败: ${status}`);
        throw new Error(`登录失败: ${JSON.stringify(body)}`);
      }
    } catch (e) {
      log(`❌ 登录请求异常: ${e.message}`);
      await page.screenshot({ path: 'e2e/screenshots/03-login-error.png' });
      throw e;
    }

    // 6. 验证登录后跳转
    log('\n步骤6: 验证登录后跳转');
    await page.waitForURL(/\/(index|home|dashboard)/, { timeout: 10000 });
    log(`✓ 已跳转到: ${page.url()}`);
    
    await page.screenshot({ path: 'e2e/screenshots/04-after-login.png' });

    // 7. 验证localStorage中的token
    log('\n步骤7: 验证Token存储');
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
    log(`✓ Token已存储: ${token.substring(0, 50)}...`);

    log('\n✅ 测试完成: 登录功能正常\n');
  });

  test('应该拒绝错误的用户名密码', async ({ page }) => {
    log('\n测试开始: 错误的登录凭据');
    
    await page.goto('http://103.246.244.229:3000/login');
    
    await page.locator('input[type="text"]').first().fill('wronguser');
    await page.locator('input[type="password"]').first().fill('wrongpass');
    
    const responsePromise = page.waitForResponse(
      response => response.url().includes('/user/login'),
      { timeout: 10000 }
    );
    
    await page.locator('button:has-text("登录")').first().click();
    
    const response = await responsePromise;
    const body = await response.json();
    
    log(`错误登录响应: ${JSON.stringify(body)}`);
    expect(body.code).not.toBe(200);
    
    log('✓ 正确拒绝了错误凭据\n');
  });

  test.afterAll(() => {
    log('\n========================================');
    log('所有测试完成');
    log('========================================\n');
  });
});
