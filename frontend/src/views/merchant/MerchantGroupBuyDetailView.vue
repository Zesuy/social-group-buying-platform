<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>团购运营</p>
        <h1>团购详情</h1>
      </div>
      <RouterLink class="ghost-link" to="/merchant/group-buys">返回团购列表</RouterLink>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchDetail" />

    <template v-else-if="detail">
      <div class="detail-grid">
        <section class="form-panel">
          <div class="detail-head">
            <ImageWithFallback
              :src="detail.groupBuy.coverImageUrl"
              :alt="detail.groupBuy.title"
              demo-kind="cover"
              width="88px"
              height="88px"
              radius="8px"
            />
            <div>
              <span class="status-pill">{{ getGroupBuyStatusText(detail.groupBuy.status) }}</span>
              <h2>{{ detail.groupBuy.title }}</h2>
              <p>{{ detail.groupBuy.introduction || '暂无介绍' }}</p>
            </div>
          </div>
          <dl class="meta-grid">
            <div>
              <dt>配送方式</dt>
              <dd>{{ getDeliveryTypeText(detail.groupBuy.deliveryType) }}</dd>
            </div>
            <div>
              <dt>开始时间</dt>
              <dd>{{ formatDateTime(detail.groupBuy.startTime) }}</dd>
            </div>
            <div>
              <dt>结束时间</dt>
              <dd>{{ formatDateTime(detail.groupBuy.endTime) }}</dd>
            </div>
            <div>
              <dt>发货说明</dt>
              <dd>{{ detail.groupBuy.shippingTime || '未填写' }}</dd>
            </div>
          </dl>
          <div class="action-row">
            <button type="button" class="ghost-button" :disabled="shareLoading" @click="openShare">分享</button>
            <button type="button" class="ghost-button" @click="startEdit">编辑基础信息</button>
            <button
              v-if="detail.groupBuy.status === 'published'"
              type="button"
              class="danger-button"
              :disabled="endLoading"
              @click="handleEnd"
            >
              结束团购
            </button>
          </div>
        </section>

        <section class="form-panel">
          <h2>基础信息编辑</h2>
          <label class="field">
            <span>标题</span>
            <input v-model="editForm.title" />
          </label>
          <label class="field">
            <span>介绍</span>
            <textarea v-model="editForm.introduction" rows="5" />
          </label>
          <div class="upload-block">
            <span>封面</span>
            <ImageUploader
              v-model="editForm.coverImageUrl"
              :disabled="editLoading"
              :preview-alt="editForm.title || '团购封面'"
              demo-kind="cover"
              :show-url-input="false"
              :show-hint="false"
              button-label="更换封面"
            />
          </div>
          <div class="delivery-grid">
            <button
              v-for="option in deliveryOptions"
              :key="option.value"
              type="button"
              :class="{ active: editForm.deliveryType === option.value }"
              @click="editForm.deliveryType = option.value"
            >
              {{ option.label }}
            </button>
          </div>
          <label class="field">
            <span>发货说明</span>
            <input v-model="editForm.shippingTime" />
          </label>
          <div class="field-grid">
            <label class="field">
              <span>开始时间</span>
              <input v-model="editForm.startTime" type="datetime-local" />
            </label>
            <label class="field">
              <span>结束时间</span>
              <input v-model="editForm.endTime" type="datetime-local" />
            </label>
          </div>
          <div class="action-row action-row--right">
            <button type="button" class="primary-button" :disabled="editLoading" @click="saveEdit">
              {{ editLoading ? '保存中...' : '保存基础信息' }}
            </button>
          </div>
        </section>
      </div>

      <section class="table-panel">
        <table class="merchant-table">
          <thead>
            <tr>
              <th>商品</th>
              <th>团购价</th>
              <th>团购库存</th>
              <th>已售</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detail.items" :key="item.id">
              <td><strong>{{ item.displayName }}</strong></td>
              <td>{{ formatAmount(item.groupPriceAmount) }}</td>
              <td>{{ item.groupStock }}</td>
              <td>{{ item.soldCount }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <GroupBuyShareSheet
        v-if="shareCard"
        v-model="shareSheetVisible"
        :payload="sharePayload"
        :share-url="shareUrl"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import ErrorView from '@/components/ErrorView.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import LoadingView from '@/components/LoadingView.vue'
import { endGroupBuy, getMyGroupBuy, getMyGroupBuyShareCard, updateMyGroupBuy } from '@/api/leaderGroupBuys'
import { buildShareTokenUrl, formatAmount, formatDateTime, getDeliveryTypeText, getGroupBuyStatusText } from '@/utils'
import type { GroupBuyManageDetailData, ShareCardData } from '@/types'

const route = useRoute()

const deliveryOptions = [
  { value: 'express', label: '快递配送' },
  { value: 'pickup', label: '到店自提' },
  { value: 'local_delivery', label: '同城配送' },
]

const loading = ref(true)
const error = ref('')
const detail = ref<GroupBuyManageDetailData | null>(null)
const editLoading = ref(false)
const endLoading = ref(false)
const shareLoading = ref(false)
const shareSheetVisible = ref(false)
const shareCard = ref<ShareCardData | null>(null)

const editForm = reactive({
  title: '',
  introduction: '',
  coverImageUrl: '',
  deliveryType: 'express',
  shippingTime: '',
  startTime: '',
  endTime: '',
})

const shareUrl = computed(() => shareCard.value ? buildShareTokenUrl(shareCard.value.shareToken) : '')
const sharePayload = computed<GroupBuySharePayload>(() => ({
  title: shareCard.value?.title || detail.value?.groupBuy.title || '团购分享',
  coverImageUrl: shareCard.value?.coverImageUrl ?? detail.value?.groupBuy.coverImageUrl ?? null,
  minPriceAmount: shareCard.value?.minPriceAmount ?? null,
  maxPriceAmount: shareCard.value?.maxPriceAmount ?? null,
  storeName: shareCard.value?.storeName || '团长店铺',
  leaderName: shareCard.value?.leaderName || '团长',
  deliveryType: shareCard.value?.deliveryType ?? detail.value?.groupBuy.deliveryType ?? null,
  shippingTime: shareCard.value?.shippingTime ?? detail.value?.groupBuy.shippingTime ?? null,
}))

function toInputDateTime(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 16)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toISOWithTZ(value: string): string | null {
  if (!value) return null
  return `${value.length === 16 ? `${value}:00` : value}+08:00`
}

function startEdit() {
  if (!detail.value) return
  const groupBuy = detail.value.groupBuy
  editForm.title = groupBuy.title
  editForm.introduction = groupBuy.introduction || ''
  editForm.coverImageUrl = groupBuy.coverImageUrl || ''
  editForm.deliveryType = groupBuy.deliveryType
  editForm.shippingTime = groupBuy.shippingTime || ''
  editForm.startTime = toInputDateTime(groupBuy.startTime)
  editForm.endTime = toInputDateTime(groupBuy.endTime)
}

async function fetchDetail() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getMyGroupBuy(route.params.id as string)
    startEdit()
  } catch (err) {
    error.value = (err as { message?: string }).message || '团购详情加载失败'
  } finally {
    loading.value = false
  }
}

