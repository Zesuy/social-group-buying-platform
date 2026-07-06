<template>
  <PageLayout title="售后管理" show-back @back="goBack">
    <LoadingView v-if="firstLoading" />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="after-sales">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab v-for="tab in tabs" :key="tab.name" :title="tab.title" :name="tab.name" />
      </van-tabs>

      <div class="after-sales__list">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="!hasMore"
            finished-text="没有更多了"
            :error="error !== null"
            error-text="加载失败，点击重试"
            :immediate-check="false"
            @load="loadMore"
          >
            <article
              v-for="item in displayedItems"
              :key="item.id"
              class="after-sale-card"
              @click="goToDetail(item.id)"
            >
              <div class="after-sale-card__head">
                <div>
                  <strong>售后单 {{ item.id }}</strong>
                  <span>订单 {{ item.orderNo }}</span>
                </div>
                <AppStatusPill :variant="statusVariant(item.status)" size="sm">
                  {{ statusText(item.status) }}
                </AppStatusPill>
              </div>
              <p>{{ item.reason }}</p>
              <div class="after-sale-card__meta">
                <span>退款 <PriceText :amount="item.amount" size="sm" /></span>
                <span>{{ formatDateTime(item.createdAt) }}</span>
              </div>
            </article>

            <EmptyState v-if="displayedItems.length === 0 && !loading" description="暂无售后申请" />
          </van-list>
        </van-pull-refresh>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import PriceText from '@/components/PriceText.vue'
import { usePagination } from '@/composables/usePagination'
import { listLeaderAfterSales } from '@/api/leaderAfterSales'
import { formatDateTime } from '@/utils'
import type { AfterSaleData } from '@/types'

const router = useRouter()
const activeTab = ref('')

const tabs = [
  { name: '', title: '全部' },
  { name: 'pending', title: '待处理' },
  { name: 'approved', title: '已同意' },
  { name: 'rejected', title: '已拒绝' },
  { name: 'completed', title: '已退款' },
]

const {
  items,
  loading,
  refreshing,
  error,
  hasMore,
  initialized,
  load,
  refresh,
  loadMore,
  reset,
} = usePagination<AfterSaleData>(
  (page, pageSize) => listLeaderAfterSales({
    status: activeTab.value || undefined,
    page,
    pageSize,
  }),
)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)
const displayedItems = computed(() => activeTab.value
  ? items.value.filter((item) => item.status === activeTab.value)
  : items.value)

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

function onTabChange() {
  reset()
  load()
}

async function onRefresh() {
  await refresh()
  if (error.value) showToast('刷新失败')
}

function goBack() {
  router.back()
}

function goToDetail(id: string) {
  router.push(`/leader/after-sales/${id}`)
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.after-sales {
  min-height: 200px;
  background: var(--color-bg);
}

.after-sales__list {
  padding: 12px 14px 18px;
}

.after-sale-card {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.after-sale-card__head,
.after-sale-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.after-sale-card__head {
  align-items: flex-start;
}

.after-sale-card__head div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.after-sale-card__head strong {
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-weight: 900;
}

.after-sale-card__head span,
.after-sale-card__meta {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.after-sale-card p {
  margin: 10px 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.after-sale-card__meta {
  align-items: center;
}

.after-sale-card__meta span:first-child {
  color: var(--color-text-secondary);
}
</style>
