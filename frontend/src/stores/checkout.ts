/**
 * Checkout 状态管理
 *
 * 保存"立即购买"上下文，不是购物车。
 * 页面刷新后需要从团购详情重新进入。
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCheckoutStore = defineStore('checkout', () => {
  // ── 下单上下文 ──
  const groupBuyId = ref<string | null>(null)
  const groupBuyItemId = ref<string | null>(null)
  const quantity = ref(1)
  const selectedAddressId = ref<string | null>(null)
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
    groupBuyId.value = params.groupBuyId
    groupBuyItemId.value = params.groupBuyItemId
    quantity.value = params.quantity
    selectedAddressId.value = null
    remark.value = ''
    snapshot.value = {
      title: params.title ?? '',
      coverImageUrl: params.coverImageUrl ?? null,
      displayName: params.displayName ?? '',
      unitPriceAmount: params.unitPriceAmount ?? 0,
    }
  }

  function clearCheckout(): void {
    groupBuyId.value = null
    groupBuyItemId.value = null
    quantity.value = 1
    selectedAddressId.value = null
    remark.value = ''
    snapshot.value = null
  }

  function setAddress(addressId: string): void {
    selectedAddressId.value = addressId
  }

  function setQuantity(newQuantity: number): void {
    quantity.value = newQuantity
  }

  function setRemark(newRemark: string): void {
    remark.value = newRemark
  }

  return {
    groupBuyId,
    groupBuyItemId,
    quantity,
    selectedAddressId,
    remark,
    snapshot,
    setCheckoutContext,
    clearCheckout,
    setAddress,
    setQuantity,
    setRemark,
  }
})
