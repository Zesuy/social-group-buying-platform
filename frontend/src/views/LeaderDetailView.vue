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
          <button type="button" class="leader-topbar__search" @click="onSearchClick">
            <van-icon name="search" size="19" />
            <span>搜索团购...</span>
          </button>
          <button type="button" class="leader-topbar__icon" aria-label="分享" @click="onShareClick">
            <van-icon name="share-o" size="22" />
          </button>
          <button type="button" class="leader-topbar__more" aria-label="更多">
            <span></span><span></span><span></span>
          </button>
        </div>

        <section class="leader-hero">
          <button type="button" class="leader-edit-home" @click="onEditHomepageClick">编辑主页</button>
        </section>

        <AppCard class="leader-store-card">
          <div class="leader-subscribe-tip">邀请团员订阅，开团消息及时通知</div>
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
                <h1>{{ leaderData.displayName }}</h1>
                <p>成员 {{ leaderData.memberCount }} ｜ 关注 {{ leaderData.followerCount }}</p>
                <div class="leader-store-card__stats">
                  <span>{{ storeData?.name || '团长小店' }}</span>
                </div>
              </div>
            </div>
            <div class="leader-store-card__actions">
              <button type="button" class="leader-action-icon" @click="onServiceClick">
                <van-icon name="chat-o" />
                <span>客服</span>
              </button>
              <button
                type="button"
                class="leader-action-icon leader-action-icon--subscribe"
                :disabled="subLoading"
                @click="toggleSubscribe"
              >
                <van-icon :name="subscribed ? 'bookmark' : 'bookmark-o'" />
                <span>{{ subscribed ? '已订阅' : '订阅邀请' }}</span>
              </button>
            </div>
          </div>

          <div class="leader-profile-lines">
            <div class="leader-profile-line">
              <van-icon name="description-o" />
              <span>{{ leaderData.bio || `我是团长${leaderData.displayName}，欢迎您来到我的团购` }}</span>
            </div>
            <button type="button" class="leader-profile-line" @click="onLocationClick">
              <van-icon name="location-o" />
              <span>添加位置，让更多的人购买</span>
              <b>更多信息</b>
              <van-icon name="arrow" />
            </button>
          </div>
        </AppCard>

        <div class="leader-follow-banner">
          <span>关注公众号，收到活动和订单、物流通知</span>
          <button type="button" @click="onWechatNoticeClick">关注</button>
          <van-icon name="cross" />
        </div>

        <AppCard class="leader-helper-card">
          <div class="leader-helper-row">
            <span>帮卖介绍</span>
            <button type="button" @click="onDistributionClick">去填写</button>
          </div>
          <button type="button" class="leader-album-row" @click="onShowcaseClick">
            <van-icon name="photo-o" />
            <span>相册素材号</span>
            <van-icon name="arrow" />
          </button>
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
const leaderAvatarUrl = computed(() => leaderData.value?.avatarUrl || storeData.value?.logoUrl || null)
const isOwnLeader = computed(() => authStore.leader?.id === leaderData.value?.id)

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

function onServiceClick() {
  showToast('客服入口仅作占位展示')
}

function onShowcaseClick() {
  showToast('晒单内容暂不展开')
}

function onShareClick() {
  showToast('分享能力仅作占位展示')
}

function onSearchClick() {
  showToast('团长主页搜索暂未开放')
}

function onEditHomepageClick() {
  if (isOwnLeader.value) {
    router.push('/leader/store')
    return
  }
  showToast('仅团长本人可编辑主页')
}

function onLocationClick() {
  showToast('位置展示后续开放')
}

function onDistributionClick() {
  if (isFeatureDisabled('distribution')) {
    showToast('帮卖介绍不在 MVP 范围内')
  }
}

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
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
  height: 84px;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 18px 14px 8px;
}

