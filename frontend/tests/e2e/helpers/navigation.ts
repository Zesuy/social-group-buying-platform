/**
 * Live E2E 导航辅助
 *
 * 在 Hash 路由的 SPA 中跳转到指定路由的通用函数。
 * 不使用 `.van-tabbar` 作为加载完成的标志，因为部分页面没有 tab bar。
 */

import type { Page } from '@playwright/test'

/**
 * 在 Hash 路由的 SPA 中跳转到指定路由
 *
 * 先 goto 到根路径，等页面挂载完成后再通过 evaluate 修改 hash。
 */
export async function navigateToHash(page: Page, hashPath: string) {
  await page.goto('/')
  // 等待页面的 body 渲染完成
  await page.waitForSelector('body', { timeout: 10000 })
  await page.waitForTimeout(500) // 给 SPA 一点初始化时间
  await page.evaluate((path) => {
    window.location.hash = `#${path}`
  }, hashPath)
  // 等待 hash 变更生效
  await page.waitForTimeout(500)
}

/**
 * 设置登录态
 *
 * 将 token 存入 localStorage 并刷新页面以触发 restoreSession。
 */
export async function setLoggedIn(page: Page, token: string) {
  await page.goto('/')
  await page.waitForSelector('body', { timeout: 10000 })
  await page.waitForTimeout(500)
  await page.evaluate((t) => localStorage.setItem('accessToken', t), token)
  await page.reload()
  await page.waitForSelector('body', { timeout: 10000 })
  await page.waitForTimeout(800)
}
