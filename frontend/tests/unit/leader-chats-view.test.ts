import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import LeaderChatsView from '@/views/leader/LeaderChatsView.vue'
import { listChatConversations } from '@/api/chats'
import type { ChatConversationData } from '@/types'

vi.mock('@/api/chats', () => ({
  listChatConversations: vi.fn(),
}))

const conversation: ChatConversationData = {
  id: '7001',
  buyerUserId: '1',
  leaderUserId: '2',
  storeId: '20',
  buyerName: '小李',
  buyerAvatarUrl: '/uploads/images/buyer.jpg',
  leaderName: '王姐',
  leaderAvatarUrl: null,
  storeName: '王姐鲜果团',
  storeLogoUrl: null,
  currentUserRole: 'leader',
  unreadCount: 2,
  lastMessageId: 'm1',
  lastMessageText: '这个订单今天能发吗',
  lastMessageType: 'text',
  lastMessageAt: '2026-07-03T11:00:00',
  createdAt: '2026-07-03T09:00:00',
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/leader/chats', component: LeaderChatsView },
      { path: '/chats/:conversationId', component: { template: '<div>聊天详情</div>' } },
    ],
  })
}

describe('LeaderChatsView', () => {
  beforeEach(() => {
    vi.mocked(listChatConversations).mockReset()
    vi.mocked(listChatConversations).mockResolvedValue({
      items: [conversation],
      page: 1,
      pageSize: 20,
      total: 1,
      hasMore: false,
    })
  })

  it('renders leader conversations and opens existing chat detail', async () => {
    const router = createTestRouter()
    await router.push('/leader/chats')
    await router.isReady()

    const wrapper = mount(LeaderChatsView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(listChatConversations).toHaveBeenCalledWith({ role: 'leader', page: 1, pageSize: 20 })
    expect(wrapper.text()).toContain('待回复会话')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('小李')
    expect(wrapper.text()).toContain('王姐鲜果团 · 订单咨询')
    expect(wrapper.find('.chat-conversation-item img').attributes('src')).toBe('/uploads/images/buyer.jpg')
    expect(wrapper.text()).toContain('这个订单今天能发吗')

    await wrapper.find('.chat-conversation-item').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/chats/7001')
  })

  it('shows empty state when there are no conversations', async () => {
    vi.mocked(listChatConversations).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0,
      hasMore: false,
    })
    const router = createTestRouter()
    await router.push('/leader/chats')
    await router.isReady()

    const wrapper = mount(LeaderChatsView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无买家咨询')
  })
})
