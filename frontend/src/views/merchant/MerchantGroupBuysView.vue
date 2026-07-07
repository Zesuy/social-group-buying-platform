<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>团购运营</p>
        <h1>团购管理</h1>
      </div>
      <RouterLink class="primary-link" to="/merchant/group-buys/new">
        <van-icon name="plus" />
        新建团购
      </RouterLink>
    </div>

    <div class="toolbar">
      <div class="segmented">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          :class="{ active: activeStatus === tab.value }"
          @click="setStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="loadGroupBuys" />

    <div v-else class="table-panel">
      <table class="merchant-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>状态</th>
            <th>可见性</th>
            <th>配送</th>
            <th>结束时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in groupBuys" :key="item.id">
            <td>
              <div class="title-cell">
                <ImageWithFallback
                  :src="item.coverImageUrl"
                  :alt="item.title"
                  demo-kind="cover"
                  width="48px"
                  height="48px"
                  radius="8px"
                />
                <div>
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.introduction || '暂无介绍' }}</span>
                </div>
              </div>
            </td>
            <td>{{ getGroupBuyStatusText(item.status) }}</td>
            <td>{{ item.visibility === 'hidden' ? '隐藏分享' : '公开展示' }}</td>
            <td>{{ getDeliveryTypeText(item.deliveryType) }}</td>
            <td>{{ formatDateTime(item.endTime) }}</td>
            <td>
              <div class="row-actions">
                <RouterLink :to="`/merchant/group-buys/${item.id}`">详情</RouterLink>
                <button v-if="item.status === 'published'" type="button" @click="openShare(item.id)">分享</button>
                <button v-if="item.status === 'published'" type="button" @click="handleEnd(item)">结束</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="groupBuys.length === 0" description="暂无团购" />
    </div>

    <GroupBuyShareSheet
      v-if="shareCard"
      v-model="shareSheetVisible"
      :payload="sharePayload"
      :share-url="shareUrl"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import ErrorView from '@/components/ErrorView.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import LoadingView from '@/components/LoadingView.vue'
import { endGroupBuy, getMyGroupBuyShareCard, listMyGroupBuys } from '@/api/leaderGroupBuys'
import { buildShareTokenUrl, formatDateTime, getDeliveryTypeText, getGroupBuyStatusText } from '@/utils'
import type { GroupBuyManageData, ShareCardData } from '@/types'

const statusTabs = [
  { label: '进行中', value: 'published' },
  { label: '草稿', value: 'draft' },
  { label: '已结束', value: 'ended' },
]

const loading = ref(true)
const error = ref('')
const activeStatus = ref('published')
const groupBuys = ref<GroupBuyManageData[]>([])
const shareSheetVisible = ref(false)
const shareCard = ref<ShareCardData | null>(null)

const shareUrl = computed(() => shareCard.value ? buildShareTokenUrl(shareCard.value.shareToken) : '')
const sharePayload = computed<GroupBuySharePayload>(() => ({
  title: shareCard.value?.title || '团购分享',
  coverImageUrl: shareCard.value?.coverImageUrl ?? null,
  minPriceAmount: shareCard.value?.minPriceAmount ?? null,
  maxPriceAmount: shareCard.value?.maxPriceAmount ?? null,
  storeName: shareCard.value?.storeName || '团长店铺',
  leaderName: shareCard.value?.leaderName || '团长',
  deliveryType: shareCard.value?.deliveryType ?? null,
  shippingTime: shareCard.value?.shippingTime ?? null,
}))

function setStatus(status: string) {
  activeStatus.value = status
  void loadGroupBuys()
}

async function loadGroupBuys() {
  loading.value = true
  error.value = ''
  try {
    const data = await listMyGroupBuys(activeStatus.value, 1, 50)
    groupBuys.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '团购列表加载失败'
  } finally {
    loading.value = false
  }
}

async function openShare(id: string) {
  try {
    shareCard.value = await getMyGroupBuyShareCard(id)
    shareSheetVisible.value = true
  } catch (err) {
    showToast((err as { message?: string }).message || '分享卡片生成失败')
  }
}

async function handleEnd(item: GroupBuyManageData) {
  try {
    await showConfirmDialog({ title: '结束团购', message: `确定结束「${item.title}」吗？结束后无法继续购买。` })
  } catch {
    return
  }

  try {
    await endGroupBuy(item.id)
    showToast('团购已结束')
    await loadGroupBuys()
  } catch (err) {
    showToast((err as { message?: string }).message || '操作失败')
  }
}

onMounted(loadGroupBuys)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head,
.toolbar {
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

.primary-link {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  border-radius: 8px;
  background: #e9563f;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.toolbar {
  justify-content: flex-start;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.segmented {
  display: inline-flex;
  padding: 3px;
  border-radius: 8px;
  background: #f3f4f6;
}

.segmented button {
  min-width: 72px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #4b5563;
  font-weight: 800;
}

.segmented button.active {
  background: #fff;
  color: #d63f2b;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.table-panel {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.merchant-table {
  width: 100%;
  min-width: 900px;
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
  background: #f9fafb;
  color: #6b7280;
  font-weight: 900;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 300px;
}

.title-cell strong,
.title-cell span {
  display: block;
}

.title-cell strong {
  color: #111827;
  font-size: 14px;
}

.title-cell span {
  max-width: 380px;
  margin-top: 3px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.row-actions a,
.row-actions button {
  border: 0;
  background: transparent;
  color: #d63f2b;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}
</style>
