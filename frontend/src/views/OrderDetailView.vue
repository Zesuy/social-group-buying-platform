<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <!-- 加载态 -->
    <LoadingView v-if="loading" />

    <!-- 错误态 -->
    <ErrorView v-else-if="error" :message="error" @retry="fetchOrder" />

    <!-- 订单详情 -->
    <template v-else-if="order">
      <div class="order-detail">
        <!-- 已取消 - 特殊横幅 -->
        <div v-if="order.orderStatus === 'canceled'" class="order-canceled-banner">
          <van-icon name="clear" :size="48" color="var(--color-text-hint)" />
          <p class="order-canceled-banner__text">订单已取消</p>
        </div>

        <!-- 订单进度步骤 -->
        <van-steps
          v-else
          :active="stepActive"
          direction="vertical"
          active-color="var(--color-primary)"
        >
          <van-step>提交订单</van-step>
          <van-step>买家付款</van-step>
          <van-step>卖家发货</van-step>
          <van-step>确认收货</van-step>
        </van-steps>

        <!-- 收货地址 -->
        <div class="info-section">
          <div class="info-section__header">
            <van-icon name="location" color="var(--color-primary)" />
            <span class="info-section__title">收货地址</span>
          </div>
          <div class="address-card">
            <div class="address-card__header">
              <span class="address-card__name">{{ order.receiverName }}</span>
              <span class="address-card__phone">{{ order.receiverPhone }}</span>
            </div>
            <p class="address-card__detail">{{ order.fullAddress }}</p>
          </div>
        </div>

        <!-- 商品列表 -->
        <div class="info-section">
          <div class="info-section__header">
            <van-icon name="shop" color="var(--color-primary)" />
            <span class="info-section__title">商品</span>
          </div>
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="order-item__img-placeholder">
              <van-icon name="photo" :size="32" color="var(--color-text-hint)" />
            </div>
            <div class="order-item__info">
              <span class="order-item__name">{{ item.productName }}</span>
              <div v-if="item.skuName" class="order-item__sku">{{ item.skuName }}</div>
              <div class="order-item__price-row">
                <PriceText :amount="item.unitPriceAmount" size="sm" color="var(--color-price)" />
                <span class="order-item__qty">x{{ item.quantity }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 金额明细 -->
        <div class="info-section">
          <OrderAmountBreakdown
            :total-amount="order.totalAmount"
            :discount-amount="order.discountAmount"
            :pay-amount="order.payAmount"
          />
        </div>

        <!-- 订单信息 -->
        <div class="info-section">
          <div class="order-info">
            <div class="order-info-row">
              <span class="order-info-label">订单编号</span>
              <span class="order-info-value">{{ order.orderNo }}</span>
            </div>
            <div v-if="order.paidAt" class="order-info-row">
              <span class="order-info-label">付款时间</span>
              <span class="order-info-value">{{ formatDate(order.paidAt) }}</span>
            </div>
            <div v-if="order.shippedAt" class="order-info-row">
              <span class="order-info-label">发货时间</span>
              <span class="order-info-value">{{ formatDate(order.shippedAt) }}</span>
            </div>
            <div v-if="order.completedAt" class="order-info-row">
              <span class="order-info-label">完成时间</span>
              <span class="order-info-value">{{ formatDate(order.completedAt) }}</span>
            </div>
            <div v-if="order.remark" class="order-info-row">
              <span class="order-info-label">备注</span>
              <span class="order-info-value">{{ order.remark }}</span>
            </div>
          </div>
        </div>

        <!-- 底部占位 -->
        <div style="height: 80px" />
      </div>
    </template>

    <!-- 底部操作栏 -->
    <template #action>
      <div class="action-bar">
        <template v-if="order?.orderStatus === 'pendingPay'">
          <van-button round plain type="default" :loading="actionLoading" @click="handleCancel">
            取消订单
          </van-button>
          <van-button round type="primary" :loading="actionLoading" @click="handlePay">
            模拟支付
          </van-button>
        </template>

        <van-button
          v-else-if="order?.orderStatus === 'shipped'"
          round
          type="success"
          :loading="actionLoading"
          @click="handleComplete"
        >
          确认收货
        </van-button>

        <span v-else-if="order?.orderStatus === 'paid'" class="action-bar__text">
          等待卖家发货
        </span>

        <span v-else-if="order?.orderStatus === 'completed'" class="action-bar__text">
          已完成
        </span>

        <span v-else-if="order?.orderStatus === 'canceled'" class="action-bar__text">
          已取消
        </span>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import OrderAmountBreakdown from '@/components/OrderAmountBreakdown.vue'
import PriceText from '@/components/PriceText.vue'
import { getMyOrder, simulatePay, cancelOrder, completeOrder } from '@/api/orders'
import type { OrderData } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<OrderData | null>(null)
const actionLoading = ref(false)

const orderId = computed(() => Number(route.params.id))

/** 订单状态到步骤索引的映射 */
const statusStepMap: Record<string, number> = {
  pendingPay: 0,
  paid: 1,
  shipped: 2,
  completed: 3,
}

const stepActive = computed(() => {
  if (!order.value) return 0
  return statusStepMap[order.value.orderStatus] ?? 0
})

/**
 * 将 ISO 日期字符串格式化为 "YYYY-MM-DD HH:mm"
 */
function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function fetchOrder() {
  loading.value = true
  error.value = null
  try {
    order.value = await getMyOrder(orderId.value)
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.replace('/orders')
}

async function handlePay() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认模拟支付？' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await simulatePay(orderId.value)
    showToast('支付成功')
    await fetchOrder()
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    showToast(apiErr.message || '支付失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleCancel() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认取消订单？' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await cancelOrder(orderId.value)
    showToast('订单已取消')
    await fetchOrder()
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    showToast(apiErr.message || '取消失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleComplete() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认已收到商品？' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await completeOrder(orderId.value)
    showToast('已确认收货')
    await fetchOrder()
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    showToast(apiErr.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail {
  padding-bottom: 16px;
}

/* ── 已取消横幅 ── */
.order-canceled-banner {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.order-canceled-banner__text {
  margin-top: 12px;
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
}

/* ── 信息区块 ── */
.info-section {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  margin: var(--spacing-sm) var(--spacing-lg);
  padding: var(--spacing-md) var(--spacing-lg);
}

.info-section__header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--spacing-sm);
}

.info-section__title {
  font-size: var(--font-size-md);
  font-weight: 500;
  color: var(--color-text-primary);
}

/* ── 地址卡片 ── */
.address-card__header {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: 2px;
}

.address-card__name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.address-card__phone {
  color: var(--color-text-secondary);
}

.address-card__detail {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

/* ── 商品行 ── */
.order-item {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) 0;
  align-items: center;
}

.order-item + .order-item {
  border-top: 1px solid var(--color-border-light);
}

.order-item__img-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.order-item__info {
  flex: 1;
  min-width: 0;
}

.order-item__name {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  display: block;
}

.order-item__sku {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 2px;
}

.order-item__price-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.order-item__qty {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

/* ── 订单信息 ── */
.order-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.order-info-row + .order-info-row {
  border-top: 1px solid var(--color-border-light);
}

.order-info-label {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  flex-shrink: 0;
  margin-right: var(--spacing-md);
}

.order-info-value {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  text-align: right;
  word-break: break-all;
}

/* ── 底部操作栏 ── */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  gap: var(--spacing-sm);
}

.action-bar__text {
  font-size: var(--font-size-md);
  color: var(--color-text-hint);
}
</style>
