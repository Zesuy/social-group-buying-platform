<template>
  <PageLayout title="团购管理" show-back @back="goBack">
    <template #action>
      <div class="page-actions">
        <van-button type="primary" round @click="goToNew">新建团购</van-button>
      </div>
    </template>

    <LoadingView v-if="firstLoading" />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="group-buys-content">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab title="进行中" name="published" />
        <van-tab title="已结束" name="ended" />
      </van-tabs>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          :loading="loading"
          :finished="!hasMore"
          finished-text="没有更多了"
          :error="error !== null"
          error-text="加载失败，点击重试"
          :immediate-check="false"
          @load="loadMore"
        >
          <div
            v-for="gb in items"
            :key="gb.id"
            class="gb-card"
            @click="goToDetail(gb.id)"
          >
            <div class="gb-card__cover">
              <img
                v-if="gb.coverImageUrl"
                :src="gb.coverImageUrl"
                :alt="gb.title"
                class="gb-card__cover-img"
              />
              <van-icon v-else name="photo" :size="32" color="var(--color-text-hint)" />
            </div>
            <div class="gb-card__info">
              <div class="gb-card__title">{{ gb.title }}</div>
              <div class="gb-card__meta-row">
                <van-tag :type="gb.status === 'published' ? 'success' : 'default'">
                  {{ getGroupBuyStatusText(gb.status) }}
                </van-tag>
                <span class="gb-card__delivery">{{ getDeliveryTypeText(gb.deliveryType) }}</span>
              </div>
              <div class="gb-card__time" v-if="gb.endTime">
                结束时间：{{ formatDate(gb.endTime) }}
              </div>
            </div>
          </div>

          <EmptyState v-if="isEmpty" description="暂无关团购" />
        </van-list>
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { usePagination } from '@/composables/usePagination'
import { listMyGroupBuys } from '@/api/leaderGroupBuys'
import { getGroupBuyStatusText, getDeliveryTypeText } from '@/utils'
import type { GroupBuyManageData } from '@/types'

const router = useRouter()
const activeTab = ref('published')

const {
  items,
  loading,
  refreshing,
  error,
  hasMore,
  isEmpty,
  initialized,
  load,
  refresh,
  loadMore,
  reset,
} = usePagination<GroupBuyManageData>(
  (page, pageSize) => listMyGroupBuys(activeTab.value, page, pageSize),
)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)

function onTabChange() {
  reset()
  load()
}

async function onRefresh() {
  await refresh()
  if (error.value) {
    showToast('刷新失败')
  }
}

function goBack() {
  router.back()
}

function goToNew() {
  router.push('/leader/group-buys/new')
}

function goToDetail(id: string) {
  router.push(`/leader/group-buys/${id}`)
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-actions {
  padding: 8px 14px;
}

.group-buys-content {
  background: var(--color-bg);
  min-height: 200px;
  padding-top: 14px;
}

.gb-card {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: var(--color-bg-white);
  border-radius: 12px;
  margin: 0 14px 10px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.gb-card__cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.gb-card__cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gb-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gb-card__title {
  font-size: var(--font-size-md);
  font-weight: 900;
  color: var(--color-text-primary);
}

.gb-card__meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gb-card__delivery {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.gb-card__time {
  font-size: 12px;
  color: var(--color-text-hint);
}
</style>
