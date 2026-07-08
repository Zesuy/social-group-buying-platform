import type { ChatConversationData } from '@/types'

export interface ChatCounterpartDisplay {
  role: 'buyer' | 'leader'
  roleText: string
  name: string
  avatarUrl: string | null
  title: string
  subtitle: string
}

export function getChatCounterpart(conversation: ChatConversationData): ChatCounterpartDisplay {
  if (conversation.currentUserRole === 'leader') {
    const name = conversation.buyerName || '买家'
    return {
      role: 'buyer',
      roleText: '买家',
      name,
      avatarUrl: conversation.buyerAvatarUrl || null,
      title: name,
      subtitle: `${conversation.storeName || '当前店铺'} · 订单咨询`,
    }
  }

  const name = conversation.leaderName || '团长'
  return {
    role: 'leader',
    roleText: '团长',
    name,
    avatarUrl: conversation.leaderAvatarUrl || conversation.storeLogoUrl || null,
    title: name,
    subtitle: `${conversation.storeName || '团长店铺'} · 履约沟通`,
  }
}

export function getChatMessageSummary(conversation: ChatConversationData): string {
  const counterpart = getChatCounterpart(conversation)
  if (conversation.lastMessageType === 'image') return `${counterpart.roleText}发来了一张图片`
  if (conversation.lastMessageType === 'card') {
    return conversation.lastMessageText || `${counterpart.roleText}发来订单卡片`
  }
  return conversation.lastMessageText || `${counterpart.subtitle}`
}
