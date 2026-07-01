<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      消息 <span class="badge-num">3</span>
    </div>

    <AppNoticeStrip
      text="关注公众号，收到活动和订单、物流通知"
      action-label="关注"
      variant="warning"
      @action="onWechatNoticeClick"
    />

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

    <AppPageNote icon="info-o" variant="warning" class="page-note-msg">
      订单、物流、售后通知仅保留视觉样式，不请求消息或推送接口。
    </AppPageNote>

    <div class="messages-placeholder">
      <AppCard
        v-for="message in visibleMessages"
        :key="message.title"
        :class="{ 'msg-card--dim': message.dim }"
        :flush="true"
      >
        <div class="msg-card-layout">
          <div :class="['msg-icon', `msg-icon--${message.tone}`]">
            <span class="msg-emoji">{{ message.icon }}</span>
            <span v-if="message.unread" class="unread-dot" />
          </div>
          <div class="msg-card__info">
            <div class="msg-title">{{ message.title }}</div>
            <div class="msg-snippet">{{ message.snippet }}</div>
          </div>
          <span class="msg-time">{{ message.time }}</span>
        </div>
      </AppCard>
    </div>

    <EmptyState v-if="visibleMessages.length === 0" description="暂无消息" />
  </PageLayout>
</template>

<script setup lang="ts">
// 消息页 — 只展示空态和占位卡片，不请求任何消息 API
import { computed, ref } from 'vue'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppNoticeStrip from '@/components/AppNoticeStrip.vue'
import AppPageNote from '@/components/AppPageNote.vue'
import AppCard from '@/components/AppCard.vue'
import { isFeatureDisabled } from '@/utils/non-mvp'

interface Message {
  type: string
  tone: string
  icon: string
  title: string
  snippet: string
  time: string
  unread: boolean
  dim?: boolean
}

const tabs = ['全部', '未读(3)', '订单', '售后']
const activeTab = ref('全部')

const placeholderMessages: Message[] = [
  {
    type: '订单',
    tone: 'orange',
    icon: '💰',
    title: '订单待发货提醒',
    snippet: '微信支付 ¥19.9 成功，订单已进入待发货。',
    time: '5分钟前',
    unread: true,
  },
  {
    type: '订单',
    tone: 'orange',
    icon: '💰',
    title: '支付成功',
    snippet: '你购买的团购商品已支付成功，等待团长发货。',
    time: '刚刚',
    unread: true,
  },
  {
    type: '订单',
    tone: 'blue',
    icon: '🚚',
    title: '发货通知',
    snippet: '团长已填写物流：顺丰速运 SF1234567890。',
    time: '1小时前',
    unread: true,
  },
  {
    type: '订单',
    tone: 'blue',
    icon: '🚚',
    title: '物流更新',
    snippet: '快递已揽收，后续物流轨迹将在订单详情展示。',
    time: '12:20',
    unread: false,
  },
  {
    type: '订单',
    tone: 'green',
    icon: '👑',
    title: '会员权益提醒',
    snippet: '你已成为「重庆好评第一团」会员，下单可享最高 9 折。',
    time: '昨天',
    unread: false,
  },
  {
    type: '售后',
    tone: 'gray',
    icon: '🧾',
    title: '售后进度',
    snippet: '退款申请已提交，等待团长审核。',
    time: '周一',
    unread: false,
  },
]

const visibleMessages = computed(() => {
  const tab = activeTab.value
  if (tab === '全部') return placeholderMessages
  if (tab === '未读(3)') return placeholderMessages.filter((m) => m.unread)
  return placeholderMessages.filter((m) => m.type === tab)
})

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
}
</script>

<style scoped>
.messages-placeholder {
  padding: 12px 14px 0;
}

.page-note-msg {
  margin: 0 14px;
}

.msg-card-layout {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  gap: 10px;
  align-items: start;
  padding: 14px;
}

.msg-card--dim {
  opacity: 0.72;
}

.msg-card__info {
  min-width: 0;
}

.msg-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  position: relative;
  flex-shrink: 0;
}

.msg-emoji {
  line-height: 1;
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

.unread-dot {
  position: absolute;
  right: -2px;
  top: -2px;
  width: 10px;
  height: 10px;
  background: #f25541;
  border: 2px solid var(--color-bg-card);
  border-radius: 50%;
}

.msg-title {
  font-weight: 900;
  font-size: 16px;
}

.msg-snippet {
  color: #7a808a;
  font-size: 13px;
  line-height: 1.45;
  margin-top: 4px;
}

.msg-time {
  color: #a8adb5;
  font-size: 11px;
  white-space: nowrap;
}
</style>
