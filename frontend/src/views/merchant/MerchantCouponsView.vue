<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>店铺营销</p>
        <h1>优惠券管理</h1>
      </div>
      <button type="button" class="primary-button" @click="openCreate">
        <van-icon name="plus" />
        新建优惠券
      </button>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchCoupons" />

    <div v-else class="content-grid">
      <section class="table-panel">
        <table class="merchant-table">
          <thead>
            <tr>
              <th>券名称</th>
              <th>面额</th>
              <th>领取条件</th>
              <th>库存</th>
              <th>有效期</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="coupon in coupons" :key="coupon.id">
              <td><strong>{{ coupon.name }}</strong></td>
              <td>{{ formatAmount(coupon.amount) }} / {{ thresholdText(coupon.thresholdAmount) }}</td>
              <td>{{ coupon.claimCondition === 'new_subscriber' ? '新人订阅券' : '普通券' }}</td>
              <td>{{ coupon.claimedQuantity }}/{{ coupon.totalQuantity }}</td>
              <td>{{ formatDateTime(coupon.startTime) }} 至 {{ formatDateTime(coupon.endTime) }}</td>
              <td>{{ coupon.status === 'active' ? '启用中' : '已停用' }}</td>
              <td>
                <div class="row-actions">
                  <button type="button" @click="openEdit(coupon)">编辑</button>
                  <button v-if="coupon.status === 'active'" type="button" @click="handleDisable(coupon)">停用</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="coupons.length === 0" description="暂无优惠券" />
      </section>

      <aside class="form-panel">
        <h2>{{ editingCoupon ? '编辑优惠券' : '新建优惠券' }}</h2>
        <label class="field">
          <span>券名称</span>
          <input v-model="form.name" placeholder="例如：新客立减 10 元" />
        </label>
        <div class="field-grid">
          <label class="field">
            <span>抵扣金额</span>
            <input v-model="form.amountYuan" inputmode="decimal" placeholder="10" />
          </label>
          <label class="field">
            <span>使用门槛</span>
            <input v-model="form.thresholdYuan" inputmode="decimal" placeholder="0" />
          </label>
        </div>
        <div class="field-grid">
          <label class="field">
            <span>总库存</span>
            <input v-model="form.totalQuantity" inputmode="numeric" />
          </label>
          <label class="field">
            <span>每人限领</span>
            <input v-model="form.perUserLimit" inputmode="numeric" />
          </label>
        </div>
        <div class="delivery-grid">
          <button
            type="button"
            :class="{ active: form.claimCondition === 'new_subscriber' }"
            @click="form.claimCondition = 'new_subscriber'"
          >
            新人订阅券
          </button>
          <button
            type="button"
            :class="{ active: form.claimCondition === 'general' }"
            @click="form.claimCondition = 'general'"
          >
            普通券
          </button>
        </div>
        <label class="field">
          <span>开始时间</span>
          <input v-model="form.startTime" type="datetime-local" />
        </label>
        <label class="field">
          <span>结束时间</span>
          <input v-model="form.endTime" type="datetime-local" />
        </label>
        <div class="form-actions">
          <button type="button" class="ghost-button" @click="resetForm">重置</button>
          <button type="button" class="primary-button" :disabled="submitting" @click="submitForm">
            {{ submitting ? '保存中...' : '保存优惠券' }}
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import ErrorView from '@/components/ErrorView.vue'
import LoadingView from '@/components/LoadingView.vue'
import { createStoreCoupon, disableStoreCoupon, listStoreCoupons, updateStoreCoupon } from '@/api/coupons'
import { useUnsavedChangesGuard } from '@/composables'
import { formatAmount, formatDateTime } from '@/utils'
import type { StoreCouponData } from '@/types'

const loading = ref(true)
const error = ref('')
const coupons = ref<StoreCouponData[]>([])
const submitting = ref(false)
const editingCoupon = ref<StoreCouponData | null>(null)
const initialSnapshot = ref('')

const form = reactive({
  name: '',
  amountYuan: '',
  thresholdYuan: '0',
  totalQuantity: '100',
  perUserLimit: '1',
  claimCondition: 'new_subscriber',
  startTime: '',
  endTime: '',
})
useUnsavedChangesGuard({
  isDirty: () => !loading.value && JSON.stringify(form) !== initialSnapshot.value,
})

function markClean() {
  initialSnapshot.value = JSON.stringify(form)
}

function thresholdText(amount: number): string {
  return amount > 0 ? `满${formatAmount(amount)}可用` : '无门槛'
}

