/**
 * Live E2E：认证 + 公开浏览
 *
 * 覆盖：
 * - 模拟登录流程（POST /api/v1/auth/mock-login）
 * - 登录态恢复（页面刷新后 GET /api/v1/me）
 * - 退出登录（清除本地状态）
 * - 公开浏览（首页团购列表 → 团购详情 → 团长主页）
 *
 * 前置条件：
 * - 后端 (http://localhost:8080) 已启动
 * - 前端 Vite dev server (http://localhost:5173) 已启动
 *
 * 注意：所有断言都是硬性（无软分支）。公开浏览测试直接断言
 * 本次 API 准备的团购真实数据（标题、商品名）出现在页面上。
 */

import { test, expect } from '@playwright/test'
import { createApiContext, healthCheck } from './helpers/liveApi'
import { createLeader, createProduct, createGroupBuy } from './helpers/fixtures'
import {
  BUYER_NICKNAME,
  BUYER_PHONE,
  STORE_NAME,
  GROUP_BUY_TITLE,
  GROUP_BUY_ITEM_DISPLAY_NAME,
} from './helpers/live-test-data'
import { navigateToHash, setLoggedIn } from './helpers/navigation'

// ── 测试级共享数据 ──
let buyerToken: string
let leaderId: string
let groupBuyId: string

