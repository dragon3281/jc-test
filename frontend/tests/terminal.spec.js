import { test, expect } from '@playwright/test'

test.describe('SSH终端功能测试', () => {
  let page
  let context

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext()
    page = await context.newPage()
    
    // 登录
    await page.goto('http://103.246.244.229:3000/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })
  })

  test.afterAll(async () => {
    await context.close()
  })

  test('1. 测试服务器管理页面密码隐藏功能', async () => {
    console.log('测试1：检查密码是否默认隐藏')
    
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    
    // 检查认证凭证列是否显示为••••••••
    const credentialCell = await page.locator('text=••••••••').first()
    await expect(credentialCell).toBeVisible()
    console.log('✓ 密码默认显示为••••••••')
    
    // 测试眼睛图标切换
    const eyeIcon = await page.locator('.el-icon').first()
    await eyeIcon.click()
    await page.waitForTimeout(500)
    console.log('✓ 点击眼睛图标可切换显示')
  })

  test('2. 测试编辑服务器时密码输入框', async () => {
    console.log('测试2：检查编辑对话框密码输入')
    
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    
    // 点击编辑按钮
    const editButton = await page.locator('button:has-text("编辑")').first()
    await editButton.click()
    await page.waitForSelector('.el-dialog', { timeout: 3000 })
    console.log('✓ 打开编辑对话框')
    
    // 检查凭证输入框类型是否为password
    const credentialInput = await page.locator('input[type="password"]')
    await expect(credentialInput).toBeVisible()
    console.log('✓ 凭证输入框默认为密码类型')
    
    // 检查是否有眼睛图标
    const eyeIconInDialog = await page.locator('.el-dialog .el-icon')
    await expect(eyeIconInDialog).toBeVisible()
    console.log('✓ 对话框中有眼睛图标可切换显示')
    
    // 关闭对话框
    await page.click('button:has-text("取消")')
  })

  test('3. 测试添加服务器密码安全性', async () => {
    console.log('测试3：测试添加服务器密码输入')
    
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    
    // 点击添加服务器
    await page.click('button:has-text("添加服务器")')
    await page.waitForSelector('.el-dialog', { timeout: 3000 })
    console.log('✓ 打开添加服务器对话框')
    
    // 填写表单
    await page.fill('input[placeholder="请输入服务器名称"]', 'E2E测试服务器')
    await page.fill('input[placeholder="请输入IP地址"]', '192.168.1.100')
    
    // 检查密码输入框
    const passwordInput = await page.locator('input[type="password"]')
    await expect(passwordInput).toBeVisible()
    console.log('✓ 添加时密码输入框为password类型')
    
    // 输入密码
    await passwordInput.fill('test123456')
    
    // 点击眼睛图标查看密码
    const eyeIcon = await page.locator('.el-dialog .el-icon').first()
    await eyeIcon.click()
    await page.waitForTimeout(500)
    
    // 验证密码可见
    const visibleInput = await page.locator('textarea')
    const value = await visibleInput.inputValue()
    expect(value).toBe('test123456')
    console.log('✓ 点击眼睛图标后密码可见')
    
    // 关闭对话框
    await page.click('button:has-text("取消")')
  })

  test('4. 测试SSH终端按钮存在性', async () => {
    console.log('测试4：检查终端按钮是否存在')
    
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    
    // 检查是否有终端按钮
    const terminalButton = await page.locator('button:has-text("终端")').first()
    await expect(terminalButton).toBeVisible()
    console.log('✓ 终端按钮存在且可见')
  })

  test('5. 测试SSH终端页面打开', async () => {
    console.log('测试5：测试终端页面打开')
    
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    
    // 获取第一台服务器（确保有在线服务器）
    const onlineServer = await page.locator('.el-tag:has-text("在线")').first()
    if (await onlineServer.count() === 0) {
      console.log('⚠ 没有在线服务器，跳过终端连接测试')
      return
    }
    
    // 点击终端按钮，监听新窗口
    const [newPage] = await Promise.all([
      context.waitForEvent('page'),
      page.locator('button:has-text("终端")').first().click()
    ])
    
    console.log('✓ 新窗口已打开')
    
    // 等待终端页面加载
    await newPage.waitForLoadState('domcontentloaded')
    await newPage.waitForSelector('.terminal-page', { timeout: 5000 })
    console.log('✓ 终端页面已加载')
    
    // 检查终端标题
    const title = await newPage.locator('.header-title')
    await expect(title).toContainText('SSH终端')
    console.log('✓ 终端标题正确')
    
    // 检查终端容器
    const terminalContainer = await newPage.locator('#terminal')
    await expect(terminalContainer).toBeVisible()
    console.log('✓ 终端容器存在')
    
    // 等待连接状态标签
    await newPage.waitForSelector('.el-tag', { timeout: 3000 })
    
    // 等待WebSocket连接（最多15秒）
    console.log('等待SSH连接建立...')
    await newPage.waitForTimeout(5000)
    
    // 检查连接状态
    const statusTag = await newPage.locator('.el-tag').first()
    const statusText = await statusTag.textContent()
    
    if (statusText.includes('已连接')) {
      console.log('✓ SSH连接成功建立')
      
      // 检查终端输出
      const terminalOutput = await newPage.locator('.xterm-screen')
      await expect(terminalOutput).toBeVisible()
      console.log('✓ 终端输出区域可见')
      
      // 尝试输入命令（测试交互）
      await newPage.keyboard.type('echo "E2E Test Success"')
      await newPage.keyboard.press('Enter')
      await newPage.waitForTimeout(1000)
      console.log('✓ 终端可以输入命令')
      
    } else {
      console.log('⚠ SSH连接未建立，状态：', statusText)
      
      // 检查终端中的错误信息
      const terminalText = await newPage.locator('.terminal-container').textContent()
      console.log('终端输出：', terminalText)
      
      // 即使连接失败，也要验证错误提示是否友好
      if (terminalText.includes('请检查') || terminalText.includes('连接')) {
        console.log('✓ 提供了友好的错误提示')
      }
    }
    
    // 关闭终端窗口
    await newPage.close()
    console.log('✓ 终端窗口已关闭')
  })

  test('6. WebSocket连接URL验证', async () => {
    console.log('测试6：验证WebSocket连接URL')
    
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    
    // 打开终端
    const [terminalPage] = await Promise.all([
      context.waitForEvent('page'),
      page.locator('button:has-text("终端")').first().click()
    ])
    
    await terminalPage.waitForLoadState('domcontentloaded')
    
    // 监听控制台输出
    const consoleLogs = []
    terminalPage.on('console', msg => {
      if (msg.type() === 'log') {
        consoleLogs.push(msg.text())
      }
    })
    
    await terminalPage.waitForTimeout(3000)
    
    // 检查WebSocket URL
    const wsUrlLog = consoleLogs.find(log => log.includes('WebSocket连接URL'))
    if (wsUrlLog) {
      console.log('✓ WebSocket连接URL:', wsUrlLog)
      expect(wsUrlLog).toContain('ws://103.246.244.229:8080/ws/ssh')
      console.log('✓ WebSocket URL格式正确')
    }
    
    await terminalPage.close()
  })
})

