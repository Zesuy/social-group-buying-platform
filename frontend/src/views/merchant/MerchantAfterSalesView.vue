<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>退款售后</p>
        <h1>售后列表</h1>
      </div>
    </div>

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

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadAfterSales" />

    <div v-else class="table-panel">
      <table class="merchant-table">
        <thead>
          <tr>
            <th>售后单号</th>
            <th>订单号</th>
            <th>买家 / 收货人</th>
            <th>退款金额</th>
            <th>原因摘要</th>
            <th>状态</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in afterSales" :key="item.id">
            <td><strong>{{ item.id }}</strong></td>
            <td>{{ item.orderNo }}</td>
            <td>
              <strong>{{ item.buyerNickname || item.receiverName || '买家' }}</strong>
              <span v-if="item.receiverName">{{ item.receiverName }} {{ item.receiverPhone }}</span>
            </td>
            <td>{{ formatAmount(item.amount) }}</td>
            <td>{{ item.reason }}</td>
            <td>{{ statusText(item.status) }}</td>
            <td>{{ formatDateTime(item.createdAt) }}</td>
            <td><RouterLink :to="`/merchant/after-sales/${item.id}`">{{ actionText(item.status) }}</RouterLink></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="afterSales.length === 0" description="暂无售后申请" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listLeaderAfterSales } from '@/api/leaderAfterSales'
import { formatAmount, formatDateTime } from '@/utils'
import type { AfterSaleData } from '@/types'

const route = useRoute()
const status = ref((route.query.status as string) || '')
const loading = ref(true)
const error = ref('')
const afterSales = ref<AfterSaleData[]>([])

const statusFilters = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'pending' },
  { label: '已同意', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '已退款', value: 'completed' },
]

function statusText(value: string) {
  return statusFilters.find((item) => item.value === value)?.label || value
}

function actionText(value: string) {
  if (value === 'pending') return '审核'
  if (value === 'approved') return '退款'
  return '查看'
}

async function loadAfterSales() {
  loading.value = true
  error.value = ''
  try {
    const data = await listLeaderAfterSales({ status: status.value || undefined, page: 1, pageSize: 30 })
    afterSales.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '售后列表加载失败'
  } finally {
    loading.value = false
  }
}

function changeStatus(next: string) {
  status.value = next
  loadAfterSales()
}

onMounted(loadAfterSales)
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

.segmented {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.segmented button {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #374151;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.segmented button.active {
  border-color: #e9563f;
  background: #e9563f;
  color: #ffffff;
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
