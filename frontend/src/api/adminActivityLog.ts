import { api } from './client'
import type { ActivityLogRow, PageResponse } from '../types'

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value as string | number
    }
  }
  return params
}

export interface ActivityLogQuery {
  page?: number
  size?: number
  actorId?: string | number
  module?: string
  httpMethod?: string
  dateFrom?: string
  dateTo?: string
}

export function fetchActivityLog(query: ActivityLogQuery) {
  return api.get<PageResponse<ActivityLogRow>>('/admin/activity-log', { params: cleanParams(query) }).then((r) => r.data)
}