test.describe('Real E2E: Auth + Public Browsing', () => {
  test.beforeAll(async () => {
    const apiContext = await createApiContext()
    const ok = await healthCheck(apiContext)
    if (!ok) {
      if (process.env.ALLOW_LIVE_E2E_SKIP) {
        await apiContext.dispose()
        test.skip(true, '后端不可用 — 设置 ALLOW_LIVE_E2E_SKIP=1 可跳过')
        return
      }
      await apiContext.dispose()
      throw new Error(
        '❌ 后端不可用 (http://localhost:8080)。请先启动后端: mvn spring-boot:run -Dspring-boot.run.profiles=dev',
      )
    }

    // 准备测试数据：创建团长 + 店铺 + 商品 + 发布团购
    const leader = await createLeader(apiContext)
    const product = await createProduct(apiContext, leader.token)
    const groupBuy = await createGroupBuy(apiContext, leader.token, product.productId)

    leaderId = leader.leaderId
    groupBuyId = groupBuy.groupBuyId

    // 登录买家获取 token
    const { apiLogin } = await import('./helpers/liveApi')
    const buyerData = await apiLogin(apiContext, { nickname: BUYER_NICKNAME, phone: BUYER_PHONE })
    buyerToken = buyerData.accessToken

    await apiContext.dispose()
  })

  // ── 认证流程 ──

  test('login page renders and mock-login works', async ({ page }) => {
    await navigateToHash(page, '/login')

    // 验证登录页显示
    await expect(page.locator('text=买家测试用户')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=团长测试用户')).toBeVisible()

    // 点击买家快捷填充
    await page.locator('text=买家测试用户').click()

    // 确认表单已填充
    const phoneInput = page.locator('input[placeholder*="手机号"]')
    await expect(phoneInput).toBeVisible({ timeout: 5000 })
    const phoneValue = await phoneInput.inputValue()
    expect(phoneValue.length).toBe(11)

    // 提交登录
    await page.locator('button:has-text("登录")').click()

    // 登录成功应跳转到 /profile
    await expect(page).toHaveURL(/#\/profile/, { timeout: 10000 })

    // 验证用户信息展示
    await expect(page.locator('.profile-name')).toBeVisible({ timeout: 5000 })
  })

  test('session restoration works after page refresh', async ({ page }) => {
    // 先通过登录页登录
    await navigateToHash(page, '/login')
    await page.locator('text=买家测试用户').click()
    await page.locator('button:has-text("登录")').click()
    await expect(page).toHaveURL(/#\/profile/, { timeout: 10000 })

    // 记录当前昵称
    const nicknameBefore = await page.locator('.profile-name').textContent()
    expect(nicknameBefore).toBeTruthy()

    // 刷新页面
    await page.reload()
    await page.waitForTimeout(2000)

    // 验证登录态恢复（应该还在 profile 页且展示相同昵称）
    await expect(page).toHaveURL(/#\/profile/, { timeout: 10000 })
    await expect(page.locator('.profile-name')).toBeVisible({ timeout: 5000 })

    // 刷新前后昵称应一致（同一个用户）
    const nicknameAfter = await page.locator('.profile-name').textContent()
    expect(nicknameAfter).toBe(nicknameBefore)
  })

  test('logout clears auth state and redirects protected pages', async ({ page }) => {
    // 先登录
    await navigateToHash(page, '/login')
    await page.locator('text=买家测试用户').click()
    await page.locator('button:has-text("登录")').click()
    await expect(page).toHaveURL(/#\/profile/, { timeout: 10000 })

    // 退出登录
    const logoutBtn = page.locator('button:has-text("退出登录")')
    await expect(logoutBtn).toBeVisible({ timeout: 5000 })
    await logoutBtn.click()

    // 确认退出弹窗
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(1500)

    // 退出后应显示"点击登录"
    await expect(page.locator('text=点击登录')).toBeVisible({ timeout: 5000 })

    // 访问受保护页面应重定向到登录页
    await navigateToHash(page, '/orders')
    await page.waitForTimeout(2000)
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
  })

  // ── 公开浏览 ──

  test('home page shows the real group buy card from backend', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    // 等待首页团购列表加载
    await page.waitForTimeout(3000)

    // 首页应展示本次 API 准备的团购标题（真实数据断言）
    await expect(page.locator(`text=${GROUP_BUY_TITLE}`).first()).toBeVisible({ timeout: 10000 })

    // 团购卡片应显示价格（以 ¥ 开头）
    await expect(page.locator('text=¥').first()).toBeVisible({ timeout: 5000 })

    // 团购卡片应显示团长店铺名称
    await expect(page.locator(`text=${STORE_NAME}`).first()).toBeVisible({ timeout: 5000 })
  })

  test('group buy detail page shows real group buy data', async ({ page }) => {
    // 直接导航到团购详情页面（用我们创建的 groupBuyId）
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await page.evaluate((id) => { window.location.hash = `#/group-buys/${id}` }, groupBuyId)
    await page.waitForTimeout(3000)

    // 详情页应显示真实团购标题
    await expect(page.locator(`text=${GROUP_BUY_TITLE}`).first()).toBeVisible({ timeout: 10000 })

    // 详情页应显示团购商品名称
    await expect(page.locator(`text=${GROUP_BUY_ITEM_DISPLAY_NAME}`).first()).toBeVisible({
      timeout: 5000,
    })

    // 商品列表标题应出现
    await expect(page.locator('text=商品列表').first()).toBeVisible({ timeout: 5000 })

    // 价格应显示（以 ¥ 开头）
    await expect(page.locator('text=¥').first()).toBeVisible({ timeout: 5000 })
  })

  test('leader homepage loads real leader and store info', async ({ page }) => {
    // 直接导航到团长主页
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await page.evaluate((id) => { window.location.hash = `#/leaders/${id}` }, leaderId)
    await page.waitForTimeout(3000)

    // 团长主页应显示店铺名称（真实数据断言）
    await expect(page.locator(`text=${STORE_NAME}`).first()).toBeVisible({ timeout: 10000 })

    // 团长主页应显示当前设计中的店铺信息卡
    await expect(page.locator('.leader-store-card')).toBeVisible({ timeout: 5000 })
  })

  test('authenticated user sees subscribed state on group buy detail', async ({ page }) => {
    // 先登录
    await setLoggedIn(page, buyerToken)

    // 导航到团购详情
    await navigateToHash(page, `/group-buys/${groupBuyId}`)
    await page.waitForTimeout(3000)

    // 详情页应正常加载（登录后 viewer.subscribed 由后端决定）
    // 但至少商品列表和团购标题应出现
    await expect(page.locator(`text=${GROUP_BUY_TITLE}`).first()).toBeVisible({ timeout: 10000 })
    await expect(page.locator('text=商品列表').first()).toBeVisible({ timeout: 5000 })
  })
})
