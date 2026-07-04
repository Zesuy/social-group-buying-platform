<template>
  <PageLayout title="确认订单" show-back @back="handleBack">
    <!-- 缺少下单信息 -->
    <ErrorView
      v-if="!hasCheckoutContext"
      message="缺少下单信息，请从团购详情或购物车重新进入"
      :show-retry="false"
    />

    <!-- 无地址引导 -->
    <template v-else-if="!hasAddress && !loading && !error">
      <div class="checkout-no-address">
        <van-icon name="location" :size="56" color="var(--color-text-hint)" />
        <p class="checkout-no-address__text">请先添加收货地址</p>
        <van-button round type="primary" size="small" @click="goToAddresses">
          去添加地址
        </van-button>
      </div>
    </template>

    <LoadingView v-else-if="loading" />

    <ErrorView v-else-if="error && !loading" :message="error" @retry="rePreview" />

    <template v-if="preview && !loading && !error">
      <div class="checkout-content">
        <!-- 收货地址 -->
        <CheckoutSection title="收货地址" clickable @click="goToAddresses">
          <template #header-right>
            <van-icon name="arrow" color="var(--color-text-hint)" size="16" />
          </template>
          <div v-if="addressInfo" class="checkout-address">
            <div class="checkout-address__header">
              <span class="checkout-address__name">{{ addressInfo.receiverName }}</span>
              <span class="checkout-address__phone">{{ addressInfo.receiverPhone }}</span>
            </div>
            <p class="checkout-address__detail">{{ addressInfo.fullAddress }}</p>
          </div>
          <div v-else class="checkout-address checkout-address--empty">
            请选择收货地址
          </div>
        </CheckoutSection>

        <!-- 商品明细 -->
        <CheckoutSection title="商品信息">
          <div v-for="item in preview.items" :key="item.groupBuyItemId" class="checkout-item">
            <div class="checkout-item__info">
              <span class="checkout-item__name">{{ item.productName }}</span>
              <div class="checkout-item__price-row">
                <PriceText :amount="item.unitPriceAmount" size="sm" color="var(--color-price)" />
                <span class="checkout-item__qty">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="checkout-item__subtotal">
              <span>小计</span>
              <PriceText :amount="item.totalAmount" size="sm" color="var(--color-text-primary)" />
            </div>
          </div>
        </CheckoutSection>

        <!-- 优惠券 -->
        <CheckoutSection v-if="hasCouponSection" title="优惠券">
          <div class="checkout-coupons">
            <button
              v-if="selectedCoupon"
              type="button"
              class="checkout-coupon checkout-coupon--selected"
              :disabled="loading"
              @click="toggleCoupon(selectedCoupon)"
            >
              <span class="checkout-coupon__main">
                <b>{{ selectedCoupon.name }}</b>
                <small>{{ couponText(selectedCoupon) }}</small>
              </span>
              <span class="checkout-coupon__action">已使用</span>
            </button>

            <button
              v-for="coupon in availableCoupons"
              :key="coupon.userCouponId || coupon.id"
              type="button"
              class="checkout-coupon"
              :class="{ 'checkout-coupon--selected': checkoutStore.userCouponId === coupon.userCouponId }"
              :disabled="loading || !coupon.userCouponId"
              @click="toggleCoupon(coupon)"
            >
              <span class="checkout-coupon__main">
                <b>{{ coupon.name }}</b>
                <small>{{ couponText(coupon) }}</small>
              </span>
              <span class="checkout-coupon__action">
                {{ checkoutStore.userCouponId === coupon.userCouponId ? '已使用' : '使用' }}
              </span>
            </button>

            <div
              v-for="coupon in unavailableCoupons"
              :key="coupon.userCouponId || coupon.id"
              class="checkout-coupon checkout-coupon--disabled"
            >
              <span class="checkout-coupon__main">
                <b>{{ coupon.name }}</b>
                <small>{{ couponText(coupon) }}</small>
              </span>
              <span class="checkout-coupon__reason">{{ coupon.unavailableReason || '暂不可用' }}</span>
            </div>

            <p v-if="!selectedCoupon && availableCoupons.length === 0 && unavailableCoupons.length === 0" class="checkout-coupon-empty">
              暂无可用优惠券
            </p>
          </div>
        </CheckoutSection>

        <!-- 备注 -->
        <AppFormCard>
          <van-field
            v-model="remark"
            type="textarea"
            placeholder="选填：给团长留言"
            :maxlength="200"
            rows="2"
            autosize
            @blur="onRemarkChange"
          />
        </AppFormCard>

        <!-- 协议 -->
        <CheckoutSection>
          <van-checkbox v-model="agreed" shape="square" checked-color="var(--color-primary)">
            提交即代表同意
            <span class="checkout-agreement__link">《团购协议》</span>
          </van-checkbox>
        </CheckoutSection>

        <!-- 金额区 -->
        <CheckoutSection>
          <OrderAmountBreakdown
            :total-amount="preview.totalAmount"
            :discount-amount="preview.discountAmount"
            :pay-amount="preview.payAmount"
          />
        </CheckoutSection>

        <!-- 底部占位 -->
        <div class="checkout-spacer" />
      </div>
    </template>

    <!-- 底部提交栏（仅在有效下单信息时显示） -->
    <template v-if="hasCheckoutContext" #action>
      <AppFixedActions single>
        <AppButton
          variant="primary"
          block
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交订单
        </AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import PriceText from '@/components/PriceText.vue'
