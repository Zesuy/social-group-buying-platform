<template>
  <PageLayout h5-constrained>
    <LoadingView v-if="loading" />

    <ErrorView
      v-if="error && !loading"
      :message="error"
      @retry="fetchDetail"
    />

    <template v-if="groupBuy && !loading">
      <div class="detail-page app-content--buybar">
        <div class="detail-topbar">
          <button type="button" class="detail-topbar__back" aria-label="返回" @click="goBack()">
            <van-icon name="arrow-left" size="22" />
          </button>
          <div class="detail-topbar__title">团购详情</div>
          <button type="button" class="detail-topbar__share" @click="handleShare">分享</button>
        </div>

        <section id="section-activity" class="activity-hero">
          <div class="activity-cover">
            <ImageWithFallback
              :src="heroImageUrl"
              width="100%"
              height="220px"
              fit="cover"
              radius="0"
              :alt="groupBuy.title"
            />
            <div class="activity-cover__actions">
              <button type="button" aria-label="返回" @click="goBack()">
                <van-icon name="arrow-left" />
              </button>
              <button type="button" aria-label="团长主页" @click="goToLeader">
                <van-icon name="manager-o" />
              </button>
              <button type="button" aria-label="分享" @click="handleShare">
                <van-icon name="share-o" />
              </button>
            </div>
          </div>

          <div v-if="leader && store" class="leader-overlay">
            <button type="button" class="leader-strip" @click="goToLeader">
              <img
                v-if="leaderAvatarUrl"
                :src="leaderAvatarUrl"
                class="leader-avatar"
                :alt="`${leader.displayName}头像`"
              />
              <div v-else class="leader-avatar leader-avatar--text">{{ leaderAvatarText }}</div>
              <div class="leader-copy">
                <strong>{{ leader.displayName }}</strong>
                <span>{{ store.name }} · {{ leader.followerCount }}人关注</span>
              </div>
              <van-icon name="arrow" color="var(--color-text-hint)" />
            </button>
            <div class="leader-trust">
              <span>团长组织</span>
              <span>{{ deliveryText }}</span>
              <span v-if="distanceText">{{ distanceText }}</span>
              <span>可订阅复购</span>
            </div>
            <div v-if="subscribed !== null" class="leader-actions">
              <button class="outline-btn" type="button" @click="goToLeader">看团长主页</button>
              <button
                class="solid-btn"
                type="button"
                :disabled="subLoading"
                @click="toggleSubscribe"
              >
                {{ subscribed ? '已订阅团长' : '订阅团长' }}
              </button>
            </div>
          </div>

          <div class="activity-panel">
            <div class="status-row">
              <span class="status-chip" :class="`status-chip--${statusTone}`">{{ statusText }}</span>
              <span>{{ activityWindowShortText }}</span>
            </div>
            <h1>{{ groupBuy.title }}</h1>
            <p class="activity-intro">{{ introText }}</p>
            <div class="activity-stats" aria-label="团购活动数据">
              <div>
                <b>{{ totalSold }}</b>
                <span>人已团</span>
              </div>
              <div>
                <b>{{ totalStock }}</b>
                <span>本团库存</span>
              </div>
              <div>
                <b>{{ items.length }}</b>
                <span>可选商品</span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="featuredItem" class="featured-product">
          <div class="section-heading">
            <div>
              <div class="section-eyebrow">本团热销商品</div>
            </div>
            <span>{{ featuredItem.soldCount }}人已团</span>
          </div>
          <button type="button" class="featured-card" @click="openSkuSheet(featuredItem)">
            <ImageWithFallback
              :src="featuredItem.coverImageUrl || featuredItem.product?.coverImageUrl || groupBuy.coverImageUrl"
              width="124px"
              height="124px"
              fit="cover"
              radius="8px"
              :alt="featuredItem.displayName"
            />
            <div class="featured-card__copy">
              <strong>{{ featuredItem.displayName }}</strong>
              <p>{{ productSummary(featuredItem) }}</p>
              <div class="featured-card__meta">
                <PriceText :amount="featuredItem.groupPriceAmount" size="xl" color="var(--color-price)" />
                <span>库存 {{ featuredItem.groupStock }}</span>
              </div>
              <span class="featured-card__cta">去跟团</span>
            </div>
          </button>
        </section>

        <nav class="anchor-tabs" aria-label="详情页栏目">
          <button type="button" @click="scrollToSection('section-activity')">团购</button>
          <button type="button" @click="scrollToSection('section-story')">介绍</button>
          <button type="button" @click="scrollToSection('section-items')">商品</button>
        </nav>

        <section id="section-story" class="detail-section section-card">
          <div class="section-heading">
            <div>
              <div class="section-eyebrow">团购活动介绍</div>
            </div>
          </div>
          <div class="activity-content">
            <template v-if="contentBlocks.length > 0">
              <div
                v-for="(block, index) in contentBlocks"
                :key="`${block.type}-${index}`"
                class="content-block"
                :class="`content-block--${block.type}`"
              >
                <p v-if="block.type === 'paragraph'">{{ block.text }}</p>
                <template v-else-if="block.type === 'section'">
                  <h3 v-if="block.title">{{ block.title }}</h3>
                  <p v-if="block.text">{{ block.text }}</p>
                </template>
                <figure v-else-if="block.type === 'image' && block.url">
                  <ImageWithFallback
                    :src="block.url"
                    width="100%"
                    height="220px"
                    fit="cover"
                    radius="8px"
                    :alt="block.caption || groupBuy.title"
                  />
                  <figcaption v-if="block.caption">{{ block.caption }}</figcaption>
                </figure>
                <ul v-else-if="block.type === 'list' && block.items?.length">
                  <li v-for="item in block.items" :key="item">{{ item }}</li>
                </ul>
                <div v-else-if="block.type === 'deliveryNote'" class="delivery-note">
                  <van-icon name="logistics" />
                  <span>{{ block.text }}</span>
                </div>
              </div>
            </template>
            <p v-else>{{ introText }}</p>
          </div>
        </section>

        <section id="section-items" class="detail-section section-card">
          <div class="section-heading">
            <div>
              <div class="section-eyebrow">本团商品</div>
            </div>
            <span>{{ items.length }} 款</span>
          </div>

          <div class="detail-items">
            <div
              v-for="item in items"
              :key="item.id"
              class="detail-item"
              :class="{ 'detail-item--selected': selectedItemId === item.id }"
            >
              <button type="button" class="detail-item__main" @click="openSkuSheet(item)">
                <ImageWithFallback
                  :src="item.coverImageUrl || item.product?.coverImageUrl"
                  width="84px"
                  height="84px"
                  fit="cover"
                  radius="8px"
                  :alt="item.displayName"
                />
                <div class="detail-item__info">
                  <span class="detail-item__name">{{ item.displayName }}</span>
                  <p>{{ productSummary(item) }}</p>
                  <PriceText :amount="item.groupPriceAmount" size="lg" color="var(--color-price)" />
                  <div class="detail-item__stock">
                    <span>库存 {{ item.groupStock }}</span>
                    <span>{{ item.soldCount }}人已团</span>
                  </div>
                </div>
              </button>
              <van-button
                v-if="isPurchasable && item.groupStock > 0"
                size="small"
                round
                type="primary"
                @click="openSkuSheet(item)"
              >
                {{ selectedItemId === item.id ? '已选商品' : '查看购买' }}
              </van-button>
              <van-tag v-if="item.groupStock <= 0" type="danger" size="medium">已售罄</van-tag>
            </div>
          </div>
        </section>

        <section id="section-fulfillment" class="detail-section section-card">
          <div class="section-heading">
            <div>
              <div class="section-eyebrow">履约说明</div>
            </div>
          </div>
          <div class="promise-list">
            <div class="promise-row">
              <van-icon name="clock-o" />
              <div>
                <b>活动时间</b>
                <span>{{ activityWindowText }}</span>
              </div>
            </div>
            <div class="promise-row">
              <van-icon name="logistics" />
              <div>
                <b>履约方式</b>
                <span>{{ deliveryText }}，{{ shippingText }}</span>
              </div>
            </div>
            <div class="promise-row">
              <van-icon name="shop-o" />
              <div>
                <b>店铺位置</b>
                <span>{{ storeLocationText }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="buybar detail-buybar h5-constrained-fixed">
        <button class="mini" type="button" @click="router.push('/')">
          <van-icon name="wap-home-o" size="23" />
          <span>首页</span>
        </button>
        <button class="mini" type="button" @click="goToLeader">
          <van-icon name="manager-o" size="23" />
          <span>团长</span>
        </button>
        <button class="mini" type="button" :disabled="subLoading" @click="toggleSubscribe">
          <van-icon :name="subscribed ? 'star' : 'star-o'" size="23" />
          <span>{{ subscribed ? '已订阅' : '订阅' }}</span>
        </button>
        <button class="mini" type="button" @click="goToCart">
          <van-icon name="cart-o" size="23" />
          <span>购物车</span>
        </button>
        <button
          class="big"
          type="button"
          :disabled="!isPurchasable"
          @click="openSkuSheetForBuybar"
        >
          {{ isPurchasable ? '跟团购买' : buyDisabledText }}
          <span v-if="isPurchasable" class="detail-buybar__sub">{{ totalSold }}人已团</span>
        </button>
      </div>

      <SkuSheet
        v-model="skuSheetVisible"
        :item="skuTargetItem"
        :default-delivery-type="groupBuy.deliveryType"
        :show-cart-action="true"
        @add-to-cart="onSkuAddToCart"
        @buy-now="onSkuBuyNow"
      />
      <CartSheet
        v-model="cartSheetVisible"
        :current-group-buy-id="groupBuy.id"
        :share-token="currentShareToken"
      />
      <GroupBuyShareSheet
        v-model="shareSheetVisible"
        :payload="sharePayload"
        :share-url="shareUrl"
      />
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import PriceText from '@/components/PriceText.vue'
import SkuSheet from '@/components/SkuSheet.vue'
import CartSheet from '@/components/CartSheet.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import { useSmartNavigation } from '@/composables'
import { useAuthStore, useCheckoutStore } from '@/stores'
import { getGroupBuyDetailByShareToken, getPublicGroupBuyDetail } from '@/api/groupBuys'
import { subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { addCartItem } from '@/api/cart'
import { buildGroupBuyShareUrl, buildShareTokenUrl, getDeliveryTypeText, getGroupBuyStatusText } from '@/utils'
import { resolveDisplayImageUrl } from '@/utils/demo-images'
import type {
  GroupBuyDetail,
  LeaderDetail,
  StoreDetail,
  PublicGroupBuyDetailItem,
  ViewerInfo,
  ContentBlockData,
} from '@/types'

const route = useRoute()
const router = useRouter()
const { goBack } = useSmartNavigation('/')
const authStore = useAuthStore()
const checkoutStore = useCheckoutStore()

const groupBuy = ref<GroupBuyDetail | null>(null)
const leader = ref<LeaderDetail | null>(null)
const store = ref<StoreDetail | null>(null)
const items = ref<PublicGroupBuyDetailItem[]>([])
const featuredItemFromApi = ref<PublicGroupBuyDetailItem | null>(null)
const viewer = ref<ViewerInfo | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const selectedItemId = ref<string | null>(null)
const subLoading = ref(false)
const skuSheetVisible = ref(false)
const skuTargetItem = ref<PublicGroupBuyDetailItem | null>(null)
const cartSheetVisible = ref(false)
const shareSheetVisible = ref(false)

const hasAnyStock = computed(() => items.value.some(item => item.groupStock > 0))
const isPurchasable = computed(() => {
  if (!groupBuy.value) return false
  if (groupBuy.value.status !== 'published') return false
  if (items.value.length === 0) return false
  return hasAnyStock.value
})
const buyDisabledText = computed(() => {
  if (!groupBuy.value) return ''
  if (groupBuy.value.status === 'ended') return '团购已结束'
  if (groupBuy.value.status === 'draft') return '未发布'
  if (items.value.length === 0) return '暂无商品'
  if (!hasAnyStock.value) return '库存不足'
  return '暂不可购买'
})
const subscribed = computed(() => viewer.value?.subscribed ?? null)
const deliveryText = computed(() => {
  if (!groupBuy.value?.deliveryType) return '快递配送'
  return getDeliveryTypeText(groupBuy.value.deliveryType)
})
const totalSold = computed(() => items.value.reduce((sum, item) => sum + item.soldCount, 0))
const totalStock = computed(() => items.value.reduce((sum, item) => sum + item.groupStock, 0))
const featuredItem = computed(() => {
  if (featuredItemFromApi.value) return featuredItemFromApi.value
  return [...items.value].sort((a, b) => b.soldCount - a.soldCount || a.sortOrder - b.sortOrder)[0] ?? null
})
const leaderAvatarText = computed(() => leader.value?.displayName.slice(0, 1) || store.value?.name.slice(0, 1) || '团')
const leaderAvatarUrl = computed(() => resolveDisplayImageUrl(
  leader.value?.avatarUrl,
  leader.value?.displayName || store.value?.name || '团长',
  'avatar',
))
const introText = computed(() => groupBuy.value?.introduction?.trim() || '团长正在组织这次短周期团购，集中收单后按约定方式履约。')
const heroImageUrl = computed(() => {
  return groupBuy.value?.coverImageUrl
    || groupBuy.value?.galleryImageUrls?.[0]
    || leader.value?.avatarUrl
    || store.value?.logoUrl
    || featuredItem.value?.coverImageUrl
    || featuredItem.value?.product?.coverImageUrl
    || null
})
const contentBlocks = computed<ContentBlockData[]>(() => groupBuy.value?.contentBlocks ?? [])
const statusText = computed(() => groupBuy.value ? getGroupBuyStatusText(groupBuy.value.status) : '团购')
const statusTone = computed(() => {
  if (!groupBuy.value) return 'default'
  if (groupBuy.value.status === 'published') return 'active'
  if (groupBuy.value.status === 'ended') return 'ended'
  return 'default'
})
const distanceText = computed(() => store.value?.distanceText || '')
const activityWindowShortText = computed(() => {
  if (!groupBuy.value?.endTime) return '限时开团'
  return `${groupBuy.value.endTime.slice(5, 10)} 截止`
})
const activityWindowText = computed(() => {
  const start = groupBuy.value?.startTime ? groupBuy.value.startTime.slice(0, 16).replace('T', ' ') : '现在'
  const end = groupBuy.value?.endTime ? groupBuy.value.endTime.slice(0, 16).replace('T', ' ') : '团长结束前'
  return `${start} 至 ${end}`
})
const shippingText = computed(() => {
  if (!groupBuy.value?.shippingTime) return '具体发货时间以团长通知为准'
  return `${groupBuy.value.shippingTime.slice(0, 16).replace('T', ' ')} 前后履约`
})
const storeLocationText = computed(() => {
  if (!store.value) return '店铺信息加载中'
  if (store.value.distanceText) return `${store.value.name}，距离你 ${store.value.distanceText}`
  if (store.value.latitude !== null && store.value.longitude !== null) return `${store.value.name}，已配置店铺位置`
  return `${store.value.name}，暂未提供距离`
})
const currentShareToken = computed(() => (
  route.name === 'groupBuyShareDetail' ? (route.params.shareToken as string) : null
))
const shareUrl = computed(() => {
  if (currentShareToken.value) return buildShareTokenUrl(currentShareToken.value)
  return groupBuy.value ? buildGroupBuyShareUrl(groupBuy.value.id) : ''
})
const sharePayload = computed<GroupBuySharePayload>(() => {
  const prices = items.value.map(item => item.groupPriceAmount).filter(amount => Number.isFinite(amount))
  return {
    title: groupBuy.value?.title || '团购分享',
    coverImageUrl: heroImageUrl.value,
    minPriceAmount: prices.length > 0 ? Math.min(...prices) : null,
    maxPriceAmount: prices.length > 0 ? Math.max(...prices) : null,
    storeName: store.value?.name || '团长店铺',
    leaderName: leader.value?.displayName || '团长',
    deliveryType: groupBuy.value?.deliveryType || null,
    shippingTime: groupBuy.value?.shippingTime || null,
  }
})

async function fetchDetail() {
  loading.value = true
  error.value = null
  try {
    const data = currentShareToken.value
      ? await getGroupBuyDetailByShareToken(currentShareToken.value)
      : await getPublicGroupBuyDetail(
          route.params.id as string,
          readLocationParams(),
        )
    groupBuy.value = data.groupBuy
    leader.value = data.leader
    store.value = data.store
    items.value = data.items
    featuredItemFromApi.value = data.featuredItem ?? null
    viewer.value = data.viewer
    selectedItemId.value = null
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function readLocationParams() {
  const latitude = readNumberQuery('latitude')
  const longitude = readNumberQuery('longitude')
  return latitude !== undefined && longitude !== undefined ? { latitude, longitude } : undefined
}

function readNumberQuery(key: string) {
  const raw = route.query[key]
  const value = Array.isArray(raw) ? raw[0] : raw
  if (!value) return undefined
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

function productSummary(item: PublicGroupBuyDetailItem) {
  return item.product?.description?.trim() || item.product?.name || '商品说明由团长在本次团购中统一承接'
}

function openSkuSheet(item: PublicGroupBuyDetailItem) {
  if (item.groupStock <= 0) {
    showToast('该商品已售罄')
    return
  }
  selectedItemId.value = item.id
  skuTargetItem.value = item
  skuSheetVisible.value = true
}

function openSkuSheetForBuybar() {
  if (items.value.length === 0) return
  const target = items.value.find(item => item.id === selectedItemId.value && item.groupStock > 0)
    ?? featuredItem.value
    ?? items.value.find(item => item.groupStock > 0)
  if (target && target.groupStock > 0) openSkuSheet(target)
  else showToast('暂无库存')
}

function onSkuBuyNow(payload: { itemId: string; quantity: number; deliveryType: string }) {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  if (!groupBuy.value) return
  const item = items.value.find(candidate => candidate.id === payload.itemId)
  if (!item) return
  checkoutStore.setCheckoutContext({
    groupBuyId: groupBuy.value.id,
    groupBuyItemId: payload.itemId,
    quantity: payload.quantity,
    shareToken: currentShareToken.value,
    title: groupBuy.value.title,
    coverImageUrl: groupBuy.value.coverImageUrl || item.coverImageUrl || item.product?.coverImageUrl || null,
    displayName: item.displayName,
    unitPriceAmount: item.groupPriceAmount,
  })
  router.push('/checkout')
}

async function onSkuAddToCart(payload: { itemId: string; quantity: number; deliveryType: string }) {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  try {
    await addCartItem({
      groupBuyItemId: payload.itemId,
      quantity: payload.quantity,
      shareToken: currentShareToken.value,
    })
    skuSheetVisible.value = false
    cartSheetVisible.value = true
    showToast('已加入购物车')
  } catch (err) {
    showToast((err as { message?: string }).message || '加入购物车失败')
  }
}

async function toggleSubscribe() {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  if (!leader.value || !viewer.value) return

  subLoading.value = true
  try {
    if (subscribed.value) {
      await unsubscribeLeader(leader.value.id)
      viewer.value.subscribed = false
      showToast('已取消订阅')
    } else {
      await subscribeLeader(leader.value.id, 'groupBuyDetail')
      viewer.value.subscribed = true
      showToast('订阅成功')
    }
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    if (apiErr.code === 'SUBSCRIPTION_EXISTS') {
      viewer.value.subscribed = true
      showToast('已订阅')
    } else {
      showToast(apiErr.message || '操作失败')
    }
  } finally {
    subLoading.value = false
  }
}

function handleShare() {
  if (!groupBuy.value || !shareUrl.value) return
  shareSheetVisible.value = true
}

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goToLeader() {
  if (leader.value) {
    router.push(`/leaders/${leader.value.id}`)
  }
}

function goToCart() {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  cartSheetVisible.value = true
}

onMounted(() => {
  fetchDetail()
})

watch(
  () => route.fullPath,
  () => {
    fetchDetail()
  },
)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.detail-topbar {
  height: 50px;
  background: rgba(255, 255, 255, 0.96);
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid var(--color-border-light);
  backdrop-filter: blur(12px);
}

.detail-topbar__back,
.detail-topbar__share {
  position: absolute;
  min-width: 44px;
  min-height: 44px;
  border: 0;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
  font-weight: 800;
}

.detail-topbar__back {
  left: 8px;
}

.detail-topbar__share {
  right: 10px;
  color: var(--color-primary);
}

.detail-topbar__title {
  font-size: 18px;
  font-weight: 900;
}

.activity-hero,
.featured-product,
.detail-section {
  background: #fff;
}

.activity-cover {
  position: relative;
}

.activity-cover__actions {
  position: absolute;
  top: 12px;
  left: 12px;
  right: 12px;
  display: flex;
  justify-content: space-between;
  pointer-events: none;
}

.activity-cover__actions button {
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}

.leader-overlay {
  margin: -24px 14px 0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(18, 34, 25, 0.12);
  position: relative;
  z-index: 2;
  overflow: hidden;
}

.leader-strip {
  width: 100%;
  border: 0;
  background: #fff;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 18px;
  gap: 12px;
  align-items: center;
  text-align: left;
  padding: 14px;
}

.leader-avatar {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  object-fit: cover;
  background: linear-gradient(135deg, #10c468, #87d38f);
}

.leader-avatar--text {
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 900;
}

.leader-copy {
  min-width: 0;
}

.leader-copy strong {
  display: block;
  color: var(--color-text-primary);
  font-size: 19px;
  line-height: 1.25;
}

.leader-copy span {
  display: block;
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.leader-trust,
.status-row,
.detail-item__stock {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.leader-trust {
  padding: 0 14px 14px;
}

.leader-trust span {
  border-radius: 999px;
  padding: 5px 9px;
  background: var(--color-bg-soft);
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.leader-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 0 14px 14px;
}

.outline-btn,
.solid-btn {
  min-height: 44px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 900;
}

.outline-btn {
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
}

.solid-btn {
  border: 0;
  background: var(--color-primary);
  color: #fff;
}

.activity-panel {
  padding: 16px 16px 18px;
}

.status-row {
  color: var(--color-text-secondary);
  font-size: 13px;
  margin-bottom: 10px;
}

.status-chip {
  border-radius: 999px;
  padding: 4px 10px;
  font-weight: 900;
  color: var(--color-text-secondary);
  background: var(--color-bg-soft);
}

.status-chip--active {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.status-chip--ended {
  color: var(--color-danger);
  background: rgba(238, 67, 67, 0.09);
}

.activity-panel h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 26px;
  line-height: 1.28;
  font-weight: 900;
}

.activity-intro {
  margin: 10px 0 0;
  color: var(--color-text-primary);
  font-size: 16px;
  line-height: 1.65;
}

.activity-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 14px;
  padding: 12px;
  border-radius: 8px;
  background: #f7fbf8;
}

.activity-stats b {
  display: block;
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: 1.2;
  font-weight: 900;
}

.activity-stats span {
  display: block;
  margin-top: 4px;
  color: var(--color-text-hint);
  font-size: 12px;
}

.featured-product,
.section-card {
  margin-top: 10px;
  padding: 16px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 900;
  margin-bottom: 4px;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 20px;
  line-height: 1.25;
  font-weight: 900;
}

.section-heading > span {
  color: var(--color-price);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.featured-card {
  width: 100%;
  border: 0;
  border-radius: 8px;
  background: #f8faf8;
  padding: 12px;
  display: grid;
  grid-template-columns: 124px minmax(0, 1fr);
  gap: 12px;
  text-align: left;
}

.featured-card__copy {
  min-width: 0;
}

.featured-card__copy strong,
.detail-item__name {
  display: block;
  color: var(--color-text-primary);
  font-size: 17px;
  line-height: 1.35;
  font-weight: 900;
}

.featured-card__copy p,
.detail-item__info p {
  margin: 6px 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-card__meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.featured-card__meta span {
  color: var(--color-text-hint);
  font-size: 12px;
}

.featured-card__cta {
  display: inline-flex;
  margin-top: 10px;
  min-height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 900;
}

.anchor-tabs {
  height: 48px;
  background: #fff;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 50px;
  z-index: 15;
}

.anchor-tabs button {
  min-height: 48px;
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 15px;
  font-weight: 800;
}

.activity-content {
  color: var(--color-text-primary);
  font-size: 16px;
  line-height: 1.7;
}

.content-block {
  margin-top: 14px;
}

.content-block:first-child {
  margin-top: 0;
}

.content-block p {
  margin: 0;
  white-space: pre-line;
}

.content-block h3 {
  margin: 0 0 8px;
  color: var(--color-text-primary);
  font-size: 19px;
  line-height: 1.35;
}

.content-block figure {
  margin: 0;
}

.content-block figcaption {
  margin-top: 6px;
  color: var(--color-text-hint);
  font-size: 12px;
  text-align: center;
}

.content-block ul {
  margin: 0;
  padding-left: 20px;
}

.content-block li + li {
  margin-top: 6px;
}

.delivery-note {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 8px;
  border-radius: 8px;
  padding: 12px;
  background: #f7fbf8;
  color: var(--color-text-secondary);
}

.delivery-note :deep(.van-icon) {
  color: var(--color-primary);
  margin-top: 2px;
}

.detail-items {
  display: grid;
  gap: 10px;
}

.detail-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--color-border-light);
  border-radius: 8px;
  background: #fff;
}

.detail-item--selected {
  border-color: rgba(16, 196, 104, 0.45);
  background: #f8fff9;
}

.detail-item__main {
  min-width: 0;
  border: 0;
  background: transparent;
  padding: 0;
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 12px;
  text-align: left;
}

.detail-item__info {
  min-width: 0;
}

.detail-item__stock {
  color: var(--color-text-hint);
  font-size: 12px;
}

.promise-list {
  display: grid;
  gap: 12px;
}

.promise-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
}

.promise-row :deep(.van-icon) {
  color: var(--color-primary);
  margin-top: 2px;
}

.promise-row b {
  display: block;
  color: var(--color-text-primary);
  font-size: 15px;
  line-height: 1.35;
}

.promise-row span {
  display: block;
  margin-top: 3px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.detail-buybar {
  grid-template-columns: 58px 58px 58px 58px 1fr;
}

.detail-buybar .mini {
  color: var(--color-text-secondary);
}

.detail-buybar__sub {
  font-size: 13px;
  font-weight: 700;
  margin-top: 2px;
}

@media (max-width: 374px) {
  .activity-panel h1 {
    font-size: 23px;
  }

  .featured-card {
    grid-template-columns: 104px minmax(0, 1fr);
  }

  .featured-card :deep(.image-with-fallback) {
    width: 104px !important;
    height: 104px !important;
  }

  .detail-buybar {
    grid-template-columns: 50px 50px 50px 50px 1fr;
  }
}
</style>
