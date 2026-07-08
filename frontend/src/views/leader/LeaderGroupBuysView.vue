<template>
  <PageLayout title="团购管理" show-back @back="goBack">
    <template #action>
      <AppFixedActions single>
        <AppButton variant="primary" @click="goToNew">新建团购</AppButton>
      </AppFixedActions>
    </template>

    <LoadingView v-if="firstLoading" />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="group-buys-content">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab title="进行中" name="published" />
        <van-tab title="已结束" name="ended" />
      </van-tabs>

      <div class="group-buys-list-wrap">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            :loading="loading"
            :finished="!hasMore"
            finished-text="没有更多了"
            :error="error !== null"
            error-text="加载失败，点击重试"
            :immediate-check="false"
            @load="loadMore"
          >
            <GroupBuyManageCard
              v-for="gb in items"
              :key="gb.id"
              :group-buy="gb"
              :clickable="true"
              @click="goToDetail(gb.id)"
            >
              <template #actions>
                <AppButton
                  v-if="gb.status === 'published'"
                  variant="ghost"
                  size="sm"
                  :loading="shareLoadingId === gb.id"
                  @click.stop="openShare(gb.id)"
                >
                  分享
                </AppButton>
                <AppButton variant="primary" size="sm" @click.stop="goToDetail(gb.id)">详情</AppButton>
              </template>
            </GroupBuyManageCard>

            <EmptyState v-if="isEmpty" description="暂无关团购" />
          </van-list>
        </van-pull-refresh>
      </div>
    </div>
    <GroupBuyShareSheet
      v-if="shareCard"
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
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import GroupBuyManageCard from '@/components/GroupBuyManageCard.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import { usePagination, useSmartNavigation } from '@/composables'
import { getMyGroupBuyShareCard, listMyGroupBuys } from '@/api/leaderGroupBuys'
import { buildShareTokenUrl } from '@/utils'
import type { GroupBuyManageData, ShareCardData } from '@/types'

const router = useRouter()
const { goBack } = useSmartNavigation('/leader/dashboard')
const activeTab = ref('published')
const shareSheetVisible = ref(false)
const shareCard = ref<ShareCardData | null>(null)
const shareLoadingId = ref<string | null>(null)

const {
  items,
  loading,
  refreshing,
  error,
  hasMore,
  isEmpty,
  initialized,
  load,
  refresh,
  loadMore,
  reset,
} = usePagination<GroupBuyManageData>(
  (page, pageSize) => listMyGroupBuys(activeTab.value, page, pageSize),
)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)
const shareUrl = computed(() => shareCard.value ? buildShareTokenUrl(shareCard.value.shareToken) : '')
const sharePayload = computed<GroupBuySharePayload>(() => ({
  title: shareCard.value?.title || '团购分享',
  coverImageUrl: shareCard.value?.coverImageUrl ?? null,
  minPriceAmount: shareCard.value?.minPriceAmount ?? null,
  maxPriceAmount: shareCard.value?.maxPriceAmount ?? null,
  storeName: shareCard.value?.storeName || '团长店铺',
  leaderName: shareCard.value?.leaderName || '团长',
  deliveryType: shareCard.value?.deliveryType ?? null,
  shippingTime: shareCard.value?.shippingTime ?? null,
}))

function onTabChange() {
  reset()
  load()
}

async function onRefresh() {
  await refresh()
  if (error.value) {
    showToast('刷新失败')
  }
}

function goToNew() {
  router.push('/leader/group-buys/new')
}

function goToDetail(id: string) {
  router.push(`/leader/group-buys/${id}`)
}

async function openShare(id: string) {
  shareLoadingId.value = id
  try {
    shareCard.value = await getMyGroupBuyShareCard(id)
    shareSheetVisible.value = true
  } catch (err) {
    showToast((err as { message?: string }).message || '分享卡片生成失败')
  } finally {
    shareLoadingId.value = null
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-actions {
  padding: 8px 14px;
}

.group-buys-content {
  background: var(--color-bg);
  min-height: 200px;
}
.group-buys-list-wrap {
  padding: 14px;
}
</style>
