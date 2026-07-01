<template>
  <PageLayout>
    <LoadingView v-if="loading" />

    <ErrorView
      v-if="error && !loading"
      :message="error"
      @retry="fetchDetail"
    />

    <template v-if="groupBuy && !loading">
      <div class="detail-page app-content--buybar">
        <div class="detail-topbar">
          <button type="button" class="detail-topbar__back" aria-label="返回" @click="goBack">
            <van-icon name="arrow-left" size="22" />
          </button>
          <div class="detail-topbar__title">团购详情</div>
        </div>

        <section id="section-product" class="product-post">
          <div class="product-head">
            <h1 class="product-title-main">
              {{ groupBuy.title }}
              <span class="ship-chip">{{ deliveryText }}</span>
            </h1>

            <div class="hot-board">
              <div class="hot-board-title">
                火爆热卖中
                <span>| {{ totalSold }}人已经跟团，快来抢购吧</span>
              </div>
              <div class="hot-list">
                <button
                  v-for="row in hotRows"
                  :key="row.label"
                  type="button"
                  class="hot-row"
                  @click="scrollToSection('section-items')"
                >
                  <div class="round-avatar">{{ row.avatar }}</div>
                  <div class="hot-row__copy">
                    <b>{{ row.time }}跟团</b>
                    <span>{{ row.label }}</span>
                  </div>
                  <div class="hot-row__plus">+1件</div>
                  <div class="hot-row__go">
                    <small>热销商品</small>
                    去跟团
                  </div>
                </button>
              </div>
            </div>

            <div class="meta-block">
              {{ publishText }} 发布
              <span>|</span>
              <b>{{ endText }}</b>
              <br />
              {{ viewCount }}人查看
              <span>|</span>
              {{ totalSold }}次跟团
            </div>
          </div>

          <div class="product-image-wrap">
            <ImageWithFallback
              :src="groupBuy.coverImageUrl"
              width="100%"
              height="520px"
              fit="cover"
              radius="2px"
              :alt="groupBuy.title"
            />
            <div class="product-image-caption">
              <span>商品主图</span>
              <span>{{ deliveryText }}</span>
            </div>
          </div>
        </section>

        <nav class="anchor-tabs" aria-label="详情页栏目">
          <button type="button" class="active" @click="scrollToSection('section-product')">商品</button>
          <button type="button" @click="scrollToSection('section-details')">详情</button>
          <button type="button" @click="scrollToSection('section-showcase')">晒单</button>
          <button type="button" @click="scrollToSection('section-records')">记录</button>
        </nav>

        <div class="section-gap" />

        <section id="section-details" class="detail-section">
          <div v-if="leader && store" class="leader-card" @click="goToLeader">
            <div class="leader-main">
              <img
                v-if="leader.avatarUrl"
                :src="leader.avatarUrl"
                class="avatar"
                :alt="`${leader.displayName}头像`"
              />
              <div v-else class="avatar">{{ leaderAvatarText }}</div>
              <div class="leader-copy">
                <div class="leader-name">{{ leader.displayName }}</div>
                <div class="muted">{{ store.name }} · {{ leader.followerCount }}人关注</div>
              </div>
              <van-icon name="arrow" color="var(--color-text-hint)" />
            </div>
            <div class="trust-row">
              <span class="trust">回复超快</span>
              <span class="trust">回头客多</span>
              <span class="trust">团长靠谱</span>
            </div>
            <div class="benefit-row">
              <div class="benefit"><span>售后协助</span><b>有保障</b></div>
              <div class="benefit"><span>社区团购</span><b>省心买</b></div>
            </div>
          </div>

          <div v-if="subscribed !== null" class="detail-subscribe-row">
            <button class="btn ghost" type="button" @click="goToLeader">店铺主页</button>
            <button
              class="btn primary"
              type="button"
              :disabled="subLoading"
              @click="toggleSubscribe"
            >
              {{ subscribed ? '已订阅' : '+ 订阅团长' }}
            </button>
          </div>

          <div id="section-items" class="summary-card">
            <ImageWithFallback
              :src="selectedItem?.coverImageUrl || groupBuy.coverImageUrl"
              width="142px"
              height="142px"
              fit="cover"
              radius="8px"
              :alt="selectedItem?.displayName || groupBuy.title"
            />
            <div class="summary-copy">
              <div class="summary-title">{{ selectedItem ? '已选团购商品' : groupBuy.title }}</div>
              <div class="sold">已团{{ totalSold }}</div>
              <div class="muted summary-spec">{{ selectedItem ? '规格已选择，可继续调整数量' : '请选择商品规格' }}</div>
              <div class="summary-buy">
                <PriceText :amount="selectedItem?.groupPriceAmount ?? minPriceAmount" size="xl" color="var(--color-price)" />
                <button
                  type="button"
                  class="btn primary"
                  :disabled="!isPurchasable"
                  @click="openSkuSheetForSummary"
                >
                  跟团购买
                </button>
              </div>
            </div>
          </div>

          <div class="detail-items">
            <h3 class="detail-items__title">商品列表</h3>
            <div
              v-for="item in items"
              :key="item.id"
              class="detail-item"
              :class="{ 'detail-item--selected': selectedItemId === item.id }"
            >
              <ImageWithFallback
                :src="item.coverImageUrl"
                width="80px"
                height="80px"
                fit="cover"
                radius="var(--radius-sm)"
                :alt="item.displayName"
              />
              <div class="detail-item__info">
                <span class="detail-item__name">{{ item.displayName }}</span>
                <PriceText :amount="item.groupPriceAmount" size="lg" color="var(--color-price)" />
                <div class="detail-item__stock">
                  <span>库存 {{ item.groupStock }}</span>
                  <span>已售 {{ item.soldCount }}</span>
                </div>
              </div>
              <van-button
                v-if="isPurchasable && item.groupStock > 0"
                size="small"
                round
                type="primary"
                @click="openSkuSheet(item)"
              >
                {{ selectedItemId === item.id ? '已选' : '选择' }}
              </van-button>
              <van-tag v-if="item.groupStock <= 0" type="danger" size="medium">已售罄</van-tag>
            </div>
          </div>

          <div v-if="groupBuy.introduction" class="copy-section">
            <h2 class="copy-title">{{ groupBuy.introduction }}</h2>
            <div class="bullet">
              <van-icon name="passed" />
              <span>团长精选普通团购，页面展示与下单链路均可真实联调。</span>
            </div>
            <div class="bullet">
              <van-icon name="passed" />
              <span>商品价格按整数分格式展示，提交订单仍使用后端金额口径。</span>
            </div>
            <div class="bullet">
              <van-icon name="passed" />
              <span>配送方式为 {{ deliveryText }}，发货时间以团购详情接口返回为准。</span>
            </div>
          </div>
        </section>

        <div class="section-gap" />

        <section id="section-showcase" class="detail-section">
          <div class="section-title">
            该团购所属团长主页晒单
            <span class="muted">查看全部</span>
          </div>
          <div class="show-tags">
            <span>包装完好 (53)</span>
            <span>价格实惠 (48)</span>
            <span>性价比高 (39)</span>
            <span>发货快 (37)</span>
          </div>
          <div class="placeholder">
            <strong>晒单 MVP 暂不展开</strong>
            这里保留晒单标签、查看全部、晒单卡片入口，不请求晒单或评价接口。
          </div>
          <div class="show-grid">
            <div class="show-card">
              <div class="show-photo">晒单图</div>
              <b>包装很好，价格也划算</b>
              <span class="muted small">2小时前 · 已购用户</span>
            </div>
            <div class="show-card">
              <div class="show-photo show-photo--cool">晒单图</div>
              <b>团长发货很及时</b>
              <span class="muted small">昨天 · 已购用户</span>
            </div>
          </div>
        </section>

        <div class="section-gap" />

        <section id="section-records" class="detail-section">
          <div class="section-title">跟团记录</div>
          <div class="section-note">MVP 先做轻量展示：序号、头像、时间、商品摘要、数量 +1。</div>
          <div class="record-list">
            <div v-for="record in recordRows" :key="record.num" class="record-row">
              <div class="record-row__num">{{ record.num }}</div>
              <div class="round-avatar">{{ record.avatar }}</div>
              <div>
                {{ record.time }}<br />
                <span>{{ record.label }}</span>
              </div>
              <b>+1</b>
            </div>
          </div>
          <div class="footer-brand">
            “包装完好”“价格实惠”<br />
            <b>邻鲜团</b> | 反馈与建议
          </div>
        </section>

        <button class="detail-fab-cart floating-cart-entry" type="button" aria-label="购物车" @click="openCartSheet">
          <van-icon name="cart-o" size="26" color="var(--color-primary)" />
        </button>
      </div>

      <div class="buybar detail-buybar">
        <button class="mini" type="button" @click="router.push('/')">
          <van-icon name="wap-home-o" size="23" />
          <span>主页</span>
        </button>
        <button class="mini" type="button" @click="router.push('/orders')">
          <van-icon name="orders-o" size="23" />
          <span>订单</span>
        </button>
        <button class="mini" type="button" @click="openCartSheet">
          <van-icon name="cart-o" size="23" />
          <span>购物车</span>
        </button>
        <button
          class="big"
          type="button"
          :disabled="!isPurchasable"
          @click="openSkuSheetForBuybar"
        >
          <div class="faces" aria-hidden="true">
            <span>买</span><span>团</span><span>邻</span>
          </div>
          {{ isPurchasable ? '跟团购买' : buyDisabledText }}
          <span v-if="isPurchasable" class="detail-buybar__sub">{{ totalSold }}人已跟团</span>
        </button>
      </div>
      <!-- SKU 规格弹窗 -->
      <SkuSheet
        v-model="skuSheetVisible"
        :item="skuTargetItem"
        @add-to-cart="onSkuAddToCart"
        @buy-now="onSkuBuyNow"
      />

      <!-- 购物车预览弹窗（本地内存，无后端购物车 API） -->
      <CartPreviewSheet
        v-model="cartSheetVisible"
        :items="cartItems"
        :total-amount="cartTotalAmount"
        @checkout="onCartCheckout"
      />
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
import PriceText from '@/components/PriceText.vue'
import { useAuthStore } from '@/stores'
import { useCheckoutStore } from '@/stores'
import { getPublicGroupBuyDetail } from '@/api/groupBuys'
import { subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { getDeliveryTypeText } from '@/utils'
import SkuSheet from '@/components/SkuSheet.vue'
import CartPreviewSheet from '@/components/CartPreviewSheet.vue'
import type { CartSheetItem } from '@/components/CartPreviewSheet.vue'
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

const groupBuy = ref<GroupBuyDetail | null>(null)
const leader = ref<LeaderDetail | null>(null)
const store = ref<StoreDetail | null>(null)
const items = ref<PublicGroupBuyDetailItem[]>([])
const viewer = ref<ViewerInfo | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const selectedItemId = ref<string | null>(null)
const subLoading = ref(false)

// SKU sheet & 购物车弹窗
const skuSheetVisible = ref(false)
const cartSheetVisible = ref(false)
const skuTargetItem = ref<PublicGroupBuyDetailItem | null>(null)
const cartItems = ref<CartSheetItem[]>([])

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
  if (!groupBuy.value?.deliveryType) return '快递'
  return getDeliveryTypeText(groupBuy.value.deliveryType)
})
const totalSold = computed(() => items.value.reduce((sum, item) => sum + item.soldCount, 0))
const minPriceAmount = computed(() => {
  if (items.value.length === 0) return 0
  return Math.min(...items.value.map(item => item.groupPriceAmount))
})
const selectedItem = computed(() => items.value.find(item => item.id === selectedItemId.value) ?? null)
const leaderAvatarText = computed(() => leader.value?.displayName.slice(0, 1) || store.value?.name.slice(0, 1) || '团')
const publishText = computed(() => {
  if (!groupBuy.value?.startTime) return '今天'
  return groupBuy.value.startTime.slice(0, 10)
})
const endText = computed(() => {
  if (!groupBuy.value?.endTime) return '限时开团中'
  return `${groupBuy.value.endTime.slice(0, 10)}后结束`
})
const viewCount = computed(() => Math.max(totalSold.value * 4 + 24, 128))
const hotRows = computed(() => {
  const label = groupBuy.value?.title || '热销商品'
  return [
    { avatar: '买', time: '1小时前', label },
    { avatar: '邻', time: '2小时前', label },
  ]
})
const recordRows = computed(() => {
  const label = groupBuy.value?.title || '团购商品'
  return [
    { num: Math.max(totalSold.value, 4), avatar: '买', time: '25分钟前', label },
    { num: Math.max(totalSold.value - 1, 3), avatar: '团', time: '53分钟前', label },
    { num: Math.max(totalSold.value - 2, 2), avatar: '邻', time: '1小时前', label },
    { num: Math.max(totalSold.value - 3, 1), avatar: '省', time: '2小时前', label },
  ]
})

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

