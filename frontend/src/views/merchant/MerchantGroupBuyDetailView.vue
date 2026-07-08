<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>团购运营</p>
        <h1>团购详情</h1>
      </div>
      <RouterLink class="ghost-link" to="/merchant/group-buys">返回团购列表</RouterLink>
    </div>

    <LoadingView v-if="loading" text="正在加载团购详情..." />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchDetail" />

    <template v-else-if="detail">
      <section class="overview-panel">
        <ImageWithFallback
          :src="detail.groupBuy.coverImageUrl"
          :alt="detail.groupBuy.title"
          demo-kind="cover"
          width="176px"
          height="132px"
          radius="8px"
        />

        <div class="overview-main">
          <div class="overview-main__top">
            <span class="status-pill" :class="`status-pill--${detail.groupBuy.status}`">
              {{ getGroupBuyStatusText(detail.groupBuy.status) }}
            </span>
            <span class="visibility-chip">{{ visibilityText }}</span>
          </div>
          <h2>{{ detail.groupBuy.title }}</h2>
          <p>{{ detail.groupBuy.introduction || '暂无介绍，可在右侧编辑基础信息补充活动亮点。' }}</p>
          <div class="overview-actions">
            <button type="button" class="ghost-button" :disabled="shareLoading" @click="openShare">
              {{ shareLoading ? '生成中...' : '分享团购' }}
            </button>
            <RouterLink class="ghost-button" :to="`/group-buys/${detail.groupBuy.id}`">查看用户页</RouterLink>
            <button
              v-if="detail.groupBuy.status === 'published'"
              type="button"
              class="danger-button"
              :disabled="endLoading"
              @click="handleEnd"
            >
              {{ endLoading ? '处理中...' : '结束团购' }}
            </button>
          </div>
        </div>

        <dl class="metric-grid">
          <div>
            <dt>商品款数</dt>
            <dd>{{ detail.items.length }}</dd>
          </div>
          <div>
            <dt>已售件数</dt>
            <dd>{{ soldTotal }}</dd>
          </div>
          <div>
            <dt>剩余库存</dt>
            <dd>{{ remainingStock }}</dd>
          </div>
          <div>
            <dt>团购价区间</dt>
            <dd>{{ priceRangeText }}</dd>
          </div>
        </dl>
      </section>

      <div class="detail-layout">
        <main class="detail-main">
          <section class="content-panel">
            <div class="section-head">
              <div>
                <p>商品表现</p>
                <h2>本团商品和库存</h2>
              </div>
              <span>{{ stockUsageText }}</span>
            </div>

            <div class="item-list">
              <article v-for="item in detail.items" :key="item.id" class="item-row">
                <div class="item-row__main">
                  <strong>{{ item.displayName }}</strong>
                  <span>团购价 {{ formatAmount(item.groupPriceAmount) }}</span>
                </div>
                <div class="item-row__numbers">
                  <span>库存 {{ item.groupStock }}</span>
                  <span>已售 {{ item.soldCount }}</span>
                </div>
                <div class="stock-bar" aria-label="库存销售进度">
                  <i :style="{ width: `${soldPercent(item)}%` }" />
                </div>
              </article>
              <EmptyState v-if="detail.items.length === 0" image="goods-collect-o" description="暂无团购商品" />
            </div>
          </section>

          <section class="content-panel">
            <div class="section-head">
              <div>
                <p>活动内容</p>
                <h2>用户页展示素材</h2>
              </div>
              <span>{{ contentBlocks.length > 0 ? `${contentBlocks.length} 个内容块` : '使用基础介绍' }}</span>
            </div>

            <div v-if="contentBlocks.length > 0" class="content-blocks">
              <article
                v-for="(block, index) in contentBlocks"
                :key="`${block.type}-${index}`"
                class="content-block"
              >
                <span>{{ blockTypeText(block.type) }}</span>
                <h3 v-if="block.title">{{ block.title }}</h3>
                <p v-if="block.text">{{ block.text }}</p>
                <ul v-if="block.items?.length">
                  <li v-for="item in block.items" :key="item">{{ item }}</li>
                </ul>
                <ImageWithFallback
                  v-if="block.type === 'image' && block.url"
                  :src="block.url"
                  :alt="block.caption || detail.groupBuy.title"
                  demo-kind="cover"
                  width="100%"
                  height="180px"
                  radius="8px"
                />
                <small v-if="block.caption">{{ block.caption }}</small>
              </article>
            </div>
            <div v-else class="plain-intro">
              {{ detail.groupBuy.introduction || '暂无活动内容。新建团购时可通过结构化内容块补充推荐理由、配送说明和商品图片。' }}
            </div>
          </section>

          <section class="content-panel">
            <div class="section-head">
              <div>
                <p>履约信息</p>
                <h2>时间与配送</h2>
              </div>
            </div>
            <dl class="info-grid">
              <div>
                <dt>配送方式</dt>
                <dd>{{ getDeliveryTypeText(detail.groupBuy.deliveryType) }}</dd>
              </div>
              <div>
                <dt>发货说明</dt>
                <dd>{{ detail.groupBuy.shippingTime || '未填写' }}</dd>
              </div>
              <div>
                <dt>开始时间</dt>
                <dd>{{ formatDateTime(detail.groupBuy.startTime) || '未设置' }}</dd>
              </div>
              <div>
                <dt>结束时间</dt>
                <dd>{{ formatDateTime(detail.groupBuy.endTime) || '未设置' }}</dd>
              </div>
            </dl>
          </section>
        </main>

        <aside class="edit-panel">
          <div class="section-head">
            <div>
              <p>基础信息编辑</p>
              <h2>调整活动资料</h2>
            </div>
          </div>

          <label class="field">
            <span>标题</span>
            <input v-model="editForm.title" />
          </label>
          <label class="field">
            <span>介绍</span>
            <textarea v-model="editForm.introduction" rows="5" />
          </label>
          <div class="content-editor-field">
            <span>活动内容块</span>
            <ContentBlocksEditor v-model="editForm.contentBlocks" :disabled="editLoading" />
          </div>
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
          <button type="button" class="primary-button" :disabled="editLoading" @click="saveEdit">
            {{ editLoading ? '保存中...' : '保存基础信息' }}
          </button>
        </aside>
      </div>

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
import ContentBlocksEditor from '@/components/ContentBlocksEditor.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorView from '@/components/ErrorView.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import LoadingView from '@/components/LoadingView.vue'
import { endGroupBuy, getMyGroupBuy, getMyGroupBuyShareCard, updateMyGroupBuy } from '@/api/leaderGroupBuys'
import {
  buildShareTokenUrl,
  contentBlockTypeText,
  formatAmount,
  formatDateTime,
  getDeliveryTypeText,
  getGroupBuyStatusText,
  normalizeContentBlocks,
} from '@/utils'
import type { ContentBlockData, GroupBuyManageDetailData, GroupBuyManageItem, ShareCardData } from '@/types'

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
  contentBlocks: [] as ContentBlockData[],
})

