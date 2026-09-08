import { api } from './client'
import type { AdminCustomerDetail, AdminCustomerRow, AdminOrderSummary, PageResponse } from '../types'

export interface AdminCustomerQuery {
  page?: number
  size?: number
  q?: string
  enabled?: boolean
  sort?: 'createdAt' | 'fullName' | 'email'
  dir?: 'asc' | 'desc'
}

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number | boolean> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value as string | number | boolean
    }
  }
  return params
}

export function fetchAdminCustomers(query: AdminCustomerQuery = {}) {
  return api.get<PageResponse<AdminCustomerRow>>('/admin/customers', { params: cleanParams(query) }).then((r) => r.data)
}

export function fetchAdminCustomer(id: number | string) {
  return api.get<AdminCustomerDetail>(`/admin/customers/${id}`).then((r) => r.data)
}

export function fetchAdminCustomerOrders(id: number | string, page = 0, size = 20) {
  return api
    .get<PageResponse<AdminOrderSummary>>(`/admin/customers/${id}/orders`, { params: { page, size } })
    .then((r) => r.data)
}

export function updateAdminCustomerStatus(id: number | string, enabled: boolean) {
  return api.patch<AdminCustomerDetail>(`/admin/customers/${id}/status`, { enabled }).then((r) => r.data)
}
