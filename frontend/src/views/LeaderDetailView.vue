<template>
  <PageLayout>
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="fetchData" />

    <template v-if="leaderData && !loading">
      <div class="leader-home">
        <div class="leader-topbar">
          <button type="button" class="leader-topbar__back" aria-label="返回" @click="goBack">
            <van-icon name="arrow-left" size="22" />
          </button>
          <div class="leader-topbar__title">团长主页</div>
          <button type="button" class="leader-topbar__icon" aria-label="分享" @click="onShareClick">
            <van-icon name="share-o" size="22" />
          </button>
        </div>

        <section class="leader-hero">
          <div class="leader-hero__copy">
            <span>私域团购小店</span>
            <h1>{{ storeData?.name || `${leaderData.displayName}的小店` }}</h1>
            <p>{{ leaderData.bio || storeData?.description || '精选社群团购，按约定履约发货。' }}</p>
          </div>
          <button v-if="isOwnLeader" type="button" class="leader-edit-home" @click="onEditHomepageClick">
            编辑店铺
          </button>
        </section>

        <AppCard class="leader-store-card">
          <div class="leader-store-card__main">
            <div class="leader-store-card__identity">
              <img
                v-if="leaderAvatarUrl"
                :src="leaderAvatarUrl"
                class="leader-store-card__avatar"
                :alt="`${leaderData.displayName}头像`"
              />
              <div v-else class="leader-store-card__avatar leader-store-card__avatar--fallback">
                {{ avatarText }}
                <span>金牌</span>
              </div>
              <div class="leader-store-card__copy">
                <h2>{{ leaderData.displayName }}</h2>
                <p>{{ storeData?.name || '团长小店' }}</p>
                <div class="leader-store-card__stats">
                  <span>{{ leaderData.memberCount }} 位成员</span>
                  <span>{{ leaderData.followerCount }} 人订阅</span>
                </div>
              </div>
            </div>
            <div class="leader-store-card__actions">
              <button
                type="button"
                class="leader-subscribe-button"
                :disabled="subLoading"
                @click="toggleSubscribe"
              >
                <van-icon :name="subscribed ? 'bookmark' : 'bookmark-o'" />
                <span>{{ subLoading ? '处理中' : subscribed ? '已订阅' : '订阅' }}</span>
              </button>
            </div>
          </div>

          <div class="leader-trust-strip">
            <div>
              <b>履约方式</b>
              <span>{{ deliveryTypeText }}</span>
            </div>
            <div>
              <b>当前团购</b>
              <span>{{ groupBuys.length }} 个</span>
            </div>
          </div>

          <button
            v-if="showCouponEntry"
            type="button"
            class="leader-coupon-entry"
            @click="openCouponSheet"
          >
            <van-icon name="coupon-o" />
            <span>
              <b>{{ couponEntryTitle }}</b>
              <small>{{ couponEntrySubtitle }}</small>
            </span>
            <van-icon name="arrow" />
          </button>

          <div class="leader-store-location">
            <van-icon name="location-o" />
            <div>
              <b>店铺位置</b>
              <span>{{ storeAddressText }}</span>
              <small>{{ storeLocationMetaText }}</small>
            </div>
            <button v-if="!hasUserLocation && !isOwnLeader" type="button" @click="onLocationClick">
              开启定位
            </button>
          </div>

          <div class="leader-profile-lines" v-if="showLocationLine">
            <div class="leader-profile-line">
              <button type="button" class="leader-profile-line__main" @click="onLocationClick">
                <van-icon name="location-o" />
                <span>{{ locationLineText }}</span>
                <b>{{ locationActionText }}</b>
                <van-icon v-if="isOwnLeader" name="arrow" />
              </button>
              <button
                type="button"
                class="leader-profile-line__close"
                aria-label="关闭位置提示"
                @click="dismissLocationLine"
              >
                <van-icon name="cross" size="16" />
              </button>
            </div>
          </div>
        </AppCard>

        <nav class="leader-tabs" aria-label="团购排序">
          <button
            v-for="tab in sortTabs"
            :key="tab.key"
            type="button"
            :class="{ active: activeSort === tab.key }"
            @click="onSortChange(tab.key)"
          >
            {{ tab.label }}
          </button>
        </nav>

        <section class="leader-feed">
          <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
            <van-list
              v-model:loading="listLoading"
              :finished="!hasMore"
              finished-text="没有更多了"
              @load="onLoadMore"
            >
              <GroupBuyFeedCard
                v-for="item in groupBuys"
                :key="item.id"
                :item="item"
                :show-location-signals="false"
                :show-store-header="false"
                @click="goToDetail(item.id)"
                @share="onShareClick"
                @subscribe="toggleSubscribe"
              />
              <div v-if="groupBuys.length === 0 && !listLoading" class="leader-empty">
                <div class="leader-empty__illustration">
                  <van-icon name="info-o" />
                </div>
                <p>暂时还没有发布的团购</p>
              </div>
            </van-list>
          </van-pull-refresh>
        </section>
      </div>
    </template>

    <van-popup
      v-model:show="couponSheetVisible"
      position="bottom"
      round
      :style="{ maxHeight: '82vh' }"
      @click-overlay="rememberCouponSheetDismissed"
    >
      <div class="leader-coupon-sheet">
        <div class="leader-coupon-sheet__header">
          <div>
            <span>店铺新人福利</span>
            <h2>订阅后领取店铺券</h2>
            <p>领取后可在确认订单页选择使用。</p>
          </div>
          <button type="button" aria-label="关闭新人券弹窗" @click="closeCouponSheet">
            <van-icon name="cross" />
          </button>
        </div>

        <div class="leader-coupon-sheet__list">
          <article
            v-for="coupon in couponOffers"
            :key="coupon.id"
            class="leader-coupon-card"
            :class="{ 'leader-coupon-card--claimed': coupon.claimed }"
          >
            <div class="leader-coupon-card__amount">
              <strong>{{ formatAmount(coupon.amount) }}</strong>
              <small>{{ coupon.thresholdAmount > 0 ? `满${formatAmount(coupon.thresholdAmount)}可用` : '无门槛' }}</small>
            </div>
            <div class="leader-coupon-card__copy">
              <h3>{{ coupon.name }}</h3>
              <p>{{ coupon.unavailableReason || '订阅后可立即领取' }}</p>
              <small>剩余 {{ Math.max(coupon.totalQuantity - coupon.claimedQuantity, 0) }} 张</small>
            </div>
            <button
              type="button"
              class="leader-coupon-card__button"
              :disabled="couponActionLoadingId === coupon.id || coupon.claimed"
              @click="handleCouponAction(coupon)"
            >
              {{ couponButtonText(coupon) }}
            </button>
          </article>
        </div>
      </div>
    </van-popup>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import AppCard from '@/components/AppCard.vue'
