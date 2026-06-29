<template>
  <PageLayout show-tab-bar>
    <!-- 品牌区 -->
    <div class="index-header">
      <div class="index-header__brand">
        <span class="index-header__title">团购商城</span>
      </div>
    </div>

    <!-- 提醒横幅 -->
    <ReminderBanner message="下单前请先查看团购说明和发货时间" />

    <!-- 频道 Tab 占位 -->
    <ChannelTabs
      :tabs="channels"
      :active="activeChannel"
      @change="onChannelChange"
    />

    <!-- 分类横滑占位 -->
    <CategoryChips
      :chips="categories"
      :active="activeCategory"
      @change="onCategoryChange"
    />

    <!-- 团购卡片流 -->
    <div class="index-feed">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="!hasMore"
          finished-text="没有更多了"
          :error="error !== null"
          error-text="加载失败，点击重试"
          @load="onLoadMore"
          @error="onErrorRetry"
        >
          <GroupBuyFeedCard
            v-for="item in items"
            :key="item.id"
            :item="item"
            @click="goToDetail(item.id)"
          />
          <EmptyState v-if="isEmpty" description="暂无团购活动" />
        </van-list>
      </van-pull-refresh>

      <!-- 首次加载 -->
      <LoadingView v-if="firstLoading" />
      <!-- 错误重试 -->
      <ErrorView
        v-if="showError"
        :message="error ?? undefined"
        @retry="initLoad"
      />
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import ReminderBanner from '@/components/ReminderBanner.vue'
import ChannelTabs from '@/components/ChannelTabs.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import { listPublicGroupBuys } from '@/api/groupBuys'
import type { PublicGroupBuyItem } from '@/types'

const router = useRouter()

// ── 频道 Tab（占位） ──
const channels = [
  { key: 'recommend', label: '推荐' },
  { key: 'newest', label: '最新' },
  { key: 'popular', label: '热门' },
]
const activeChannel = ref('recommend')
function onChannelChange(key: string) {
  activeChannel.value = key
  showToast('功能开发中，敬请期待')
}

// ── 分类（占位） ──
const categories = [
  { key: 'all', label: '全部' },
  { key: 'fruit', label: '水果' },
  { key: 'veg', label: '蔬菜' },
  { key: 'meat', label: '肉禽' },
  { key: 'seafood', label: '海鲜' },
  { key: 'snack', label: '零食' },
  { key: 'drink', label: '饮品' },
]
const activeCategory = ref('all')
function onCategoryChange(key: string) {
  activeCategory.value = key
  showToast('分类筛选功能开发中')
}

// ── 分页 ──
const items = ref<PublicGroupBuyItem[]>([])
const page = ref(1)
const loading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const initialized = ref(false)

const firstLoading = computed(() => !initialized.value && loading.value)
const isEmpty = computed(() => initialized.value && !error.value && items.value.length === 0)
const showError = computed(() => error.value && items.value.length === 0)

async function fetchList(p: number): Promise<boolean> {
  try {
    const data = await listPublicGroupBuys(p)
    if (p === 1) {
      items.value = data.items
    } else {
      items.value = [...items.value, ...data.items]
    }
    hasMore.value = data.hasMore
    page.value = p
    error.value = null
    return true
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
    return false
  }
}

async function initLoad() {
  loading.value = true
  await fetchList(1)
  loading.value = false
  initialized.value = true
}

async function onRefresh() {
  const ok = await fetchList(1)
  refreshing.value = false
  if (!ok) {
    showToast('刷新失败')
  }
}

async function onLoadMore() {
  if (!hasMore.value) return
  await fetchList(page.value + 1)
  loading.value = false
}

function onErrorRetry() {
  loading.value = true
  error.value = null
  initLoad()
}

function goToDetail(id: number) {
  router.push(`/group-buys/${id}`)
}

onMounted(() => {
  initLoad()
})
</script>

<style scoped>
.index-header {
  background: var(--color-primary);
  padding: var(--spacing-md) var(--spacing-lg);
  padding-top: calc(var(--spacing-md) + env(safe-area-inset-top, 0px));
}

.index-header__title {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: #fff;
}

.index-feed {
  padding: var(--spacing-md);
}
</style>
