import request from './request'
import type {
  ApiResponse,
  EmptySuccessResponse,
  NotificationData,
  NotificationListParams,
  PageResponse,
  UnreadCountData,
} from '@/types'

export async function listNotifications(
  params: NotificationListParams = {},
): Promise<PageResponse<NotificationData>> {
  const res = await request.get('/my/notifications', { params }) as ApiResponse<PageResponse<NotificationData>>
  return res.data
}

export async function getUnreadCount(): Promise<UnreadCountData> {
  const res = await request.get('/my/notifications/unread-count') as ApiResponse<UnreadCountData>
  return res.data
}

export async function getNotification(notificationId: string): Promise<NotificationData> {
  const res = await request.get(`/my/notifications/${notificationId}`) as ApiResponse<NotificationData>
  return res.data
}

export async function markNotificationRead(notificationId: string): Promise<NotificationData> {
  const res = await request.post(`/my/notifications/${notificationId}/read`) as ApiResponse<NotificationData>
  return res.data
}

export async function markAllNotificationsRead(): Promise<void> {
  await request.post('/my/notifications/read-all') as EmptySuccessResponse
}
