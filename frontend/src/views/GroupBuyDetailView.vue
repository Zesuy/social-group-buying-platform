<template>
  <PageLayout :title="pageTitle" show-back @back="goBack">
    <!-- 加载态 -->
    <LoadingView v-if="loading" />

    <!-- 错误态 -->
    <ErrorView
      v-if="error && !loading"
      :message="error"
      @retry="fetchDetail"
    />

    <!-- 内容 -->
    <template v-if="groupBuy && !loading">
      <div class="detail-content app-content--buybar">
        <!-- 封面 -->
        <div class="hero detail-hero-cover">
          <ImageWithFallback
            :src="groupBuy.coverImageUrl"
            width="100%"
            height="220px"
            fit="cover"
          />
        </div>

        <!-- 团长/店铺信任区 -->
        <LeaderTrustBlock
          v-if="leader && store"
          :leader="leader"
          :store="store"
          @click="goToLeader"
        />

        <div v-if="subscribed !== null" class="detail-subscribe-row">
          <button class="btn ghost" type="button" @click="goToLeader">
            店铺主页
          </button>
          <button
            class="btn primary"
            type="button"
            :disabled="subLoading"
            @click="toggleSubscribe"
          >
            {{ subscribed ? '已订阅' : '+ 订阅团长' }}
          </button>
        </div>

        <!-- 团购信息 -->
        <div class="detail-info-card">
          <h1 class="detail-info-card__title">{{ groupBuy.title }}</h1>
          <p v-if="groupBuy.introduction" class="detail-info-card__intro">
            {{ groupBuy.introduction }}
          </p>

          <div class="detail-info-card__meta">
            <div class="detail-info-card__meta-item">
              <span class="detail-info-card__meta-label">配送方式</span>
              <span class="detail-info-card__meta-value">{{ deliveryText }}</span>
            </div>
            <div v-if="groupBuy.shippingTime" class="detail-info-card__meta-item">
              <span class="detail-info-card__meta-label">发货时间</span>
              <span class="detail-info-card__meta-value">{{ groupBuy.shippingTime }}</span>
            </div>
            <div v-if="groupBuy.startTime" class="detail-info-card__meta-item">
              <span class="detail-info-card__meta-label">开始时间</span>
              <span class="detail-info-card__meta-value">{{ groupBuy.startTime }}</span>
            </div>
            <div v-if="groupBuy.endTime" class="detail-info-card__meta-item">
              <span class="detail-info-card__meta-label">结束时间</span>
              <span class="detail-info-card__meta-value">{{ groupBuy.endTime }}</span>
            </div>
          </div>
        </div>

        <!-- 商品列表 -->
        <div class="detail-items">
          <h3 class="detail-items__title">商品列表</h3>
          <div
            v-for="item in items"
            :key="item.id"
            class="detail-item"
          >
            <ImageWithFallback
              :src="item.coverImageUrl"
              width="80px"
              height="80px"
              fit="cover"
              radius="var(--radius-sm)"
            />
            <div class="detail-item__info">
              <span class="detail-item__name">{{ item.displayName }}</span>
              <PriceText
                :amount="item.groupPriceAmount"
                size="lg"
                color="var(--color-price)"
              />
              <div class="detail-item__stock">
                <span>库存 {{ item.groupStock }}</span>
                <span>已售 {{ item.soldCount }}</span>
              </div>
              <!-- 数量选择 -->
              <van-stepper
                v-if="isPurchasable && selectedItemId === item.id"
                v-model="quantity"
                :min="1"
                :max="item.groupStock"
                :disable-input="true"
                integer
                theme="round"
                button-size="26"
                @change="onQuantityChange"
              />
            </div>
            <!-- 选择按钮 -->
            <van-button
              v-if="isPurchasable && selectedItemId !== item.id && item.groupStock > 0"
              size="small"
              round
              type="primary"
              @click="selectItem(item)"
            >
              选择
            </van-button>
            <van-tag v-if="item.groupStock <= 0" type="danger" size="medium">已售罄</van-tag>
          </div>
        </div>

        <!-- 灰态购物车浮动入口（非 MVP，不接真实逻辑） -->
        <button class="detail-fab-cart floating-cart-entry" type="button" @click="onCartClick">
          <van-icon name="cart-o" size="26" color="var(--color-primary)" />
        </button>
      </div>

      <div class="buybar">
        <button class="mini" type="button" @click="router.push('/')">
          <van-icon name="wap-home-o" size="22" />
          <span>主页</span>
        </button>
        <button class="mini" type="button" @click="onCartClick">
          <van-icon name="cart-o" size="22" />
          <span>购物车</span>
        </button>
        <button
          class="big"
          type="button"
          :disabled="!isPurchasable || !selectedItemId"
          @click="handleBuy"
        >
          <div class="faces" aria-hidden="true">
            <span>买</span><span>团</span><span>省</span>
          </div>
          {{ isPurchasable ? '跟团购买' : buyDisabledText }}
          <span v-if="selectedItemId" class="detail-buybar__sub">立即购买 · 已选 {{ quantity }} 件</span>
          <span v-else-if="isPurchasable" class="detail-buybar__sub">先选商品</span>
        </button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import LeaderTrustBlock from '@/components/LeaderTrustBlock.vue'
