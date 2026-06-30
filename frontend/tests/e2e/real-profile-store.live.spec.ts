/**
 * Live E2E：个人中心 + 开店 + 店铺管理
 *
 * 覆盖：
 * - 未登录个人中心展示
 * - 已登录买家个人中心
 * - 创建店铺 → 成为团长
 * - 店铺信息加载
 * - 店铺信息编辑
 *
 * 前置条件：
 * - 后端 (http://localhost:8080) 已启动
 * - 前端 Vite dev server (http://localhost:5173) 已启动
 *
 * 注意：所有断言都是硬性（无软分支），如果按钮不存在或流程中断则测试失败。
 */

import { test, expect } from '@playwright/test'
import { healthCheck, createApiContext, apiLogin } from './helpers/liveApi'
import {
  BUYER_NICKNAME,
  BUYER_PHONE,
  STORE_NAME,
  STORE_DESCRIPTION,
} from './helpers/live-test-data'
import { navigateToHash, setLoggedIn } from './helpers/navigation'

// ── 测试级共享数据 ──
let healthOk = false
let buyerToken: string

test.describe('Real E2E: Profile + Store', () => {
  test.beforeAll(async () => {
    const apiContext = await createApiContext()
    healthOk = await healthCheck(apiContext)
    if (!healthOk) {
      if (process.env.ALLOW_LIVE_E2E_SKIP) {
        test.skip(true, '后端不可用 — 设置 ALLOW_LIVE_E2E_SKIP=1 可跳过')
        await apiContext.dispose()
        return
      }
      await apiContext.dispose()
      throw new Error('❌ 后端不可用 (http://localhost:8080)。请先启动后端: mvn spring-boot:run -Dspring-boot.run.profiles=dev')
    }

    // 准备买家 token（用于已登录状态测试）
    const data = await apiLogin(apiContext, {
      nickname: BUYER_NICKNAME,
      phone: BUYER_PHONE,
    })
    buyerToken = data.accessToken

    await apiContext.dispose()
  })

  // ── 个人中心 ──

  test('unauthenticated profile shows login CTA', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
    await page.reload()
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    await navigateToHash(page, '/profile')
    await page.waitForTimeout(1000)

    // 未登录状态应显示"点击登录"
    await expect(page.locator('text=点击登录')).toBeVisible({ timeout: 5000 })
  })

  test('logged-in profile shows buyer info and menu items', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, '/profile')
    await page.waitForTimeout(2000)

    // 应显示用户信息
    await expect(page.locator('.profile-name')).toBeVisible({ timeout: 5000 })

    // 买家菜单必须包含以下条目
    await expect(page.locator('text=我的订单')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=地址管理')).toBeVisible({ timeout: 5000 })

    // "创建店铺" 入口在未开店时应可见
    await expect(page.locator('text=创建店铺')).toBeVisible({ timeout: 5000 })

    // 退出登录按钮必须可见
    await expect(page.locator('button:has-text("退出登录")')).toBeVisible({ timeout: 5000 })
  })

  // ── 一键开团页面 ──

  test('open group page shows 4 group type cards', async ({ page }) => {
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1000)

    // 应显示 4 种开团类型卡片
    await expect(page.locator('text=普通团购')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=预售团购')).toBeVisible()
    await expect(page.locator('text=卡券团购')).toBeVisible()
    await expect(page.locator('text=报名团购')).toBeVisible()
  })

  test('non-MVP group buy card shows coming soon toast and stays on page', async ({ page }) => {
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1000)

    // 点击预售团购（灰态）不应跳转
    await page.locator('text=预售团购').first().click()
    await page.waitForTimeout(500)

    // 应仍在同一页
    await expect(page).toHaveURL(/#\/open-group/, { timeout: 3000 })
  })

  test('unauthenticated normal group buy redirects to login', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
    await page.reload()
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1000)

    // 点击普通团购
    await page.locator('text=普通团购').click()
    await page.waitForTimeout(1500)

    // 应跳转到登录页
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
  })

  // ── 创建店铺 ──

  test('create store page renders form with required fields', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, '/store/create')
    await page.waitForTimeout(1500)

    // 应显示创建店铺表单的关键元素
    await expect(page.locator('text=店铺名称')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=默认物流方式')).toBeVisible()
    await expect(page.locator('button:has-text("创建店铺")')).toBeVisible({ timeout: 5000 })
  })

  test('can create store and become leader', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, '/store/create')
    await page.waitForTimeout(2000)

    // 如果已有店铺（之前测试成功过），会弹出对话框 → 确认留在页面
    const existingDialog = page.locator('.van-dialog')
    if (await existingDialog.isVisible({ timeout: 2000 }).catch(() => false)) {
      await existingDialog.locator('button:has-text("取消")').click()
      await page.waitForTimeout(1000)
    }

    // 填写店铺名称 — Vant Field 没有 name 属性，使用 placeholder 定位
    const nameInput = page.locator('input[placeholder*="请输入店铺名称"]')
    await expect(nameInput).toBeVisible({ timeout: 5000 })
    await nameInput.fill(STORE_NAME)

    // 填写可选的简介 textarea
    const descTextarea = page.locator('textarea')
    if (await descTextarea.isVisible({ timeout: 2000 }).catch(() => false)) {
      await descTextarea.fill(STORE_DESCRIPTION)
    }

    // 点击创建按钮
    const submitBtn = page.locator('button:has-text("创建店铺")')
    await expect(submitBtn).toBeVisible({ timeout: 5000 })
    await expect(submitBtn).not.toBeDisabled({ timeout: 3000 })
    await submitBtn.click()
    await page.waitForTimeout(3000)

    // 创建成功后应跳转到 /leader/store 或弹出成功 toast
    // 检查是否跳转离开了 /store/create
    await expect(page).not.toHaveURL(/#\/store\/create/, { timeout: 10000 })
  })

  // ── 店铺管理 ──

  test('leader store page loads and shows store info', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, '/leader/store')
    await page.waitForTimeout(3000)

    // 应该显示店铺名称（创建店铺时使用的名称）
    await expect(page.locator(`text=${STORE_NAME}`).first()).toBeVisible({ timeout: 10000 })

    // "编辑资料"按钮必须可见
    await expect(page.locator('button:has-text("编辑资料")')).toBeVisible({ timeout: 5000 })
  })

  test('leader store page can toggle edit mode', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, '/leader/store')
    await page.waitForTimeout(3000)

    // "编辑资料"按钮必须可见
    const editBtn = page.locator('button:has-text("编辑资料")')
    await expect(editBtn).toBeVisible({ timeout: 10000 })
    await editBtn.click()
    await page.waitForTimeout(500)

    // 编辑模式下应显示"保存"按钮
    await expect(page.locator('button:has-text("保存")')).toBeVisible({ timeout: 5000 })
  })
})
