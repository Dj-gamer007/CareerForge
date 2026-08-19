import { apiClient } from '@/lib/axios';
import { ApiResponse, SpringPage } from '@/types/api.types';
import { NotificationResponse, UnreadCountResponse } from '@/types/notification.types';

export const notificationService = {
  async getNotifications(params?: { page?: number; size?: number }): Promise<SpringPage<NotificationResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<NotificationResponse>>>('/notifications', { params });
    return res.data.data;
  },

  async getUnreadCount(): Promise<number> {
    const res = await apiClient.get<ApiResponse<UnreadCountResponse>>('/notifications/unread-count');
    return res.data.data.unreadCount;
  },

  async markAsRead(id: number): Promise<void> {
    await apiClient.patch(`/notifications/${id}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.patch('/notifications/read-all');
  },
};
