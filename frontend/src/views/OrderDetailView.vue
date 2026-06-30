<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchOrder" />

    <template v-else-if="order">
      <div class="order-detail">
        <!-- 已取消 - 特殊横幅 -->
        <div v-if="order.orderStatus === 'canceled'" class="card pad" style="text-align:center;padding:24px">
          <van-icon name="clear" :size="48" color="var(--color-text-hint)" />
          <p style="font-weight:900;font-size:18px;margin:8px 0 16px;color:var(--color-text-hint)">订单已取消</p>
          <div class="order-step" style="margin-bottom:0">
            <div>已下单</div>
            <div>已支付</div>
            <div>待发货</div>
            <div>完成</div>
          </div>
        </div>

        <!-- 订单进度步骤（横向 4 步） -->
        <div v-else class="order-step">
          <div :class="{ done: stepActive >= 0 }">已下单</div>
          <div :class="{ done: stepActive >= 1 }">已支付</div>
          <div :class="{ done: stepActive >= 2 }">待发货</div>
          <div :class="{ done: stepActive >= 3 }">完成</div>
        </div>

        <!-- 状态卡 -->
        <div v-if="order.orderStatus !== 'canceled'" class="card pad">
          <div class="between">
            <b>{{ getStatusLabel(order.orderStatus) }}</b>
            <span class="muted">{{ getStatusDescription(order.orderStatus) }}</span>
          </div>
        </div>

        <!-- 收货地址 -->
        <div class="card pad">
          <h3 style="margin:0 0 6px;font-size:16px">收货地址</h3>
          <p style="font-weight:900">{{ order.receiverName }} {{ order.receiverPhone }}</p>
          <p class="muted" style="margin:0">{{ order.fullAddress }}</p>
        </div>

        <!-- 商品列表 -->
        <div class="card pad">
          <div v-for="item in order.items" :key="item.id" class="row" style="gap:12px;padding:8px 0">
            <div class="order-item__img-placeholder">
              <van-icon name="photo" :size="32" color="var(--color-text-hint)" />
            </div>
            <div class="grow">
              <b>{{ item.productName }}</b>
              <p class="muted" style="margin:2px 0">数量 x{{ item.quantity }}</p>
              <b style="color:var(--color-price)">¥{{ formatPrice(item.unitPriceAmount) }}</b>
            </div>
          </div>
        </div>

        <!-- 订单信息（form-card 模式） -->
        <div class="form-card">
          <div class="form-title">订单信息</div>
          <div class="field">
            <label>订单编号</label>
            <span class="value">{{ order.orderNo }}</span>
            <span></span>
          </div>
          <div v-if="order.paidAt" class="field">
            <label>支付时间</label>
            <span class="value">{{ formatDate(order.paidAt) }}</span>
            <span></span>
          </div>
          <div v-if="order.shippedAt" class="field">
            <label>发货时间</label>
            <span class="value">{{ formatDate(order.shippedAt) }}</span>
            <span></span>
          </div>
          <div v-if="order.completedAt" class="field">
            <label>完成时间</label>
            <span class="value">{{ formatDate(order.completedAt) }}</span>
            <span></span>
          </div>
          <div v-if="order.remark" class="field">
            <label>备注</label>
            <span class="value">{{ order.remark }}</span>
            <span></span>
          </div>
          <div class="field">
            <label>实付金额</label>
            <span class="value" style="color:var(--color-price);font-weight:900">¥{{ formatPrice(order.payAmount) }}</span>
            <span></span>
          </div>
        </div>
      </div>
    </template>

    <!-- 底部操作栏 -->
    <template v-if="order" #action>
      <div class="fixed-actions">
        <template v-if="order.orderStatus === 'pendingPay'">
          <button class="btn ghost" :disabled="actionLoading" @click="handleCancel">取消订单</button>
          <button class="btn primary" :disabled="actionLoading" @click="handlePay">模拟支付</button>
        </template>

        <button
          v-else-if="order.orderStatus === 'shipped'"
          class="btn primary"
          :disabled="actionLoading"
          @click="handleComplete"
        >确认收货</button>

        <button
          v-else-if="order.orderStatus === 'paid'"
          class="btn primary"
          disabled
        >等待卖家发货</button>

        <button
          v-else-if="order.orderStatus === 'completed'"
          class="btn primary"
          disabled
        >已完成</button>

        <button
          v-else-if="order.orderStatus === 'canceled'"
          class="btn ghost"
          disabled
        >已取消</button>
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
import { getMyOrder, simulatePay, cancelOrder, completeOrder } from '@/api/orders'
import type { OrderData } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<OrderData | null>(null)
const actionLoading = ref(false)

const orderId = computed(() => route.params.id as string)

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

/** 将整数分格式化为显示字符串 */
function formatPrice(amount: number): string {
  return (amount / 100).toFixed(2)
}

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
  padding: 8px 14px;
}

/* ── 商品图占位 ── */
.order-item__img-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
