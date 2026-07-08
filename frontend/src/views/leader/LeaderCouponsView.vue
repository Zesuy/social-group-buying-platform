<template>
  <PageLayout title="店铺优惠券" show-back @back="goBack">
    <template #action>
      <AppFixedActions single>
        <AppButton variant="primary" icon="coupon-o" @click="openCreate">新建新人券</AppButton>
      </AppFixedActions>
    </template>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchCoupons" />

    <div v-else class="coupon-page">
      <section class="coupon-summary">
        <div>
          <span>新人订阅券</span>
          <strong>{{ homepageCoupons.length }} 张会在主页弹出</strong>
        </div>
        <p>只有“新人订阅券”会在团长主页弹出；普通券仍只会在下单优惠券列表中使用。</p>
      </section>

      <div v-if="coupons.length > 0" class="coupon-list">
        <article
          v-for="coupon in coupons"
          :key="coupon.id"
          class="coupon-card"
          :class="{ 'coupon-card--disabled': coupon.status !== 'active' }"
        >
          <div class="coupon-card__main">
            <div class="coupon-card__amount">
              <span>{{ formatAmount(coupon.amount) }}</span>
              <small>{{ thresholdText(coupon.thresholdAmount) }}</small>
            </div>
            <div class="coupon-card__copy">
              <div class="coupon-card__title-row">
                <h2>{{ coupon.name }}</h2>
                <div class="coupon-card__badges">
                  <AppStatusPill :variant="coupon.claimCondition === 'new_subscriber' ? 'green' : 'gray'" size="sm">
                    {{ claimConditionText(coupon) }}
                  </AppStatusPill>
                  <AppStatusPill :variant="coupon.status === 'active' ? 'green' : 'gray'" size="sm">
                    {{ coupon.status === 'active' ? '启用中' : '已停用' }}
                  </AppStatusPill>
                </div>
              </div>
              <p>{{ claimConditionHint(coupon) }} · 每人限领 {{ coupon.perUserLimit }} 张</p>
              <small>{{ formatDateTime(coupon.startTime) }} 至 {{ formatDateTime(coupon.endTime) }}</small>
            </div>
          </div>
          <div class="coupon-card__footer">
            <span>已领 {{ coupon.claimedQuantity }}/{{ coupon.totalQuantity }}</span>
            <div class="coupon-card__actions">
              <AppButton
                v-if="coupon.status === 'active' && coupon.claimCondition !== 'new_subscriber'"
                variant="plain"
                @click="convertToNewSubscriber(coupon)"
              >
                转新人券
              </AppButton>
              <AppButton variant="ghost" @click="openEdit(coupon)">编辑</AppButton>
              <AppButton
                v-if="coupon.status === 'active'"
                variant="danger"
                @click="handleDisable(coupon)"
              >
                停用
              </AppButton>
            </div>
          </div>
        </article>
      </div>

      <EmptyState v-else description="还没有新人订阅券">
        <AppButton variant="primary" class="coupon-empty__button" @click="openCreate">新建新人券</AppButton>
      </EmptyState>
    </div>

    <van-popup
      v-model:show="formVisible"
      position="bottom"
      round
      :style="{ maxHeight: '86vh' }"
    >
      <div class="coupon-form-sheet">
        <div class="coupon-form-sheet__header">
          <div>
            <span>店铺新人订阅券</span>
            <h2>{{ editingCoupon ? '编辑优惠券' : '新建优惠券' }}</h2>
          </div>
          <button type="button" aria-label="关闭" @click="formVisible = false">
            <van-icon name="cross" />
          </button>
        </div>

        <div class="coupon-form">
          <label>
            <span>券名称</span>
            <input v-model="form.name" placeholder="例如：新客立减 10 元" />
          </label>
          <div class="coupon-form__grid">
            <label>
              <span>抵扣金额</span>
              <input v-model="form.amountYuan" inputmode="decimal" placeholder="10" />
            </label>
            <label>
              <span>使用门槛</span>
              <input v-model="form.thresholdYuan" inputmode="decimal" placeholder="0" />
            </label>
          </div>
          <div class="coupon-form__grid">
            <label>
              <span>总库存</span>
              <input v-model="form.totalQuantity" inputmode="numeric" placeholder="100" />
            </label>
            <label>
              <span>每人限领</span>
              <input v-model="form.perUserLimit" inputmode="numeric" placeholder="1" />
            </label>
          </div>
          <label>
            <span>开始时间</span>
            <input v-model="form.startTime" type="datetime-local" />
          </label>
          <label>
            <span>结束时间</span>
            <input v-model="form.endTime" type="datetime-local" />
          </label>
        </div>

        <div class="coupon-form-sheet__actions">
          <AppButton variant="ghost" :disabled="submitting" @click="formVisible = false">取消</AppButton>
          <AppButton variant="primary" :loading="submitting" @click="submitForm">保存</AppButton>
        </div>
      </div>
    </van-popup>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import {
  createStoreCoupon,
  disableStoreCoupon,
  listStoreCoupons,
  updateStoreCoupon,
} from '@/api/coupons'
import { useSmartNavigation, useUnsavedChangesGuard } from '@/composables'
import { formatAmount, formatDateTime } from '@/utils/format'
import type { StoreCouponData } from '@/types'

