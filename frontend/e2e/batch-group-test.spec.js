import { test, expect } from '@playwright/test';

/**
 * E2E测试：批量设置分组功能全量测试
 * 测试场景：
 * 1. 登录系统
 * 2. 进入代理资源池页面
 * 3. 选中多个代理节点
 * 4. 批量设置分组
 * 5. 验证分组设置成功
 * 6. 清空分组
 */

test.describe('批量设置分组功能测试', () => {
  let page;
  
  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    
    // 启用详细日志
    page.on('console', msg => {
      console.log(`浏览器控制台 [${msg.type()}]:`, msg.text());
    });
    
    page.on('pageerror', error => {
      console.error('页面错误:', error.message);
    });
    
    page.on('requestfailed', request => {
      console.error('请求失败:', request.url(), request.failure().errorText);
    });
  });
  
  test.afterAll(async () => {
    await page.close();
  });
  
  test('01 - 用户登录', async () => {
    console.log('=== 开始测试：用户登录 ===');
    
    // 访问登录页
    await page.goto('http://localhost:3000/#/login', { waitUntil: 'networkidle' });
    console.log('已打开登录页面');
    
    // 输入用户名和密码
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[type="password"]', 'admin123');
    console.log('已输入登录凭证');
    
    // 点击登录按钮
    await page.click('button:has-text("登录")');
    console.log('已点击登录按钮');
    
    // 等待跳转到首页
    await page.waitForURL('http://localhost:3000/#/', { timeout: 5000 });
    console.log('登录成功，已跳转到首页');
    
    expect(page.url()).toContain('#/');
  });
  
  test('02 - 进入代理资源池页面', async () => {
    console.log('=== 开始测试：进入代理资源池 ===');
    
    // 直接访问代理资源池页面
    await page.goto('http://localhost:3000/#/proxy', { waitUntil: 'networkidle' });
    console.log('已访问代理资源池页面');
    
    // 等待表格加载
    await page.waitForSelector('table', { timeout: 5000 });
    console.log('代理资源池表格已加载');
    
    // 验证页面标题
    const title = await page.locator('.page-toolbar .toolbar-title').textContent();
    console.log('页面标题:', title);
    expect(title).toContain('代理资源池');
  });
  
  test('03 - 添加测试代理节点', async () => {
    console.log('=== 开始测试：添加测试代理节点 ===');
    
    // 点击"添加代理节点"按钮
    await page.click('button:has-text("添加代理节点")');
    console.log('已点击添加代理节点按钮');
    
    // 等待对话框出现
    await page.waitForSelector('.el-dialog', { timeout: 3000 });
    console.log('添加对话框已显示');
    
    // 填写节点信息
    await page.fill('input[placeholder*="节点名称"]', 'E2E测试节点1');
    await page.fill('input[placeholder*="1.2.3.4"]', '192.168.1.100');
    await page.fill('.el-input-number input', '8080');
    console.log('已填写节点基本信息');
    
    // 选择协议类型（HTTP）
    await page.click('label:has-text("HTTP")');
    console.log('已选择HTTP协议');
    
    // 点击确定按钮
    await page.click('.el-dialog__footer button:has-text("确定")');
    console.log('已提交添加请求');
    
    // 等待对话框关闭和数据刷新
    await page.waitForTimeout(2000);
    
    // 验证节点已添加
    const nodeExists = await page.locator('text=E2E测试节点1').count() > 0;
    console.log('节点1添加结果:', nodeExists ? '成功' : '失败');
    expect(nodeExists).toBeTruthy();
  });
  
  test('04 - 添加第二个测试节点', async () => {
    console.log('=== 开始测试：添加第二个测试节点 ===');
    
    // 点击"添加代理节点"按钮
    await page.click('button:has-text("添加代理节点")');
    await page.waitForSelector('.el-dialog', { timeout: 3000 });
    
    // 填写第二个节点信息
    await page.fill('input[placeholder*="节点名称"]', 'E2E测试节点2');
    await page.fill('input[placeholder*="1.2.3.4"]', '192.168.1.101');
    await page.fill('.el-input-number input', '8081');
    
    // 选择SOCKS5协议
    await page.click('label:has-text("SOCKS5")');
    console.log('已选择SOCKS5协议');
    
    // 点击确定
    await page.click('.el-dialog__footer button:has-text("确定")');
    await page.waitForTimeout(2000);
    
    // 验证节点已添加
    const nodeExists = await page.locator('text=E2E测试节点2').count() > 0;
    console.log('节点2添加结果:', nodeExists ? '成功' : '失败');
    expect(nodeExists).toBeTruthy();
  });
  
  test('05 - 选中多个节点', async () => {
    console.log('=== 开始测试：选中多个节点 ===');
    
    // 刷新页面确保数据最新
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForSelector('table', { timeout: 5000 });
    
    // 获取表格中的所有复选框
    const checkboxes = await page.locator('table .el-checkbox').all();
    console.log('表格中的复选框数量:', checkboxes.length);
    
    // 点击前两个复选框（跳过表头的全选框）
    if (checkboxes.length >= 3) {
      await checkboxes[1].click();
      console.log('已选中第1个节点');
      await page.waitForTimeout(500);
      
      await checkboxes[2].click();
      console.log('已选中第2个节点');
      await page.waitForTimeout(500);
    }
    
    // 验证批量操作栏是否显示
    const batchBar = await page.locator('text=/已选择.*个节点/').isVisible();
    console.log('批量操作栏显示:', batchBar);
    expect(batchBar).toBeTruthy();
    
    // 获取选中数量
    const selectedText = await page.locator('text=/已选择.*个节点/').textContent();
    console.log('选中节点信息:', selectedText);
  });
  
  test('06 - 创建测试分组', async () => {
    console.log('=== 开始测试：创建测试分组 ===');
    
    // 点击"分组管理"按钮
    await page.click('button:has-text("分组管理")');
    console.log('已点击分组管理按钮');
    
    // 等待分组管理对话框
    await page.waitForSelector('.el-dialog:has-text("分组管理")', { timeout: 3000 });
    console.log('分组管理对话框已显示');
    
    // 点击"创建分组"
    await page.click('button:has-text("创建分组")');
    await page.waitForTimeout(500);
    
    // 输入分组名称
    await page.fill('.el-message-box input', 'E2E测试分组');
    console.log('已输入分组名称');
    
    // 点击创建按钮
    await page.click('.el-message-box button:has-text("创建")');
    await page.waitForTimeout(1000);
    
    // 关闭分组管理对话框
    await page.click('.el-dialog:has-text("分组管理") .el-dialog__close');
    console.log('已关闭分组管理对话框');
    
    await page.waitForTimeout(500);
  });
  
  test('07 - 批量设置分组', async () => {
    console.log('=== 开始测试：批量设置分组 ===');
    
    // 确保节点仍然被选中
    const batchBar = await page.locator('text=/已选择.*个节点/').isVisible();
    if (!batchBar) {
      console.log('节点选中状态丢失，重新选中...');
      // 重新选中节点
      const checkboxes = await page.locator('table .el-checkbox').all();
      if (checkboxes.length >= 3) {
        await checkboxes[1].click();
        await page.waitForTimeout(300);
        await checkboxes[2].click();
        await page.waitForTimeout(300);
      }
    }
    
    // 点击"批量设置分组"按钮
    await page.click('button:has-text("批量设置分组")');
    console.log('已点击批量设置分组按钮');
    
    // 等待选择分组对话框
    await page.waitForSelector('.el-message-box', { timeout: 3000 });
    console.log('设置分组对话框已显示');
    
    // 选择"E2E测试分组"
    await page.click('.el-select');
    await page.waitForTimeout(500);
    
    // 点击分组选项
    await page.click('.el-select-dropdown__item:has-text("E2E测试分组")');
    console.log('已选择E2E测试分组');
    
    await page.waitForTimeout(500);
    
    // 点击确定按钮
    await page.click('.el-message-box button:has-text("确定")');
    console.log('已点击确定按钮');
    
    // 等待操作完成
    await page.waitForTimeout(2000);
    
    // 验证成功消息
    const successMsg = await page.locator('.el-message--success').isVisible().catch(() => false);
    console.log('成功消息显示:', successMsg);
  });
  
  test('08 - 验证分组设置成功', async () => {
    console.log('=== 开始测试：验证分组设置 ===');
    
    // 刷新页面
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForSelector('table', { timeout: 5000 });
    
    // 查找包含"E2E测试分组"的单元格
    const groupTags = await page.locator('.el-tag:has-text("E2E测试分组")').count();
    console.log('找到的分组标签数量:', groupTags);
    
    // 至少应该有2个节点被设置了分组
    expect(groupTags).toBeGreaterThanOrEqual(2);
    console.log('✓ 分组设置成功验证通过');
  });
  
  test('09 - 批量清空分组', async () => {
    console.log('=== 开始测试：批量清空分组 ===');
    
    // 选中有分组的节点
    const checkboxes = await page.locator('table .el-checkbox').all();
    if (checkboxes.length >= 3) {
      await checkboxes[1].click();
      await page.waitForTimeout(300);
      await checkboxes[2].click();
      await page.waitForTimeout(300);
    }
    
    // 点击"批量设置分组"
    await page.click('button:has-text("批量设置分组")');
    await page.waitForSelector('.el-message-box', { timeout: 3000 });
    
    // 选择"清空分组"
    await page.click('.el-select');
    await page.waitForTimeout(500);
    await page.click('.el-select-dropdown__item:has-text("清空分组")');
    console.log('已选择清空分组');
    
    await page.waitForTimeout(500);
    
    // 点击确定
    await page.click('.el-message-box button:has-text("确定")');
    await page.waitForTimeout(2000);
    
    console.log('✓ 清空分组操作完成');
  });
  
  test('10 - 清理测试数据', async () => {
    console.log('=== 开始清理测试数据 ===');
    
    // 刷新页面
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForSelector('table', { timeout: 5000 });
    
    // 删除测试节点1
    const deleteButtons = await page.locator('button:has-text("删除")').all();
    if (deleteButtons.length > 0) {
      // 删除第一个测试节点
      await deleteButtons[0].click();
      await page.waitForTimeout(500);
      await page.click('.el-message-box button:has-text("确定")');
      await page.waitForTimeout(1500);
      console.log('已删除测试节点1');
    }
    
    // 刷新后删除第二个节点
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForSelector('table', { timeout: 5000 });
    
    const deleteButtons2 = await page.locator('button:has-text("删除")').all();
    if (deleteButtons2.length > 0) {
      await deleteButtons2[0].click();
      await page.waitForTimeout(500);
      await page.click('.el-message-box button:has-text("确定")');
      await page.waitForTimeout(1500);
      console.log('已删除测试节点2');
    }
    
    // 删除测试分组
    await page.click('button:has-text("分组管理")');
    await page.waitForSelector('.el-dialog:has-text("分组管理")', { timeout: 3000 });
    
    const groupDeleteButtons = await page.locator('.el-dialog:has-text("分组管理") button:has-text("删除")').all();
    if (groupDeleteButtons.length > 0) {
      await groupDeleteButtons[0].click();
      await page.waitForTimeout(500);
      await page.click('.el-message-box button:has-text("确定")');
      await page.waitForTimeout(1000);
      console.log('已删除测试分组');
    }
    
    console.log('✓ 测试数据清理完成');
  });
});
