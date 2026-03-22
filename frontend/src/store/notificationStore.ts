import { create } from 'zustand'

export interface Notification {
  id: string
  type: string
  title: string
  body?: string
  isRead: boolean
  actionUrl?: string
  createdAt: string
}

interface NotificationStore {
  notifications: Notification[]
  unreadCount: number
  setNotifications: (n: Notification[]) => void
  markAllRead: () => void
}

export const useNotificationStore = create<NotificationStore>()((set) => ({
  notifications: [],
  unreadCount: 0,
  setNotifications: (notifications) =>
    set({ notifications, unreadCount: notifications.filter((n) => !n.isRead).length }),
  markAllRead: () =>
    set((s) => ({
      notifications: s.notifications.map((n) => ({ ...n, isRead: true })),
      unreadCount: 0,
    })),
}))
