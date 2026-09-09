import { api } from './client'
import type { NotificationSettings } from '../types'

export function fetchNotificationSettings() {
  return api.get<NotificationSettings>('/admin/notification-settings').then((r) => r.data)
}

export function updateNotificationSettings(payload: NotificationSettings) {
  return api.put<NotificationSettings>('/admin/notification-settings', payload).then((r) => r.data)
}
