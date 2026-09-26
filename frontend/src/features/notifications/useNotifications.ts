import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
  notificationKeys,
} from './api';

export function useNotifications() {
  return useQuery({
    queryKey: notificationKeys.list,
    queryFn: fetchNotifications,
  });
}

/** Cuantas alertas sin leer tiene la persona. Lo usa la insignia de la campana. */
export function useUnreadCount(): number {
  const { data } = useQuery({
    queryKey: notificationKeys.unreadCount,
    queryFn: fetchUnreadCount,
  });
  return data ?? 0;
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