import { useAuthStore } from '@/stores'
import { getLeaderHomepage, subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { getMyStore } from '@/api/stores'
import { claimCoupon, listLeaderHomepageCoupons } from '@/api/coupons'
import { formatAmount } from '@/utils/format'
import { resolveDisplayImageUrl } from '@/utils/demo-images'
import type {
  LeaderHomepageLeader,
  LeaderHomepageStore,
  PublicGroupBuyItem,
  StoreCouponOfferData,
} from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const leaderData = ref<LeaderHomepageLeader | null>(null)
const storeData = ref<LeaderHomepageStore | null>(null)
const subscribed = ref(false)
const groupBuys = ref<PublicGroupBuyItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const listPage = ref(1)
const listLoading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)
const subLoading = ref(false)
const activeSort = ref('default')
const locationLineDismissed = ref(false)
const couponOffers = ref<StoreCouponOfferData[]>([])
const couponSheetVisible = ref(false)
const couponActionLoadingId = ref<string | null>(null)
const couponSheetSessionDismissed = ref(false)

const sortTabs = [
  { key: 'default', label: '默认' },
  { key: 'newest', label: '上新' },
  { key: 'sales', label: '销量' },
]

const avatarText = computed(() => {
  const name = storeData.value?.name || leaderData.value?.displayName || '团'
  return name.slice(0, 1)
})
const leaderAvatarUrl = computed(() => resolveDisplayImageUrl(
  leaderData.value?.avatarUrl || storeData.value?.logoUrl,
  leaderData.value?.displayName || storeData.value?.name || '团长',
  'avatar',
))
const isOwnLeader = computed(() => (
  authStore.leader?.id != null
  && leaderData.value?.id != null
  && String(authStore.leader.id) === String(leaderData.value.id)
))
const hasStoreLocation = computed(() => storeData.value?.latitude != null && storeData.value?.longitude != null)
const hasUserLocation = computed(() => Boolean(readSavedUserLocation()))
const homepageLocationParams = computed(() => readSavedUserLocation() ?? undefined)
const showLocationLine = computed(() => {
  if (locationLineDismissed.value) return false
  if (isOwnLeader.value) return true
  return !hasUserLocation.value || hasStoreLocation.value
})
const locationLineText = computed(() => {
  if (isOwnLeader.value) {
    return hasStoreLocation.value ? '店铺位置已设置，首页可参与附近筛选' : '添加位置，让更多人购买'
  }
  if (!hasUserLocation.value) return '开启定位后，可更精确找到周边团购'
  return '这家店铺在你的附近团购范围内'
})
const locationActionText = computed(() => {
  if (isOwnLeader.value) return hasStoreLocation.value ? '去修改' : '去设置'
  return hasUserLocation.value ? '已开启' : '去开启'
})
const deliveryTypeText = computed(() => {
  const map: Record<string, string> = {
    express: '快递配送',
    pickup: '到店自提',
    local_delivery: '同城配送',
  }
  return map[storeData.value?.defaultDeliveryType || ''] || '按团购说明履约'
})
const storeAddressText = computed(() => (
  storeData.value?.addressText
  || storeData.value?.fullAddress
  || (isOwnLeader.value ? '暂未填写详细地址' : '店铺暂未填写详细地址')
))
const storeLocationMetaText = computed(() => {
  if (storeData.value?.distanceText) return `距你 ${storeData.value.distanceText}`
  if (!hasUserLocation.value && !isOwnLeader.value) return '开启定位后显示与你的距离'
  if (hasStoreLocation.value) return '店铺已设置定位'
  return '还没有设置店铺定位'
})
const showCouponEntry = computed(() => !isOwnLeader.value && couponOffers.value.length > 0)
const claimableCouponCount = computed(() => couponOffers.value.filter((coupon) => coupon.claimable).length)
const unclaimedCouponCount = computed(() => couponOffers.value.filter((coupon) => !coupon.claimed).length)
const couponEntryTitle = computed(() => {
  if (!authStore.isLoggedIn) return '订阅可领新人券'
  if (!subscribed.value) return '订阅后领取新人券'
  if (claimableCouponCount.value > 0) return `${claimableCouponCount.value} 张新人券待领取`
  return '新人券'
})
const couponEntrySubtitle = computed(() => {
  const first = couponOffers.value[0]
  if (!first) return ''
  if (unclaimedCouponCount.value === 0) return '已领取，可在下单时使用'
  return `${formatAmount(first.amount)} 起，进店订阅后可领`
})

