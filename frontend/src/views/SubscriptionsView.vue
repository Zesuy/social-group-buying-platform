<template>
  <PageLayout title="我的订阅" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchSubscriptions" />

    <div v-else class="subscriptions-content">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <div v-if="items.length > 0" class="subscriptions-list">
          <SubscriptionCard
            v-for="sub in items"
            :key="sub.id"
            :subscription="sub"
            @click="goToLeader(sub.leaderId)"
          />
        </div>

        <EmptyState v-else description="暂无订阅" />
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import SubscriptionCard from '@/components/SubscriptionCard.vue'
import { listMySubscriptions } from '@/api/subscriptions'
import type { SubscriptionListItem } from '@/types'

const router = useRouter()

const loading = ref(true)
const refreshing = ref(false)
const error = ref<string | null>(null)
const items = ref<SubscriptionListItem[]>([])

async function fetchSubscriptions() {
  loading.value = true
  error.value = null
  try {
    const data = await listMySubscriptions()
    items.value = data.items
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  error.value = null
  try {
    const data = await listMySubscriptions()
    items.value = data.items
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '刷新失败'
    showToast('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function goToLeader(leaderId: string) {
  router.push(`/leaders/${leaderId}`)
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchSubscriptions()
})
</script>

<style scoped>
.subscriptions-content {
  padding: 14px;
  min-height: 200px;
}

.subscriptions-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
