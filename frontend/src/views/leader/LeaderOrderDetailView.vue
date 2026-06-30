<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchOrder" />

    <template v-else-if="order">
      <div class="order-detail">
        <!-- 状态标签 -->
        <div class="status-banner">
          <van-tag size="medium" :type="statusTagType as any">
            {{ getOrderStatusText(order.orderStatus) }}
          </van-tag>
        </div>

        <!-- 收货地址 -->
        <div class="card pad">
          <h4>收货信息</h4>
          <p class="receiver-info">{{ order.receiverName }} {{ order.receiverPhone }}</p>
          <p class="receiver-address">{{ order.fullAddress }}</p>
        </div>

        <!-- 商品列表 -->
        <div class="card pad">
          <h4>商品信息</h4>
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="order-item__info">
              <span class="order-item__name">{{ item.productName }}</span>
              <span class="order-item__price">{{ formatAmount(item.unitPriceAmount) }}</span>
            </div>
            <div class="order-item__meta">
              <span>数量 x{{ item.quantity }}</span>
              <span>小计：{{ formatAmount(item.totalAmount) }}</span>
            </div>
          </div>
        </div>

        <!-- 支付信息 -->
        <div class="card pad">
          <h4>支付信息</h4>
          <div class="info-row">
            <span>支付金额</span>
            <span class="price">{{ formatAmount(order.payAmount) }}</span>
          </div>
          <div class="info-row">
            <span>支付状态</span>
            <span>{{ getPayStatusText(order.payStatus) }}</span>
          </div>
          <div v-if="order.paidAt" class="info-row">
            <span>支付时间</span>
            <span>{{ formatDate(order.paidAt) }}</span>
          </div>
        </div>

        <!-- 发货信息 -->
        <div v-if="order.orderStatus === 'shipped' || order.orderStatus === 'completed'" class="card pad">
          <h4>发货信息</h4>
          <div class="info-row">
            <span>发货状态</span>
            <span>{{ getOrderStatusText(order.orderStatus) }}</span>
          </div>
          <div v-if="order.shippedAt" class="info-row">
            <span>发货时间</span>
            <span>{{ formatDate(order.shippedAt) }}</span>
          </div>
        </div>

        <!-- 发货表单（仅待发货状态） -->
        <div v-if="order.orderStatus === 'paid'" class="card pad">
          <h4>确认发货</h4>
          <ShipmentForm :submitting="shipLoading" @submit="handleShip" />
        </div>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import ShipmentForm from '@/components/ShipmentForm.vue'
import { getLeaderOrder, shipOrder } from '@/api/leaderOrders'
import { getOrderStatusText, getPayStatusText, formatAmount } from '@/utils'
import type { LeaderOrderData } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<LeaderOrderData | null>(null)
const shipLoading = ref(false)

const orderId = computed(() => route.params.id as string)

const statusTagType = computed(() => {
  const map: Record<string, string> = {
    paid: 'danger',
    shipped: 'warning',
    completed: 'success',
    canceled: 'default',
    pendingPay: 'warning',
  }
  return map[order.value?.orderStatus || ''] || 'default'
})

async function fetchOrder() {
  loading.value = true
  error.value = null
  try {
    order.value = await getLeaderOrder(orderId.value)
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

async function handleShip(data: {
  deliveryType: string
  logisticsCompany: string
  trackingNo: string
}) {
  shipLoading.value = true
  try {
    await shipOrder(orderId.value, data)
    showToast('发货成功')
    await fetchOrder()
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '发货失败')
  } finally {
    shipLoading.value = false
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail {
  padding: 8px 14px 80px;
}

.card {
  background: var(--color-bg-white);
  border-radius: 10px;
  margin-bottom: 10px;
}

.pad {
  padding: 14px;
}

.status-banner {
  padding: 12px 0;
  text-align: center;
}

h4 {
  margin: 0 0 10px;
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
}

.receiver-info {
  font-weight: 900;
  margin: 0 0 4px;
}

.receiver-address {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin: 0;
}

.order-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}

.order-item:last-child {
  border-bottom: none;
}

.order-item__info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-item__name {
  font-weight: 900;
  color: var(--color-text-primary);
}

.order-item__price {
  color: var(--color-price);
  font-weight: 900;
}

.order-item__meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 4px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: var(--font-size-sm);
}

.info-row .price {
  color: var(--color-price);
  font-weight: 900;
}
</style>