function readSavedUserLocation(): { latitude: number; longitude: number } | null {
  try {
    const raw = window.localStorage.getItem('index:user-location')
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<{ latitude: number; longitude: number }>
    if (typeof parsed.latitude !== 'number' || typeof parsed.longitude !== 'number') return null
    return { latitude: parsed.latitude, longitude: parsed.longitude }
  } catch {
    return null
  }
}

function locationDismissKey(leaderId = leaderData.value?.id) {
  return leaderId ? `leader-home:${leaderId}:location-line-dismissed` : ''
}

function readLocationLineDismissed(leaderId: string): boolean {
  try {
    return window.localStorage.getItem(locationDismissKey(leaderId)) === '1'
  } catch {
    return false
  }
}

function dismissLocationLine() {
  locationLineDismissed.value = true
  try {
    const key = locationDismissKey()
    if (key) window.localStorage.setItem(key, '1')
  } catch {
    // 本地偏好保存失败不影响继续浏览。
  }
}

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const id = route.params.id as string
    const data = await getLeaderHomepage(id, 1, 20, homepageLocationParams.value)
    leaderData.value = data.leader
    storeData.value = data.store
    locationLineDismissed.value = readLocationLineDismissed(data.leader.id)
    subscribed.value = data.viewer.subscribed
    groupBuys.value = data.groupBuys.items
    hasMore.value = data.groupBuys.hasMore
    listPage.value = 1
    await syncOwnStoreLocation()
    await fetchCouponOffers({ autoOpen: true })
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function couponDismissKey(leaderId = leaderData.value?.id) {
  return leaderId ? `leader-home:${leaderId}:coupon-sheet-dismissed` : ''
}

