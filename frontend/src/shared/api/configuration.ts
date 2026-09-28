import { api } from './client'
import type { AdminSiteConfiguration, SiteConfiguration, Theme } from '../types'

/** Public, unauthenticated — powers Navbar/Footer/login-page branding. */
export function fetchPublicConfiguration() {
  return api.get<SiteConfiguration>('/configuration').then((r) => r.data)
}

/** SUPER_ADMIN only. */
export function fetchThemes() {
  return api.get<Theme[]>('/admin/themes').then((r) => r.data)
}

export function activateTheme(id: number | string) {
  return api.post<SiteConfiguration>(`/admin/themes/${id}/activate`).then((r) => r.data)
}

export function fetchAdminConfiguration() {
  return api.get<AdminSiteConfiguration>('/admin/configuration').then((r) => r.data)
}

/** All SiteConfiguration fields except `theme` — theme changes go through activateTheme(). */
export type SiteConfigurationUpdate = Omit<SiteConfiguration, 'theme'>

export function updateConfiguration(payload: SiteConfigurationUpdate) {
  return api.put<SiteConfiguration>('/admin/configuration', payload).then((r) => r.data)
}

export interface MediaUploadResponse {
  url: string
}

export function uploadMedia(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return api
    .post<MediaUploadResponse>('/admin/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)
}

/** Read the active theme's id from an admin config response, whichever field carries it. */
export function getActiveThemeId(config: AdminSiteConfiguration | null | undefined): number | string | null {
  if (!config) return null
  if (config.activeThemeId !== undefined && config.activeThemeId !== null) return config.activeThemeId
  return config.activeTheme?.id ?? config.theme?.id ?? null
}
