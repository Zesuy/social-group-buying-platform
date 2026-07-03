<template>
  <PageLayout show-tab-bar>
    <div class="index-shell">
      <section class="index-hero" aria-labelledby="index-hero-title">
        <div class="index-hero__top">
          <div class="index-brand__logo" aria-label="邻鲜团">
            <span class="index-brand__icon">邻</span>
            <span class="index-brand__name">邻鲜团</span>
          </div>
          <button type="button" class="index-hero__open" @click="goToOpenGroup">
            一键开团
          </button>
        </div>

        <div class="index-hero__body">
          <p class="index-hero__eyebrow">私域社群团购活动</p>
          <h1 id="index-hero-title">把一次开团，变成微信群里的订单</h1>
          <p class="index-hero__desc">
            看团长、看活动、看发货承诺。普通用户也能开店发布自己的团购。
          </p>
        </div>

        <div class="index-hero__stats" aria-label="平台当前活动概览">
          <div>
            <strong>{{ activeGroupBuyCount }}</strong>
            <span>个进行中团购</span>
          </div>
          <div>
            <strong>{{ totalSoldCount }}</strong>
            <span>人已跟团</span>
          </div>
          <div>
            <strong>{{ leaderCount }}</strong>
            <span>位团长在经营</span>
          </div>
        </div>
      </section>

      <div class="index-guide">
        <div class="index-guide__item">
          <span>1</span>
          团长开团
        </div>
        <div class="index-guide__item">
          <span>2</span>
          群里分享
        </div>
        <div class="index-guide__item">
          <span>3</span>
          集中履约
        </div>
      </div>

      <div class="index-section-head">
        <div>
          <h2>正在团购</h2>
          <p>按活动浏览，先看团长和履约承诺</p>
        </div>
        <button type="button" class="index-section-head__action" @click="goToOpenGroup">
          我要开团
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
              @leader="goToLeader(item.leader.id)"
            />
            <EmptyState v-if="isEmpty" description="暂无正在进行的团购，可先去一键开团" />
          </van-list>
        </van-pull-refresh>

        <LoadingView v-if="firstLoading" />
        <ErrorView
          v-if="showError"
          :message="error ?? undefined"
          @retry="initLoad"
        />
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import { listPublicGroupBuys } from '@/api/groupBuys'
import type { PublicGroupBuyItem } from '@/types'

const router = useRouter()

// ── 活动筛选：MVP 阶段只做本地高亮，不额外请求后端 ──
const categories = [
  { key: 'all', label: '全部团购' },
  { key: 'nearby', label: '本地履约' },
  { key: 'fresh', label: '生鲜水果' },
  { key: 'seasonal', label: '节令特产' },
  { key: 'repeat', label: '适合复购' },
]
const activeCategory = ref('all')
function onCategoryChange(key: string) {
  activeCategory.value = key
  if (key !== 'all') {
    showToast('当前仅高亮活动标签，真实筛选后续开放')
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
const activeGroupBuyCount = computed(() => items.value.filter(item => item.status !== 'ended').length)
const totalSoldCount = computed(() => items.value.reduce((total, item) => total + item.soldCount, 0))
const leaderCount = computed(() => new Set(items.value.map(item => item.leader.id)).size)

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

function goToLeader(id: string) {
  router.push(`/leaders/${id}`)
}

function goToOpenGroup() {
  router.push('/open-group')
}

onMounted(() => {
  initLoad()
})
</script>

<style scoped>
.index-shell {
  min-height: calc(100vh - var(--tabbar-height));
  background: var(--color-bg);
  padding: 12px 0 calc(var(--tabbar-height) + 14px + var(--safe-area-bottom));
}

.index-hero {
  margin: 0 12px 12px;
  padding: calc(14px + var(--safe-area-top)) 14px 14px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(232, 255, 242, 0.96), rgba(255, 255, 255, 0.96)),
    var(--color-bg-card);
  border: 1px solid rgba(16, 196, 104, 0.12);
  box-shadow: var(--shadow-card);
}

.index-hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.index-brand__logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.index-brand__icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
  font-size: var(--font-size-lg);
}

.index-brand__name {
  font-size: 22px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.index-hero__open {
  min-height: 38px;
  border: 0;
  border-radius: var(--radius-pill);
  padding: 0 14px;
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-md);
  font-weight: 800;
  white-space: nowrap;
}

.index-hero__open:active,
.index-section-head__action:active {
  transform: scale(0.96);
  opacity: 0.9;
}

.index-hero__body {
  margin-top: 18px;
}

.index-hero__eyebrow {
  margin: 0 0 6px;
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.index-hero h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 24px;
  line-height: 1.28;
  font-weight: 900;
}

.index-hero__desc {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.index-hero__stats {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.index-hero__stats div {
  flex: 1;
  min-width: 0;
  padding: 10px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
}

.index-hero__stats strong,
.index-hero__stats span {
  display: block;
}

.index-hero__stats strong {
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: 1.2;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.index-hero__stats span {
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.25;
}

.index-guide {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 0 12px 14px;
}

.index-guide__item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  min-height: 38px;
  border-radius: 12px;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 800;
  box-shadow: var(--shadow-card);
}

.index-guide__item span {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.index-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 12px 10px;
}

.index-section-head h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 20px;
  line-height: 1.25;
  font-weight: 900;
}

.index-section-head p {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.index-section-head__action {
  flex-shrink: 0;
  min-height: 36px;
  border: 1px solid rgba(16, 196, 104, 0.3);
  border-radius: var(--radius-pill);
  padding: 0 12px;
  background: #fff;
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.index-shell :deep(.category-chips) {
  padding-top: 0;
  padding-bottom: 12px;
}

.index-feed {
  padding: 0 var(--spacing-md) var(--spacing-md);
}
</style>
