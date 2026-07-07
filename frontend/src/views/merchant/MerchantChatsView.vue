<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>订单上下文客服</p>
        <h1>客服会话</h1>
      </div>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadChats" />

    <div v-else class="table-panel">
      <table class="merchant-table">
        <thead>
          <tr>
            <th>买家</th>
            <th>店铺</th>
            <th>最后消息</th>
            <th>未读</th>
            <th>最后时间</th>
            <th>关联订单</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="chat in chats" :key="chat.id">
            <td><strong>{{ chat.buyerName }}</strong></td>
            <td>{{ chat.storeName }}</td>
            <td>{{ chat.lastMessageText || messageTypeText(chat.lastMessageType) }}</td>
            <td><mark v-if="chat.unreadCount">{{ chat.unreadCount }}</mark><span v-else>0</span></td>
            <td>{{ formatDateTime(chat.lastMessageAt) }}</td>
            <td>订单咨询</td>
            <td><RouterLink :to="`/chats/${chat.id}`">进入会话</RouterLink></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="chats.length === 0" description="暂无客服会话" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listChatConversations } from '@/api/chats'
import { formatDateTime } from '@/utils'
import type { ChatConversationData } from '@/types'

const loading = ref(true)
const error = ref('')
const chats = ref<ChatConversationData[]>([])

function messageTypeText(type?: string | null) {
  if (type === 'image') return '[图片]'
  if (type === 'card') return '[订单卡片]'
  return '暂无消息'
}

async function loadChats() {
  loading.value = true
  error.value = ''
  try {
    const data = await listChatConversations({ role: 'leader', page: 1, pageSize: 30 })
    chats.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '客服会话加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadChats)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
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

.table-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  overflow-x: auto;
}

.merchant-table {
  width: 100%;
  min-width: 880px;
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
  color: #6b7280;
  background: #f9fafb;
  font-weight: 900;
}

.merchant-table a {
  color: #d63f2b;
  font-weight: 900;
  text-decoration: none;
}

.merchant-table mark {
  display: inline-flex;
  min-width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #e9563f;
  color: #ffffff;
  font-size: 12px;
}
</style>
