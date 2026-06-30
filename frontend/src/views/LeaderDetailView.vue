<template>
  <PageLayout title="团长主页" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="fetchData" />

    <template v-if="leaderData && !loading">
      <!-- 团长信息 -->
      <div class="leader-header">
        <img
          :src="leaderData.avatarUrl || undefined"
          class="leader-header__avatar"
          alt=""
        />
        <div class="leader-header__info">
          <h2 class="leader-header__name">{{ leaderData.displayName }}</h2>
          <p v-if="leaderData.bio" class="leader-header__bio">{{ leaderData.bio }}</p>
          <div class="leader-header__stats">
            <span>粉丝 {{ leaderData.followerCount }}</span>
            <span class="leader-header__dot">|</span>
            <span>会员 {{ leaderData.memberCount }}</span>
          </div>
        </div>
      </div>

      <!-- 店铺摘要 -->
      <div class="leader-section">
        <StoreSummaryCard
          v-if="storeData"
          :store="storeData"
        />
      </div>

      <!-- 订阅按钮 -->
      <div class="leader-section">
        <van-button
          round
          block
          :type="subscribed ? 'default' : 'primary'"
          :loading="subLoading"
          @click="toggleSubscribe"
        >
          {{ subscribed ? '已订阅' : '订阅团长' }}
        </van-button>
      </div>

      <!-- 团购列表 -->
      <div class="leader-section">
        <h3 class="leader-section__title">他的团购</h3>
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
            />
            <EmptyState v-if="groupBuys.length === 0 && !listLoading" description="暂无团购活动" />
          </van-list>
        </van-pull-refresh>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import StoreSummaryCard from '@/components/StoreSummaryCard.vue'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useAuthStore } from '@/stores'
import { getLeaderHomepage } from '@/api/leaders'
import { subscribeLeader, unsubscribeLeader } from '@/api/leaders'
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

// 分页
const listPage = ref(1)
const listLoading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)
const subLoading = ref(false)

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
.leader-header {
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary-deep-light));
  padding: 28px 20px 34px;
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
  color: #fff;
}

.leader-header__avatar {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  flex-shrink: 0;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.leader-header__info {
  flex: 1;
  min-width: 0;
}

.leader-header__name {
  font-size: 22px;
  font-weight: 900;
  color: #fff;
  margin-bottom: 4px;
}

.leader-header__bio {
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 4px;
  line-height: 1.4;
}

.leader-header__stats {
  display: flex;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.7);
}

.leader-header__dot {
  color: rgba(255, 255, 255, 0.3);
}

.leader-section {
  padding: 12px 14px 0;
}

.leader-section__title {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: 10px;
  color: var(--color-text-primary);
}

.leader-section :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}
</style>
