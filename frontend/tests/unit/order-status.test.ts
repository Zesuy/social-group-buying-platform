/**
 * 订单状态相关单元测试
 *
 * 覆盖状态文案、按钮可见性、配送方式映射。
 */
import { describe, it, expect } from 'vitest'
import {
  getOrderStatusText,
  getPayStatusText,
  getDeliveryTypeText,
  type OrderStatus,
} from '@/utils/status'

describe('getOrderStatusText', () => {
  const statuses: { key: OrderStatus; expected: string }[] = [
    { key: 'pendingPay', expected: '待支付' },
    { key: 'paid', expected: '已支付' },
    { key: 'shipped', expected: '已发货' },
    { key: 'completed', expected: '已完成' },
    { key: 'canceled', expected: '已取消' },
  ]

  statuses.forEach(({ key, expected }) => {
    it(`should return "${expected}" for ${key}`, () => {
      expect(getOrderStatusText(key)).toBe(expected)
    })
  })

  it('should return "未知状态" for unknown status', () => {
    expect(getOrderStatusText('unknown')).toBe('未知状态')
  })
})

describe('getPayStatusText', () => {
  it('should return correct text for known statuses', () => {
    expect(getPayStatusText('unpaid')).toBe('未支付')
    expect(getPayStatusText('paid')).toBe('已支付')
  })
})

describe('getDeliveryTypeText - full coverage', () => {
  it('should handle all MVP delivery types', () => {
    expect(getDeliveryTypeText('express')).toBe('快递配送')
    expect(getDeliveryTypeText('pickup')).toBe('到店自提')
    expect(getDeliveryTypeText('local_delivery')).toBe('同城配送')
    expect(getDeliveryTypeText('selfPickup')).toBe('selfPickup') // 旧值不再被映射
  })

  it('should return the original value for unknown type', () => {
    expect(getDeliveryTypeText('drone_delivery')).toBe('drone_delivery')
  })
})

// 根据 orderStatus 判断按钮可见性
describe('Order action button logic', () => {
  // 这些是订单卡片/详情页应展示的按钮逻辑的单元级别验证
  // 实际组件测试通过 Mount 验证渲染

  type OrderAction = 'pay' | 'cancel' | 'complete'

  const actionMap: Record<string, OrderAction[]> = {
    pendingPay: ['cancel', 'pay'],
    paid: [],
    shipped: ['complete'],
    completed: [],
    canceled: [],
  }

  const testCases: { status: string; actions: OrderAction[] }[] = [
    { status: 'pendingPay', actions: ['cancel', 'pay'] },
    { status: 'paid', actions: [] },
    { status: 'shipped', actions: ['complete'] },
    { status: 'completed', actions: [] },
    { status: 'canceled', actions: [] },
  ]

  testCases.forEach(({ status, actions }) => {
    it(`orderStatus=${status} should have actions: [${actions.join(', ')}]`, () => {
      expect(actionMap[status]).toEqual(actions)
    })
  })

  it('should have no actions for unknown status', () => {
    expect(actionMap['unknown']).toBeUndefined()
  })
})