const { goBack } = useSmartNavigation('/leader/dashboard')
const formSnapshot = ref('')

const coupons = ref<StoreCouponData[]>([])
const loading = ref(true)
const error = ref('')
const formVisible = ref(false)
const submitting = ref(false)
const editingCoupon = ref<StoreCouponData | null>(null)

const form = reactive({
  name: '',
  amountYuan: '',
  thresholdYuan: '0',
  totalQuantity: '100',
  perUserLimit: '1',
  startTime: '',
  endTime: '',
})
useUnsavedChangesGuard({
  isDirty: () => formVisible.value && JSON.stringify(form) !== formSnapshot.value,
})

function markFormClean() {
  formSnapshot.value = JSON.stringify(form)
}

const activeCoupons = computed(() => coupons.value.filter((coupon) => coupon.status === 'active'))
const homepageCoupons = computed(() => activeCoupons.value.filter((coupon) => coupon.claimCondition === 'new_subscriber'))

function thresholdText(thresholdAmount: number): string {
  return thresholdAmount > 0 ? `满${formatAmount(thresholdAmount)}可用` : '无门槛'
}

function claimConditionText(coupon: StoreCouponData): string {
  return coupon.claimCondition === 'new_subscriber' ? '主页弹出' : '普通券'
}

function claimConditionHint(coupon: StoreCouponData): string {
  return coupon.claimCondition === 'new_subscriber'
    ? '新人订阅后领取'
    : '不会在团长主页弹出'
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
  form.name = '新客订阅立减券'
  form.amountYuan = '10'
  form.thresholdYuan = '0'
  form.totalQuantity = '100'
  form.perUserLimit = '1'
  form.startTime = toInputDateTime(now)
  form.endTime = toInputDateTime(end)
  markFormClean()
}

function validateForm(): boolean {
  if (!form.name.trim()) {
    showToast('请输入券名称')
    return false
  }
  if (yuanToCents(form.amountYuan) <= 0) {
    showToast('请输入有效抵扣金额')
    return false
  }
  if (Number(form.totalQuantity) <= 0 || !Number.isInteger(Number(form.totalQuantity))) {
    showToast('请输入有效库存')
    return false
  }
  if (Number(form.perUserLimit) <= 0 || !Number.isInteger(Number(form.perUserLimit))) {
    showToast('请输入有效限领数量')
    return false
  }
  if (!form.startTime || !form.endTime) {
    showToast('请选择有效期')
    return false
  }
  if (new Date(form.startTime).getTime() >= new Date(form.endTime).getTime()) {
    showToast('结束时间需晚于开始时间')
    return false
  }
  return true
}

