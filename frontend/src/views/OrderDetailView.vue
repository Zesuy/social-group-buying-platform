<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchOrder" />

    <template v-else-if="order">
      <div class="order-detail">
        <!-- 已取消 - 特殊横幅 -->
        <AppCard v-if="order.orderStatus === 'canceled'" class="order-cancel-banner">
          <van-icon name="clear" :size="48" color="var(--color-text-hint)" />
          <p class="order-cancel-text">订单已取消</p>
          <OrderStatusSteps status="canceled" />
        </AppCard>

        <!-- 订单进度步骤（横向 4 步） -->
        <OrderStatusSteps v-else :status="order.orderStatus" />

        <!-- 状态卡 -->
        <AppCard v-if="order.orderStatus !== 'canceled'">
          <div class="order-status-row">
            <b>{{ getStatusLabel(order.orderStatus) }}</b>
            <span class="order-status-desc">{{ getStatusDescription(order.orderStatus) }}</span>
          </div>
        </AppCard>

        <!-- 收货地址 -->
        <AppCard>
          <h3 class="order-section-title">收货地址</h3>
          <p class="order-receiver">{{ order.receiverName }} {{ order.receiverPhone }}</p>
          <p class="order-address">{{ order.fullAddress }}</p>
        </AppCard>

        <!-- 商品列表 -->
        <OrderSnapshotCard :items="order.items" :totalAmount="order.payAmount" />

        <!-- 订单信息（form-card 模式） -->
        <AppFormCard title="订单信息">
          <AppFormRow label="订单编号">{{ order.orderNo }}</AppFormRow>
          <AppFormRow v-if="order.paidAt" label="支付时间">{{ formatDateTime(order.paidAt) }}</AppFormRow>
          <AppFormRow v-if="order.shippedAt" label="发货时间">{{ formatDateTime(order.shippedAt) }}</AppFormRow>
          <AppFormRow v-if="order.completedAt" label="完成时间">{{ formatDateTime(order.completedAt) }}</AppFormRow>
          <AppFormRow v-if="order.remark" label="备注">{{ order.remark }}</AppFormRow>
          <AppFormRow label="实付金额">
            <PriceText :amount="order.payAmount" color="var(--color-price)" />
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

        <AppButton
          v-else-if="order.orderStatus === 'shipped'"
          variant="primary"
          :disabled="actionLoading"
          @click="handleComplete"
        >确认收货</AppButton>

        <AppButton
          v-else-if="order.orderStatus === 'paid'"
          variant="primary"
          disabled
        >等待卖家发货</AppButton>

        <AppButton
          v-else-if="order.orderStatus === 'completed'"
          variant="primary"
          disabled
        >已完成</AppButton>

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
import PriceText from '@/components/PriceText.vue'
import { getMyOrder, simulatePay, cancelOrder, completeOrder } from '@/api/orders'
import { formatDateTime } from '@/utils/format'
import type { OrderData } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<OrderData | null>(null)
const actionLoading = ref(false)

const orderId = computed(() => route.params.id as string)

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

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail {
  padding: 8px 14px;
}

.order-cancel-banner {
  text-align: center;
}

.order-cancel-text {
  font-weight: 900;
  font-size: 18px;
  margin: 8px 0 16px;
  color: var(--color-text-hint);
}

.order-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-status-desc {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.order-section-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
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
</style>
