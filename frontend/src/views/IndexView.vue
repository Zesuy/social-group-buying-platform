<template>
  <PageLayout show-tab-bar h5-constrained>
    <div class="index-shell">
      <section class="index-hero" aria-labelledby="index-hero-title">
        <div class="index-hero__top">
          <div class="index-brand__logo" aria-label="邻鲜团">
            <span class="index-brand__icon">邻</span>
            <span class="index-brand__name">邻鲜团</span>
          </div>
          <button type="button" class="index-hero__open" @click="goToOpenGroup">
            开团
          </button>
        </div>

        <div class="index-hero__body">
          <p class="index-hero__eyebrow">私域社群团购</p>
          <h1 id="index-hero-title">先看团长，再跟团</h1>
          <p class="index-hero__desc">
            正在进行的社区团购活动，集中收单后按约履约。
          </p>
        </div>

        <div class="index-hero__stats" aria-label="平台当前活动概览">
          <span>{{ activitySummaryText }}</span>
        </div>
      </section>

      <form class="index-search" role="search" @submit.prevent="onSearchSubmit">
        <van-icon name="search" class="index-search__icon" />
        <input
          v-model="searchInput"
          class="index-search__input"
          type="search"
          enterkeyhint="search"
          placeholder="搜索团购、店铺、团长"
        >
        <button
          v-if="searchInput"
          type="button"
          class="index-search__clear"
          aria-label="清空搜索"
          @click="clearSearch"
        >
          <van-icon name="cross" size="15" />
        </button>
        <button type="submit" class="index-search__submit" :disabled="loading">
          搜索
        </button>
      </form>

      <div class="index-section-head">
        <div>
          <h2>正在团购</h2>
          <p>按活动浏览，先看团长和履约承诺</p>
        </div>
      </div>

      <CategoryChips
        :chips="categories"
        :active="activeCategory"
        @change="onCategoryChange"
      />

      <section
        v-if="showLocationBanner"
        class="index-location"
        :class="{ 'index-location--active': hasUserLocation }"
      >
        <div class="index-location__icon" aria-hidden="true">
          <van-icon name="location-o" size="18" />
        </div>
        <div class="index-location__content">
          <strong>{{ locationTitle }}</strong>
          <span>{{ locationDescription }}</span>
        </div>
        <button
          type="button"
          class="index-location__action"
          :disabled="locating"
          @click="enableLocation"
        >
          {{ locationActionText }}
        </button>
        <button
          type="button"
          class="index-location__close"
          aria-label="关闭定位提示"
          @click="dismissLocationBanner"
        >
          <van-icon name="cross" size="16" />
        </button>
      </section>

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
            <template v-for="item in visibleItems" :key="item.id">
              <GroupBuyFeedCard
                :item="item"
                :subscribed="subscribedLeaderIds.has(item.leader.id)"
                :subscribe-loading="subscribingLeaderId === item.leader.id"
                @click="goToDetail(item.id)"
                @share="onShareClick(item)"
                @subscribe="onSubscribeClick(item)"
                @leader="goToLeader(item.leader.id)"
              />
            </template>
            <div v-if="isNearbyEmpty" class="index-nearby-empty">
              <div class="index-nearby-empty__icon" aria-hidden="true">
                <van-icon name="location-o" size="28" />
              </div>
              <strong>附近暂无</strong>
              <p>{{ nearbyEmptyDescription }}</p>
              <button type="button" class="index-nearby-empty__action" @click="showAllGroupBuys">
                查看其他团购
              </button>
            </div>
            <EmptyState v-else-if="isEmpty" :description="emptyDescription" />
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
    <GroupBuyShareSheet
      v-if="shareItem"
      v-model="shareSheetVisible"
      :payload="sharePayload"
      :share-url="shareUrl"
    />
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores'
import PageLayout from '@/components/PageLayout.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import { listPublicGroupBuys, type ListPublicGroupBuysParams } from '@/api/groupBuys'
import { subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { listMySubscriptions } from '@/api/subscriptions'
import {
  NEARBY_DISTANCE_METERS,
  buildGroupBuyShareUrl,
  matchesGroupBuyCategory,
  requestCurrentLocation,
  shareBySystem,
} from '@/utils'
import type { PublicGroupBuyItem } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

interface UserLocation {
  latitude: number
  longitude: number
}

// ── 活动筛选：分类关键词在前端筛选，附近和搜索使用后端参数 ──
const categories = [
  { key: 'all', label: '全部团购' },
  { key: 'nearby', label: '附近' },
  { key: 'fresh', label: '生鲜水果' },
  { key: 'seasonal', label: '节令特产' },
  { key: 'repeat', label: '适合复购' },
]
const activeCategory = ref('all')
async function onCategoryChange(key: string) {
  activeCategory.value = key
  if (key === 'nearby') {
    const ok = await ensureLocation()
    if (!ok) return
    await reloadList()
    return
  }
  if (hasUserLocation.value) {
    await reloadList()
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
const subscribedLeaderIds = ref(new Set<string>())
const subscribingLeaderId = ref<string | null>(null)
const searchInput = ref('')
const searchKeyword = ref('')
const userLocation = ref<UserLocation | null>(readSavedLocation())
const locating = ref(false)
const locationError = ref<string | null>(null)
const locationBannerDismissed = ref(readLocationBannerDismissed())
const shareSheetVisible = ref(false)
const shareItem = ref<PublicGroupBuyItem | null>(null)

const firstLoading = computed(() => !initialized.value && loading.value)
const isEmpty = computed(() => initialized.value && !error.value && visibleItems.value.length === 0)
const isNearbyEmpty = computed(() => isEmpty.value && activeCategory.value === 'nearby' && hasUserLocation.value)
const showError = computed(() => error.value && items.value.length === 0)
const visibleItems = computed(() => items.value.filter(matchesActiveCategory))
const activeGroupBuyCount = computed(() => items.value.filter(item => item.status !== 'ended').length)
const totalSoldCount = computed(() => items.value.reduce((total, item) => total + item.soldCount, 0))
const leaderCount = computed(() => new Set(items.value.map(item => item.leader.id)).size)
const activitySummaryText = computed(() => (
  `${activeGroupBuyCount.value} 个进行中团购 · ${totalSoldCount.value} 人已跟团 · ${leaderCount.value} 位团长在经营`
))
const hasUserLocation = computed(() => Boolean(userLocation.value))
const showLocationBanner = computed(() => !locationBannerDismissed.value)
const locationTitle = computed(() => (hasUserLocation.value ? '已按你的位置展示距离' : '开启定位，精确找周边团购'))
const locationDescription = computed(() => {
  if (locationError.value) return locationError.value
  if (hasUserLocation.value) return '附近会筛选 5km 内团购，列表按距离优先展示'
  return '允许定位后，只在首页计算距离和附近筛选'
})
const locationActionText = computed(() => {
  if (locating.value) return '定位中'
  return hasUserLocation.value ? '重新定位' : '开启定位'
})
const emptyDescription = computed(() => {
  const label = categories.find(category => category.key === activeCategory.value)?.label ?? '团购'
  if (searchKeyword.value) return `暂无“${searchKeyword.value}”相关团购`
  if (activeCategory.value === 'nearby' && !hasUserLocation.value) return '开启定位后查看附近团购'
  return activeCategory.value === 'all' ? '暂无正在进行的团购，可先去一键开团' : `暂无${label}团购`
})
const nearbyEmptyDescription = '5km 内暂时没有可履约团购，先看看其他团长正在开团的活动。'
const shareUrl = computed(() => shareItem.value ? buildGroupBuyShareUrl(shareItem.value.id) : '')
const sharePayload = computed<GroupBuySharePayload>(() => ({
  title: shareItem.value?.title || '团购分享',
  coverImageUrl: shareItem.value?.coverImageUrl ?? null,
  minPriceAmount: shareItem.value?.minPriceAmount ?? null,
  maxPriceAmount: shareItem.value?.minPriceAmount ?? null,
  storeName: shareItem.value?.store.name || '团长店铺',
  leaderName: shareItem.value?.leader.displayName || '团长',
  deliveryType: null,
  shippingTime: null,
}))

function matchesActiveCategory(item: PublicGroupBuyItem): boolean {
  return matchesGroupBuyCategory(item, activeCategory.value)
}

function isValidCoordinate(latitude: number, longitude: number): boolean {
  return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
}

function readSavedLocation(): UserLocation | null {
  try {
    const raw = window.localStorage.getItem('index:user-location')
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<UserLocation>
    if (typeof parsed.latitude !== 'number' || typeof parsed.longitude !== 'number') return null
    if (!isValidCoordinate(parsed.latitude, parsed.longitude)) return null
    return { latitude: parsed.latitude, longitude: parsed.longitude }
  } catch {
    return null
  }
}

function saveLocation(location: UserLocation) {
  window.localStorage.setItem('index:user-location', JSON.stringify(location))
}

function readLocationBannerDismissed(): boolean {
  try {
    return window.localStorage.getItem('index:location-banner-dismissed') === '1'
  } catch {
    return false
  }
}

function dismissLocationBanner() {
  locationBannerDismissed.value = true
  try {
    window.localStorage.setItem('index:location-banner-dismissed', '1')
  } catch {
    // 本地偏好保存失败不影响继续浏览。
  }
}

async function enableLocation(): Promise<boolean> {
  locating.value = true
  locationError.value = null
  try {
    const location = await requestCurrentLocation({ enableHighAccuracy: false, timeout: 8000, maximumAge: 5 * 60 * 1000 })
    userLocation.value = location
    saveLocation(location)
    locationBannerDismissed.value = false
    window.localStorage.removeItem('index:location-banner-dismissed')
    await reloadList()
    showToast('已按当前位置更新团购距离')
    return true
  } catch (err) {
    const locationErr = err as { message?: string }
    locationError.value = locationErr.message || '定位失败，请稍后重试'
    showToast(locationError.value)
    return false
  } finally {
    locating.value = false
  }
}

async function ensureLocation(): Promise<boolean> {
  if (hasUserLocation.value) return true
  return enableLocation()
}

function buildListParams(p: number): ListPublicGroupBuysParams {
  const params: ListPublicGroupBuysParams = { page: p, pageSize: 20 }
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    params.keyword = keyword
  }
  if (!userLocation.value) return params

  params.latitude = userLocation.value.latitude
  params.longitude = userLocation.value.longitude
  params.sort = 'distance'
  if (activeCategory.value === 'nearby') {
    params.maxDistanceMeters = NEARBY_DISTANCE_METERS
  }
  return params
}

async function onSearchSubmit(): Promise<void> {
  const keyword = searchInput.value.trim()
  if (keyword === searchKeyword.value && initialized.value) return
  searchKeyword.value = keyword
  await reloadList()
}

function clearSearch(): void {
  const hadKeyword = Boolean(searchKeyword.value)
  searchInput.value = ''
  searchKeyword.value = ''
  if (hadKeyword) {
    void reloadList()
  }
}

async function initSubscriptions(): Promise<void> {
  if (!authStore.isLoggedIn) return
  try {
    const data = await listMySubscriptions()
    subscribedLeaderIds.value = new Set(
      data.items
        .filter(item => item.status !== 'canceled')
        .map(item => item.leaderId),
    )
  } catch {
    subscribedLeaderIds.value = new Set()
  }
}

async function onSubscribeClick(item: PublicGroupBuyItem): Promise<void> {
  if (!authStore.isLoggedIn) {
    router.push('/login?redirect=/')
    return
  }
  const leaderId = item.leader.id
  if (subscribingLeaderId.value) return

  subscribingLeaderId.value = leaderId
  try {
    const nextSet = new Set(subscribedLeaderIds.value)
    if (nextSet.has(leaderId)) {
      await unsubscribeLeader(leaderId)
      nextSet.delete(leaderId)
      showToast('已取消订阅')
    } else {
      await subscribeLeader(leaderId, 'indexFeed')
      nextSet.add(leaderId)
      showToast('订阅成功')
    }
    subscribedLeaderIds.value = nextSet
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    if (apiErr.code === 'SUBSCRIPTION_EXISTS') {
      const nextSet = new Set(subscribedLeaderIds.value)
      nextSet.add(leaderId)
      subscribedLeaderIds.value = nextSet
      showToast('已订阅')
    } else {
      showToast(apiErr.message || '操作失败')
    }
  } finally {
    subscribingLeaderId.value = null
  }
}

async function onShareClick(item: PublicGroupBuyItem): Promise<void> {
  shareItem.value = item
  const url = buildGroupBuyShareUrl(item.id)
  const result = await shareBySystem({
    title: item.title,
    text: `${item.store.name}的团购正在进行`,
    url,
  })
  if (result === 'shared' || result === 'aborted') return
  shareSheetVisible.value = true
}

async function fetchList(p: number): Promise<boolean> {
  try {
    const data = await listPublicGroupBuys(buildListParams(p))
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

async function reloadList(): Promise<void> {
  loading.value = true
  await fetchList(1)
  loading.value = false
  initialized.value = true
}

async function initLoad() {
  await reloadList()
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

async function showAllGroupBuys() {
  activeCategory.value = 'all'
  await reloadList()
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
  initSubscriptions()
})
</script>

<style scoped>
.index-shell {
  min-height: calc(100vh - var(--tabbar-height));
  background: var(--color-bg);
  padding: 8px 0 calc(var(--tabbar-height) + 14px + var(--safe-area-bottom));
}

.index-hero {
  margin: 0 12px 8px;
  padding: calc(10px + var(--safe-area-top)) 12px 10px;
  border-radius: 14px;
  background: var(--color-bg-card);
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
  width: 32px;
  height: 32px;
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
  font-size: 19px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.index-hero__open {
  min-height: 32px;
  border: 0;
  border-radius: var(--radius-pill);
  padding: 0 14px;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  font-weight: 800;
  white-space: nowrap;
}

.index-hero__open:active,
.index-section-head__action:active {
  transform: scale(0.96);
  opacity: 0.9;
}

.index-hero__body {
  margin-top: 8px;
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
  font-size: 20px;
  line-height: 1.24;
  font-weight: 900;
}

.index-hero__desc {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.index-hero__stats {
  margin-top: 8px;
  min-height: 28px;
  border-radius: var(--radius-pill);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  display: flex;
  align-items: center;
  padding: 0 10px;
  font-size: var(--font-size-xs);
  font-weight: 800;
  line-height: 1.2;
}

.index-hero__stats span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.index-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 2px 12px 6px;
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

.index-search {
  min-height: 42px;
  margin: 0 12px 8px;
  padding: 4px 5px 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  display: flex;
  align-items: center;
  gap: 8px;
}

.index-search__icon {
  flex-shrink: 0;
  color: var(--color-text-hint);
  font-size: 18px;
}

.index-search__input {
  flex: 1;
  min-width: 0;
  height: 32px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  line-height: 1.4;
}

.index-search__input::placeholder {
  color: var(--color-text-hint);
}

.index-search__clear {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-hint);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.index-search__submit {
  min-width: 58px;
  min-height: 34px;
  border: 0;
  border-radius: var(--radius-pill);
  padding: 0 12px;
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 800;
  flex-shrink: 0;
}

.index-search__submit:disabled {
  opacity: 0.58;
}

.index-search__clear:active,
.index-search__submit:active {
  transform: scale(0.96);
}

.index-shell :deep(.category-chips) {
  padding-top: 0;
  padding-bottom: 8px;
}

.index-location {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 12px 8px;
  padding: 7px 8px 7px 10px;
  border: 1px solid rgba(16, 196, 104, 0.16);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.index-location--active {
  border-color: rgba(16, 196, 104, 0.32);
  background: linear-gradient(135deg, rgba(232, 255, 242, 0.92), #fff);
}

.index-location__icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.index-location__content {
  display: grid;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.index-location__content strong {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  line-height: 1.3;
  font-weight: 800;
}

.index-location__content span {
  display: none;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.35;
}

.index-location__action {
  min-height: 32px;
  flex-shrink: 0;
  border: 1px solid rgba(16, 196, 104, 0.28);
  border-radius: var(--radius-pill);
  padding: 0 10px;
  background: #fff;
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.index-location__action:disabled {
  opacity: 0.62;
}

.index-location__close {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-hint);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.index-location__close:active {
  background: var(--color-bg-subtle);
}

.index-feed {
  padding: 0 var(--spacing-md) var(--spacing-md);
}

.index-nearby-empty {
  min-height: 238px;
  margin: 0 0 12px;
  padding: 28px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.index-nearby-empty__icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.index-nearby-empty strong {
  margin-top: 14px;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  line-height: 1.35;
  font-weight: 900;
}

.index-nearby-empty p {
  max-width: 260px;
  margin: 6px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.index-nearby-empty__action {
  min-height: 44px;
  margin-top: 18px;
  padding: 0 18px;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-md);
  font-weight: 800;
}

.index-nearby-empty__action:active {
  transform: scale(0.97);
  opacity: 0.9;
}

@media (max-width: 360px) {
  .index-location {
    align-items: center;
    gap: 8px;
  }

  .index-location__icon {
    width: 28px;
    height: 28px;
  }

  .index-location__action {
    min-height: 30px;
    padding: 0 8px;
  }

  .index-location__close {
    width: 28px;
  }

  .index-search {
    padding-left: 10px;
    gap: 6px;
  }

  .index-search__submit {
    min-width: 50px;
    padding: 0 10px;
  }
}
</style>
