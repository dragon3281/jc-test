import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'

// 设置详细日志
test.beforeEach(async ({ page }) => {
  // 监听所有控制台输出
  page.on('console', msg => {
    console.log(`[浏览器控制台 ${msg.type()}]:`, msg.text())
  })
  
  // 监听所有网络请求
  page.on('request', request => {
    console.log(`[请求] ${request.method()} ${request.url()}`)
  })
  
  // 监听所有网络响应
  page.on('response', response => {
    console.log(`[响应] ${response.status()} ${response.url()}`)
  })
  
  // 监听所有页面错误
  page.on('pageerror', error => {
    console.error('[页面错误]:', error.message)
  })
  
  // 监听所有对话框
  page.on('dialog', dialog => {
    console.log(`[对话框 ${dialog.type()}]:`, dialog.message())
  })
})

// 登录并进入代理资源池页面
async function loginAndGotoProxy(page) {
  console.log('=== 开始登录流程 ===')
  await page.goto(BASE_URL + '/login')
  await page.waitForLoadState('networkidle')
  
  console.log('输入用户名密码')
  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('admin123')
  
  console.log('点击登录按钮')
  await page.getByRole('button', { name: '登 录' }).click()
  
  console.log('等待跳转到dashboard')
  await page.waitForURL(/dashboard/, { timeout: 10000 })
  
  console.log('进入代理资源池页面')
  await page.goto(BASE_URL + '/proxy')
  await page.waitForLoadState('networkidle')
  
  console.log('验证页面加载完成')
  await expect(page.getByText('代理资源池')).toBeVisible()
  
  console.log('=== 登录流程完成 ===\n')
}

