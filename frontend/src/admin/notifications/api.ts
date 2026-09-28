import { api } from '@/shared/api/client'
import type { NotificationSettings, SmtpSettings, SmtpSettingsUpdate, SmtpTestResult } from '@/shared/types'

export function fetchNotificationSettings() {
  return api.get<NotificationSettings>('/admin/notification-settings').then((r) => r.data)
}

export function updateNotificationSettings(payload: NotificationSettings) {
  return api.put<NotificationSettings>('/admin/notification-settings', payload).then((r) => r.data)
}

export function fetchSmtpSettings() {
  return api.get<SmtpSettings>('/admin/smtp-settings').then((r) => r.data)
}

export function updateSmtpSettings(payload: SmtpSettingsUpdate) {
  return api.put<SmtpSettings>('/admin/smtp-settings', payload).then((r) => r.data)
}

export function sendTestEmail(toEmail: string) {
  return api.post<SmtpTestResult>('/admin/smtp-settings/test', { toEmail }).then((r) => r.data)
}
