<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>订单履约</p>
        <h1>订单列表</h1>
      </div>
    </div>

    <div class="toolbar">
      <div class="segmented">
        <button
          v-for="item in statusFilters"
          :key="item.value"
          type="button"
          :class="{ active: status === item.value }"
          @click="changeStatus(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <form class="search" @submit.prevent="loadOrders">
        <input v-model="keyword" placeholder="订单号 / 收货人 / 手机号">
        <button type="submit">搜索</button>
      </form>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadOrders" />

    <div v-else class="table-panel">
      <table class="merchant-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>买家 / 收货人</th>
            <th>商品摘要</th>
            <th>金额</th>
            <th>订单状态</th>
            <th>支付时间</th>
            <th>发货状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td><strong>{{ order.orderNo }}</strong></td>
            <td>
              <strong>{{ order.buyerNickname || order.receiverName }}</strong>
              <span>{{ order.receiverName }} {{ order.receiverPhone }}</span>
            </td>
            <td>{{ order.items[0]?.productName || '团购商品' }}<span v-if="order.items.length > 1"> 等 {{ order.items.length }} 件</span></td>
            <td>{{ formatAmount(order.payAmount) }}</td>
            <td>{{ getOrderStatusText(order.orderStatus) }}</td>
            <td>{{ formatDateTime(order.paidAt) }}</td>
            <td>{{ order.shippedAt ? formatDateTime(order.shippedAt) : '待发货' }}</td>
            <td>
              <RouterLink :to="`/merchant/orders/${order.id}`">
                {{ order.orderStatus === 'paid' ? '发货' : '查看' }}
              </RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="orders.length === 0" description="暂无订单" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listLeaderOrdersByParams } from '@/api/leaderOrders'
import { formatAmount, formatDateTime, getOrderStatusText } from '@/utils'
import type { LeaderOrderData } from '@/types'

const route = useRoute()
const status = ref((route.query.status as string) || '')
const keyword = ref('')
const loading = ref(true)
const error = ref('')
const orders = ref<LeaderOrderData[]>([])

const statusFilters = [
  { label: '全部', value: '' },
  { label: '待发货', value: 'paid' },
  { label: '已发货', value: 'shipped' },
  { label: '已完成', value: 'completed' },
  { label: '售后中', value: 'after_sale' },
  { label: '已取消', value: 'canceled' },
]

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const data = await listLeaderOrdersByParams({
      status: status.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: 1,
      pageSize: 30,
    })
    orders.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '订单加载失败'
  } finally {
    loading.value = false
  }
}

function changeStatus(next: string) {
  status.value = next
  loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.page-head h1 {
  margin: 4px 0 0;
  font-size: 26px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.segmented {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.segmented button,
.search button {
  height: 34px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #374151;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.segmented button {
  padding: 0 12px;
}

.segmented button.active,
.search button {
  border-color: #e9563f;
  background: #e9563f;
  color: #ffffff;
}

.search {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.search input {
  width: 240px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
}

.search button {
  padding: 0 14px;
}

.table-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  overflow-x: auto;
}

.merchant-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.merchant-table th,
.merchant-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  font-size: 13px;
  vertical-align: middle;
}

.merchant-table th {
  color: #6b7280;
  background: #f9fafb;
  font-weight: 900;
}

.merchant-table strong,
.merchant-table span {
  display: block;
}

.merchant-table span {
  margin-top: 3px;
  color: #6b7280;
}

.merchant-table a {
  color: #d63f2b;
  font-weight: 900;
  text-decoration: none;
}
</style>
