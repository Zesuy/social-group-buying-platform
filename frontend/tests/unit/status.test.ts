import { describe, it, expect } from 'vitest'
import {
  getStoreStatusText,
  getGroupBuyStatusText,
  getPayStatusText,
  getOrderStatusText,
  getDeliveryTypeText,
  getOrderStatusColor,
  getOrderHintText,
  getOrderDotClass,
  getGroupBuyStatusColor,
  getPayStatusColor,
} from '@/utils/status'

describe('getStoreStatusText', () => {
  it('should return correct text for known statuses', () => {
    expect(getStoreStatusText('active')).toBe('营业中')
    expect(getStoreStatusText('suspended')).toBe('暂停营业')
    expect(getStoreStatusText('closed')).toBe('已关闭')
  })

  it('should return "未知状态" for unknown status', () => {
    expect(getStoreStatusText('unknown')).toBe('未知状态')
  })
})

describe('getGroupBuyStatusText', () => {
  it('should return correct text for known statuses', () => {
    expect(getGroupBuyStatusText('published')).toBe('进行中')
    expect(getGroupBuyStatusText('ended')).toBe('已结束')
  })

  it('should return "未知状态" for unknown status', () => {
    expect(getGroupBuyStatusText('unknown')).toBe('未知状态')
  })
})

describe('getPayStatusText', () => {
  it('should return correct text for known statuses', () => {
    expect(getPayStatusText('unpaid')).toBe('未支付')
    expect(getPayStatusText('paid')).toBe('已支付')
  })

  it('should return "未知状态" for unknown status', () => {
    expect(getPayStatusText('unknown')).toBe('未知状态')
  })
})

describe('getOrderStatusText', () => {
  it('should return correct text for known order statuses', () => {
    expect(getOrderStatusText('pendingPay')).toBe('待支付')
    expect(getOrderStatusText('paid')).toBe('已支付')
    expect(getOrderStatusText('shipped')).toBe('已发货')
    expect(getOrderStatusText('completed')).toBe('已完成')
    expect(getOrderStatusText('canceled')).toBe('已取消')
  })

  it('should return "未知状态" for unknown status', () => {
    expect(getOrderStatusText('unknown')).toBe('未知状态')
  })
})

describe('getDeliveryTypeText', () => {
  it('should return correct text for express', () => {
    expect(getDeliveryTypeText('express')).toBe('快递配送')
  })

  it('should return correct text for pickup', () => {
    expect(getDeliveryTypeText('pickup')).toBe('到店自提')
  })

  it('should return correct text for local_delivery', () => {
    expect(getDeliveryTypeText('local_delivery')).toBe('同城配送')
  })

  it('should return the original value for unknown type', () => {
    expect(getDeliveryTypeText('unknown')).toBe('unknown')
  })
})

describe('getOrderStatusColor', () => {
  it('should return correct color for known statuses', () => {
    expect(getOrderStatusColor('pendingPay')).toBe('var(--color-price)')
    expect(getOrderStatusColor('paid')).toBe('var(--color-primary)')
    expect(getOrderStatusColor('completed')).toBe('var(--color-text-hint)')
  })
})

describe('getOrderHintText', () => {
  it('should return correct hints for order statuses', () => {
    expect(getOrderHintText('pendingPay')).toContain('支付')
    expect(getOrderHintText('paid')).toContain('等待团长发货')
    expect(getOrderHintText('shipped')).toContain('确认收货')
    expect(getOrderHintText('completed')).toContain('已完成')
    expect(getOrderHintText('canceled')).toContain('已取消')
  })
})

describe('getOrderDotClass', () => {
  it('should return correct dot class', () => {
    expect(getOrderDotClass('pendingPay')).toBe('dot--orange')
    expect(getOrderDotClass('paid')).toBe('dot--green')
    expect(getOrderDotClass('completed')).toBe('dot--gray')
  })
})

describe('getGroupBuyStatusColor', () => {
  it('should return correct color', () => {
    expect(getGroupBuyStatusColor('published')).toBe('var(--color-primary)')
    expect(getGroupBuyStatusColor('ended')).toBe('var(--color-text-hint)')
  })
})

describe('getPayStatusColor', () => {
  it('should return correct color', () => {
    expect(getPayStatusColor('unpaid')).toBe('var(--color-price)')
    expect(getPayStatusColor('paid')).toBe('var(--color-primary)')
  })
})