function openCreate() {
  editingCoupon.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(coupon: StoreCouponData) {
  editingCoupon.value = coupon
  form.name = coupon.name
  form.amountYuan = String(coupon.amount / 100)
  form.thresholdYuan = String(coupon.thresholdAmount / 100)
  form.totalQuantity = String(coupon.totalQuantity)
  form.perUserLimit = String(coupon.perUserLimit)
  form.startTime = toInputDateTime(coupon.startTime)
  form.endTime = toInputDateTime(coupon.endTime)
  markFormClean()
  formVisible.value = true
}

async function submitForm() {
  if (!validateForm()) return

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      couponType: 'amount',
      claimCondition: 'new_subscriber',
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
    formVisible.value = false
    markFormClean()
    await fetchCoupons()
  } catch (err) {
    showToast((err as { message?: string }).message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDisable(coupon: StoreCouponData) {
  try {
    await showConfirmDialog({
      title: '停用优惠券',
      message: `停用后，用户不能再领取「${coupon.name}」。`,
    })
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

async function convertToNewSubscriber(coupon: StoreCouponData) {
  try {
    await showConfirmDialog({
      title: '转为新人订阅券',
      message: `转为新人订阅券后，「${coupon.name}」会在团长主页弹出，用户订阅后可领取。`,
    })
  } catch {
    return
  }

  try {
    await updateStoreCoupon(coupon.id, { claimCondition: 'new_subscriber' })
    showToast('已转为新人订阅券')
    await fetchCoupons()
  } catch (err) {
    showToast((err as { message?: string }).message || '转换失败')
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
  void fetchCoupons()
})
</script>

<style scoped>
.coupon-page {
  min-height: 100%;
  padding: 14px 14px calc(var(--actionbar-height) + var(--safe-area-bottom) + 14px);
  background: var(--color-bg);
}

.coupon-summary {
  padding: 14px;
  margin-bottom: 12px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.coupon-summary div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.coupon-summary span,
.coupon-summary p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.coupon-summary strong {
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.coupon-list {
  display: grid;
  gap: 12px;
}

.coupon-card {
  padding: 14px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.coupon-card--disabled {
  opacity: 0.62;
}

.coupon-card__main {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
}

.coupon-card__amount {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 76px;
  padding: 10px 8px;
  border-radius: 8px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  text-align: center;
}

.coupon-card__amount span {
  font-weight: 800;
  font-size: 20px;
  line-height: 1.2;
}

.coupon-card__amount small,
.coupon-card__copy small {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.4;
}

.coupon-card__copy {
  min-width: 0;
}

.coupon-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.coupon-card__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  max-width: 116px;
}

.coupon-card__copy h2 {
  min-width: 0;
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 700;
  line-height: 1.35;
}

.coupon-card__copy p {
  margin: 6px 0 4px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.coupon-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid var(--color-border);
}

.coupon-card__footer > span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.coupon-card__actions {
  display: flex;
  gap: 8px;
}

.coupon-empty__button {
  margin-top: 10px;
}

.coupon-form-sheet {
  padding: 16px 14px calc(var(--safe-area-bottom) + 14px);
  background: var(--color-bg-card);
}

.coupon-form-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.coupon-form-sheet__header span {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.coupon-form-sheet__header h2 {
  margin: 4px 0 0;
  color: var(--color-text-primary);
  font-size: 20px;
  line-height: 1.3;
}

.coupon-form-sheet__header button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 8px;
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
}

.coupon-form {
  display: grid;
  gap: 12px;
}

.coupon-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.coupon-form label {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.coupon-form label span {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.coupon-form input {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
  font: inherit;
}

.coupon-form-sheet__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

@media (max-width: 340px) {
  .coupon-card__main,
  .coupon-form__grid {
    grid-template-columns: 1fr;
  }

  .coupon-card__title-row,
  .coupon-card__footer,
  .coupon-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .coupon-card__badges {
    justify-content: flex-start;
    max-width: none;
  }

  .coupon-card__amount {
    min-height: 62px;
  }
}
</style>