import OrderAmountBreakdown from '@/components/OrderAmountBreakdown.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import CheckoutSection from '@/components/CheckoutSection.vue'
import { useCheckoutStore } from '@/stores'
import { listAddresses } from '@/api/addresses'
import { previewOrder, createOrder } from '@/api/orders'
import { formatAmount } from '@/utils/format'
import type { OrderPreviewData, AddressData, AvailableCouponData } from '@/types'

const router = useRouter()
const checkoutStore = useCheckoutStore()

// ── 状态 ──
const preview = ref<OrderPreviewData | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const submitting = ref(false)
const agreed = ref(false)
const remark = ref('')
const hasAddress = ref(false)
const lastPreviewAddressId = ref<string | null>(null)

// ── 计算属性 ──
const hasCheckoutContext = computed(() => {
  if (!checkoutStore.groupBuyId) return false
  if (checkoutStore.mode === 'cart') return checkoutStore.cartItemIds.length > 0
  return !!checkoutStore.groupBuyItemId
})
const addressInfo = computed(() => preview.value?.address ?? null)
const selectedCoupon = computed(() => preview.value?.selectedCoupon ?? null)
const availableCoupons = computed(() => {
  const selectedUserCouponId = selectedCoupon.value?.userCouponId
  return (preview.value?.availableCoupons ?? []).filter((coupon) => (
    !selectedUserCouponId || coupon.userCouponId !== selectedUserCouponId
  ))
})
const unavailableCoupons = computed(() => preview.value?.unavailableCoupons ?? [])
const hasCouponSection = computed(() => !!preview.value)
const canSubmit = computed(() => {
  if (!preview.value) return false
  if (!checkoutStore.selectedAddressId) return false
  if (!agreed.value) return false
  if (submitting.value) return false
  return true
})

// ── 初始化：先拉地址列表，再调 preview ──
async function initCheckout() {
  if (!hasCheckoutContext.value) {
    loading.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    // 1. 拉地址列表
    const addresses = await listAddresses()

    if (addresses.length === 0) {
      hasAddress.value = false
      loading.value = false
      return
    }

    hasAddress.value = true

    // 2. 如果还没有选地址，自动选中默认或第一个
    if (!checkoutStore.selectedAddressId) {
      const defaultAddr = addresses.find((a: AddressData) => a.isDefault)
      checkoutStore.setAddress((defaultAddr || addresses[0]).id)
    }

    // 3. 调用预览
    await doPreview()
  } catch (err) {
    const apiErr = err as { message?: string }
    if (apiErr.message?.includes('地址')) {
      hasAddress.value = false
      loading.value = false
    } else {
      error.value = apiErr.message || '加载订单信息失败'
      loading.value = false
    }
  }
}

// ── 调预览（确保有地址时才调） ──
async function doPreview(): Promise<void> {
  if (!hasCheckoutContext.value || !checkoutStore.selectedAddressId) {
    return
  }

  loading.value = true
  error.value = null
  preview.value = null // 清除旧预览，避免 stale 数据残留

  try {
    const data = checkoutStore.mode === 'cart'
      ? await previewOrder({
          cartItemIds: checkoutStore.cartItemIds,
          addressId: checkoutStore.selectedAddressId,
          userCouponId: checkoutStore.userCouponId,
          shareToken: checkoutStore.shareToken,
        })
      : await previewOrder({
          groupBuyId: checkoutStore.groupBuyId!,
          addressId: checkoutStore.selectedAddressId,
          userCouponId: checkoutStore.userCouponId,
          shareToken: checkoutStore.shareToken,
          items: [{ groupBuyItemId: checkoutStore.groupBuyItemId!, quantity: checkoutStore.quantity }],
        })
    preview.value = data
    checkoutStore.setCoupon(data.selectedCoupon?.userCouponId ?? null)
    lastPreviewAddressId.value = checkoutStore.selectedAddressId
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载订单信息失败'
    preview.value = null
  } finally {
    loading.value = false
  }
}

