<template>
  <PageLayout title="客服工作台" show-back @back="goBack">
    <LoadingView v-if="firstLoading" text="正在加载买家会话..." />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="leader-chats">
      <section class="chat-summary">
        <div>
          <span>待回复会话</span>
          <strong>{{ unreadTotal }}</strong>
        </div>
        <p>这里聚合当前店铺的订单上下文聊天，点击后进入原聊天详情继续沟通。</p>
      </section>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="!hasMore"
          finished-text="没有更多了"
          :error="error !== null"
          error-text="加载失败，点击重试"
          :immediate-check="false"
          @load="loadMore"
        >
          <div class="conversation-list">
            <div
              v-for="conversation in items"
              :key="conversation.id"
              class="leader-chat-row"
            >
              <ChatConversationListItem :conversation="conversation" @open="openConversation(conversation.id)" />
              <span class="order-hint">关联订单咨询 · {{ conversation.currentUserRole === 'leader' ? '买家侧消息' : '团长侧消息' }}</span>
            </div>
          </div>

          <EmptyState v-if="isEmpty" image="chat-o" description="暂无买家咨询" />
        </van-list>
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import ChatConversationListItem from '@/components/ChatConversationListItem.vue'
import { usePagination } from '@/composables/usePagination'
import { listChatConversations } from '@/api/chats'
import type { ChatConversationData } from '@/types'

const router = useRouter()

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
} = usePagination<ChatConversationData>(
  (page, pageSize) => listChatConversations({ role: 'leader', page, pageSize }),
)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)
const unreadTotal = computed(() => items.value.reduce((sum, item) => sum + item.unreadCount, 0))

async function onRefresh() {
  await refresh()
  if (error.value) showToast('刷新失败')
}

function openConversation(conversationId: string) {
  router.push(`/chats/${conversationId}`)
}

function goBack() {
  router.back()
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.leader-chats {
  padding: 12px 14px 20px;
}

.chat-summary {
  padding: 14px;
  margin-bottom: 12px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.chat-summary div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.chat-summary span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  font-weight: 800;
}

.chat-summary strong {
  color: var(--color-primary);
  font-size: 26px;
  font-weight: 900;
}

.chat-summary p {
  margin: 6px 0 0;
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.leader-chat-row :deep(.chat-conversation-item) {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.order-hint {
  display: block;
  padding: 8px 14px 10px;
  border: 1px solid var(--color-border);
  border-top: 0;
  border-bottom-left-radius: var(--radius-card);
  border-bottom-right-radius: var(--radius-card);
  background: var(--color-bg-card);
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
  box-shadow: var(--shadow-card);
}
</style>