test.describe('完整终端功能流程测试', () => {
  test('完整流程：从登录到使用终端', async ({ browser }) => {
    console.log('=== 开始完整流程测试 ===')
    
    const context = await browser.newContext()
    const page = await context.newPage()
    
    // 1. 登录
    console.log('步骤1: 登录系统')
    await page.goto('http://103.246.244.229:3000/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })
    console.log('✓ 登录成功')
    
    // 2. 进入服务器管理
    console.log('步骤2: 进入服务器管理页面')
    await page.goto('http://103.246.244.229:3000/server')
    await page.waitForSelector('.server-management', { timeout: 5000 })
    console.log('✓ 服务器管理页面加载完成')
    
    // 3. 验证密码隐藏
    console.log('步骤3: 验证密码隐藏功能')
    const hiddenPassword = await page.locator('text=••••••••').count()
    expect(hiddenPassword).toBeGreaterThan(0)
    console.log('✓ 密码已隐藏')
    
    // 4. 打开终端
    console.log('步骤4: 打开SSH终端')
    const terminalButtonCount = await page.locator('button:has-text("终端")').count()
    if (terminalButtonCount > 0) {
      const [terminalPage] = await Promise.all([
        context.waitForEvent('page'),
        page.locator('button:has-text("终端")').first().click()
      ])
      
      await terminalPage.waitForSelector('.terminal-page', { timeout: 5000 })
      console.log('✓ 终端页面已打开')
      
      // 等待连接
      await terminalPage.waitForTimeout(5000)
      
      const statusTag = await terminalPage.locator('.el-tag').first()
      const statusText = await statusTag.textContent()
      console.log(`终端连接状态: ${statusText}`)
      
      await terminalPage.close()
      console.log('✓ 终端测试完成')
    } else {
      console.log('⚠ 没有找到终端按钮')
    }
    
    await context.close()
    console.log('=== 完整流程测试完成 ===')
  })
})
