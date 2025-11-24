import { test, expect } from '@playwright/test'

const BASE_URL = 'http://103.246.244.229:3000'

// 登录并进入代理资源池页面
async function loginAndGotoProxy(page) {
  await page.goto(BASE_URL + '/login')
  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('admin123')
  await page.getByRole('button', { name: '登 录' }).click()
  await page.waitForURL(/dashboard/)
  await page.goto(BASE_URL + '/proxy')
  await expect(page.getByText('代理资源池')).toBeVisible()
}

// 添加代理池并同时添加首个代理节点
test('代理资源池：新增/编辑/列表/删除校验', async ({ page }) => {
  await loginAndGotoProxy(page)

  // 点击 添加代理池
  await page.getByRole('button', { name: '添加代理池' }).click()
  // 填写代理池名称与类型
  const poolName = 'E2E-Pool-' + Date.now()
  await page.getByLabel('代理池名称').getByRole('textbox').fill(poolName)
  await page.getByLabel('代理类型').getByRole('radio', { name: 'HTTP' }).check()
  await page.getByLabel('描述').getByRole('textbox').fill('E2E 自动化创建的代理池')

  // 开启同时添加首个代理节点
  await page.getByLabel('添加首个节点').getByRole('switch').click()
  await page.getByLabel('IP地址').getByRole('textbox').fill('1.1.1.1')
  await page.getByLabel('端口').getByRole('spinbutton').fill('8080')
  await page.getByLabel('协议类型').getByRole('radio', { name: 'HTTP' }).check()
  await page.getByLabel('是否认证').getByRole('switch').click()
  await page.getByLabel('用户名').getByRole('textbox').fill('user')
  await page.getByLabel('密码').getByRole('textbox').fill('pass')
  await page.getByLabel('地区').getByRole('textbox').fill('TestRegion')
  await page.getByLabel('运营商').getByRole('textbox').fill('TestISP')

  // 提交
  await page.getByRole('button', { name: '确定' }).click()
  // 验证消息提示（添加成功）
  await expect(page.getByText('添加成功')).toBeVisible()

  // 列表应该出现新建的代理池
  await expect(page.getByRole('cell', { name: poolName })).toBeVisible()

  // 展开节点列表（如果存在展开按钮）
  await page.getByRole('button', { name: '添加代理节点' }).first().isVisible()

  // 编辑代理池
  await page.getByRole('button', { name: '编辑' }).first().click()
  await page.getByLabel('描述').getByRole('textbox').fill('E2E 编辑后的描述')
  await page.getByRole('button', { name: '确定' }).click()
  await expect(page.getByText('更新成功')).toBeVisible()

  // 删除代理池（应因存在节点而被后端阻止）
  await page.getByRole('button', { name: '删除' }).first().click()
  await page.getByRole('button', { name: '确定' }).click()
  await expect(page.getByText('删除失败').or(page.getByText('该代理池下还有'))).toBeVisible()
})
