import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import {
  getChatUnreadCount,
  listChatConversations,
  listChatMessages,
  markChatRead,
  openChatByOrder,
  sendChatCard,
  sendChatMessage,
} from '@/api/chats'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('chats api', () => {
  beforeEach(() => {
    vi.mocked(request.get).mockReset()
    vi.mocked(request.post).mockReset()
  })

  it('opens chat by order and unwraps response', async () => {
    vi.mocked(request.post).mockResolvedValue({
      success: true,
      data: { id: 'c1', storeName: '团长小店' },
      traceId: 'req_1',
    })

    const result = await openChatByOrder('o1')

    expect(request.post).toHaveBeenCalledWith('/my/chat-conversations/orders/o1')
    expect(result.id).toBe('c1')
  })

  it('lists conversations and messages with params', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
      traceId: 'req_1',
    })

    await listChatConversations({ page: 1, pageSize: 20, role: 'buyer' })
    await listChatMessages('c1', { afterMessageId: 'm1', pageSize: 20 })

    expect(request.get).toHaveBeenNthCalledWith(1, '/my/chat-conversations', {
      params: { page: 1, pageSize: 20, role: 'buyer' },
    })
    expect(request.get).toHaveBeenNthCalledWith(2, '/my/chat-conversations/c1/messages', {
      params: { afterMessageId: 'm1', pageSize: 20 },
    })
  })

  it('sends text message, card, read marker and unread count', async () => {
    vi.mocked(request.post).mockResolvedValue({
      success: true,
      data: { id: 'm1' },
      traceId: 'req_1',
    })
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { unreadCount: 3 },
      traceId: 'req_2',
    })

    await sendChatMessage('c1', { messageType: 'text', content: '你好', clientMessageId: 'client-1' })
    await sendChatCard('c1', { cardType: 'prepare_done', orderId: 'o1' })
    await markChatRead('c1')
    const count = await getChatUnreadCount()

    expect(request.post).toHaveBeenNthCalledWith(1, '/my/chat-conversations/c1/messages', {
      messageType: 'text',
      content: '你好',
      clientMessageId: 'client-1',
    })
    expect(request.post).toHaveBeenNthCalledWith(2, '/my/chat-conversations/c1/cards', {
      cardType: 'prepare_done',
      orderId: 'o1',
    })
    expect(request.post).toHaveBeenNthCalledWith(3, '/my/chat-conversations/c1/read')
    expect(count.unreadCount).toBe(3)
  })
})
