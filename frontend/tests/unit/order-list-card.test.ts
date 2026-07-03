import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderListCard from '@/components/OrderListCard.vue'
import type { OrderData } from '@/types'

const mockOrder: OrderData = {
  id: '1',
  storeId: '1',
  orderNo: 'ORD-001',
  orderStatus: 'pendingPay',
  payStatus: 'unpaid',
  payAmount: 2990,
  totalAmount: 2990,
  discountAmount: 0,
  receiverName: '陈小满',
  receiverPhone: '13800138000',
  fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
  items: [
    {
      id: 'item1',
      productName: '阳山水蜜桃 5 斤装',
      skuName: '标准',
      coverImageUrl: '',
      unitPriceAmount: 2990,
      totalAmount: 2990,
      quantity: 1,
      payAmount: 2990,
    },
  ],
  createdAt: '2024-01-15T10:00:00',
  paidAt: null,
  shippedAt: null,
  completedAt: null,
}

describe('OrderListCard', () => {
  it('renders order info', () => {
    const wrapper = mount(OrderListCard, {
      props: { order: mockOrder },
    })
    expect(wrapper.text()).toContain('ORD-001')
    expect(wrapper.text()).toContain('阳山水蜜桃 5 斤装')
  })

  it('renders in buyer mode', () => {
    const wrapper = mount(OrderListCard, {
      props: { order: mockOrder, mode: 'buyer' },
    })
    expect(wrapper.text()).toContain('待支付')
  })

  it('renders status pill', () => {
    const wrapper = mount(OrderListCard, {
      props: { order: mockOrder },
    })
    expect(wrapper.find('.app-status-pill').exists()).toBe(true)
  })

  it('renders action buttons', () => {
    const wrapper = mount(OrderListCard, {
      props: {
        order: mockOrder,
        actionButtons: [
          { text: '去支付', variant: 'primary' as const, onClick: () => {} },
        ],
      },
    })
    expect(wrapper.text()).toContain('去支付')
  })

  it('renders discount amount when coupon discount exists', () => {
    const wrapper = mount(OrderListCard, {
      props: {
        order: {
          ...mockOrder,
          totalAmount: 3990,
          discountAmount: 1000,
          payAmount: 2990,
          couponName: '新人满减券',
        },
      },
    })
    expect(wrapper.text()).toContain('已优惠')
    expect(wrapper.text()).toContain('¥10.00')
  })
})
