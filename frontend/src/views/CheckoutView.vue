<template>
  <PageLayout title="确认订单" show-back @back="handleBack">
    <!-- 缺少下单信息 -->
    <ErrorView
      v-if="!checkoutStore.groupBuyId || !checkoutStore.groupBuyItemId"
      message="缺少下单信息，请从团购详情重新进入"
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
        <div class="checkout-section" @click="goToAddresses">
          <div class="checkout-section__header">
            <van-icon name="location" color="var(--color-primary)" />
            <span class="checkout-section__title">收货地址</span>
          </div>
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
          <van-icon name="arrow" color="var(--color-text-hint)" size="16" />
        </div>

        <!-- 商品明细 -->
        <div class="checkout-section">
          <div class="checkout-section__header">
            <van-icon name="shop" color="var(--color-primary)" />
            <span class="checkout-section__title">商品</span>
          </div>
          <div v-for="item in preview.items" :key="item.groupBuyItemId" class="checkout-item">
            <div class="checkout-item__info">
              <span class="checkout-item__name">{{ item.productName }}</span>
              <div class="checkout-item__price-row">
                <PriceText :amount="item.unitPriceAmount" size="sm" color="var(--color-price)" />
                <span class="checkout-item__qty">x{{ item.quantity }}</span>
              </div>
            </div>
            <!-- 数量调整 -->
            <van-stepper
              v-if="editingItemId === item.groupBuyItemId"
              v-model="quantity"
              :min="1"
              :max="item.availableStock"
              integer
              theme="round"
              button-size="26"
              @change="onQuantityChange"
            />
            <van-button
              v-else
              size="small"
              round
              plain
              type="primary"
              @click="startEditQuantity(item)"
            >
              修改
            </van-button>
          </div>
        </div>

        <!-- 备注 -->
        <div class="checkout-section">
          <van-field
            v-model="remark"
            type="textarea"
            placeholder="选填：给团长留言"
            :maxlength="200"
            rows="2"
            autosize
            @blur="onRemarkChange"
          />
        </div>

        <!-- 协议 -->
        <div class="checkout-section">
          <div class="checkout-agreement">
            <van-checkbox v-model="agreed" shape="square" checked-color="var(--color-primary)">
              提交即代表同意
              <span class="checkout-agreement__link">《团购协议》</span>
            </van-checkbox>
          </div>
        </div>

        <!-- 金额区 -->
        <div class="checkout-section">
          <OrderAmountBreakdown
            :total-amount="preview.totalAmount"
            :discount-amount="preview.discountAmount"
            :pay-amount="preview.payAmount"
          />
        </div>

        <!-- 底部占位 -->
        <div style="height: 80px" />
      </div>
    </template>

    <!-- 底部提交栏 -->
    <template v-if="preview && !error" #action>
      <div class="checkout-action-bar">
        <div class="checkout-action-bar__amount">
          <span class="checkout-action-bar__label">合计：</span>
          <PriceText
            :amount="preview.payAmount"
            size="xl"
            color="var(--color-price)"
          />
        </div>
        <van-button
          round
          type="primary"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交订单
        </van-button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import PriceText from '@/components/PriceText.vue'
import OrderAmountBreakdown from '@/components/OrderAmountBreakdown.vue'
import { useCheckoutStore } from '@/stores'
import { listAddresses } from '@/api/addresses'
import { previewOrder, createOrder } from '@/api/orders'
import type { OrderPreviewData, AddressData } from '@/types'

const router = useRouter()
const checkoutStore = useCheckoutStore()

// ── 状态 ──
const preview = ref<OrderPreviewData | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const submitting = ref(false)
const agreed = ref(false)
const remark = ref('')
const editingItemId = ref<string | null>(null)
const quantity = ref(1)
const hasAddress = ref(false)

// ── 计算属性 ──
const addressInfo = computed(() => preview.value?.address ?? null)
const canSubmit = computed(() => {
  if (!preview.value) return false
  if (!checkoutStore.selectedAddressId) return false
  if (!agreed.value) return false
  if (submitting.value) return false
  return true
})

// ── 初始化：先拉地址列表，再调 preview ──
async function initCheckout() {
  if (!checkoutStore.groupBuyId || !checkoutStore.groupBuyItemId) {
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
  if (!checkoutStore.groupBuyId || !checkoutStore.groupBuyItemId || !checkoutStore.selectedAddressId) {
    return
  }

  loading.value = true
  error.value = null
  preview.value = null // 清除旧预览，避免 stale 数据残留

  try {
    const data = await previewOrder({
      groupBuyId: checkoutStore.groupBuyId,
      addressId: checkoutStore.selectedAddressId,
      items: [{ groupBuyItemId: checkoutStore.groupBuyItemId, quantity: checkoutStore.quantity }],
    })
    preview.value = data
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载订单信息失败'
    preview.value = null
  } finally {
    loading.value = false
  }
}

// ── 重新预览（数量变化 / 地址变化） ──
async function rePreview(): Promise<void> {
  if (!checkoutStore.selectedAddressId) {
    error.value = '请选择收货地址'
    return
  }
  await doPreview()
}

// ── 数量编辑 ──
function startEditQuantity(item: OrderPreviewData['items'][0]) {
  editingItemId.value = item.groupBuyItemId
  quantity.value = item.quantity
}

function onQuantityChange(val: number | string) {
  const newQty = Number(val)
  quantity.value = newQty
  checkoutStore.setQuantity(newQty)
  editingItemId.value = null
  rePreview()
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
    const orderData = await createOrder({
      groupBuyId: checkoutStore.groupBuyId!,
      addressId: checkoutStore.selectedAddressId,
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
</script>

<style scoped>
.checkout-content {
  padding: 8px 14px;
}

.checkout-section {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  margin: 0 0 12px;
  padding: 14px;
  display: block;
  position: relative;
  cursor: pointer;
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(237, 240, 242, 0.72);
}

.checkout-section > :deep(.van-icon:last-child) {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
}

.checkout-section__header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--spacing-xs);
}

.checkout-section__title {
  font-size: var(--font-size-md);
  font-weight: 900;
  color: var(--color-text-primary);
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

.checkout-agreement {
  width: 100%;
}

.checkout-agreement__link {
  color: var(--color-primary);
}

.checkout-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: var(--spacing-sm);
}

.checkout-action-bar__amount {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.checkout-action-bar__label {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  font-weight: 800;
}
</style>
