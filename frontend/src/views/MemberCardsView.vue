<template>
  <PageLayout title="我的会员卡" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchMemberCards" />

    <div v-else class="member-cards-content">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <div v-if="items.length > 0" class="member-cards-list">
          <MemberCardItem
            v-for="card in items"
            :key="card.id"
            :card="card"
            @click="goToLeader(card.leader.id)"
          />
        </div>

        <EmptyState v-else description="暂无会员卡" />
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
import MemberCardItem from '@/components/MemberCardItem.vue'
import { listMyMemberCards } from '@/api/memberCards'
import type { MemberCardData } from '@/types'

const router = useRouter()

const loading = ref(true)
const refreshing = ref(false)
const error = ref<string | null>(null)
const items = ref<MemberCardData[]>([])

async function fetchMemberCards() {
  loading.value = true
  error.value = null
  try {
    const data = await listMyMemberCards()
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
    const data = await listMyMemberCards()
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
  fetchMemberCards()
})
</script>

<style scoped>
.member-cards-content {
  padding: 14px;
  min-height: 200px;
}

.member-cards-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