const contentBlocks = computed<ContentBlockData[]>(() => detail.value?.groupBuy.contentBlocks ?? [])
const soldTotal = computed(() => detail.value?.items.reduce((sum, item) => sum + item.soldCount, 0) ?? 0)
const stockTotal = computed(() => detail.value?.items.reduce((sum, item) => sum + item.groupStock, 0) ?? 0)
const remainingStock = computed(() => Math.max(stockTotal.value - soldTotal.value, 0))
const stockUsageText = computed(() => stockTotal.value > 0 ? `已售 ${Math.round((soldTotal.value / stockTotal.value) * 100)}%` : '暂无库存')
const priceRangeText = computed(() => {
  const prices = detail.value?.items.map((item) => item.groupPriceAmount) ?? []
  if (prices.length === 0) return '暂无'
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  return min === max ? formatAmount(min) : `${formatAmount(min)} - ${formatAmount(max)}`
})
const visibilityText = computed(() => detail.value?.groupBuy.visibility === 'hidden' ? '隐藏分享' : '公开展示')
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
  editForm.contentBlocks = normalizeContentBlocks(groupBuy.contentBlocks)
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
      contentBlocks: normalizeContentBlocks(editForm.contentBlocks),
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

function soldPercent(item: GroupBuyManageItem): number {
  if (item.groupStock <= 0) return 0
  return Math.min(100, Math.round((item.soldCount / item.groupStock) * 100))
}