function readCouponSheetDismissed(leaderId: string): boolean {
  try {
    return window.sessionStorage.getItem(couponDismissKey(leaderId)) === '1'
  } catch {
    return couponSheetSessionDismissed.value
  }
}

function rememberCouponSheetDismissed() {
  couponSheetSessionDismissed.value = true
  try {
    const key = couponDismissKey()
    if (key) window.sessionStorage.setItem(key, '1')
  } catch {
    // 会话偏好保存失败不影响领券。
  }
}

async function fetchCouponOffers(options: { autoOpen?: boolean } = {}) {
  if (!leaderData.value || isOwnLeader.value) {
    couponOffers.value = []
    return
  }
  try {
    const offers = await listLeaderHomepageCoupons(leaderData.value.id)
    couponOffers.value = offers
    const shouldAutoOpen = options.autoOpen
      && offers.some((coupon) => !coupon.claimed)
      && !readCouponSheetDismissed(leaderData.value.id)
    if (shouldAutoOpen) couponSheetVisible.value = true
  } catch {
    couponOffers.value = []
  }
}

function openCouponSheet() {
  couponSheetVisible.value = true
}

function closeCouponSheet() {
  rememberCouponSheetDismissed()
  couponSheetVisible.value = false
}

function couponButtonText(coupon: StoreCouponOfferData) {
  if (couponActionLoadingId.value === coupon.id) return '处理中'
  if (coupon.claimed) return '已领取'
  if (!authStore.isLoggedIn) return '登录订阅'
  if (!subscribed.value || !coupon.viewerSubscribed) return '订阅领券'
  if (coupon.claimable) return '立即领取'
  return coupon.unavailableReason || '暂不可领'
}

async function handleCouponAction(coupon: StoreCouponOfferData) {
  if (coupon.claimed) return
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }

  couponActionLoadingId.value = coupon.id
  try {
    const id = route.params.id as string
    if (!subscribed.value || !coupon.viewerSubscribed) {
      await subscribeLeader(id, 'homepageCoupon')
      subscribed.value = true
      showToast('订阅成功，现在可以领取')
      await fetchCouponOffers()
      return
    }

    await claimCoupon(coupon.id)
    showToast('优惠券已领取')
    await fetchCouponOffers()
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    if (apiErr.code === 'SUBSCRIPTION_EXISTS') {
      subscribed.value = true
      showToast('已订阅，现在可以领取')
      await fetchCouponOffers()
    } else {
      showToast(apiErr.message || '操作失败')
    }
  } finally {
    couponActionLoadingId.value = null
  }
}

async function syncOwnStoreLocation() {
  if (!isOwnLeader.value || !storeData.value || hasStoreLocation.value) return
  try {
    const ownStoreData = await getMyStore()
    const ownStore = ownStoreData?.store
    if (!ownStore || String(ownStore.id) !== String(storeData.value.id)) return
    storeData.value = {
      ...storeData.value,
      description: storeData.value.description || ownStore.description,
      defaultDeliveryType: storeData.value.defaultDeliveryType || ownStore.defaultDeliveryType,
      latitude: ownStore.latitude,
      longitude: ownStore.longitude,
    }
  } catch {
    // 公开主页仍可正常展示，店铺位置兜底失败时不打断用户浏览。
  }
}

async function loadMoreGroupBuys() {
  if (listLoading.value || !hasMore.value) return
  listLoading.value = true
  try {
    const id = route.params.id as string
    const nextPage = listPage.value + 1
    const data = await getLeaderHomepage(id, nextPage, 20, homepageLocationParams.value)
    groupBuys.value = [...groupBuys.value, ...data.groupBuys.items]
    hasMore.value = data.groupBuys.hasMore
    listPage.value = nextPage
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '加载更多失败')
  } finally {
    listLoading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await fetchData()
  refreshing.value = false
}

async function onLoadMore() {
  await loadMoreGroupBuys()
}

async function toggleSubscribe() {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }

  subLoading.value = true
  try {
    const id = route.params.id as string
    if (subscribed.value) {
      await unsubscribeLeader(id)
      subscribed.value = false
      showToast('已取消订阅')
      await fetchCouponOffers()
    } else {
      await subscribeLeader(id, 'homepage')
      subscribed.value = true
      showToast('订阅成功')
      await fetchCouponOffers()
    }
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    if (apiErr.code === 'SUBSCRIPTION_EXISTS') {
      subscribed.value = true
      showToast('已订阅')
      await fetchCouponOffers()
    } else {
      showToast(apiErr.message || '操作失败')
    }
  } finally {
    subLoading.value = false
  }
}

