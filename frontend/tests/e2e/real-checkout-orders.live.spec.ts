/**
 * Live E2E：地址 + 下单 + 订单 + 支付 + 取消 + 确认收货
 *
 * 覆盖：
 * - 从团购详情进入 checkout → 创建订单
 * - 订单列表拉取真实订单
 * - 订单详情展示
 * - 待支付订单取消（断言状态变为 canceled）
 * - 去支付（断言状态变为 paid / "等待卖家发货"）
 * - 确认收货（断言状态变为 completed / "已完成"）
 *
 * 前置条件：
 * - 后端 (http://localhost:8080) 已启动
 * - 前端 Vite dev server (http://localhost:5173) 已启动
 *
 * 注意：所有断言都是硬性（无软分支）。每个状态变更操作后都通过页面文案
 * 验证状态已正确流转。
 */

import { test, expect } from '@playwright/test'
import { healthCheck, createApiContext } from './helpers/liveApi'
import {
  prepareFullTestData,
  createOrder as createOrderFixture,
  payOrder,
  shipOrder,
} from './helpers/fixtures'
import { navigateToHash, setLoggedIn } from './helpers/navigation'

// ── 测试级共享数据 ──
let healthOk = false
let buyerToken: string
let leaderToken: string
let groupBuyId: string
let groupBuyItemId: string
let addressId: string
let orderForCancelId: string   // pendingPay → 取消测试
let orderForPayId: string      // pendingPay → 支付测试
let completableOrderId: string // shipped → 确认收货测试

