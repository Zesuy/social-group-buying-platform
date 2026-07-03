/**
 * Checkout 状态管理
 *
 * 保存下单上下文：支持“立即购买”和“购物车结算”。
 * 页面刷新后需要从团购详情重新进入。
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCheckoutStore = defineStore('checkout', () => {
  // ── 下单上下文 ──
  const mode = ref<'direct' | 'cart' | null>(null)
  const groupBuyId = ref<string | null>(null)
  const groupBuyItemId = ref<string | null>(null)
  const cartItemIds = ref<string[]>([])
  const quantity = ref(1)
  const selectedAddressId = ref<string | null>(null)
  const userCouponId = ref<string | null>(null)
  const remark = ref('')

  // 商品快照（用于 checkout 页快速展示）
  const snapshot = ref<{
    title: string
    coverImageUrl: string | null
    displayName: string
    unitPriceAmount: number
  } | null>(null)

  /**
   * 从团购详情进入 checkout 时调用
   */
  function setCheckoutContext(params: {
    groupBuyId: string
    groupBuyItemId: string
    quantity: number
    title?: string
    coverImageUrl?: string | null
    displayName?: string
    unitPriceAmount?: number
  }): void {
    mode.value = 'direct'
    groupBuyId.value = params.groupBuyId
    groupBuyItemId.value = params.groupBuyItemId
    cartItemIds.value = []
    quantity.value = params.quantity
    selectedAddressId.value = null
    userCouponId.value = null
    remark.value = ''
    snapshot.value = {
      title: params.title ?? '',
      coverImageUrl: params.coverImageUrl ?? null,
      displayName: params.displayName ?? '',
      unitPriceAmount: params.unitPriceAmount ?? 0,
    }
  }

  function setCartCheckoutContext(params: {
    groupBuyId: string
    cartItemIds: string[]
  }): void {
    mode.value = 'cart'
    groupBuyId.value = params.groupBuyId
    groupBuyItemId.value = null
    cartItemIds.value = params.cartItemIds
    quantity.value = 1
    selectedAddressId.value = null
    userCouponId.value = null
    remark.value = ''
    snapshot.value = null
  }

  function clearCheckout(): void {
    mode.value = null
    groupBuyId.value = null
    groupBuyItemId.value = null
    cartItemIds.value = []
    quantity.value = 1
    selectedAddressId.value = null
    userCouponId.value = null
    remark.value = ''
    snapshot.value = null
  }

  function setAddress(addressId: string): void {
    selectedAddressId.value = addressId
  }

  function setQuantity(newQuantity: number): void {
    quantity.value = newQuantity
  }

  function setCoupon(couponId: string | null): void {
    userCouponId.value = couponId
  }

  function setRemark(newRemark: string): void {
    remark.value = newRemark
  }

  return {
    groupBuyId,
    groupBuyItemId,
    cartItemIds,
    mode,
    quantity,
    selectedAddressId,
    userCouponId,
    remark,
    snapshot,
    setCheckoutContext,
    setCartCheckoutContext,
    clearCheckout,
    setAddress,
    setQuantity,
    setCoupon,
    setRemark,
  }
})
