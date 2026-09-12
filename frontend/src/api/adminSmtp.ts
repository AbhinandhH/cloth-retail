import { api } from './client'
import type { SmtpSettings, SmtpSettingsUpdate, SmtpTestResult } from '../types'

export function fetchSmtpSettings() {
  return api.get<SmtpSettings>('/admin/smtp-settings').then((r) => r.data)
}

export function updateSmtpSettings(payload: SmtpSettingsUpdate) {
  return api.put<SmtpSettings>('/admin/smtp-settings', payload).then((r) => r.data)
}

export function sendTestEmail(toEmail: string) {
  return api.post<SmtpTestResult>('/admin/smtp-settings/test', { toEmail }).then((r) => r.data)
}
