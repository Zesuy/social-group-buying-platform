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
        </div>

        <section class="leader-hero">
          <span>店铺头图 / 客服信息</span>
        </section>

        <section class="leader-header leader-store-card">
          <div class="leader-store-card__main">
            <div class="leader-store-card__identity">
              <img
                v-if="leaderData.avatarUrl"
                :src="leaderData.avatarUrl"
                class="leader-store-card__avatar"
                :alt="`${leaderData.displayName}头像`"
              />
              <div v-else class="leader-store-card__avatar leader-store-card__avatar--fallback">
                {{ avatarText }}
                <span>金牌</span>
              </div>
              <div class="leader-store-card__copy">
                <h1>{{ storeData?.name || leaderData.displayName }}</h1>
                <p>{{ leaderData.bio || '社区好物精选团长' }}</p>
                <div class="leader-store-card__stats">
                  <span>粉丝 {{ leaderData.followerCount }}</span>
                  <span>会员 {{ leaderData.memberCount }}</span>
                </div>
              </div>
            </div>
            <div class="leader-store-card__actions">
              <button class="btn" type="button" @click="onServiceClick">
                <van-icon name="service-o" />
                客服
              </button>
              <button
                class="btn primary"
                type="button"
                :disabled="subLoading"
                @click="toggleSubscribe"
              >
                {{ subscribed ? '已订阅' : '+订阅' }}
              </button>
            </div>
          </div>

          <div class="leader-coupon">
            <span>新人立减5元</span>
            <button type="button" class="btn orange" @click="onCouponClick">领取</button>
          </div>

          <div class="leader-benefits">
            <button type="button" class="leader-benefit" @click="onMemberClick">
              <span>会员权益</span>
              <b>最高9.0折</b>
            </button>
            <button type="button" class="leader-benefit" @click="onPointsClick">
              <span>积分商城</span>
              <b>跟团赢积分</b>
            </button>
          </div>
        </section>

        <button type="button" class="leader-showcase" @click="onShowcaseClick">
          <b>团员晒单</b>
          <span>“收到啦，包装很好，价格也划算...”</span>
          <div class="leader-showcase__photo">晒单图</div>
        </button>

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
                @click="goToDetail(item.id)"
                @share="onShareClick"
                @subscribe="toggleSubscribe"
              />
              <EmptyState v-if="groupBuys.length === 0 && !listLoading" description="暂无团购活动" />
            </van-list>
          </van-pull-refresh>
        </section>
      </div>
    </template>
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
import EmptyState from '@/components/EmptyState.vue'
import { useAuthStore } from '@/stores'
import { getLeaderHomepage, subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { isFeatureDisabled } from '@/utils/non-mvp'
import type { LeaderHomepageLeader, LeaderHomepageStore, PublicGroupBuyItem } from '@/types'

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

const sortTabs = [
  { key: 'default', label: '默认' },
  { key: 'newest', label: '上新' },
  { key: 'sales', label: '销量' },
]

const avatarText = computed(() => {
  const name = storeData.value?.name || leaderData.value?.displayName || '团'
  return name.slice(0, 1)
})

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const id = route.params.id as string
    const data = await getLeaderHomepage(id)
    leaderData.value = data.leader
    storeData.value = data.store
    subscribed.value = data.viewer.subscribed
    groupBuys.value = data.groupBuys.items
    hasMore.value = data.groupBuys.hasMore
    listPage.value = 1
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMoreGroupBuys() {
  if (listLoading.value || !hasMore.value) return
  listLoading.value = true
  try {
    const id = route.params.id as string
    const nextPage = listPage.value + 1
    const data = await getLeaderHomepage(id, nextPage)
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
    } else {
      await subscribeLeader(id, 'homepage')
      subscribed.value = true
      showToast('订阅成功')
    }
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    if (apiErr.code === 'SUBSCRIPTION_EXISTS') {
      subscribed.value = true
      showToast('已订阅')
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

function onCouponClick() {
  if (isFeatureDisabled('coupon')) {
    showToast('优惠券不在 MVP 范围内')
  }
}

function onMemberClick() {
  if (isFeatureDisabled('memberCards')) {
    showToast('会员卡展示后续开放')
  }
}

function onPointsClick() {
  if (isFeatureDisabled('pointsMall')) {
    showToast('积分商城不在 MVP 范围内')
  }
}

function onServiceClick() {
  showToast('客服入口仅作占位展示')
}

function onShowcaseClick() {
  showToast('晒单内容暂不展开')
}

function onShareClick() {
  showToast('分享能力仅作占位展示')
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
  padding-bottom: 18px;
}

.leader-topbar {
  height: 58px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.leader-topbar__back {
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

.leader-topbar__title {
  font-size: 20px;
  font-weight: 900;
}

.leader-hero {
  height: 190px;
  background: linear-gradient(135deg, #bdd4ff, #ffd0c0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
  font-size: 24px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
}

.leader-store-card {
  background: #fff;
  border-radius: var(--radius-card);
  margin: -30px 14px 12px;
  padding: 14px;
  box-shadow: var(--shadow-card);
  position: relative;
  z-index: 4;
}

.leader-store-card__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.leader-store-card__identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.leader-store-card__avatar {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  object-fit: cover;
  flex: none;
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

.leader-store-card__copy h1 {
  font-size: 19px;
  font-weight: 900;
  color: var(--color-text-primary);
  line-height: 1.35;
  margin: 0;
}

.leader-store-card__copy p {
  margin: 6px 0 0;
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.leader-store-card__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #f36b2a;
  font-size: var(--font-size-xs);
  font-weight: 800;
  margin-top: 8px;
}

.leader-store-card__actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.leader-coupon {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.leader-coupon span {
  background: var(--color-price);
  border-radius: 9px;
  color: #fff;
  padding: 8px 13px;
  font-weight: 900;
}

.leader-benefits {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

.leader-benefit {
  min-height: 58px;
  border: 0;
  background: #fff7de;
  border-radius: 12px;
  padding: 10px 12px;
  color: #7a6740;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  text-align: left;
  cursor: pointer;
}

.leader-benefit b {
  font-size: var(--font-size-sm);
}

.leader-showcase {
  min-height: 76px;
  width: calc(100% - 28px);
  margin: 0 14px 12px;
  border: 0;
  background: #fff;
  border-radius: 12px;
  padding: 13px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-primary);
  text-align: left;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.leader-showcase span {
  flex: 1;
  min-width: 0;
  color: var(--color-text-hint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leader-showcase__photo {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(145deg, #f8cadc, #9f89df);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: 900;
  flex: none;
}

.leader-tabs {
  display: flex;
  gap: 28px;
  align-items: center;
  background: #fff;
  padding: 0 18px;
  height: 54px;
  margin-bottom: 12px;
}

.leader-tabs button {
  height: 54px;
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 18px;
  position: relative;
  cursor: pointer;
}

.leader-tabs button.active {
  color: var(--color-text-primary);
  font-weight: 900;
}

.leader-tabs button.active::after {
  content: '';
  position: absolute;
  left: 6px;
  right: 6px;
  bottom: 0;
  height: 3px;
  background: var(--color-primary);
  border-radius: 6px;
}

.leader-feed {
  padding: 0 12px;
}

@media (max-width: 374px) {
  .leader-store-card__main {
    flex-direction: column;
  }

  .leader-store-card__actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
