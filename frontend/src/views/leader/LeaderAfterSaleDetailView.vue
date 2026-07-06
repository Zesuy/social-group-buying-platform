<template>
  <PageLayout title="售后详情" show-back @back="goBack">
    <template #action>
      <AppFixedActions v-if="afterSale && actionable" :single="afterSale.status === 'approved'">
        <AppButton v-if="afterSale.status === 'pending'" variant="ghost" @click="rejectSheetVisible = true">拒绝</AppButton>
        <AppButton
          v-if="afterSale.status === 'pending'"
          variant="primary"
          :loading="actionLoading === 'approve'"
          @click="approve"
        >
          同意退款
        </AppButton>
        <AppButton
          v-if="afterSale.status === 'approved'"
          variant="primary"
          :loading="actionLoading === 'refund'"
          @click="completeRefund"
        >
          完成退款
        </AppButton>
      </AppFixedActions>
    </template>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchDetail" />

    <div v-else-if="afterSale" class="after-sale-detail">
      <AppCard>
        <div class="detail-head">
          <div>
            <span>售后单 {{ afterSale.id }}</span>
            <h1>{{ statusText(afterSale.status) }}</h1>
          </div>
          <AppStatusPill :variant="statusVariant(afterSale.status)" size="sm">
            {{ statusText(afterSale.status) }}
          </AppStatusPill>
        </div>
        <p class="reason">{{ afterSale.reason }}</p>
      </AppCard>

      <AppCard>
        <h2 class="section-title">订单快照</h2>
        <AppFormRow label="订单号">{{ afterSale.orderNo }}</AppFormRow>
        <AppFormRow label="订单状态">{{ getOrderStatusText(afterSale.orderStatus) }}</AppFormRow>
        <AppFormRow label="支付状态">{{ getPayStatusText(afterSale.payStatus) }}</AppFormRow>
        <AppFormRow v-if="afterSale.buyerNickname" label="买家">{{ afterSale.buyerNickname }}</AppFormRow>
        <AppFormRow v-if="afterSale.fullAddress" label="收货地址">{{ afterSale.fullAddress }}</AppFormRow>
      </AppCard>

      <AppCard v-if="afterSale.items?.length">
        <h2 class="section-title">商品信息</h2>
        <div v-for="item in afterSale.items" :key="item.id" class="snapshot-item">
          <div>
            <strong>{{ item.productName }}</strong>
            <span>数量 x{{ item.quantity }}</span>
          </div>
          <PriceText :amount="item.totalAmount" size="sm" />
        </div>
      </AppCard>

      <AppCard>
        <h2 class="section-title">退款信息</h2>
        <AppFormRow label="退款类型">仅退款</AppFormRow>
        <AppFormRow label="退款金额">
          <PriceText :amount="afterSale.amount" size="md" color="var(--color-price)" />
        </AppFormRow>
        <AppFormRow label="申请时间">{{ formatDateTime(afterSale.createdAt) }}</AppFormRow>
        <AppFormRow v-if="afterSale.approvedAt" label="同意时间">{{ formatDateTime(afterSale.approvedAt) }}</AppFormRow>
        <AppFormRow v-if="afterSale.refundedAt" label="退款时间">{{ formatDateTime(afterSale.refundedAt) }}</AppFormRow>
        <AppFormRow v-if="afterSale.rejectReason" label="拒绝原因">{{ afterSale.rejectReason }}</AppFormRow>
      </AppCard>
    </div>

    <van-popup v-model:show="rejectSheetVisible" position="bottom" round>
      <div class="reject-sheet">
        <div class="reject-sheet__head">
          <h2>拒绝售后</h2>
          <button type="button" aria-label="关闭" @click="rejectSheetVisible = false">
            <van-icon name="cross" />
          </button>
        </div>
        <label>
          <span>拒绝原因</span>
          <textarea v-model="rejectReason" rows="4" placeholder="请说明不予退款的原因" />
        </label>
        <div class="reject-sheet__actions">
          <AppButton variant="ghost" :disabled="actionLoading === 'reject'" @click="rejectSheetVisible = false">取消</AppButton>
          <AppButton variant="danger" :loading="actionLoading === 'reject'" @click="reject">确认拒绝</AppButton>
        </div>
      </div>
    </van-popup>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AppCard from '@/components/AppCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import PriceText from '@/components/PriceText.vue'
