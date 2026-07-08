import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import MerchantChatsView from '@/views/merchant/MerchantChatsView.vue'
import { listChatConversations, listChatMessages, markChatRead, sendChatMessage } from '@/api/chats'
import type { ChatConversationData, ChatMessageData } from '@/types'

vi.mock('@/api/chats', () => ({
  listChatConversations: vi.fn(),
  listChatMessages: vi.fn(),
  markChatRead: vi.fn(),
  sendChatMessage: vi.fn(),
}))

vi.mock('@/api/uploads', () => ({
  uploadImage: vi.fn(),
}))

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showImagePreview: vi.fn(),
    showToast: vi.fn(),
  }
})

const conversations: ChatConversationData[] = [
  {
    id: 'c1',
    buyerUserId: 'u1',
    leaderUserId: 'u2',
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
    lastMessageText: '什么时候发货',
    lastMessageType: 'text',
    lastMessageAt: '2026-07-06T10:30:00',
    createdAt: '2026-07-06T10:00:00',
  },
  {
    id: 'c2',
    buyerUserId: 'u3',
    leaderUserId: 'u2',
    storeId: '20',
    buyerName: '小周',
    buyerAvatarUrl: null,
    leaderName: '王姐',
    leaderAvatarUrl: null,
    storeName: '王姐鲜果团',
    storeLogoUrl: null,
    currentUserRole: 'leader',
    unreadCount: 0,
    lastMessageId: 'm2',
    lastMessageText: '收到',
    lastMessageType: 'text',
    lastMessageAt: '2026-07-06T10:20:00',
    createdAt: '2026-07-06T10:00:00',
  },
]

const messages: ChatMessageData[] = [
  {
    id: 'm1',
    conversationId: 'c1',
    senderUserId: 'u1',
    senderRole: 'buyer',
    messageType: 'text',
    content: '什么时候发货',
    mine: false,
    createdAt: '2026-07-06T10:30:00',
  },
]

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/merchant/chats', component: MerchantChatsView },
      { path: '/merchant/chats/:conversationId', component: MerchantChatsView },
      { path: '/merchant/orders/:id', component: { template: '<div>订单详情</div>' } },
    ],
  })
}

describe('MerchantChatsView', () => {
  beforeEach(() => {
    vi.mocked(listChatConversations).mockReset()
    vi.mocked(listChatMessages).mockReset()
    vi.mocked(markChatRead).mockReset()
    vi.mocked(sendChatMessage).mockReset()

    vi.mocked(listChatConversations).mockResolvedValue({
      items: conversations,
      page: 1,
      pageSize: 30,
      total: 2,
      hasMore: false,
    })
    vi.mocked(listChatMessages).mockResolvedValue({
      items: messages,
      page: 1,
      pageSize: 30,
      total: 1,
      hasMore: false,
    })
    vi.mocked(markChatRead).mockResolvedValue()
    vi.mocked(sendChatMessage).mockResolvedValue({
      id: 'm3',
      conversationId: 'c1',
      senderUserId: 'u2',
      senderRole: 'leader',
      messageType: 'text',
      content: '今天下午发货',
      mine: true,
      createdAt: '2026-07-06T10:35:00',
    })
  })

  it('auto selects the first conversation in the merchant workbench', async () => {
    const router = createTestRouter()
    await router.push('/merchant/chats')
    await router.isReady()

    const wrapper = mount(MerchantChatsView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(listChatConversations).toHaveBeenCalledWith({ role: 'leader', page: 1, pageSize: 30 })
    expect(router.currentRoute.value.fullPath).toBe('/merchant/chats/c1')
    expect(listChatMessages).toHaveBeenCalledWith('c1', { pageSize: 30 })
    expect(markChatRead).toHaveBeenCalledWith('c1')
    expect(wrapper.text()).toContain('小李')
    expect(wrapper.text()).toContain('当前沟通对象')
    expect(wrapper.text()).toContain('什么时候发货')
    expect(wrapper.find('.conversation-row img').attributes('src')).toBe('/uploads/images/buyer.jpg')
  })

  it('switches conversations and sends text messages', async () => {
    const router = createTestRouter()
    await router.push('/merchant/chats/c1')
    await router.isReady()

    const wrapper = mount(MerchantChatsView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    await wrapper.findAll('.conversation-row')[1].trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/merchant/chats/c2')
    expect(listChatMessages).toHaveBeenCalledWith('c2', { pageSize: 30 })

    await wrapper.find('.composer input[type="text"]').setValue('今天下午发货')
    await wrapper.find('form.composer').trigger('submit')
    await flushPromises()

    expect(sendChatMessage).toHaveBeenCalledWith('c2', expect.objectContaining({
      messageType: 'text',
      content: '今天下午发货',
    }))
  })

  it('shows empty state without conversations', async () => {
    vi.mocked(listChatConversations).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 30,
      total: 0,
      hasMore: false,
    })
    const router = createTestRouter()
    await router.push('/merchant/chats')
    await router.isReady()

    const wrapper = mount(MerchantChatsView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无买家咨询')
  })
})
