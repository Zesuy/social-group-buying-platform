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
                <AppButton variant="primary" size="sm" @click.stop="goToDetail(gb.id)">详情</AppButton>
              </template>
            </GroupBuyManageCard>

            <EmptyState v-if="isEmpty" description="暂无关团购" />
          </van-list>
        </van-pull-refresh>
      </div>
    </div>
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
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import { usePagination } from '@/composables/usePagination'
import { listMyGroupBuys } from '@/api/leaderGroupBuys'
import type { GroupBuyManageData } from '@/types'

const router = useRouter()
const activeTab = ref('published')

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

function goBack() {
  router.back()
}

function goToNew() {
  router.push('/leader/group-buys/new')
}

function goToDetail(id: string) {
  router.push(`/leader/group-buys/${id}`)
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
