<template>
  <PageLayout title="订阅用户" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchSubscribers" />

    <div v-else class="leader-subscribers">
      <section class="subscriber-summary">
        <div>
          <span>当前订阅用户</span>
          <strong>{{ total }}</strong>
        </div>
        <p>这些团员会沉淀为你的复购关系，后续开团通知会围绕这批用户展开。</p>
      </section>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <div v-if="items.length > 0" class="subscriber-list">
          <article
            v-for="item in visibleItems"
            :key="item.subscriptionId"
            class="subscriber-card"
            :class="{ 'subscriber-card--focused': isFocusedSubscriber(item) }"
          >
            <img
              v-if="avatarUrl(item)"
              :src="avatarUrl(item)"
              class="subscriber-card__avatar"
              :alt="`${displayName(item)}头像`"
            />
            <span v-else class="subscriber-card__avatar subscriber-card__avatar--fallback">
              {{ displayName(item).slice(0, 1) }}
            </span>

            <div class="subscriber-card__body">
              <div class="subscriber-card__head">
                <strong>{{ displayName(item) }}</strong>
                <span v-if="isFocusedSubscriber(item)" class="subscriber-card__focus-badge">
                  本次新增订阅
                </span>
                <span v-else>{{ sourceText(item.source) }}</span>
              </div>
              <p>
                <van-icon name="phone-o" />
                {{ formatPhone(item.phone) }}
              </p>
              <p>
                <van-icon name="clock-o" />
                {{ formatDateTime(item.subscribedAt) }}
              </p>
            </div>
          </article>
        </div>

        <EmptyState
          v-else
          description="暂无订阅用户"
        />
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listMySubscribers } from '@/api/subscriptions'
import { useSmartNavigation } from '@/composables'
import { formatDateTime, formatPhone, resolveDisplayImageUrl } from '@/utils'
import type { LeaderSubscriberData } from '@/types'

const route = useRoute()
const { goBack } = useSmartNavigation('/leader/dashboard')

const loading = ref(true)
const refreshing = ref(false)
const error = ref('')
const items = ref<LeaderSubscriberData[]>([])
const total = computed(() => items.value.length)
const focusedSubscriptionId = computed(() => {
  const value = route.query.subscriptionId
  return typeof value === 'string' ? value : ''
})
const visibleItems = computed(() => {
  if (!focusedSubscriptionId.value) return items.value
  const focused = items.value.find((item) => item.subscriptionId === focusedSubscriptionId.value)
  if (!focused) return items.value
  return [
    focused,
    ...items.value.filter((item) => item.subscriptionId !== focusedSubscriptionId.value),
  ]
})

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

async function onRefresh() {
  refreshing.value = true
  try {
    const data = await listMySubscribers()
    items.value = data.items
  } catch (err) {
    showToast((err as { message?: string }).message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

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

function isFocusedSubscriber(item: LeaderSubscriberData): boolean {
  return focusedSubscriptionId.value === item.subscriptionId
}

onMounted(() => {
  void fetchSubscribers()
})
</script>

<style scoped>
.leader-subscribers {
  min-height: 100%;
  padding: 12px 14px 24px;
  background: var(--color-bg);
}

.subscriber-summary {
  padding: 16px;
  margin-bottom: 12px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.subscriber-summary div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.subscriber-summary span {
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
}

.subscriber-summary strong {
  color: var(--color-primary);
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
}

.subscriber-summary p {
  margin: 10px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.subscriber-list {
  display: grid;
  gap: 10px;
}

.subscriber-card {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 12px;
  align-items: flex-start;
  padding: 14px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.subscriber-card--focused {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.subscriber-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  object-fit: cover;
}

.subscriber-card__avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-size: 20px;
  font-weight: 900;
}

.subscriber-card__body {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.subscriber-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.subscriber-card__head strong {
  min-width: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
  line-height: 1.3;
  word-break: break-word;
}

.subscriber-card__head span {
  flex-shrink: 0;
  min-height: 24px;
  border-radius: var(--radius-pill);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  padding: 4px 8px;
  font-size: var(--font-size-xs);
  font-weight: 800;
  line-height: 1;
}

.subscriber-card__head .subscriber-card__focus-badge {
  background: var(--color-primary);
  color: #fff;
}

.subscriber-card__body p {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.35;
}
</style>
