import { test, expect } from '@playwright/test'

test.describe('App smoke test', () => {
  test('should load the app without errors', async ({ page }) => {
    // 收集控制台错误
    const consoleErrors: string[] = []
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text())
      }
    })

    await page.goto('/')

    // 验证页面内容非空
    const body = page.locator('body')
    await expect(body).not.toBeEmpty()

    // 验证页面有标题
    const title = await page.title()
    expect(title).toBeTruthy()

    // 验证无严重错误（允许 404 等非关键错误）
    const criticalErrors = consoleErrors.filter(
      (e) =>
        !e.includes('favicon') &&
        !e.includes('404') &&
        !e.includes('Failed to load resource'),
    )
    expect(criticalErrors).toEqual([])
  })

  test('should navigate to index route', async ({ page }) => {
    await page.goto('/')

    // 验证页面存在 #app 容器
    const app = page.locator('#app')
    await expect(app).toBeVisible()
  })
})
