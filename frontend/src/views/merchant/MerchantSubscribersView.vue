<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>私域关系</p>
        <h1>订阅用户</h1>
      </div>
      <div class="summary-chip">共 {{ total }} 位订阅用户</div>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchSubscribers" />

    <section v-else class="table-panel">
      <table class="merchant-table">
        <thead>
          <tr>
            <th>用户</th>
            <th>手机号</th>
            <th>来源</th>
            <th>订阅时间</th>
            <th>Subscription ID</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in visibleItems"
            :key="item.subscriptionId"
            :class="{ focused: item.subscriptionId === focusedSubscriptionId }"
          >
            <td>
              <div class="user-cell">
                <img v-if="avatarUrl(item)" :src="avatarUrl(item)" :alt="displayName(item)" />
                <span v-else>{{ displayName(item).slice(0, 1) }}</span>
                <strong>{{ displayName(item) }}</strong>
              </div>
            </td>
            <td>{{ formatPhone(item.phone) }}</td>
            <td>{{ sourceText(item.source) }}</td>
            <td>{{ formatDateTime(item.subscribedAt) }}</td>
            <td><code>{{ item.subscriptionId }}</code></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="visibleItems.length === 0" description="暂无订阅用户" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import ErrorView from '@/components/ErrorView.vue'
import LoadingView from '@/components/LoadingView.vue'
import { listMySubscribers } from '@/api/subscriptions'
import { formatDateTime, formatPhone, resolveDisplayImageUrl } from '@/utils'
import type { LeaderSubscriberData } from '@/types'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const items = ref<LeaderSubscriberData[]>([])
const total = computed(() => items.value.length)
const focusedSubscriptionId = computed(() => typeof route.query.subscriptionId === 'string' ? route.query.subscriptionId : '')
const visibleItems = computed(() => {
  if (!focusedSubscriptionId.value) return items.value
  const focused = items.value.find((item) => item.subscriptionId === focusedSubscriptionId.value)
  if (!focused) return items.value
  return [focused, ...items.value.filter((item) => item.subscriptionId !== focusedSubscriptionId.value)]
})

function displayName(item: LeaderSubscriberData): string {
  return item.nickname || `用户 ${item.userId.slice(-6)}`
}

function avatarUrl(item: LeaderSubscriberData): string | undefined {
  return resolveDisplayImageUrl(item.avatarUrl, displayName(item), 'avatar') || undefined
}

function sourceText(source: string | null): string {
  const map: Record<string, string> = {
    homepage: '主页订阅',
    groupBuyDetail: '团购详情',
    indexFeed: '首页订阅',
  }
  return source ? map[source] ?? source : '自然订阅'
}

async function fetchSubscribers() {
  loading.value = true
  error.value = ''
  try {
    const data = await listMySubscribers()
    items.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '订阅用户加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(fetchSubscribers)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.page-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.page-head h1 {
  margin: 4px 0 0;
  font-size: 26px;
}

.summary-chip {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 8px;
  background: #fff1ed;
  color: #d63f2b;
  font-size: 13px;
  font-weight: 900;
}

.table-panel {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.merchant-table {
  width: 100%;
  min-width: 840px;
  border-collapse: collapse;
}

.merchant-table th,
.merchant-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  font-size: 13px;
}

.merchant-table th {
  background: #f9fafb;
  color: #6b7280;
  font-weight: 900;
}

.merchant-table tr.focused {
  background: #fff7ed;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell img,
.user-cell span {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.user-cell img {
  object-fit: cover;
}

.user-cell span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef2f7;
  color: #374151;
  font-weight: 900;
}

code {
  color: #b42318;
  font-weight: 900;
}
</style>