.leader-topbar__back,
.leader-topbar__icon,
.leader-topbar__more {
  height: 44px;
  border: 0;
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.leader-topbar__back,
.leader-topbar__icon {
  width: 44px;
  border-radius: 50%;
}

.leader-topbar__search {
  height: 44px;
  min-width: 0;
  flex: 1;
  border: 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  color: #8a929d;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  font-size: 15px;
}

.leader-topbar__search span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leader-topbar__more {
  width: 74px;
  border-radius: 999px;
  gap: 5px;
}

.leader-topbar__more span {
  width: 6px;
  height: 6px;
  background: currentColor;
  border-radius: 50%;
}

.leader-hero {
  height: 230px;
  margin-top: -1px;
  background:
    radial-gradient(circle at 18% 28%, rgba(255, 255, 255, 0.14) 0 30px, transparent 31px),
    radial-gradient(circle at 74% 42%, rgba(255, 255, 255, 0.12) 0 36px, transparent 37px),
    linear-gradient(180deg, #0bbf67, #08b960);
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 0 18px 82px;
  overflow: hidden;
}

.leader-hero::before {
  content: '□  ▱  ○  ◇  ◎  ♡  ⌂  ✚  ◌  ▣';
  position: absolute;
  inset: 14px 16px auto;
  color: rgba(255, 255, 255, 0.13);
  font-size: 46px;
  line-height: 1.8;
  letter-spacing: 17px;
  word-break: break-all;
}

.leader-edit-home {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(0, 0, 0, 0.14);
  color: #fff;
  border-radius: 8px;
  min-height: 34px;
  padding: 0 12px;
  font-weight: 900;
}

.leader-store-card {
  background: #fff;
  border-radius: 18px 18px 0 0;
  margin: -34px 0 0;
  padding: 0;
  box-shadow: none;
  position: relative;
  z-index: 4;
  overflow: visible;
}

.leader-subscribe-tip {
  position: absolute;
  right: 24px;
  top: -38px;
  background: rgba(0, 0, 0, 0.72);
  color: #fff;
  border-radius: 7px;
  padding: 7px 10px;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.leader-store-card__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 26px 18px 12px;
}

.leader-store-card__identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.leader-store-card__avatar {
  width: 78px;
  height: 78px;
  border-radius: 10px;
  object-fit: cover;
  flex: none;
  margin-top: -56px;
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

.leader-store-card__copy h1 {
  font-size: 22px;
  font-weight: 900;
  color: var(--color-text-primary);
  line-height: 1.35;
  margin: 0;
}

.leader-store-card__copy p {
  margin: 5px 0 0;
  color: var(--color-text-hint);
  font-size: 13px;
  line-height: 1.45;
}

.leader-store-card__stats {
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
  margin-top: 5px;
}

.leader-store-card__actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.leader-action-icon {
  border: 0;
  background: transparent;
  color: #7a808a;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 54px;
  font-size: 13px;
  font-weight: 700;
}

.leader-action-icon .van-icon {
  color: var(--color-primary);
  font-size: 31px;
}

.leader-action-icon--subscribe {
  position: relative;
}

.leader-action-icon--subscribe::after {
  content: '';
  position: absolute;
  right: 7px;
  top: 1px;
  width: 10px;
  height: 10px;
  background: #f25541;
  border: 2px solid #fff;
  border-radius: 50%;
}

.leader-profile-lines {
  border-top: 1px solid var(--color-border-light);
  padding: 9px 18px 12px;
}

.leader-profile-line {
  min-height: 34px;
  border: 0;
  background: transparent;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0;
  color: #717780;
  font-size: 15px;
  text-align: left;
}

.leader-profile-line > span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leader-profile-line b {
  color: #9aa0a6;
  font-size: 14px;
  font-weight: 500;
}

.leader-follow-banner {
  min-height: 58px;
  background: #fff7e6;
  color: #e96c2b;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  font-weight: 800;
}

.leader-follow-banner span {
  flex: 1;
  min-width: 0;
}

.leader-follow-banner button {
  border: 0;
  border-radius: 9px;
  background: var(--color-primary);
  color: #fff;
  min-height: 36px;
  padding: 0 18px;
  font-weight: 900;
}

.leader-follow-banner > .van-icon {
  color: #b8a995;
  font-size: 18px;
}

.leader-helper-card {
  margin: 12px 14px;
  border-radius: 12px;
}

.leader-helper-row {
  min-height: 58px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 18px;
  font-weight: 900;
  border-bottom: 1px solid var(--color-border-light);
}

.leader-helper-row button {
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  background: #fff;
  color: var(--color-primary);
  min-height: 36px;
  padding: 0 18px;
  font-size: 15px;
  font-weight: 900;
}

.leader-album-row {
  width: 100%;
  min-height: 58px;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 800;
  text-align: left;
}

.leader-album-row span {
  flex: 1;
}

.leader-tabs {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #fff;
  padding: 0;
  height: 64px;
  margin: 0;
}

.leader-tabs button {
  height: 64px;
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 19px;
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

@media (max-width: 374px) {
  .leader-topbar {
    gap: 7px;
    padding-inline: 10px;
  }

  .leader-topbar__more {
    width: 58px;
  }

  .leader-store-card__actions {
    gap: 8px;
  }

  .leader-action-icon {
    min-width: 44px;
    font-size: 12px;
  }
}
</style>
