<template>
  <PageLayout show-tab-bar>
    <!-- 品牌区（深绿色背景） -->
    <div class="index-brand">
      <div class="index-brand__bar">
        <div class="index-brand__logo">
          <span class="index-brand__icon">邻</span>
          <span class="index-brand__name">邻鲜团</span>
        </div>
        <div class="index-brand__pill">
          <van-icon name="like" size="14" />
          小程序提供服务
        </div>
      </div>
    </div>

    <!-- 关注公众号提醒 -->
    <div class="index-follow-banner">
      <span>关注公众号，收到活动和订单、物流通知</span>
      <span class="index-follow-banner__btn">关注</span>
    </div>

    <!-- 搜索占位（pill 样式） -->
    <div class="index-search marketplace-search">
      <van-icon name="search" size="16" color="var(--color-text-hint)" />
      <span>搜索商品或店铺</span>
    </div>

    <!-- 频道 Tab -->
    <ChannelTabs
      :tabs="channels"
      :active="activeChannel"
      @change="onChannelChange"
    />

    <!-- 分类 cs -->
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

    <!-- 灰态购物车浮动入口（非 MVP，只展示图标） -->
    <div class="index-fab-cart floating-cart-entry" @click="onCartClick">
      <van-icon name="cart-o" size="26" color="var(--color-primary)" />
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import ChannelTabs from '@/components/ChannelTabs.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import { isFeatureDisabled } from '@/utils/non-mvp'
import { listPublicGroupBuys } from '@/api/groupBuys'
import type { PublicGroupBuyItem } from '@/types'

const router = useRouter()

// ── 频道 Tab ──
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

// ── 分类 ──
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

// ── 购物车入口灰态 ──
function onCartClick() {
  if (isFeatureDisabled('cart')) {
    showToast('购物车功能即将开放')
    return
  }
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

function goToDetail(id: string) {
  router.push(`/group-buys/${id}`)
}

onMounted(() => {
  initLoad()
})
</script>

<style scoped>
/* ── 品牌区 ── */
.index-brand {
  background: #fff;
  padding: 10px 14px 0;
  padding-top: calc(var(--spacing-md) + var(--safe-area-top));
}

.index-brand__bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.index-brand__logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.index-brand__icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
  font-size: var(--font-size-lg);
}

.index-brand__name {
  font-size: 26px;
  font-weight: 900;
  color: var(--color-primary);
}

.index-brand__pill {
  border-radius: 99px;
  background: var(--color-bg);
  padding: 7px 12px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

/* ── 关注公众号横幅 ── */
.index-follow-banner {
  background: #fff5df;
  color: #f26b2c;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.index-follow-banner__btn {
  background: var(--color-primary);
  color: #fff;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

/* ── 搜索占位 ── */
.index-search {
  margin: var(--spacing-md);
}

/* ── 团购卡片区 ── */
.index-feed {
  padding: 0 var(--spacing-md) var(--spacing-md);
}

/* ── 购物车浮动入口（灰态） ── */
.index-fab-cart {
  bottom: calc(var(--tabbar-height) + 16px + var(--safe-area-bottom));
}
</style>
