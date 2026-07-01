<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchOrder" />

    <template v-else-if="order">
      <div class="order-detail">
        <!-- 状态标签 -->
        <div class="status-banner">
          <AppStatusPill
            :variant="statusVariant"
            size="sm"
          >
            {{ getOrderStatusText(order.orderStatus) }}
          </AppStatusPill>
        </div>

        <!-- 收货地址 -->
        <AppCard>
          <h4 class="section-title">收货信息</h4>
          <p class="receiver-info">{{ order.receiverName }} {{ order.receiverPhone }}</p>
          <p class="receiver-address">{{ order.fullAddress }}</p>
        </AppCard>

        <!-- 商品列表 -->
        <AppCard>
          <h4 class="section-title">商品信息</h4>
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="order-item__info">
              <span class="order-item__name">{{ item.productName }}</span>
              <PriceText :amount="item.unitPriceAmount" size="sm" />
            </div>
            <div class="order-item__meta">
              <span>数量 x{{ item.quantity }}</span>
              <span>小计：<PriceText :amount="item.totalAmount" size="sm" /></span>
            </div>
          </div>
        </AppCard>

        <!-- 支付信息 -->
        <AppCard>
          <h4 class="section-title">支付信息</h4>
          <AppFormRow label="支付金额">
            <PriceText :amount="order.payAmount" size="md" color="var(--color-price)" />
          </AppFormRow>
          <AppFormRow label="支付状态">
            {{ getPayStatusText(order.payStatus) }}
          </AppFormRow>
          <AppFormRow v-if="order.paidAt" label="支付时间">
            {{ formatDateTime(order.paidAt) }}
          </AppFormRow>
        </AppCard>

        <!-- 发货信息 -->
        <AppCard v-if="order.orderStatus === 'shipped' || order.orderStatus === 'completed'">
          <h4 class="section-title">发货信息</h4>
          <AppFormRow label="发货状态">
            {{ getOrderStatusText(order.orderStatus) }}
          </AppFormRow>
          <AppFormRow v-if="order.shippedAt" label="发货时间">
            {{ formatDateTime(order.shippedAt) }}
          </AppFormRow>
        </AppCard>

        <!-- 发货表单（仅待发货状态） -->
        <AppCard v-if="order.orderStatus === 'paid'">
          <h4 class="section-title">确认发货</h4>
          <ShipmentForm :submitting="shipLoading" @submit="handleShip" />
        </AppCard>
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
import AppCard from '@/components/AppCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import PriceText from '@/components/PriceText.vue'
import { getLeaderOrder, shipOrder } from '@/api/leaderOrders'
import { getOrderStatusText, getPayStatusText, formatDateTime } from '@/utils'
import type { LeaderOrderData } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<LeaderOrderData | null>(null)
const shipLoading = ref(false)

const orderId = computed(() => route.params.id as string)

const statusVariant = computed(() => {
  const map: Record<string, 'green' | 'orange' | 'gray' | 'red'> = {
    paid: 'orange',
    shipped: 'green',
    completed: 'green',
    canceled: 'gray',
    pendingPay: 'orange',
  }
  return map[order.value?.orderStatus || ''] || 'gray'
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

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail {
  padding: 8px 14px 80px;
}

.status-banner {
  padding: 12px 0;
  text-align: center;
}

.section-title {
  margin: 0 0 10px;
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  font-weight: 900;
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

.order-item__meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 4px;
}
</style>
