<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>售后详情</p>
        <h1>售后单 {{ afterSale?.id || '' }}</h1>
      </div>
      <button type="button" class="ghost-link" @click="goBack()">返回</button>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadDetail" />

    <template v-else-if="afterSale">
      <section class="detail-grid">
        <article class="panel">
          <h2>退款申请</h2>
          <p><strong>{{ statusText(afterSale.status) }}</strong> · {{ formatAmount(afterSale.amount) }}</p>
          <p>{{ afterSale.reason }}</p>
        </article>
        <article class="panel">
          <h2>订单快照</h2>
          <p>{{ afterSale.orderNo }} · {{ afterSale.receiverName || afterSale.buyerNickname || '买家' }}</p>
          <p>{{ afterSale.fullAddress || '未返回收货地址' }}</p>
        </article>
      </section>

      <section v-if="afterSale.status === 'pending'" class="panel actions">
        <h2>审核售后</h2>
        <div class="action-row">
          <button type="button" :disabled="actionLoading === 'approve'" @click="approve">同意退款</button>
          <textarea v-model="rejectReason" rows="3" placeholder="拒绝时必须填写原因" />
          <button type="button" class="danger" :disabled="actionLoading === 'reject'" @click="reject">拒绝申请</button>
        </div>
      </section>

      <section v-if="afterSale.status === 'approved'" class="panel actions">
        <h2>完成模拟退款</h2>
        <button type="button" :disabled="actionLoading === 'refund'" @click="completeRefund">
          {{ actionLoading === 'refund' ? '处理中...' : '完成退款' }}
        </button>
      </section>

      <section class="panel">
        <h2>处理记录</h2>
        <p>申请时间：{{ formatDateTime(afterSale.createdAt) }}</p>
        <p v-if="afterSale.approvedAt">同意时间：{{ formatDateTime(afterSale.approvedAt) }}</p>
        <p v-if="afterSale.rejectedAt">拒绝时间：{{ formatDateTime(afterSale.rejectedAt) }}</p>
        <p v-if="afterSale.refundedAt">退款时间：{{ formatDateTime(afterSale.refundedAt) }}</p>
        <p v-if="afterSale.rejectReason">拒绝原因：{{ afterSale.rejectReason }}</p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import {
  approveLeaderAfterSale,
  completeLeaderAfterSaleRefund,
  getLeaderAfterSale,
  rejectLeaderAfterSale,
} from '@/api/leaderAfterSales'
import { useSmartNavigation } from '@/composables'
import { formatAmount, formatDateTime } from '@/utils'
import type { AfterSaleData } from '@/types'

const route = useRoute()
const { goBack } = useSmartNavigation('/merchant/after-sales')
const loading = ref(true)
const error = ref('')
const actionLoading = ref('')
const rejectReason = ref('')
const afterSale = ref<AfterSaleData | null>(null)

function statusText(value: string) {
  const map: Record<string, string> = {
    pending: '待处理',
    approved: '已同意',
    rejected: '已拒绝',
    completed: '已退款',
  }
  return map[value] || value
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    afterSale.value = await getLeaderAfterSale(route.params.id as string)
  } catch (err) {
    error.value = (err as { message?: string }).message || '售后详情加载失败'
  } finally {
    loading.value = false
  }
}

async function approve() {
  try {
    await showConfirmDialog({ title: '同意退款', message: '确认同意该售后申请？' })
    actionLoading.value = 'approve'
    afterSale.value = await approveLeaderAfterSale(route.params.id as string)
    showToast('已同意退款')
  } catch (err) {
    if ((err as string) !== 'cancel') showToast((err as { message?: string }).message || '操作失败')
  } finally {
    actionLoading.value = ''
  }
}

async function reject() {
  if (!rejectReason.value.trim()) {
    showToast('请填写拒绝原因')
    return
  }
  actionLoading.value = 'reject'
  try {
    afterSale.value = await rejectLeaderAfterSale(route.params.id as string, { rejectReason: rejectReason.value.trim() })
    showToast('已拒绝售后')
  } catch (err) {
    showToast((err as { message?: string }).message || '操作失败')
  } finally {
    actionLoading.value = ''
  }
}

async function completeRefund() {
  try {
    await showConfirmDialog({ title: '完成退款', message: '确认执行模拟退款？' })
    actionLoading.value = 'refund'
    afterSale.value = await completeLeaderAfterSaleRefund(route.params.id as string)
    showToast('退款已完成')
  } catch (err) {
    if ((err as string) !== 'cancel') showToast((err as { message?: string }).message || '操作失败')
  } finally {
    actionLoading.value = ''
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
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

.page-head a,
.ghost-link {
  border: 0;
  background: transparent;
  color: #d63f2b;
  font-family: inherit;
  font-weight: 900;
  text-decoration: none;
  cursor: pointer;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.panel h2 {
  margin: 0 0 10px;
  font-size: 17px;
}

.panel p {
  margin: 6px 0;
  color: #374151;
  font-size: 14px;
}

.actions button {
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 8px;
  background: #e9563f;
  color: #ffffff;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.actions button.danger {
  background: #b91c1c;
}

.action-row {
  display: grid;
  grid-template-columns: auto minmax(280px, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.action-row textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
  resize: vertical;
}
</style>
