<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchOrder" />

    <template v-else-if="order">
      <div class="order-detail">
        <AppCard class="order-status-card">
          <div class="order-status-card__main">
            <span>{{ getStatusLabel(order.orderStatus) }}</span>
            <p>{{ getStatusDescription(order.orderStatus) }}</p>
          </div>
          <PriceText :amount="order.payAmount" size="xl" color="var(--color-price)" />
        </AppCard>

        <!-- 履约进度 -->
        <AppCard>
          <template #header>
            <span>履约进度</span>
          </template>
          <OrderStatusSteps :status="order.orderStatus" />
        </AppCard>

        <!-- 收货地址 -->
        <AppCard>
          <template #header>
            <span>收货地址</span>
          </template>
          <p class="order-receiver">{{ order.receiverName }} {{ order.receiverPhone }}</p>
          <p class="order-address">{{ order.fullAddress }}</p>
        </AppCard>

        <!-- 商品列表 -->
        <OrderSnapshotCard :items="order.items" :totalAmount="order.totalAmount" />

        <!-- 金额明细 -->
        <AppCard>
          <template #header>
            <span>金额明细</span>
          </template>
          <OrderAmountBreakdown
            class="order-amount-breakdown--plain"
            :total-amount="order.totalAmount"
            :discount-amount="order.discountAmount"
            :pay-amount="order.payAmount"
          />
          <div v-if="hasCouponSnapshot" class="order-coupon-line">
            <span>优惠券</span>
            <strong>{{ order.couponName || '已使用优惠券' }}</strong>
          </div>
        </AppCard>

        <!-- 订单信息 -->
        <AppFormCard title="订单信息">
          <AppFormRow label="订单编号">{{ order.orderNo }}</AppFormRow>
          <AppFormRow v-if="order.paidAt" label="支付时间">{{ formatDateTime(order.paidAt) }}</AppFormRow>
          <AppFormRow v-if="order.shippedAt" label="发货时间">{{ formatDateTime(order.shippedAt) }}</AppFormRow>
          <AppFormRow v-if="order.completedAt" label="完成时间">{{ formatDateTime(order.completedAt) }}</AppFormRow>
          <AppFormRow v-if="order.remark" label="备注">{{ order.remark }}</AppFormRow>
          <AppFormRow v-if="order.couponName" label="优惠券">
            {{ order.couponName }}
          </AppFormRow>
          <AppFormRow v-if="(order.discountAmount ?? 0) > 0" label="优惠金额">
            <PriceText :amount="order.discountAmount" color="var(--color-primary)" />
          </AppFormRow>
        </AppFormCard>
      </div>
    </template>

    <!-- 底部操作栏 -->
    <template v-if="order" #action>
      <AppFixedActions>
        <template v-if="order.orderStatus === 'pendingPay'">
          <AppButton variant="ghost" :disabled="actionLoading" @click="handleCancel">取消订单</AppButton>
          <AppButton variant="primary" :disabled="actionLoading" @click="handlePay">模拟支付</AppButton>
        </template>

        <template v-else-if="order.orderStatus === 'shipped'">
          <AppButton variant="ghost" :disabled="actionLoading" @click="openOrderChat">联系团长</AppButton>
          <AppButton variant="primary" :disabled="actionLoading" @click="handleComplete">确认收货</AppButton>
        </template>

        <template v-else-if="order.orderStatus === 'paid'">
          <AppButton variant="ghost" :disabled="actionLoading" @click="openOrderChat">联系团长</AppButton>
          <AppButton variant="primary" disabled>等待卖家发货</AppButton>
        </template>

        <template v-else-if="order.orderStatus === 'completed'">
          <AppButton variant="ghost" :disabled="actionLoading" @click="openOrderChat">联系团长</AppButton>
          <AppButton variant="primary" disabled>已完成</AppButton>
        </template>

        <AppButton
          v-else-if="order.orderStatus === 'canceled'"
          variant="ghost"
          disabled
        >已取消</AppButton>
      </AppFixedActions>
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
import AppCard from '@/components/AppCard.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppButton from '@/components/AppButton.vue'
import OrderStatusSteps from '@/components/OrderStatusSteps.vue'
import OrderSnapshotCard from '@/components/OrderSnapshotCard.vue'
import OrderAmountBreakdown from '@/components/OrderAmountBreakdown.vue'
import PriceText from '@/components/PriceText.vue'
import { getMyOrder, simulatePay, cancelOrder, completeOrder } from '@/api/orders'
import { openChatByOrder } from '@/api/chats'
import { formatDateTime } from '@/utils/format'
import type { OrderData } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<OrderData | null>(null)
const actionLoading = ref(false)

const orderId = computed(() => route.params.id as string)
const hasCouponSnapshot = computed(() => !!order.value?.couponName || (order.value?.discountAmount ?? 0) > 0)

/** 状态标签文字 */
function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pendingPay: '待支付',
    paid: '待发货',
    shipped: '已发货',
    completed: '已完成',
    canceled: '已取消',
  }
  return map[status] || status
}

/** 状态描述文字 */
function getStatusDescription(status: string): string {
  const map: Record<string, string> = {
    pendingPay: '等待买家付款',
    paid: '商家正在处理',
    shipped: '快递运输中',
    completed: '已完成',
    canceled: '已取消',
  }
  return map[status] || ''
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

async function openOrderChat() {
  actionLoading.value = true
  try {
    const conversation = await openChatByOrder(orderId.value)
    await router.push(`/chats/${conversation.id}`)
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '聊天打开失败')
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
  padding: 8px 14px;
}

.order-status-card {
  margin-bottom: 12px;
}

.order-status-card__main,
.order-status-card :deep(.app-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-status-card__main {
  flex-direction: column;
  align-items: flex-start;
}

.order-status-card__main span {
  color: var(--color-text-primary);
  font-size: 20px;
  font-weight: 900;
  line-height: 1.25;
}

.order-status-card__main p {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.order-receiver {
  font-weight: 900;
  margin: 0 0 4px;
}

.order-address {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
  margin: 0;
}

.order-coupon-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid var(--color-border);
  margin-top: 10px;
  padding-top: 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.order-coupon-line strong {
  color: var(--color-primary-dark);
  font-weight: 800;
  text-align: right;
}

:deep(.order-amount-breakdown--plain) {
  padding: 0;
  background: transparent;
  border-radius: 0;
}
</style>
