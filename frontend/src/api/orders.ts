import { api } from './client'
import type { CreateOrderRequest, OrderDetail, OrderListItem, PageResponse } from '../types'

// NOTE: this file is shared with the Cart/Checkout/Payment flow (built in
// parallel by another agent) — both sides need order-fetching functions.
// Keep exports named and additive; do not remove or overwrite the other
// agent's exports (e.g. anything related to placing an order/payment).

export interface OrderQuery {
  page?: number
  size?: number
}

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value as string | number
    }
  }
  return params
}

/** GET /api/orders?page=&size= — the current customer's own orders, paginated. */
export function fetchMyOrders(query: OrderQuery = {}) {
  return api
    .get<PageResponse<OrderListItem>>('/orders', { params: cleanParams(query) })
    .then((r) => r.data)
}

/** GET /api/orders/{id} — full order detail (also used as the post-checkout confirmation screen). */
export function fetchOrderById(id: number | string) {
  return api.get<OrderDetail>(`/orders/${id}`).then((r) => r.data)
}

/**
 * POST /api/orders — places an order from the current cart. Idempotent on
 * `idempotencyKey`: CheckoutPage generates a fresh crypto.randomUUID() per
 * checkout attempt so a duplicate click/retry can't create two orders.
 * Returns the created order in PENDING_PAYMENT status.
 */
export function createOrder(payload: CreateOrderRequest) {
  return api.post<OrderDetail>('/orders', payload).then((r) => r.data)
}
