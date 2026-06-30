<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      消息 <span class="badge-num">3</span>
    </div>

    <div class="notice-strip">
      <span>关注公众号，收到活动和订单、物流通知</span>
      <button type="button" @click="onWechatNoticeClick">关注</button>
    </div>

    <div class="msg-tabs">
      <button
        v-for="tab in tabs"
        :key="tab"
        type="button"
        :class="['msg-tab', { active: tab === activeTab }]"
        @click="activeTab = tab"
      >
        {{ tab }}
      </button>
    </div>

    <div class="page-note" style="margin:0 14px 0">
      消息为占位展示：订单、物流、活动、团长通知仅保留视觉样式，不请求消息或推送接口。
    </div>

    <div class="messages-placeholder">
      <div
        v-for="message in visibleMessages"
        :key="message.title"
        class="msg-card"
        :class="{ 'msg-card--dim': message.dim }"
      >
        <div :class="['msg-icon', `msg-icon--${message.tone}`]">
          <van-icon :name="message.icon" size="24" />
          <span v-if="message.unread" class="unread-dot" />
        </div>
        <div class="msg-card__info">
          <div class="msg-title">{{ message.title }}</div>
          <div class="msg-snippet">{{ message.snippet }}</div>
        </div>
        <span class="msg-time">{{ message.time }}</span>
      </div>
    </div>

    <EmptyState description="暂无消息" />
  </PageLayout>
</template>

<script setup lang="ts">
// 消息页 — 只展示空态和占位卡片，不请求任何消息 API
import { computed, ref } from 'vue'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import EmptyState from '@/components/EmptyState.vue'
import { isFeatureDisabled } from '@/utils/non-mvp'

const tabs = ['全部', '订单', '物流', '活动', '团长']
const activeTab = ref('全部')

const placeholderMessages = [
  {
    type: '订单',
    tone: 'orange',
    icon: 'orders-o',
    title: '订单待发货提醒',
    snippet: '你购买的团购商品已支付成功，团长会尽快发货。',
    time: '刚刚',
    unread: true,
  },
  {
    type: '物流',
    tone: 'blue',
    icon: 'logistics',
    title: '物流更新',
    snippet: '快递已揽收，后续物流轨迹将在订单详情展示。',
    time: '12:20',
    unread: true,
  },
  {
    type: '活动',
    tone: 'green',
    icon: 'fire-o',
    title: '订阅团长开团通知',
    snippet: '你订阅的团长发布了新的普通团购活动。',
    time: '昨天',
    unread: false,
  },
  {
    type: '团长',
    tone: 'gray',
    icon: 'setting-o',
    title: '系统通知',
    snippet: '消息中心仅为视觉占位，真实推送后续开放。',
    time: '周一',
    unread: false,
    dim: true,
  },
]

const visibleMessages = computed(() => {
  if (activeTab.value === '全部') return placeholderMessages
  return placeholderMessages.filter((message) => message.type === activeTab.value)
})

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
}
</script>

<style scoped>
.notice-strip button {
  border: 0;
  background: var(--color-primary);
  color: #fff;
  border-radius: 999px;
  padding: 5px 12px;
  min-height: 34px;
  font-weight: 900;
  flex-shrink: 0;
}

.messages-placeholder {
  padding: 12px 14px 0;
}

.msg-card--dim {
  opacity: 0.72;
}

.msg-card__info {
  min-width: 0;
}

.msg-icon--green {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.msg-icon--orange {
  background: #fff2e8;
  color: #ff7a2f;
}

.msg-icon--blue {
  background: #edf5ff;
  color: #3c85e8;
}

.msg-icon--gray {
  background: #f2f4f5;
  color: #6b7280;
}
</style>