/** 购物车商品总金额 */
const cartTotalAmount = computed(() =>
  cartItems.value.reduce((sum, item) => sum + item.unitPriceAmount * item.quantity, 0),
)

/** 打开 SKU 弹窗选择一个商品 */
function openSkuSheet(item: PublicGroupBuyDetailItem) {
  selectedItemId.value = item.id
  skuTargetItem.value = item
  skuSheetVisible.value = true
}

/** 摘要卡 "跟团购买" — 选第一个可买商品 */
function openSkuSheetForSummary() {
  if (items.value.length === 0) return
  const target = items.value.find(i => i.id === selectedItemId.value && i.groupStock > 0)
    ?? items.value.find(i => i.groupStock > 0)
  if (target) openSkuSheet(target)
  else showToast('暂无库存')
}

/** 底部购买栏 "跟团购买" — 同摘要逻辑 */
function openSkuSheetForBuybar() {
  openSkuSheetForSummary()
}

/** SKU sheet "加入购物车" → 本地内存 */
function onSkuAddToCart(payload: { itemId: string; quantity: number; deliveryType: string }) {
  const item = items.value.find(i => i.id === payload.itemId)
  if (!item) return
  const existing = cartItems.value.find(c => c.itemId === item.id)
  if (existing) {
    existing.quantity += payload.quantity
  } else {
    cartItems.value.push({
      itemId: item.id,
      productName: item.displayName,
      skuName: payload.deliveryType === 'express' ? '全国包邮' : payload.deliveryType === 'pickup' ? '同城自提' : '同城配送',
      quantity: payload.quantity,
      unitPriceAmount: item.groupPriceAmount,
    })
  }
  showToast('已加入购物车')
  cartSheetVisible.value = true
}

