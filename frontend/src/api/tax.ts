import { api } from './client'
import type { TaxSettings, TaxSettingsUpdate } from '../types'

export function fetchTaxSettings() {
  return api.get<TaxSettings>('/admin/tax-settings').then((r) => r.data)
}

export function updateTaxSettings(payload: TaxSettingsUpdate) {
  return api.put<TaxSettings>('/admin/tax-settings', payload).then((r) => r.data)
}