import {
  approveLeaderAfterSale,
  completeLeaderAfterSaleRefund,
  getLeaderAfterSale,
  rejectLeaderAfterSale,
} from '@/api/leaderAfterSales'
import { formatDateTime, getOrderStatusText, getPayStatusText } from '@/utils'
import type { AfterSaleData } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const afterSale = ref<AfterSaleData | null>(null)
const actionLoading = ref<'approve' | 'reject' | 'refund' | ''>('')
const rejectSheetVisible = ref(false)
const rejectReason = ref('')

const afterSaleId = computed(() => route.params.id as string)
const actionable = computed(() => afterSale.value?.status === 'pending' || afterSale.value?.status === 'approved')

function statusText(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    approved: '已同意',
    rejected: '已拒绝',
    completed: '已退款',
  }
  return map[status] || status
}

function statusVariant(status: string): 'green' | 'orange' | 'gray' | 'red' {
  const map: Record<string, 'green' | 'orange' | 'gray' | 'red'> = {
    pending: 'orange',
    approved: 'green',
    rejected: 'red',
    completed: 'gray',
  }
  return map[status] || 'gray'
}

async function fetchDetail() {
  loading.value = true
  error.value = ''
  try {
    afterSale.value = await getLeaderAfterSale(afterSaleId.value)
  } catch (err) {
    error.value = (err as { message?: string }).message || '售后详情加载失败'
  } finally {
    loading.value = false
  }
}

async function approve() {
  try {
    await showConfirmDialog({ title: '同意退款', message: '确认同意该售后退款申请？' })
    actionLoading.value = 'approve'
    afterSale.value = await approveLeaderAfterSale(afterSaleId.value)
    showToast('已同意售后')
  } catch (err) {
    if ((err as string) !== 'cancel') showToast((err as { message?: string }).message || '操作失败')
  } finally {
    actionLoading.value = ''
  }
}

async function reject() {
  if (!rejectReason.value.trim()) {
    showToast('请输入拒绝原因')
    return
  }
  actionLoading.value = 'reject'
  try {
    afterSale.value = await rejectLeaderAfterSale(afterSaleId.value, { rejectReason: rejectReason.value.trim() })
    rejectSheetVisible.value = false
    showToast('已拒绝售后')
  } catch (err) {
    showToast((err as { message?: string }).message || '操作失败')
  } finally {
    actionLoading.value = ''
  }
}

async function completeRefund() {
  try {
    await showConfirmDialog({ title: '完成退款', message: '确认执行模拟退款并完成售后？' })
    actionLoading.value = 'refund'
    afterSale.value = await completeLeaderAfterSaleRefund(afterSaleId.value)
    showToast('退款已完成')
  } catch (err) {
    if ((err as string) !== 'cancel') showToast((err as { message?: string }).message || '操作失败')
  } finally {
    actionLoading.value = ''
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.after-sale-detail {
  padding: 10px 14px 86px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.detail-head span {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.detail-head h1,
.section-title,
.reject-sheet__head h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
}

.reason {
  margin: 10px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.section-title {
  margin-bottom: 10px;
  font-size: var(--font-size-md);
}

.snapshot-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
}

.snapshot-item:last-child {
  border-bottom: 0;
}

.snapshot-item div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.snapshot-item strong {
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
}

.snapshot-item span {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.reject-sheet {
  padding: 18px 16px calc(18px + var(--safe-area-bottom));
}

.reject-sheet__head,
.reject-sheet__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.reject-sheet__head button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: #f2f4f5;
  color: var(--color-text-secondary);
}

.reject-sheet label {
  display: block;
  margin: 14px 0;
}

.reject-sheet label span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.reject-sheet textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: #f8f9fa;
  color: var(--color-text-primary);
  font: inherit;
  resize: none;
}

.reject-sheet__actions :deep(.app-button) {
  flex: 1;
}
</style>
