<template>
  <PageLayout show-tab-bar>
    <div class="index-shell">
      <div class="index-brand">
        <div class="index-brand__bar">
          <div class="index-brand__logo" aria-label="邻鲜团">
            <span class="index-brand__icon">邻</span>
            <span class="index-brand__name">邻鲜团</span>
          </div>
          <div class="index-brand__pill">
            <van-icon name="like" size="14" />
            <span>邻鲜团 小程序提供服务</span>
          </div>
        </div>
      </div>

      <div class="index-follow-banner">
        <span>关注公众号，收到活动和订单、物流通知</span>
        <button type="button" class="index-follow-banner__btn" @click="onWechatNoticeClick">
          关注
        </button>
        <button
          type="button"
          class="index-follow-banner__close"
          aria-label="关闭关注提示"
          @click="showToast('公众号提醒仅作占位展示')"
        >
          <van-icon name="cross" />
        </button>
      </div>

      <div class="index-tabs">
        <ChannelTabs
          :tabs="channels"
          :active="activeChannel"
          @change="onChannelChange"
        />
        <button type="button" class="index-coupon-entry" @click="onCouponClick">
          <van-icon name="coupon-o" size="16" />
          领券
          <van-icon name="arrow" size="12" />
        </button>
      </div>

      <CategoryChips
        :chips="categories"
        :active="activeCategory"
        @change="onCategoryChange"
      />

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
              v-for="item in visibleItems"
              :key="item.id"
              :item="item"
              @click="goToDetail(item.id)"
              @share="onShareClick"
              @subscribe="onSubscribeClick"
            />
            <EmptyState v-if="isEmpty" description="暂无团购活动" />
          </van-list>
        </van-pull-refresh>

        <LoadingView v-if="firstLoading" />
        <ErrorView
          v-if="showError"
          :message="error ?? undefined"
          @retry="initLoad"
        />
      </div>

      <button
        type="button"
        class="index-fab-cart floating-cart-entry"
        aria-label="购物车"
        @click="onCartClick"
      >
        <van-icon name="cart-o" size="28" color="var(--color-primary)" />
      </button>
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
  { key: 'recommend', label: '全部' },
  { key: 'video', label: '视频' },
  { key: 'newest', label: '最新' },
]
const activeChannel = ref('recommend')
function onChannelChange(key: string) {
  activeChannel.value = key
  showToast('功能开发中，敬请期待')
}

// ── 分类 ──
const categories = [
  { key: 'all', label: '综合' },
  { key: 'fresh', label: '生鲜' },
  { key: 'food', label: '食品' },
  { key: 'clothing', label: '服饰' },
  { key: 'beauty', label: '美妆' },
  { key: 'daily', label: '百货' },
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

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
}

function onCouponClick() {
  if (isFeatureDisabled('coupon')) {
    showToast('优惠券不在 MVP 范围内')
  }
}

function onShareClick() {
  showToast('分享能力仅作占位展示')
}

function onSubscribeClick() {
  showToast('请进入团长主页完成订阅')
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
const visibleItems = computed(() => items.value)

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
.index-shell {
  min-height: calc(100vh - var(--tabbar-height));
  background: var(--color-bg);
  padding-bottom: calc(var(--tabbar-height) + 14px + var(--safe-area-bottom));
}

.index-brand {
  background: #fff;
  padding: 10px 14px 0;
  padding-top: calc(10px + var(--safe-area-top));
}

.index-brand__bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
}

.index-brand__logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.index-brand__icon {
  width: 38px;
  height: 38px;
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
  background: #f5f6f7;
  padding: 7px 10px;
  font-size: var(--font-size-sm);
  color: #4f555f;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  min-width: 0;
  justify-content: center;
  line-height: 1.2;
}

.index-brand__pill span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.index-follow-banner {
  background: #fff5df;
  color: #f26b2c;
  margin: 0 14px 12px;
  padding: 10px 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  font-weight: 700;
  line-height: 1.35;
}

.index-follow-banner span:first-child {
  flex: 1;
  min-width: 0;
}

.index-follow-banner__btn {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: 8px;
  padding: 0 14px;
  min-height: 32px;
  font-size: var(--font-size-sm);
  font-weight: 800;
  flex-shrink: 0;
}

.index-follow-banner__close {
  width: 32px;
  min-width: 32px;
  min-height: 32px;
  border: 0;
  background: transparent;
  color: #b9a891;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.index-tabs {
  position: relative;
  background: #fff;
  margin-bottom: 10px;
}

.index-tabs :deep(.channel-tabs) {
  padding-right: 96px;
}

.index-coupon-entry {
  position: absolute;
  top: 0;
  right: 12px;
  min-height: 54px;
  border: 0;
  background: transparent;
  color: #ff7a2f;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: var(--font-size-md);
  font-weight: 800;
}

.index-shell :deep(.category-chips) {
  padding-top: 0;
  padding-bottom: 10px;
}

.index-feed {
  padding: 0 var(--spacing-md) var(--spacing-md);
}

.index-fab-cart {
  bottom: calc(var(--tabbar-height) + 16px + var(--safe-area-bottom));
  border: 0;
  opacity: 1;
  box-shadow: var(--shadow-float);
}
</style>
