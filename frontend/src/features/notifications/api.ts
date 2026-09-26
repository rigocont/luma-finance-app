import { api } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';

import type { AppNotification } from './types';

export const notificationKeys = {
  all: ['notifications'] as const,
  list: ['notifications', 'list'] as const,
  unreadCount: ['notifications', 'unread-count'] as const,
};

export async function fetchNotifications(): Promise<PageResponse<AppNotification>> {
  const { data } = await api.get<PageResponse<AppNotification>>('/notifications');
  return data;
}

export async function fetchUnreadCount(): Promise<number> {
  const { data } = await api.get<{ count: number }>('/notifications/unread-count');
  return data.count;
}

export async function markNotificationRead(id: string): Promise<AppNotification> {
  const { data } = await api.post<AppNotification>(`/notifications/${id}/read`);
  return data;
}

export async function markAllNotificationsRead(): Promise<number> {
  const { data } = await api.post<{ count: number }>('/notifications/read-all');
  return data.count;
}