/** SKU sheet "立即购买" → checkout */
function onSkuBuyNow(payload: { itemId: string; quantity: number; deliveryType: string }) {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  if (!groupBuy.value) return
  const item = items.value.find(i => i.id === payload.itemId)
  if (!item) return
  checkoutStore.setCheckoutContext({
    groupBuyId: groupBuy.value.id,
    groupBuyItemId: payload.itemId,
    quantity: payload.quantity,
    title: groupBuy.value.title,
    coverImageUrl: groupBuy.value.coverImageUrl,
    displayName: item.displayName,
    unitPriceAmount: item.groupPriceAmount,
  })
  router.push('/checkout')
}

/** 打开购物车弹窗（本地内存，无后端） */
function openCartSheet() {
  if (cartItems.value.length === 0) {
    showToast('购物车为空')
    return
  }
  skuSheetVisible.value = false
  cartSheetVisible.value = true
}

/** 购物车弹窗 "去结算" — 汇总选中商品 */
function onCartCheckout() {
  const checkedItems = cartItems.value.filter(i => (i as CartSheetItem & { checked?: boolean }).checked !== false)
  if (checkedItems.length === 0) { showToast('请选择商品'); return }
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  if (!groupBuy.value) return
  // 用购物车中第一件商品跳到 checkout（MVP 简化：一次只买一种商品）
  const first = checkedItems[0]
  if (!first.itemId) { showToast('请选择商品'); return }
  const item = items.value.find(i => i.id === first.itemId)
  if (!item) return
  checkoutStore.setCheckoutContext({
    groupBuyId: groupBuy.value.id,
    groupBuyItemId: first.itemId,
    quantity: first.quantity,
    title: groupBuy.value.title,
    coverImageUrl: groupBuy.value.coverImageUrl,
    displayName: item.displayName,
    unitPriceAmount: item.groupPriceAmount,
  })
  router.push('/checkout')
}


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
      viewer.value!.subscribed = true
      showToast('已订阅')
    } else {
      showToast(apiErr.message || '操作失败')
    }
  } finally {
    subLoading.value = false
  }
}

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
.detail-page {
  position: relative;
  background: #fff;
}