async function saveEdit() {
  if (!detail.value) return
  if (!editForm.title.trim()) {
    showToast('请输入标题')
    return
  }
  editLoading.value = true
  try {
    await updateMyGroupBuy(detail.value.groupBuy.id, {
      title: editForm.title.trim(),
      introduction: editForm.introduction.trim() || null,
      coverImageUrl: editForm.coverImageUrl || null,
      deliveryType: editForm.deliveryType,
      shippingTime: editForm.shippingTime.trim() || null,
      startTime: toISOWithTZ(editForm.startTime),
      endTime: toISOWithTZ(editForm.endTime),
    })
    showToast('保存成功')
    await fetchDetail()
  } catch (err) {
    showToast((err as { message?: string }).message || '保存失败')
  } finally {
    editLoading.value = false
  }
}

async function handleEnd() {
  if (!detail.value) return
  try {
    await showConfirmDialog({ title: '结束团购', message: `确定结束「${detail.value.groupBuy.title}」吗？` })
  } catch {
    return
  }
  endLoading.value = true
  try {
    await endGroupBuy(detail.value.groupBuy.id)
    showToast('团购已结束')
    await fetchDetail()
  } catch (err) {
    showToast((err as { message?: string }).message || '操作失败')
  } finally {
    endLoading.value = false
  }
}

async function openShare() {
  if (!detail.value) return
  shareLoading.value = true
  try {
    shareCard.value = await getMyGroupBuyShareCard(detail.value.groupBuy.id)
    shareSheetVisible.value = true
  } catch (err) {
    showToast((err as { message?: string }).message || '分享卡片生成失败')
  } finally {
    shareLoading.value = false
  }
}

onMounted(fetchDetail)
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

.ghost-link,
.ghost-button,
.primary-button,
.danger-button {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.ghost-link,
.ghost-button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

.primary-button {
  border: 0;
  background: #e9563f;
  color: #fff;
}

.danger-button {
  border: 0;
  background: #b42318;
  color: #fff;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 16px;
  align-items: start;
}

.form-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.form-panel h2 {
  margin: 0;
  font-size: 16px;
}

.detail-head {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.detail-head h2,
.detail-head p {
  margin: 6px 0 0;
}

.detail-head p {
  color: #6b7280;
  line-height: 1.55;
}

.status-pill {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 8px;
  border-radius: 999px;
  background: #e8f8ef;
  color: #087a3f;
  font-size: 12px;
  font-weight: 900;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.meta-grid div {
  padding: 12px;
  border-radius: 8px;
  background: #f9fafb;
}

.meta-grid dt,
.meta-grid dd {
  margin: 0;
}

.meta-grid dt {
  color: #6b7280;
  font-size: 12px;
  font-weight: 900;
}

.meta-grid dd {
  margin-top: 5px;
  color: #111827;
  font-weight: 900;
}

.field,
.upload-block {
  display: grid;
  gap: 8px;
}

.field span,
.upload-block > span {
  color: #374151;
  font-size: 13px;
  font-weight: 900;
}

.field input,
.field textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: 0;
}

.field textarea {
  resize: vertical;
  line-height: 1.55;
}

.field-grid,
.delivery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.delivery-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-row--right {
  justify-content: flex-end;
}

.table-panel {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.merchant-table {
  width: 100%;
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

@media (max-width: 1180px) {
  .detail-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