function onSortChange(key: string) {
  activeSort.value = key
  if (key !== 'default') {
    showToast('排序能力暂未开放')
  }
}

function onShareClick() {
  showToast('分享能力仅作占位展示')
}

function onEditHomepageClick() {
  if (isOwnLeader.value) {
    router.push('/leader/store')
    return
  }
  showToast('仅团长本人可编辑主页')
}

function onLocationClick() {
  if (isOwnLeader.value) {
    router.push('/leader/store')
    return
  }
  if (hasUserLocation.value) {
    showToast('已开启附近展示')
    return
  }
  router.push('/')
}

function goToDetail(id: string) {
  router.push(`/group-buys/${id}`)
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.leader-home {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 24px;
}

.leader-topbar {
  height: 64px;
  background: var(--color-bg-card);
  display: flex;
  align-items: center;
  gap: 12px;
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 10px 14px;
  border-bottom: 1px solid var(--color-border-light);
}

.leader-topbar__back,
.leader-topbar__icon {
  height: 44px;
  border: 0;
  background: var(--color-bg-subtle);
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.leader-topbar__back,
.leader-topbar__icon {
  width: 44px;
  border-radius: var(--radius-md);
}

.leader-topbar__title {
  flex: 1;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leader-hero {
  min-height: 178px;
  background: linear-gradient(180deg, var(--color-primary), var(--color-primary-dark));
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 18px 70px;
  overflow: hidden;
}

.leader-hero__copy {
  position: relative;
  z-index: 1;
  min-width: 0;
  color: #fff;
}

.leader-hero__copy span {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.18);
  padding: 0 10px;
  font-size: var(--font-size-xs);
  font-weight: 800;
}

.leader-hero__copy h1 {
  margin: 12px 0 8px;
  font-size: 25px;
  line-height: 1.25;
  font-weight: 900;
}

.leader-hero__copy p {
  margin: 0;
  max-width: 26em;
  color: rgba(255, 255, 255, 0.86);
  font-size: var(--font-size-sm);
  line-height: 1.55;
}

.leader-edit-home {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border-radius: var(--radius-md);
  min-height: 36px;
  padding: 0 12px;
  font-weight: 800;
  flex: none;
}

.leader-store-card {
  background: #fff;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  margin: -46px 0 0;
  padding: 0;
  box-shadow: none;
  position: relative;
  z-index: 4;
  overflow: visible;
}

.leader-store-card__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 24px 18px 14px;
}

.leader-store-card__identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.leader-store-card__avatar {
  width: 76px;
  height: 76px;
  border-radius: var(--radius-md);
  object-fit: cover;
  flex: none;
  margin-top: -50px;
  border: 4px solid #fff;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.16);
}

.leader-store-card__avatar--fallback {
  background: linear-gradient(135deg, #ff9827, #d87016);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 900;
  line-height: 1.1;
}

.leader-store-card__avatar--fallback span {
  font-size: var(--font-size-xs);
}

.leader-store-card__copy {
  min-width: 0;
}

.leader-store-card__copy h2 {
  font-size: 21px;
  font-weight: 900;
  color: var(--color-text-primary);
  line-height: 1.35;
  margin: 0;
}

.leader-store-card__copy p {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.leader-store-card__stats {
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.leader-store-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.leader-subscribe-button {
  min-height: 44px;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 14px;
  font-size: var(--font-size-sm);
  font-weight: 900;
}

.leader-subscribe-button:disabled {
  opacity: 0.72;
}

.leader-trust-strip {
  margin: 0 18px 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.leader-trust-strip div {
  min-height: 58px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.leader-trust-strip b {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: 900;
}

.leader-trust-strip span {
  margin-top: 3px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.leader-coupon-entry {
  width: calc(100% - 36px);
  min-height: 64px;
  margin: 0 18px 12px;
  border: 1px solid rgba(16, 196, 104, 0.22);
  border-radius: var(--radius-md);
  background: var(--color-primary-light);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  text-align: left;
}

.leader-coupon-entry > .van-icon:first-child {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #fff;
  color: var(--color-primary-dark);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.leader-coupon-entry span {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 2px;
}

.leader-coupon-entry b {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: 900;
  line-height: 1.35;
}

.leader-coupon-entry small {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.35;
}

.leader-store-location {
  min-height: 64px;
  margin: 0 18px 12px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.leader-store-location > .van-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.leader-store-location div {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 2px;
}

.leader-store-location b {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: 900;
}

.leader-store-location span,
.leader-store-location small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.35;
}

.leader-store-location span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.leader-store-location small {
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
}

.leader-store-location button {
  min-height: 34px;
  border: 1px solid rgba(16, 196, 104, 0.28);
  border-radius: var(--radius-pill);
  background: #fff;
  color: var(--color-primary-dark);
  padding: 0 10px;
  font-size: var(--font-size-sm);
  font-weight: 800;
  flex-shrink: 0;
}

.leader-profile-lines {
  border-top: 1px solid var(--color-border-light);
  padding: 9px 18px 12px;
}

.leader-profile-line {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #717780;
}

.leader-profile-line__main {
  min-height: 44px;
  min-width: 0;
  flex: 1;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0;
  color: #717780;
  font-size: var(--font-size-sm);
  text-align: left;
}

.leader-profile-line__main > span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leader-profile-line__main b {
  color: #9aa0a6;
  font-size: 14px;
  font-weight: 500;
}

.leader-profile-line__close {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-hint);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.leader-profile-line__close:active {
  background: var(--color-bg-subtle);
}

.leader-tabs {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #fff;
  padding: 0;
  height: 64px;
  margin: 12px 0 0;
  border-top: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
}

.leader-tabs button {
  height: 64px;
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  position: relative;
  cursor: pointer;
  min-width: 72px;
}

.leader-tabs button.active {
  color: var(--color-text-primary);
  font-weight: 900;
}

.leader-tabs button.active::after {
  content: '';
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 0;
  height: 4px;
  background: var(--color-primary);
  border-radius: 6px;
}

.leader-feed {
  padding: 0 14px;
  min-height: 420px;
  background: #fff;
}

.leader-empty {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #a3a8af;
  font-size: 16px;
}

.leader-empty__illustration {
  width: 116px;
  height: 116px;
  border-radius: 34px;
  background: #f3f7f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 58px;
  margin-bottom: 20px;
}

.leader-coupon-sheet {
  padding: 16px 14px calc(var(--safe-area-bottom) + 14px);
  background: var(--color-bg-card);
}

.leader-coupon-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.leader-coupon-sheet__header span {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.leader-coupon-sheet__header h2 {
  margin: 4px 0;
  color: var(--color-text-primary);
  font-size: 20px;
  line-height: 1.3;
}

.leader-coupon-sheet__header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.leader-coupon-sheet__header button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.leader-coupon-sheet__list {
  display: grid;
  gap: 10px;
  max-height: 58vh;
  overflow-y: auto;
}

.leader-coupon-card {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-card);
  background: #fff;
}

.leader-coupon-card--claimed {
  opacity: 0.68;
}

.leader-coupon-card__amount {
  min-height: 72px;
  border-radius: 8px;
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 8px;
}

.leader-coupon-card__amount strong {
  font-size: 19px;
  line-height: 1.2;
}

.leader-coupon-card__amount small,
.leader-coupon-card__copy small,
.leader-coupon-card__copy p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.4;
}

.leader-coupon-card__copy {
  min-width: 0;
}

.leader-coupon-card__copy h3 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  line-height: 1.35;
}

.leader-coupon-card__copy p {
  margin: 4px 0 2px;
}

.leader-coupon-card__button {
  min-width: 76px;
  min-height: 40px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 800;
  padding: 0 10px;
}

.leader-coupon-card__button:disabled {
  background: var(--color-bg-surface);
  color: var(--color-text-hint);
}

@media (max-width: 374px) {
  .leader-topbar {
    gap: 8px;
    padding-inline: 10px;
  }

  .leader-hero {
    padding-inline: 14px;
  }

  .leader-store-card__main {
    padding-inline: 14px;
  }

  .leader-subscribe-button {
    padding: 0 10px;
  }

  .leader-coupon-card {
    grid-template-columns: 1fr;
  }

  .leader-coupon-card__amount {
    min-height: 58px;
  }

  .leader-coupon-card__button {
    width: 100%;
    min-height: 44px;
  }
}
</style>