.detail-topbar {
  height: 50px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.detail-topbar__back {
  position: absolute;
  left: 8px;
  width: 44px;
  min-height: 44px;
  border: 0;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
}

.detail-topbar__title {
  font-size: 18px;
  font-weight: 900;
}

.product-post {
  background: #fff;
}

.product-head {
  background: #fff;
  padding: 18px 18px 12px;
  border-bottom: 1px solid #f1f2f4;
}

.product-title-main {
  font-size: 25px;
  line-height: 1.25;
  font-weight: 900;
  margin: 0 0 12px;
  color: var(--color-text-primary);
}

.ship-chip {
  font-size: 15px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-radius: 4px;
  padding: 1px 5px;
  margin-left: 5px;
  vertical-align: middle;
  white-space: nowrap;
}

.hot-board {
  border-radius: 14px;
  background: linear-gradient(110deg, #ff6b1d, #ff912e 62%, #ffcf56);
  padding: 10px;
  margin: 10px 0 12px;
  color: #fff;
  box-shadow: 0 6px 18px rgba(255, 116, 36, 0.2);
}

.hot-board-title {
  font-weight: 900;
  font-size: 18px;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hot-board-title span {
  font-size: 15px;
  font-weight: 700;
  opacity: 0.95;
}

.hot-list {
  background: #fff;
  border: 2px solid #fff0df;
  border-radius: 10px;
  overflow: hidden;
}

.hot-row {
  width: 100%;
  border: 0;
  background: #fff;
  display: grid;
  grid-template-columns: 44px 1fr auto auto;
  gap: 9px;
  align-items: center;
  padding: 8px 9px;
  color: #888;
  border-bottom: 1px solid #fff0df;
  text-align: left;
  cursor: pointer;
}

.hot-row:last-child {
  border-bottom: 0;
}

.hot-row__copy {
  min-width: 0;
  line-height: 1.35;
}

.hot-row__copy b {
  color: #f06b2e;
  font-size: 17px;
}

.hot-row__copy span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-row__plus {
  color: #f06b2e;
  font-weight: 900;
}

.hot-row__go {
  background: var(--color-primary);
  color: #fff;
  border-radius: 7px;
  padding: 6px 9px;
  font-weight: 900;
  line-height: 1.05;
  text-align: center;
}

.hot-row__go small {
  display: block;
  font-size: 10px;
  background: #ffec95;
  color: #f06b2e;
  border-radius: 999px;
  margin: -2px -3px 2px;
}

.round-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #e7f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: var(--color-primary);
  flex: none;
}

.meta-block {
  font-size: 15px;
  line-height: 1.9;
  color: #9aa0a6;
}

.meta-block b {
  color: #f36b2a;
  font-weight: 800;
}

.meta-block span {
  margin: 0 8px;
}

.product-image-wrap {
  background: #fff;
  padding: 12px 14px 18px;
}

.product-image-wrap :deep(.image-with-fallback__placeholder) {
  background: linear-gradient(135deg, #dcefe0, #a1c49f);
}

.product-image-caption {
  font-size: 13px;
  color: #9aa0a6;
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
}

.anchor-tabs {
  height: 54px;
  background: #fff;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-items: end;
  border-top: 1px solid #eef0f2;
  border-bottom: 1px solid #eef0f2;
  position: sticky;
  top: 50px;
  z-index: 15;
}

.anchor-tabs button {
  height: 54px;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #5d626b;
  position: relative;
  cursor: pointer;
}

.anchor-tabs button.active {
  color: var(--color-primary);
  font-weight: 900;
}

.anchor-tabs button.active::after {
  content: '';
  position: absolute;
  left: 30%;
  right: 30%;
  bottom: 0;
  height: 3px;
  border-radius: 9px;
  background: var(--color-primary);
}

.section-gap {
  height: 10px;
  background: var(--color-bg);
}

.detail-section {
  background: #fff;
}

.leader-card {
  background: #fff;
  padding: 14px 14px 10px;
  cursor: pointer;
}

.leader-main {
  display: flex;
  gap: 12px;
  align-items: center;
}

.avatar {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff9827, #d87016);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  text-align: center;
  font-size: 20px;
  line-height: 1.1;
  flex: none;
  object-fit: cover;
}

.leader-copy {
  flex: 1;
  min-width: 0;
}

.leader-name {
  font-size: 22px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.trust-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.trust {
  border: 1px solid #ffc49b;
  color: #d2691e;
  border-radius: 4px;
  padding: 3px 7px;
  font-size: 13px;
  background: #fff;
}

.benefit-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 12px 0 0;
}

.benefit {
  background: #fff7de;
  border-radius: 12px;
  padding: 12px;
  color: #7a6740;
  font-weight: 800;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.detail-subscribe-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 0 14px 12px;
}

.summary-card {
  background: #fff;
  margin: 12px 14px;
  border-radius: 14px;
  padding: 14px;
  display: grid;
  grid-template-columns: 142px minmax(0, 1fr);
  gap: 12px;
  box-shadow: var(--shadow-card);
}

.summary-copy {
  min-width: 0;
}

.summary-title {
  font-size: 20px;
  line-height: 1.3;
  font-weight: 900;
  color: var(--color-text-primary);
}

.sold {
  color: #f06b2e;
  font-weight: 900;
  font-size: 17px;
  text-align: right;
  margin-top: 6px;
}

.summary-spec {
  margin: 8px 0 12px;
}

.summary-buy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-items {
  background: #fff;
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

.detail-item--selected {
  background: #f8fff9;
  margin: 0 -8px;
  padding: 12px 8px;
  border-radius: 10px;
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

.copy-section {
  background: #fff;
  padding: 14px 18px 22px;
}

.copy-title {
  font-size: 24px;
  line-height: 1.42;
  margin: 8px 0 22px;
  font-weight: 900;
}

.bullet {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 0;
  font-size: 20px;
  line-height: 1.5;
  font-weight: 900;
  margin: 20px 0;
}

.bullet :deep(.van-icon) {
  color: var(--color-primary);
  margin-top: 4px;
}

.section-title {
  background: #fff;
  padding: 18px 18px 10px;
  font-size: 22px;
  font-weight: 900;
  border-bottom: 1px solid #f2f3f4;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.section-title .muted {
  font-size: 16px;
  font-weight: 500;
}

.section-note {
  padding: 8px 18px 14px;
  color: #9aa0a6;
  font-size: 13px;
  background: #fff;
}

.show-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  background: #fff;
  padding: 18px;
}

.show-tags span {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-radius: 7px;
  padding: 9px 13px;
  font-size: 17px;
  font-weight: 900;
}

.placeholder {
  margin: 0 14px 12px;
  background: #fff;
  border: 1px dashed var(--color-border);
  border-radius: 14px;
  padding: 16px;
  color: #7a808a;
  line-height: 1.55;
}

.placeholder strong {
  display: block;
  font-size: 17px;
  color: #222;
  margin-bottom: 5px;
}

.show-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  background: #fff;
  padding: 0 18px 18px;
}

.show-card {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 10px;
  background: #fff;
}

.show-card b {
  display: block;
  margin-bottom: 4px;
}

.show-photo {
  height: 120px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffd6af, #dff5df);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
  margin-bottom: 8px;
}

.show-photo--cool {
  background: linear-gradient(135deg, #c7e8ff, #bdebcf);
}

.record-list {
  background: #fff;
  padding: 18px;
}

.record-row {
  display: grid;
  grid-template-columns: 36px 46px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin: 14px 0;
  color: #999;
  font-size: 16px;
}

.record-row__num {
  font-size: 20px;
  color: #666;
}

.record-row span {
  color: #9ca0a6;
}

.record-row b {
  color: #f06b2e;
}

.footer-brand {
  background: #fff;
  text-align: center;
  color: #9aa0a6;
  padding: 18px 0 110px;
}

.footer-brand b {
  color: var(--color-primary);
  font-size: 24px;
}

.detail-fab-cart {
  bottom: calc(92px + var(--safe-area-bottom));
  border: 0;
}

.detail-buybar {
  grid-template-columns: 65px 65px 65px 1fr;
}

.detail-buybar .mini {
  color: #555;
}

.detail-buybar__sub {
  font-size: 13px;
  font-weight: 700;
  margin-top: 2px;
}

@media (max-width: 374px) {
  .summary-card {
    grid-template-columns: 112px minmax(0, 1fr);
  }

  .summary-card :deep(.image-with-fallback) {
    width: 112px !important;
    height: 112px !important;
  }

  .summary-buy {
    align-items: flex-start;
    flex-direction: column;
  }

  .hot-row {
    grid-template-columns: 36px 1fr auto;
  }

  .hot-row__go {
    grid-column: 2 / 4;
    width: fit-content;
  }
}
</style>
