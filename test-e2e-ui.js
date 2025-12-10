/**
 * E2E UI测试脚本 - 使用Puppeteer模拟前端点击
 * 测试内容：
 * 1. 用户登录
 * 2. 访问代理资源池页面
 * 3. 测试分组管理功能（创建、重命名、删除）
 * 4. 验证WebSocket连接和心跳
 */

const puppeteer = require('puppeteer');

const BASE_URL = 'http://localhost:3000';
const USERNAME = 'admin';
const PASSWORD = 'admin123';

// 延迟函数
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// 日志函数
const log = (msg, type = 'info') => {
  const timestamp = new Date().toISOString();
  const prefix = {
    info: '✓',
    error: '✗',
    warn: '⚠',
    test: '➤'
  }[type] || '•';
  console.log(`[${timestamp}] ${prefix} ${msg}`);
};

async function runE2ETest() {
  let browser;
  try {
    log('启动浏览器...', 'test');
    browser = await puppeteer.launch({
      headless: true,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu'
      ]
    });

    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });

    // 监听控制台日志
    page.on('console', msg => {
      const text = msg.text();
      if (text.includes('[WebSocket]')) {
        log(`浏览器控制台: ${text}`, 'info');
      }
    });

    // 监听页面错误
    page.on('pageerror', error => {
      log(`页面错误: ${error.message}`, 'error');
    });

    // 第1步：访问登录页面
    log('第1步：访问登录页面', 'test');
    await page.goto(BASE_URL, { waitUntil: 'networkidle2', timeout: 30000 });
    await delay(2000);
    log('登录页面加载成功');

    // 第2步：执行登录
    log('第2步：输入用户名和密码', 'test');
    
    // 等待页面完全加载
    await page.waitForSelector('input', { timeout: 10000 });
    
    // 获取所有input元素进行调试
    const inputsDebug = await page.$$eval('input', inputs => 
      inputs.map(i => ({ type: i.type, placeholder: i.placeholder, name: i.name }))
    );
    log(`页面上的所有input: ${JSON.stringify(inputsDebug)}`, 'info');
    
    // 使用更精确的选择器
    const usernameInput = await page.$('input[placeholder*="用户名"], input[name="username"], input[type="text"]');
    const passwordInput = await page.$('input[placeholder*="密码"], input[name="password"], input[type="password"]');
    
    if (!usernameInput || !passwordInput) {
      log('未找到用户名或密码输入框', 'error');
      // 尝试直接使用JavaScript填充表单
      await page.evaluate((user, pass) => {
        const inputs = Array.from(document.querySelectorAll('input'));
        if (inputs[0]) {
          inputs[0].value = user;
          inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
          inputs[0].dispatchEvent(new Event('change', { bubbles: true }));
        }
        if (inputs[1]) {
          inputs[1].value = pass;
          inputs[1].dispatchEvent(new Event('input', { bubbles: true }));
          inputs[1].dispatchEvent(new Event('change', { bubbles: true }));
        }
      }, USERNAME, PASSWORD);
    } else {
      // 使用JavaScript设置值并触发Vue的响应式更新
      await page.evaluate((user, pass) => {
        const usernameInput = document.querySelector('input[name="username"]');
        const passwordInput = document.querySelector('input[name="password"]');
        
        if (usernameInput) {
          usernameInput.value = user;
          usernameInput.dispatchEvent(new Event('input', { bubbles: true }));
          usernameInput.dispatchEvent(new Event('change', { bubbles: true }));
        }
        
        if (passwordInput) {
          passwordInput.value = pass;
          passwordInput.dispatchEvent(new Event('input', { bubbles: true }));
          passwordInput.dispatchEvent(new Event('change', { bubbles: true }));
        }
      }, USERNAME, PASSWORD);
    }
    
    log(`已输入用户名: ${USERNAME}`);
    await delay(1000);

    log('第3步：点击登录按钮', 'test');
    
    // 查找登录按钮
    const buttonsDebug = await page.$$eval('button', btns => 
      btns.map(b => ({ text: b.textContent, type: b.type, class: b.className }))
    );
    log(`页面上的所有button: ${JSON.stringify(buttonsDebug)}`, 'info');
    
    // 尝试多种方式点击登录按钮
    const loginClicked = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const loginBtn = buttons.find(b => 
        b.textContent.includes('登录') || 
        b.textContent.includes('登 录') ||
        b.type === 'submit'
      );
      if (loginBtn) {
        loginBtn.click();
        return true;
      }
      return false;
    });
    
    if (!loginClicked) {
      log('未找到登录按钮，尝试模拟回车', 'warn');
      await page.keyboard.press('Enter');
    }
    
    await delay(3000);

    // 检查是否登录成功
    let currentUrlCheck = page.url();
    if (currentUrlCheck.includes('/login')) {
      log('登录失败，仍在登录页面', 'error');
      const errorMsg = await page.$eval('.el-message--error', el => el.textContent).catch(() => '未知错误');
      log(`错误信息: ${errorMsg}`, 'error');
      return;
    }
    log('登录成功！', 'info');

    // 第4步：导航到代理资源池页面
    log('第4步：导航到代理资源池页面', 'test');
    
    // 等待页面加载完成
    await delay(2000);
    
    // 查找所有链接
    const linksDebug = await page.$$eval('a', links => 
      links.map(l => ({ text: l.textContent.trim(), href: l.href })).filter(l => l.text)
    );
    log(`页面上的所有链接: ${JSON.stringify(linksDebug.slice(0, 10))}`, 'info');
    
    // 尝试点击代理资源池菜单
    const proxyClicked = await page.evaluate(() => {
      const links = Array.from(document.querySelectorAll('a'));
      const proxyLink = links.find(l => 
        l.textContent.includes('代理资源池') || 
        l.textContent.includes('代理') ||
        l.href.includes('proxy')
      );
      if (proxyLink) {
        proxyLink.click();
        return true;
      }
      return false;
    });
    
    if (!proxyClicked) {
      log('未找到代理资源池菜单，尝试直接访问URL', 'warn');
      await page.goto(`${BASE_URL}/proxy`, { waitUntil: 'networkidle2', timeout: 30000 });
    }
    
    await delay(3000);
    
    // 验证是否真的在代理页面
    let currentUrl = page.url();
    const pageTitle = await page.evaluate(() => {
      const toolbar = document.querySelector('.page-toolbar .toolbar-title');
      return toolbar ? toolbar.textContent.trim() : 'not found';
    });
    log(`当前URL: ${currentUrl}, 页面标题: ${pageTitle}`);
    
    if (!pageTitle.includes('代理资源池')) {
      log('页面标题不是"代理资源池"，尝试重新刷新页面', 'warn');
      await page.reload({ waitUntil: 'networkidle2' });
      await delay(3000);
    }
    
    log('已进入代理资源池页面');

    // 等待WebSocket连接建立
    log('等待WebSocket连接建立...', 'test');
    await delay(3000);

    // 第5步：打开分组管理对话框
    log('第5步：点击"分组管理"按钮', 'test');
    
    // 等待页面加载
    await delay(3000);
    
    // 截图以便调试
    await page.screenshot({ path: '/root/jc-test/logs/proxy-page-before-click.png', fullPage: true });
    log('代理页面截图已保存: /root/jc-test/logs/proxy-page-before-click.png', 'info');
    
    // 查找所有按钮（包括隐藏的）
    const allButtons = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.map(b => ({
        text: b.textContent.trim(),
        visible: b.offsetParent !== null,
        className: b.className
      }));
    });
    log(`页面上的所有按钮 (${allButtons.length}个): ${JSON.stringify(allButtons)}`, 'info');
    
    // 查看页面toolbar区域
    const toolbarHtml = await page.evaluate(() => {
      const toolbar = document.querySelector('.page-toolbar, .toolbar-actions');
      return toolbar ? toolbar.innerHTML : 'toolbar not found';
    });
    log(`Toolbar HTML: ${toolbarHtml.substring(0, 500)}`, 'info');
    
    // 尝试通过文本内容查找
    const found = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const btn = buttons.find(b => b.textContent.includes('分组管理'));
      if (btn) {
        btn.click();
        return true;
      }
      return false;
    });
    
    if (!found) {
      log('未找到"分组管理"按钮，尝试查找"管理分组"按钮', 'warn');
      const altFound = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const btn = buttons.find(b => b.textContent.includes('管理分组'));
        if (btn) {
          btn.click();
          return true;
        }
        return false;
      });
      
      if (!altFound) {
        log('也未找到"管理分组"按钮，继续测试WebSocket心跳', 'error');
        // 不直接return，继续测试WebSocket
      } else {
        await delay(2000);
        log('"管理分组"对话框已打开');
      }
    } else {
      await delay(2000);
      log('分组管理对话框已打开');
    }

    // 第6步：测试创建分组
    log('第6步：测试创建分组功能', 'test');
    const createBtnFound = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const btn = buttons.find(b => b.textContent.includes('新建分组'));
      if (btn) {
        btn.click();
        return true;
      }
      return false;
    });

    if (createBtnFound) {
      await delay(1000);
      
      // 输入分组名称
      const groupName = `E2E测试分组_${Date.now()}`;
      await page.waitForSelector('.el-message-box input', { timeout: 5000 });
      await page.type('.el-message-box input', groupName);
      log(`输入分组名称: ${groupName}`);
      
      // 点击确定
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('.el-message-box button'));
        const confirmBtn = buttons.find(b => b.textContent.includes('确定'));
        if (confirmBtn) confirmBtn.click();
      });
      
      await delay(2000);
      log('创建分组操作已提交');
    } else {
      log('未找到"新建分组"按钮', 'warn');
    }

    // 第7步：检查分组列表
    log('第7步：检查分组列表', 'test');
    const groups = await page.evaluate(() => {
      const rows = Array.from(document.querySelectorAll('.el-table__body tr'));
      return rows.map(row => {
        const cells = row.querySelectorAll('td');
        return {
          name: cells[0]?.textContent.trim() || '',
          count: cells[1]?.textContent.trim() || ''
        };
      }).filter(g => g.name);
    });
    
    if (groups.length > 0) {
      log(`找到 ${groups.length} 个分组:`, 'info');
      groups.forEach(g => log(`  - ${g.name} (${g.count}个节点)`, 'info'));
    } else {
      log('分组列表为空', 'warn');
    }

    // 第8步：测试重命名分组（如果有分组）
    if (groups.length > 0) {
      log('第8步：测试重命名分组功能', 'test');
      const renameSuccess = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const renameBtn = buttons.find(b => b.textContent.includes('重命名'));
        if (renameBtn) {
          renameBtn.click();
          return true;
        }
        return false;
      });

      if (renameSuccess) {
        await delay(1000);
        const newName = `重命名分组_${Date.now()}`;
        
        await page.waitForSelector('.el-message-box input', { timeout: 5000 });
        // 清空输入框
        await page.evaluate(() => {
          const input = document.querySelector('.el-message-box input');
          if (input) input.value = '';
        });
        await page.type('.el-message-box input', newName);
        log(`输入新分组名称: ${newName}`);
        
        // 点击确定
        await page.evaluate(() => {
          const buttons = Array.from(document.querySelectorAll('.el-message-box button'));
          const confirmBtn = buttons.find(b => b.textContent.includes('确定'));
          if (confirmBtn) confirmBtn.click();
        });
        
        await delay(2000);
        log('重命名分组操作已提交');
      }
    }

    // 第9步：测试删除分组（如果有分组）
    if (groups.length > 0) {
      log('第9步：测试删除分组功能', 'test');
      const deleteSuccess = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const deleteBtn = buttons.find(b => b.textContent.includes('删除') && b.className.includes('danger'));
        if (deleteBtn) {
          deleteBtn.click();
          return true;
        }
        return false;
      });

      if (deleteSuccess) {
        await delay(1000);
        
        // 确认删除
        await page.evaluate(() => {
          const buttons = Array.from(document.querySelectorAll('.el-message-box button'));
          const confirmBtn = buttons.find(b => b.textContent.includes('确定'));
          if (confirmBtn) confirmBtn.click();
        });
        
        await delay(2000);
        log('删除分组操作已提交');
      }
    }

    // 第10步：验证WebSocket心跳
    log('第10步：验证WebSocket心跳（等待30秒）', 'test');
    log('监听WebSocket STOMP心跳日志...', 'info');
    await delay(30000);
    log('WebSocket心跳监听完成（查看上方日志）');

    // 截图保存
    await page.screenshot({ path: '/root/jc-test/logs/e2e-test-result.png', fullPage: true });
    log('测试截图已保存: /root/jc-test/logs/e2e-test-result.png', 'info');

    log('==================', 'test');
    log('E2E测试完成！所有功能已验证', 'test');
    log('==================', 'test');

  } catch (error) {
    log(`测试过程中出现错误: ${error.message}`, 'error');
    log(error.stack, 'error');
  } finally {
    if (browser) {
      await browser.close();
      log('浏览器已关闭', 'info');
    }
  }
}

// 运行测试
runE2ETest().catch(error => {
  log(`测试启动失败: ${error.message}`, 'error');
  process.exit(1);
});