function toInputDateTime(value: Date | string): string {
  const date = typeof value === 'string' ? new Date(value) : value
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toApiDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

function yuanToCents(value: string): number {
  return Math.round(Number(value || 0) * 100)
}

function resetForm() {
  const now = new Date()
  const end = new Date(now)
  end.setDate(now.getDate() + 30)
  editingCoupon.value = null
  form.name = '新客订阅立减券'
  form.amountYuan = '10'
  form.thresholdYuan = '0'
  form.totalQuantity = '100'
  form.perUserLimit = '1'
  form.claimCondition = 'new_subscriber'
  form.startTime = toInputDateTime(now)
  form.endTime = toInputDateTime(end)
  markClean()
}

function openCreate() {
  resetForm()
}

function openEdit(coupon: StoreCouponData) {
  editingCoupon.value = coupon
  form.name = coupon.name
  form.amountYuan = String(coupon.amount / 100)
  form.thresholdYuan = String(coupon.thresholdAmount / 100)
  form.totalQuantity = String(coupon.totalQuantity)
  form.perUserLimit = String(coupon.perUserLimit)
  form.claimCondition = coupon.claimCondition
  form.startTime = toInputDateTime(coupon.startTime)
  form.endTime = toInputDateTime(coupon.endTime)
  markClean()
}

function validateForm(): string | null {
  if (!form.name.trim()) return '请输入券名称'
  if (yuanToCents(form.amountYuan) <= 0) return '请输入有效抵扣金额'
  if (!Number.isInteger(Number(form.totalQuantity)) || Number(form.totalQuantity) <= 0) return '请输入有效库存'
  if (!Number.isInteger(Number(form.perUserLimit)) || Number(form.perUserLimit) <= 0) return '请输入有效限领数量'
  if (!form.startTime || !form.endTime) return '请选择有效期'
  if (new Date(form.startTime) >= new Date(form.endTime)) return '结束时间需晚于开始时间'
  return null
}

async function submitForm() {
  const errMsg = validateForm()
  if (errMsg) {
    showToast(errMsg)
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      couponType: 'amount',
      claimCondition: form.claimCondition,
      amount: yuanToCents(form.amountYuan),
      thresholdAmount: yuanToCents(form.thresholdYuan),
      totalQuantity: Number(form.totalQuantity),
      perUserLimit: Number(form.perUserLimit),
      startTime: toApiDateTime(form.startTime),
      endTime: toApiDateTime(form.endTime),
    }
    if (editingCoupon.value) {
      await updateStoreCoupon(editingCoupon.value.id, payload)
      showToast('优惠券已更新')
    } else {
      await createStoreCoupon(payload)
      showToast('优惠券已创建')
    }
    resetForm()
    markClean()
    await fetchCoupons()
  } catch (err) {
    showToast((err as { message?: string }).message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDisable(coupon: StoreCouponData) {
  try {
    await showConfirmDialog({ title: '停用优惠券', message: `停用后用户不能再领取「${coupon.name}」。` })
  } catch {
    return
  }
  try {
    await disableStoreCoupon(coupon.id)
    showToast('已停用')
    await fetchCoupons()
  } catch (err) {
    showToast((err as { message?: string }).message || '停用失败')
  }
}

async function fetchCoupons() {
  loading.value = true
  error.value = ''
  try {
    coupons.value = await listStoreCoupons()
  } catch (err) {
    error.value = (err as { message?: string }).message || '优惠券加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  resetForm()
  void fetchCoupons()
})
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
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

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.table-panel,
.form-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.table-panel {
  overflow-x: auto;
}

.merchant-table {
  width: 100%;
  min-width: 920px;
  border-collapse: collapse;
}

.merchant-table th,
.merchant-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  font-size: 13px;
}

.merchant-table th {
  background: #f9fafb;
  color: #6b7280;
  font-weight: 900;
}

.row-actions {
  display: flex;
  gap: 10px;
}

.row-actions button {
  border: 0;
  background: transparent;
  color: #d63f2b;
  font-weight: 900;
}

.form-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.form-panel h2 {
  margin: 0;
  font-size: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  color: #374151;
  font-size: 13px;
  font-weight: 900;
}

.field input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  outline: 0;
}

.field-grid,
.delivery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.delivery-grid button {
  min-height: 36px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-weight: 900;
}

.delivery-grid button.active {
  border-color: #e9563f;
  background: #fff1ed;
  color: #d63f2b;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.primary-button,
.ghost-button {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 900;
}

.primary-button {
  border: 0;
  background: #e9563f;
  color: #fff;
}

.ghost-button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