import PriceText from '@/components/PriceText.vue'
import { useAuthStore } from '@/stores'
import { useCheckoutStore } from '@/stores'
import { getPublicGroupBuyDetail } from '@/api/groupBuys'
import { subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { getDeliveryTypeText } from '@/utils'
import { isFeatureDisabled } from '@/utils/non-mvp'
import type {
  GroupBuyDetail,
  LeaderDetail,
  StoreDetail,
  PublicGroupBuyDetailItem,
  ViewerInfo,
} from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const checkoutStore = useCheckoutStore()

// ── 数据 ──
const groupBuy = ref<GroupBuyDetail | null>(null)
const leader = ref<LeaderDetail | null>(null)
const store = ref<StoreDetail | null>(null)
const items = ref<PublicGroupBuyDetailItem[]>([])
const viewer = ref<ViewerInfo | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

// ── 交互状态 ──
const selectedItemId = ref<string | null>(null)
const quantity = ref(1)
const subLoading = ref(false)

// ── 计算属性 ──
const pageTitle = computed(() => groupBuy.value?.title || '团购详情')
const hasAnyStock = computed(() => items.value.some(i => i.groupStock > 0))
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
  if (!groupBuy.value?.deliveryType) return ''
  return getDeliveryTypeText(groupBuy.value.deliveryType)
})

// ── 获取详情 ──
async function fetchDetail() {
  loading.value = true
  error.value = null
  try {
    const id = route.params.id as string
    const data = await getPublicGroupBuyDetail(id)
    groupBuy.value = data.groupBuy
    leader.value = data.leader
    store.value = data.store
    items.value = data.items
    viewer.value = data.viewer
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function selectItem(item: PublicGroupBuyDetailItem) {
  if (item.groupStock <= 0) return
  selectedItemId.value = item.id
  quantity.value = 1
}

function onQuantityChange(val: number | string) {
  quantity.value = Number(val)
}

// ── 购买操作 ──
function handleBuy() {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  if (!selectedItemId.value || !groupBuy.value) {
    showToast('请选择商品')
    return
  }

  checkoutStore.setCheckoutContext({
    groupBuyId: groupBuy.value.id,
    groupBuyItemId: selectedItemId.value,
    quantity: quantity.value,
    title: groupBuy.value.title,
    coverImageUrl: groupBuy.value.coverImageUrl,
    displayName: items.value.find(i => i.id === selectedItemId.value)?.displayName || '',
    unitPriceAmount: items.value.find(i => i.id === selectedItemId.value)?.groupPriceAmount || 0,
  })
  router.push('/checkout')
}

function onCartClick() {
  if (isFeatureDisabled('cart')) {
    showToast('购物车功能即将开放')
  }
}

// ── 订阅/取消订阅 ──
async function toggleSubscribe() {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  if (!leader.value) return

  subLoading.value = true
  try {
    if (subscribed.value) {
      await unsubscribeLeader(leader.value.id)
      viewer.value!.subscribed = false
      showToast('已取消订阅')
    } else {
      await subscribeLeader(leader.value.id, 'groupBuyDetail')
      viewer.value!.subscribed = true
      showToast('订阅成功')
    }
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    if (apiErr.code === 'SUBSCRIPTION_EXISTS') {
      // 幂等处理
      viewer.value!.subscribed = true
      showToast('已订阅')
    } else {
      showToast(apiErr.message || '操作失败')
    }
  } finally {
    subLoading.value = false
  }
}

function goToLeader() {
  if (leader.value) {
    router.push(`/leaders/${leader.value.id}`)
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.detail-content {
  position: relative;
}

.detail-hero-cover {
  background: var(--color-primary-deep);
  margin: 0;
}

.detail-hero-cover :deep(.image-with-fallback__placeholder) {
  background: linear-gradient(135deg, #bdd4ff, #ffd0c0);
}

.detail-subscribe-row {
  margin: -2px 14px 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.detail-info-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  margin: 0 14px 12px;
  padding: 14px;
  box-shadow: var(--shadow-card);
}

.detail-info-card__title {
  font-size: 24px;
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1.4;
}

.detail-info-card__intro {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
  line-height: 1.5;
}

.detail-info-card__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.detail-info-card__meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.detail-info-card__meta-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  min-width: 64px;
}

.detail-info-card__meta-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.detail-items {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  margin: 0 14px 12px;
  padding: 14px;
  box-shadow: var(--shadow-card);
}

.detail-items__title {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: var(--spacing-md);
  color: var(--color-text-primary);
}

.detail-item {
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item__name {
  font-size: var(--font-size-lg);
  color: var(--color-text-primary);
  font-weight: 600;
}

.detail-item__stock {
  display: flex;
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
}

.detail-fab-cart {
  bottom: calc(92px + var(--safe-area-bottom));
  border: 0;
}

.detail-buybar__sub {
  font-size: 13px;
  font-weight: 700;
  margin-top: 2px;
}
</style>
