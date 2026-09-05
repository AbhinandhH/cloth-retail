import { api } from './client'
import type {
  AdminOrderDashboardData,
  AdminOrderDetail,
  AdminOrderNote,
  AdminOrderRefund,
  AdminOrderShipment,
  AdminOrderShipmentRequest,
  AdminOrderStatus,
  AdminOrderSummary,
  PageResponse,
} from '../types'

// NOTE: this file is shared with the admin Order List/Dashboard screen (built
// in parallel) — list/dashboard fetchers may live alongside these. Keep
// exports narrowly named and self-contained so both sides can add to this
// file without collision.

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value as string | number
    }
  }
  return params
}

// --- Order list / dashboard (AdminOrderDashboard.tsx) ---------------------

export interface AdminOrderQuery {
  page?: number
  size?: number
  q?: string
  orderStatus?: AdminOrderStatus | ''
  paymentStatus?: 'PENDING' | 'SUCCESS' | 'FAILED' | ''
  dateFrom?: string
  dateTo?: string
  paymentMethod?: string
  sort?: 'createdAt' | 'totalAmount' | 'status'
  dir?: 'asc' | 'desc'
}

/** GET /admin/orders — the paginated, filterable/searchable admin order list. */
export function fetchAdminOrders(query: AdminOrderQuery = {}) {
  return api
    .get<PageResponse<AdminOrderSummary>>('/admin/orders', { params: cleanParams(query) })
    .then((r) => r.data)
}

/** GET /admin/orders/dashboard — status-bucket counts + capped recent orders list. */
export function fetchAdminOrderDashboard() {
  return api.get<AdminOrderDashboardData>('/admin/orders/dashboard').then((r) => r.data)
}

// --- Order detail (AdminOrderDetail.tsx) ----------------------------------

export function fetchAdminOrderDetail(id: number | string) {
  return api.get<AdminOrderDetail>(`/admin/orders/${id}`).then((r) => r.data)
}

export interface UpdateOrderStatusPayload {
  toStatus: AdminOrderStatus
  reason?: string | null
}

export function updateAdminOrderStatus(id: number | string, payload: UpdateOrderStatusPayload) {
  return api.post<AdminOrderDetail>(`/admin/orders/${id}/status`, payload).then((r) => r.data)
}

export function cancelAdminOrder(id: number | string, reason: string) {
  return api.post<AdminOrderDetail>(`/admin/orders/${id}/cancel`, { reason }).then((r) => r.data)
}

export function addAdminOrderNote(id: number | string, note: string) {
  return api.post<AdminOrderNote>(`/admin/orders/${id}/notes`, { note }).then((r) => r.data)
}

export function saveAdminOrderShipment(id: number | string, payload: AdminOrderShipmentRequest) {
  return api.put<AdminOrderShipment>(`/admin/orders/${id}/shipment`, payload).then((r) => r.data)
}

export interface InitiateRefundPayload {
  amount: number
  reason?: string | null
}

export function initiateAdminOrderRefund(id: number | string, payload: InitiateRefundPayload) {
  return api.post<AdminOrderRefund>(`/admin/orders/${id}/refund`, payload).then((r) => r.data)
}
