import request from './request'
import type {
  ApiResponse,
  ChatConversationData,
  ChatListParams,
  ChatMessageData,
  ChatMessageListParams,
  EmptySuccessResponse,
  PageResponse,
  SendChatCardRequest,
  SendChatMessageRequest,
  UnreadCountData,
} from '@/types'

type ChatUnreadCountData = UnreadCountData

export async function openChatByOrder(orderId: string): Promise<ChatConversationData> {
  const res = await request.post(`/my/chat-conversations/orders/${orderId}`) as ApiResponse<ChatConversationData>
  return res.data
}

export async function listChatConversations(
  params: ChatListParams = {},
): Promise<PageResponse<ChatConversationData>> {
  const res = await request.get('/my/chat-conversations', { params }) as ApiResponse<PageResponse<ChatConversationData>>
  return res.data
}

export async function getChatUnreadCount(): Promise<ChatUnreadCountData> {
  const res = await request.get('/my/chat-conversations/unread-count') as ApiResponse<ChatUnreadCountData>
  return res.data
}

export async function listChatMessages(
  conversationId: string,
  params: ChatMessageListParams = {},
): Promise<PageResponse<ChatMessageData>> {
  const res = await request.get(`/my/chat-conversations/${conversationId}/messages`, { params }) as ApiResponse<PageResponse<ChatMessageData>>
  return res.data
}

export async function sendChatMessage(
  conversationId: string,
  data: SendChatMessageRequest,
): Promise<ChatMessageData> {
  const res = await request.post(`/my/chat-conversations/${conversationId}/messages`, data) as ApiResponse<ChatMessageData>
  return res.data
}

export async function sendChatCard(
  conversationId: string,
  data: SendChatCardRequest,
): Promise<ChatMessageData> {
  const res = await request.post(`/my/chat-conversations/${conversationId}/cards`, data) as ApiResponse<ChatMessageData>
  return res.data
}

export async function markChatRead(conversationId: string): Promise<void> {
  await request.post(`/my/chat-conversations/${conversationId}/read`) as EmptySuccessResponse
}