function blockTypeText(type: string): string {
  return contentBlockTypeText(type)
}

onMounted(fetchDetail)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
  padding-bottom: 32px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.page-head p,
.section-head p {
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
  font-family: inherit;
  cursor: pointer;
}

.ghost-link,
.ghost-button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

.primary-button {
  width: 100%;
  min-height: 42px;
  border: 0;
  background: #e9563f;
  color: #fff;
}

.danger-button {
  border: 0;
  background: #b42318;
  color: #fff;
}

.overview-panel,
.content-panel,
.edit-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.overview-panel {
  display: grid;
  grid-template-columns: 176px minmax(0, 1fr) minmax(320px, 440px);
  gap: 18px;
  align-items: stretch;
  padding: 18px;
}

.overview-main {
  min-width: 0;
}

.overview-main__top,
.overview-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.overview-main h2 {
  margin: 12px 0 8px;
  color: #111827;
  font-size: 24px;
  line-height: 1.25;
}

.overview-main p {
  margin: 0;
  max-width: 760px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.7;
}

.overview-actions {
  margin-top: 16px;
}

.status-pill,
.visibility-chip {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.status-pill {
  background: #e8f8ef;
  color: #087a3f;
}

.status-pill--draft {
  background: #f3f4f6;
  color: #4b5563;
}

.status-pill--ended {
  background: #fff1ed;
  color: #d63f2b;
}

.visibility-chip {
  background: #f9fafb;
  color: #6b7280;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.metric-grid div,
.info-grid div {
  padding: 12px;
  border-radius: 8px;
  background: #f9fafb;
}

.metric-grid dt,
.metric-grid dd,
.info-grid dt,
.info-grid dd {
  margin: 0;
}

.metric-grid dt,
.info-grid dt {
  color: #6b7280;
  font-size: 12px;
  font-weight: 900;
}

.metric-grid dd {
  margin-top: 6px;
  color: #111827;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.1;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.detail-main {
  display: grid;
  gap: 16px;
}

.content-panel,
.edit-panel {
  padding: 18px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 18px;
}

.section-head > span {
  color: #6b7280;
  font-size: 13px;
  font-weight: 800;
}

.item-list {
  display: grid;
  gap: 10px;
}

.item-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 10px 18px;
  padding: 14px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
}

.item-row__main,
.item-row__numbers {
  display: grid;
  gap: 4px;
}

.item-row__main strong {
  color: #111827;
  font-size: 15px;
}

.item-row__main span,
.item-row__numbers span {
  color: #6b7280;
  font-size: 13px;
}

.item-row__numbers {
  grid-template-columns: repeat(2, 1fr);
  align-content: center;
}

.stock-bar {
  grid-column: 1 / -1;
  height: 8px;
  border-radius: 999px;
  background: #f3f4f6;
  overflow: hidden;
}

.stock-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #e9563f;
}

.content-blocks {
  display: grid;
  gap: 12px;
}

.content-block {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
}

.content-block span {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: #e8f8ef;
  color: #087a3f;
  font-size: 12px;
  font-weight: 900;
}

.content-block h3,
.content-block p,
.content-block ul {
  margin: 0;
}

.content-block h3 {
  color: #111827;
  font-size: 15px;
}

.content-block p,
.plain-intro,
.content-block li,
.content-block small {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.65;
}

.content-block ul {
  padding-left: 18px;
}

.plain-intro {
  padding: 14px;
  border-radius: 8px;
  background: #f9fafb;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.info-grid dd {
  margin-top: 5px;
  color: #111827;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.5;
}

.edit-panel {
  position: sticky;
  top: 82px;
  display: grid;
  gap: 14px;
}

.field,
.upload-block,
.content-editor-field {
  display: grid;
  gap: 8px;
}

.field span,
.upload-block > span,
.content-editor-field > span {
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

@media (max-width: 1180px) {
  .overview-panel,
  .detail-layout,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .edit-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .metric-grid,
  .item-row,
  .field-grid,
  .delivery-grid {
    grid-template-columns: 1fr;
  }
}
</style>