test.describe('Real E2E: Checkout + Orders + Payment', () => {
  test.beforeAll(async () => {
    const apiContext = await createApiContext()
    healthOk = await healthCheck(apiContext)
    if (!healthOk) {
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

    // 1. 准备完整测试数据（买家、团长、店铺、商品、团购、地址）
    const data = await prepareFullTestData(apiContext)
    buyerToken = data.buyer.token
    leaderToken = data.leader.token
    groupBuyId = data.groupBuy.groupBuyId
    groupBuyItemId = data.groupBuy.groupBuyItemId
    addressId = data.address.addressId

    // 2. 创建三个独立订单，分别用于取消/支付/确认收货
    const o1 = await createOrderFixture(apiContext, buyerToken, groupBuyId, addressId, groupBuyItemId)
    orderForCancelId = o1.orderId

    const o2 = await createOrderFixture(apiContext, buyerToken, groupBuyId, addressId, groupBuyItemId)
    orderForPayId = o2.orderId

    // 创建 → 支付 → 发货，用于确认收货测试
    const o3 = await createOrderFixture(apiContext, buyerToken, groupBuyId, addressId, groupBuyItemId)
    await payOrder(apiContext, buyerToken, o3.orderId)
    await shipOrder(apiContext, leaderToken, o3.orderId)
    completableOrderId = o3.orderId

    await apiContext.dispose()
  })

  // ── 下单流程 ──

  test('checkout flow creates order and redirects to order detail', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    // 导航到团购详情
    await navigateToHash(page, `/group-buys/${groupBuyId}`)
    await page.waitForTimeout(2000)

    // 确认页面加载 — 看到"本团商品"标题
    await expect(page.locator('text=本团商品').first()).toBeVisible({ timeout: 10000 })

    // 点击"查看购买"打开商品购买面板
    const selectBtn = page.locator('button:has-text("查看购买")')
    await expect(selectBtn).toBeVisible({ timeout: 5000 })
    await selectBtn.click()
    await page.waitForTimeout(800)

    // 点击"立即购买"
    const buyBtn = page.locator('button:has-text("立即购买")')
    await expect(buyBtn).toBeVisible({ timeout: 5000 })
    await buyBtn.click()
    await page.waitForTimeout(3000)

    // 应跳转到 checkout 页
    await expect(page).toHaveURL(/#\/checkout/, { timeout: 10000 })
    await page.waitForTimeout(2000)

    // 勾选协议
    const checkbox = page.locator('.van-checkbox')
    await expect(checkbox).toBeVisible({ timeout: 5000 })
    await checkbox.click()
    await page.waitForTimeout(500)

    // 提交订单
    const submitBtn = page.locator('button:has-text("提交订单")')
    await expect(submitBtn).toBeVisible({ timeout: 5000 })
    await expect(submitBtn).not.toBeDisabled({ timeout: 3000 })
    await submitBtn.click()
    await page.waitForTimeout(3000)

    // 应跳转到订单详情页
    await expect(page).toHaveURL(/\/orders\/\d+/, { timeout: 10000 })

    // 验证订单详情页加载 — 显示订单编号
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })

    // 新创建的订单应为待支付 → 应显示"去支付"和"取消订单"按钮
    await expect(page.locator('button:has-text("去支付")')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('button:has-text("取消订单")')).toBeVisible({ timeout: 5000 })
  })

  // ── 订单列表 ──

  test('order list shows real orders from backend', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, '/orders')
    await page.waitForTimeout(3000)

    // 订单列表 Tab 栏应加载
    await expect(page.locator('.van-tabs')).toBeVisible({ timeout: 5000 })

    // 因为我们已创建了 3 个订单，至少应看到订单卡片
    const orderCards = page.locator('.order-list-card__body')
    await expect(orderCards.first()).toBeVisible({ timeout: 10000 })

    // 点击第一个订单卡片 → 跳转到订单详情
    await orderCards.first().click()
    await page.waitForTimeout(2000)
    await expect(page).toHaveURL(/\/orders\/\d+/, { timeout: 5000 })
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })
  })

  // ── 取消订单（验证状态 → canceled） ──

  test('cancel pending pay order from order detail', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    await navigateToHash(page, `/orders/${orderForCancelId}`)
    await page.waitForTimeout(3000)

    // 订单详情应显示
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })

    // "取消订单"按钮必须存在（pendingPay 状态）
    const cancelBtn = page.locator('button:has-text("取消订单")')
    await expect(cancelBtn).toBeVisible({ timeout: 5000 })

    // 点击取消 → 确认对话框
    await cancelBtn.click()
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(3000)

    // 断言状态流转：页面应显示"订单已取消"横幅
    const canceledBanner = page.locator('.order-cancel-banner')
    await expect(canceledBanner).toBeVisible({ timeout: 5000 })
    await expect(canceledBanner.getByText('订单已取消')).toBeVisible({ timeout: 5000 })

    // "取消订单"和"去支付"按钮应不再可见
    await expect(page.locator('button:has-text("取消订单")')).not.toBeVisible({ timeout: 3000 })
    await expect(page.locator('button:has-text("去支付")')).not.toBeVisible({ timeout: 3000 })
  })

  // ── 去支付（验证状态 → paid → "等待卖家发货"） ──

  test('simulate pay on pending pay order', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    // 使用专用的待支付订单（与取消测试使用不同订单）
    await navigateToHash(page, `/orders/${orderForPayId}`)
    await page.waitForTimeout(3000)

    // 订单详情应显示
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })

    // "去支付"按钮必须存在（pendingPay 状态）
    const payBtn = page.locator('button:has-text("去支付")')
    await expect(payBtn).toBeVisible({ timeout: 5000 })

    // 点击支付 → 确认对话框
    await payBtn.click()
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(3000)

    // 断言状态流转：支付后应显示"等待卖家发货"文案
    await expect(page.locator('button:has-text("等待卖家发货")')).toBeVisible({ timeout: 5000 })

    // "去支付"按钮应消失
    await expect(page.locator('button:has-text("去支付")')).not.toBeVisible({ timeout: 3000 })
  })

  // ── 确认收货（验证状态 → completed → "已完成"） ──

  test('complete shipped order from order detail', async ({ page }) => {
    await setLoggedIn(page, buyerToken)

    // 导航到已发货订单
    await navigateToHash(page, `/orders/${completableOrderId}`)
    await page.waitForTimeout(3000)

    // 订单详情应显示
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })

    // "确认收货"按钮必须存在（shipped 状态）
    const completeBtn = page.locator('button:has-text("确认收货")')
    await expect(completeBtn).toBeVisible({ timeout: 5000 })

    // 点击确认收货 → 确认对话框
    await completeBtn.click()
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(3000)

    // 断言状态流转：确认后应显示"已完成"
    await expect(page.locator('button:has-text("已完成")')).toBeVisible({ timeout: 5000 })

    // "确认收货"按钮应消失
    await expect(page.locator('button:has-text("确认收货")')).not.toBeVisible({ timeout: 3000 })
  })
})