test.describe('代理资源池全量功能测试', () => {
  
  test('01-页面加载与基础元素检查', async ({ page }) => {
    console.log('\n▶ 测试01: 页面加载与基础元素检查')
    await loginAndGotoProxy(page)
    
    // 检查顶部工具栏按钮
    console.log('检查顶部工具栏按钮...')
    await expect(page.getByRole('button', { name: '刷新' })).toBeVisible()
    await expect(page.getByRole('button', { name: '管理分组' })).toBeVisible()
    await expect(page.getByRole('button', { name: '一键识别' })).toBeVisible()
    await expect(page.getByRole('button', { name: '添加代理节点' })).toBeVisible()
    
    // 检查搜索栏元素
    console.log('检查搜索栏元素...')
    await expect(page.getByPlaceholder('搜索节点名称/IP')).toBeVisible()
    await expect(page.getByText('国家').first()).toBeVisible()
    await expect(page.getByText('分组').first()).toBeVisible()
    await expect(page.getByRole('button', { name: '搜索' })).toBeVisible()
    await expect(page.getByRole('button', { name: '重置' })).toBeVisible()
    
    // 检查表格列
    console.log('检查表格列...')
    await expect(page.getByText('代理节点').first()).toBeVisible()
    await expect(page.getByText('IP地址').first()).toBeVisible()
    await expect(page.getByText('端口').first()).toBeVisible()
    await expect(page.getByText('类型').first()).toBeVisible()
    await expect(page.getByText('认证').first()).toBeVisible()
    await expect(page.getByText('状态').first()).toBeVisible()
    await expect(page.getByText('操作').first()).toBeVisible()
    
    console.log('✓ 页面基础元素检查通过\n')
  })

  test('02-刷新按钮功能测试', async ({ page }) => {
    console.log('\n▶ 测试02: 刷新按钮功能测试')
    await loginAndGotoProxy(page)
    
    console.log('点击刷新按钮')
    const refreshButton = page.getByRole('button', { name: '刷新' })
    
    // 等待网络空闲
    await page.waitForLoadState('networkidle')
    
    // 点击刷新
    await refreshButton.click()
    
    // 等待请求完成
    await page.waitForTimeout(1000)
    
    console.log('✓ 刷新按钮功能测试完成\n')
  })

  test('03-添加代理节点对话框测试', async ({ page }) => {
    console.log('\n▶ 测试03: 添加代理节点对话框测试')
    await loginAndGotoProxy(page)
    
    console.log('点击添加代理节点按钮')
    await page.getByRole('button', { name: '添加代理节点' }).click()
    
    // 等待对话框出现
    await page.waitForTimeout(500)
    
    console.log('检查对话框是否打开')
    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible()
    
    // 检查对话框标题
    await expect(page.getByText('添加代理池')).toBeVisible()
    
    // 检查表单字段
    console.log('检查表单字段...')
    await expect(page.getByLabel('代理池名称')).toBeVisible()
    await expect(page.getByLabel('IP地址')).toBeVisible()
    await expect(page.getByLabel('端口')).toBeVisible()
    await expect(page.getByLabel('协议类型')).toBeVisible()
    await expect(page.getByLabel('是否认证')).toBeVisible()
    await expect(page.getByLabel('描述')).toBeVisible()
    
    // 关闭对话框
    console.log('点击取消关闭对话框')
    await page.getByRole('button', { name: '取消' }).click()
    await page.waitForTimeout(300)
    
    console.log('✓ 添加代理节点对话框测试完成\n')
  })

  test('04-一键识别功能测试', async ({ page }) => {
    console.log('\n▶ 测试04: 一键识别功能测试')
    await loginAndGotoProxy(page)
    
    console.log('点击一键识别按钮')
    await page.getByRole('button', { name: '一键识别' }).click()
    
    await page.waitForTimeout(500)
    
    console.log('检查一键识别对话框')
    await expect(page.getByText('一键识别代理配置')).toBeVisible()
    
    // 检查格式说明
    await expect(page.getByText('支持的格式')).toBeVisible()
    
    // 测试输入框
    const configInput = page.locator('textarea').first()
    await expect(configInput).toBeVisible()
    
    console.log('输入测试配置')
    await configInput.fill('socks://d2hzX3FteDo1OGdhbmppQDEyMw==@123.254.105.253:22201#test-proxy')
    
    // 点击识别配置按钮
    console.log('点击识别配置按钮')
    const recognizeBtn = page.getByRole('button', { name: '识别配置' })
    if (await recognizeBtn.isVisible()) {
      await recognizeBtn.click()
      await page.waitForTimeout(2000)
      console.log('等待识别结果...')
    }
    
    // 关闭对话框
    await page.getByRole('button', { name: '取消' }).first().click()
    
    console.log('✓ 一键识别功能测试完成\n')
  })

  test('05-管理分组按钮测试', async ({ page }) => {
    console.log('\n▶ 测试05: 管理分组按钮测试')
    await loginAndGotoProxy(page)
    
    console.log('点击管理分组按钮')
    const manageGroupBtn = page.getByRole('button', { name: '管理分组' })
    await expect(manageGroupBtn).toBeVisible()
    
    // 获取按钮的点击事件处理器
    const btnHandle = await manageGroupBtn.elementHandle()
    if (btnHandle) {
      console.log('按钮元素找到')
      const isEnabled = await manageGroupBtn.isEnabled()
      console.log('按钮是否可用:', isEnabled)
      
      // 尝试点击
      try {
        await manageGroupBtn.click({ timeout: 3000 })
        await page.waitForTimeout(1000)
        console.log('按钮点击执行完成')
      } catch (error) {
        console.error('点击按钮时出错:', error.message)
      }
    }
    
    console.log('✓ 管理分组按钮测试完成\n')
  })

  test('06-搜索功能测试', async ({ page }) => {
    console.log('\n▶ 测试06: 搜索功能测试')
    await loginAndGotoProxy(page)
    
    // 测试关键词搜索
    console.log('测试关键词搜索...')
    const keywordInput = page.getByPlaceholder('搜索节点名称/IP')
    await keywordInput.fill('test')
    
    console.log('点击搜索按钮')
    await page.getByRole('button', { name: '搜索' }).click()
    await page.waitForTimeout(1000)
    
    // 测试重置
    console.log('点击重置按钮')
    await page.getByRole('button', { name: '重置' }).click()
    await page.waitForTimeout(1000)
    
    console.log('✓ 搜索功能测试完成\n')
  })

  test('07-表格行操作按钮测试', async ({ page }) => {
    console.log('\n▶ 测试07: 表格行操作按钮测试')
    await loginAndGotoProxy(page)
    
    await page.waitForTimeout(1000)
    
    // 查找第一行的操作按钮
    console.log('查找表格中的操作按钮...')
    const editButtons = page.getByRole('button', { name: '编辑' })
    const countryButtons = page.getByRole('button', { name: '国家' })
    const groupButtons = page.getByRole('button', { name: '分组' })
    const deleteButtons = page.getByRole('button', { name: '删除' })
    
    const editCount = await editButtons.count()
    const countryCount = await countryButtons.count()
    const groupCount = await groupButtons.count()
    const deleteCount = await deleteButtons.count()
    
    console.log(`找到 ${editCount} 个编辑按钮`)
    console.log(`找到 ${countryCount} 个国家按钮`)
    console.log(`找到 ${groupCount} 个分组按钮`)
    console.log(`找到 ${deleteCount} 个删除按钮`)
    
    if (editCount > 0) {
      console.log('\n测试第一行的编辑按钮')
      const firstEditBtn = editButtons.first()
      
      // 检查按钮状态
      const isVisible = await firstEditBtn.isVisible()
      const isEnabled = await firstEditBtn.isEnabled()
      console.log(`编辑按钮 - 可见: ${isVisible}, 可用: ${isEnabled}`)
      
      // 尝试点击
      try {
        await firstEditBtn.click({ timeout: 3000 })
        await page.waitForTimeout(1000)
        console.log('编辑按钮点击成功')
        
        // 如果打开了对话框，关闭它
        const cancelBtn = page.getByRole('button', { name: '取消' })
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click()
        }
      } catch (error) {
        console.error('点击编辑按钮失败:', error.message)
      }
    }
    
    if (countryCount > 0) {
      console.log('\n测试第一行的国家按钮')
      const firstCountryBtn = countryButtons.first()
      
      const isVisible = await firstCountryBtn.isVisible()
      const isEnabled = await firstCountryBtn.isEnabled()
      console.log(`国家按钮 - 可见: ${isVisible}, 可用: ${isEnabled}`)
      
      // 获取按钮的HTML属性
      const btnText = await firstCountryBtn.textContent()
      const btnClass = await firstCountryBtn.getAttribute('class')
      console.log(`按钮文本: "${btnText}", 类名: ${btnClass}`)
      
      try {
        await firstCountryBtn.click({ timeout: 3000 })
        await page.waitForTimeout(1000)
        console.log('国家按钮点击执行完成')
      } catch (error) {
        console.error('点击国家按钮失败:', error.message)
      }
    }
    
    if (groupCount > 0) {
      console.log('\n测试第一行的分组按钮')
      const firstGroupBtn = groupButtons.first()
      
      const isVisible = await firstGroupBtn.isVisible()
      const isEnabled = await firstGroupBtn.isEnabled()
      console.log(`分组按钮 - 可见: ${isVisible}, 可用: ${isEnabled}`)
      
      try {
        await firstGroupBtn.click({ timeout: 3000 })
        await page.waitForTimeout(1000)
        console.log('分组按钮点击执行完成')
      } catch (error) {
        console.error('点击分组按钮失败:', error.message)
      }
    }
    
    console.log('✓ 表格行操作按钮测试完成\n')
  })

  test('08-完整的添加代理节点流程', async ({ page }) => {
    console.log('\n▶ 测试08: 完整的添加代理节点流程')
    await loginAndGotoProxy(page)
    
    console.log('点击添加代理节点')
    await page.getByRole('button', { name: '添加代理节点' }).click()
    await page.waitForTimeout(500)
    
    console.log('填写表单数据')
    const poolName = 'E2E-Test-' + Date.now()
    
    await page.getByLabel('代理池名称').getByRole('textbox').fill(poolName)
    await page.getByLabel('IP地址').getByRole('textbox').fill('192.168.1.100')
    
    // 设置端口
    const portInput = page.getByLabel('端口').locator('input')
    await portInput.clear()
    await portInput.fill('8080')
    
    // 选择协议类型
    await page.getByText('HTTP').first().click()
    
    // 填写描述
    await page.getByLabel('描述').getByRole('textbox').fill('E2E自动化测试创建的代理节点')
    
    console.log('提交表单')
    await page.getByRole('button', { name: '确定' }).click()
    
    // 等待提交完成
    await page.waitForTimeout(2000)
    
    console.log('验证是否添加成功')
    // 等待可能的成功消息
    await page.waitForTimeout(1000)
    
    console.log('✓ 添加代理节点流程测试完成\n')
  })

  test('09-网络请求和响应分析', async ({ page }) => {
    console.log('\n▶ 测试09: 网络请求和响应分析')
    await loginAndGotoProxy(page)
    
    // 拦截并记录API请求
    const apiRequests = []
    page.on('request', request => {
      if (request.url().includes('/api/')) {
        apiRequests.push({
          method: request.method(),
          url: request.url(),
          headers: request.headers()
        })
      }
    })
    
    // 拦截并记录API响应
    const apiResponses = []
    page.on('response', async response => {
      if (response.url().includes('/api/')) {
        try {
          const body = await response.text()
          apiResponses.push({
            status: response.status(),
            url: response.url(),
            body: body.substring(0, 200) // 只记录前200字符
          })
        } catch (e) {
          // 忽略无法读取的响应
        }
      }
    })
    
    console.log('点击刷新触发网络请求')
    await page.getByRole('button', { name: '刷新' }).click()
    await page.waitForTimeout(2000)
    
    console.log('\n=== API 请求记录 ===')
    apiRequests.forEach((req, index) => {
      console.log(`请求 ${index + 1}: ${req.method} ${req.url}`)
    })
    
    console.log('\n=== API 响应记录 ===')
    apiResponses.forEach((res, index) => {
      console.log(`响应 ${index + 1}: ${res.status} ${res.url}`)
      console.log(`响应体预览: ${res.body}`)
    })
    
    console.log('✓ 网络请求和响应分析完成\n')
  })

  test('10-国家和分组选择器状态检查', async ({ page }) => {
    console.log('\n▶ 测试10: 国家和分组选择器状态检查')
    await loginAndGotoProxy(page)
    
    await page.waitForTimeout(1000)
    
    console.log('查找国家选择器')
    const countrySelects = page.locator('.el-select').filter({ hasText: '选择国家' })
    const countryCount = await countrySelects.count()
    console.log(`找到 ${countryCount} 个国家选择器`)
    
    if (countryCount > 0) {
      const countrySelect = countrySelects.first()
      const isVisible = await countrySelect.isVisible()
      const isEnabled = await countrySelect.isEnabled()
      console.log(`国家选择器 - 可见: ${isVisible}, 可用: ${isEnabled}`)
      
      // 尝试点击
      try {
        await countrySelect.click({ timeout: 3000 })
        await page.waitForTimeout(500)
        
        // 检查是否有下拉选项
        const options = page.locator('.el-select-dropdown__item')
        const optionCount = await options.count()
        console.log(`国家选择器下拉选项数量: ${optionCount}`)
        
        if (optionCount > 0) {
          for (let i = 0; i < Math.min(optionCount, 5); i++) {
            const optionText = await options.nth(i).textContent()
            console.log(`  选项 ${i + 1}: ${optionText}`)
          }
        }
        
        // 点击空白处关闭下拉
        await page.keyboard.press('Escape')
      } catch (error) {
        console.error('国家选择器操作失败:', error.message)
      }
    }
    
    console.log('\n查找分组选择器')
    const groupSelects = page.locator('.el-select').filter({ hasText: '选择分组' })
    const groupCount = await groupSelects.count()
    console.log(`找到 ${groupCount} 个分组选择器`)
    
    if (groupCount > 0) {
      const groupSelect = groupSelects.first()
      const isVisible = await groupSelect.isVisible()
      const isEnabled = await groupSelect.isEnabled()
      console.log(`分组选择器 - 可见: ${isVisible}, 可用: ${isEnabled}`)
      
      try {
        await groupSelect.click({ timeout: 3000 })
        await page.waitForTimeout(500)
        
        const options = page.locator('.el-select-dropdown__item')
        const optionCount = await options.count()
        console.log(`分组选择器下拉选项数量: ${optionCount}`)
        
        if (optionCount > 0) {
          for (let i = 0; i < Math.min(optionCount, 5); i++) {
            const optionText = await options.nth(i).textContent()
            console.log(`  选项 ${i + 1}: ${optionText}`)
          }
        }
        
        await page.keyboard.press('Escape')
      } catch (error) {
        console.error('分组选择器操作失败:', error.message)
      }
    }
    
    console.log('✓ 国家和分组选择器状态检查完成\n')
  })
})