// ── 重新预览（优惠券 / 地址变化） ──
async function rePreview(): Promise<void> {
  if (!checkoutStore.selectedAddressId) {
    error.value = '请选择收货地址'
    return
  }
  await doPreview()
}

function couponText(coupon: AvailableCouponData): string {
  const threshold = coupon.thresholdAmount > 0 ? `满${formatAmount(coupon.thresholdAmount)}可用` : '无门槛'
  return `${threshold}，优惠${formatAmount(coupon.amount)}`
}

async function toggleCoupon(coupon: AvailableCouponData): Promise<void> {
  if (!coupon.userCouponId) {
    showToast('请先领取该优惠券')
    return
  }
  checkoutStore.setCoupon(checkoutStore.userCouponId === coupon.userCouponId ? null : coupon.userCouponId)
  await rePreview()
}

// ── 备注 ──
function onRemarkChange() {
  checkoutStore.setRemark(remark.value)
}

// ── 提交订单 ──
async function handleSubmit() {
  if (!agreed.value) {
    showToast('请先同意团购协议')
    return
  }
  if (!checkoutStore.selectedAddressId) {
    showToast('请选择收货地址')
    return
  }
  if (!preview.value) return

  submitting.value = true
  try {
    const orderData = checkoutStore.mode === 'cart'
      ? await createOrder({
          cartItemIds: checkoutStore.cartItemIds,
          addressId: checkoutStore.selectedAddressId,
          userCouponId: checkoutStore.userCouponId,
          shareToken: checkoutStore.shareToken,
          remark: remark.value || null,
        })
      : await createOrder({
          groupBuyId: checkoutStore.groupBuyId!,
          addressId: checkoutStore.selectedAddressId,
          userCouponId: checkoutStore.userCouponId,
          shareToken: checkoutStore.shareToken,
          remark: remark.value || null,
          items: [{ groupBuyItemId: checkoutStore.groupBuyItemId!, quantity: checkoutStore.quantity }],
        })
    const orderId = orderData.id
    showToast('订单提交成功')
    checkoutStore.clearCheckout()
    router.replace(`/orders/${orderId}`)
  } catch (err) {
    const apiErr = err as { message?: string; code?: string }
    if (apiErr.code === 'INSUFFICIENT_STOCK') {
      showDialog({
        title: '库存不足',
        message: '抱歉，部分商品库存不足，请调整数量',
        confirmButtonText: '知道了',
      })
    } else if (apiErr.code === 'GROUP_BUY_ENDED') {
      showDialog({
        title: '团购已结束',
        message: '该团购活动已结束，无法继续下单',
        confirmButtonText: '返回首页',
      }).then(() => {
        router.replace('/')
      })
    } else {
      showToast(apiErr.message || '提交失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

// ── 地址选择 ──
function goToAddresses() {
  router.push('/addresses?from=checkout')
}

// ── 返回 ──
function handleBack() {
  router.back()
}

onMounted(() => {
  initCheckout()
})

watch(
  () => checkoutStore.selectedAddressId,
  (addressId) => {
    if (!addressId || addressId === lastPreviewAddressId.value || !preview.value) return
    rePreview()
  },
)
</script>

<style scoped>
.checkout-content {
  padding: 8px 14px;
}

.checkout-address {
  flex: 1;
  min-width: 0;
  padding-right: 24px;
}

.checkout-address--empty {
  color: var(--color-text-hint);
  font-size: var(--font-size-md);
}

.checkout-address__header {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: 2px;
}

.checkout-address__name {
  font-weight: 900;
  color: var(--color-text-primary);
}

.checkout-address__phone {
  color: var(--color-text-secondary);
}

.checkout-address__detail {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: 0;
}

.checkout-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.checkout-item:last-child {
  border-bottom: none;
}

.checkout-item__name {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  font-weight: 800;
}

.checkout-item__price-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.checkout-item__qty {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.checkout-item__subtotal {
  display: grid;
  gap: 4px;
  justify-items: end;
  flex-shrink: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.checkout-coupons {
  display: grid;
  gap: 8px;
}

.checkout-coupon {
  width: 100%;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  cursor: pointer;
}

.checkout-coupon--selected {
  border-color: rgba(16, 196, 104, 0.45);
  background: var(--color-primary-light);
}

.checkout-coupon--disabled {
  cursor: default;
  opacity: 0.62;
}

.checkout-coupon__main {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.checkout-coupon__main b {
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  line-height: 1.25;
}

.checkout-coupon__main small,
.checkout-coupon__reason {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.3;
}

.checkout-coupon__action {
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  font-weight: 800;
  white-space: nowrap;
}

.checkout-coupon-empty {
  margin: 0;
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.checkout-agreement__link {
  color: var(--color-primary);
}

.checkout-spacer {
  height: 80px;
}
</style>
