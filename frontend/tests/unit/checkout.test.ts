import { describe, it, expect } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { useCheckoutStore } from '@/stores/checkout'
import OrderAmountBreakdown from '@/components/OrderAmountBreakdown.vue'
import CheckoutSection from '@/components/CheckoutSection.vue'

describe('checkoutStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('starts with null groupBuyId', () => {
    const store = useCheckoutStore()
    expect(store.groupBuyId).toBeNull()
    expect(store.groupBuyItemId).toBeNull()
    expect(store.quantity).toBe(1)
    expect(store.selectedAddressId).toBeNull()
    expect(store.userCouponId).toBeNull()
    expect(store.shareToken).toBeNull()
    expect(store.remark).toBe('')
    expect(store.snapshot).toBeNull()
  })

  it('setCheckoutContext fills fields and clears address', () => {
    const store = useCheckoutStore()
    store.setCheckoutContext({
      groupBuyId: 100,
      groupBuyItemId: 1001,
      quantity: 2,
      title: '周末阳山水蜜桃社区团',
      coverImageUrl: null,
      displayName: '阳山水蜜桃 5 斤装',
      unitPriceAmount: 2990,
      shareToken: 'share-token-1',
    })
    expect(store.groupBuyId).toBe(100)
    expect(store.groupBuyItemId).toBe(1001)
    expect(store.quantity).toBe(2)
    expect(store.selectedAddressId).toBeNull() // 从详情进入时清空地址
    expect(store.userCouponId).toBeNull()
    expect(store.shareToken).toBe('share-token-1')
    expect(store.snapshot?.title).toBe('周末阳山水蜜桃社区团')
    expect(store.snapshot?.displayName).toBe('阳山水蜜桃 5 斤装')
  })

  it('setCartCheckoutContext can keep share token', () => {
    const store = useCheckoutStore()
    store.setCartCheckoutContext({
      groupBuyId: '100',
      cartItemIds: ['cart-1', 'cart-2'],
      shareToken: 'share-token-2',
    })
    expect(store.mode).toBe('cart')
    expect(store.groupBuyId).toBe('100')
    expect(store.cartItemIds).toEqual(['cart-1', 'cart-2'])
    expect(store.shareToken).toBe('share-token-2')
  })

  it('setAddress updates selected address', () => {
    const store = useCheckoutStore()
    store.setAddress(300)
    expect(store.selectedAddressId).toBe(300)
  })

  it('setQuantity updates quantity', () => {
    const store = useCheckoutStore()
    store.setQuantity(3)
    expect(store.quantity).toBe(3)
  })

  it('setRemark updates remark', () => {
    const store = useCheckoutStore()
    store.setRemark('请尽快发货')
    expect(store.remark).toBe('请尽快发货')
  })

  it('setCoupon updates selected coupon and checkout context clears it', () => {
    const store = useCheckoutStore()
    store.setCoupon('coupon-1')
    expect(store.userCouponId).toBe('coupon-1')

    store.setCheckoutContext({
      groupBuyId: 100, groupBuyItemId: 1001, quantity: 2,
    })
    expect(store.userCouponId).toBeNull()
  })

  it('clearCheckout resets all state', () => {
    const store = useCheckoutStore()
    store.setCheckoutContext({
      groupBuyId: 100, groupBuyItemId: 1001, quantity: 2,
    })
    store.setAddress(300)
    store.setCoupon('coupon-1')
    store.setRemark('备注')
    store.clearCheckout()
    expect(store.groupBuyId).toBeNull()
    expect(store.groupBuyItemId).toBeNull()
    expect(store.quantity).toBe(1)
    expect(store.selectedAddressId).toBeNull()
    expect(store.userCouponId).toBeNull()
    expect(store.shareToken).toBeNull()
    expect(store.remark).toBe('')
    expect(store.snapshot).toBeNull()
  })
})

describe('OrderAmountBreakdown', () => {
  it('renders total, discount and pay amounts', () => {
    const wrapper = mount(OrderAmountBreakdown, {
      props: { totalAmount: 5980, discountAmount: 500, payAmount: 5480 },
    })
    expect(wrapper.text()).toContain('商品金额')
    expect(wrapper.text()).toContain('¥59.80')
    expect(wrapper.text()).toContain('优惠')
    expect(wrapper.text()).toContain('应付')
    expect(wrapper.text()).toContain('¥54.80')
  })

  it('does not show discount row when no discount', () => {
    const wrapper = mount(OrderAmountBreakdown, {
      props: { totalAmount: 2990, discountAmount: 0, payAmount: 2990 },
    })
    expect(wrapper.text()).not.toContain('优惠')
  })

  it('handles zero amounts', () => {
    const wrapper = mount(OrderAmountBreakdown, {
      props: { totalAmount: 0, discountAmount: 0, payAmount: 0 },
    })
    expect(wrapper.text()).toContain('¥0.00')
  })
})

describe('CheckoutSection', () => {
  it('emits click when clickable', async () => {
    const wrapper = mount(CheckoutSection, {
      props: { title: '收货地址', clickable: true },
      slots: { default: '地址内容' },
    })
    await wrapper.find('.app-card').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
