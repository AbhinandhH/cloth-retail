import { api } from "./client";
import type {
  CreateOrderRequest,
  OrderDetail,
  OrderListItem,
  PageResponse,
} from "../types";

// NOTE: this file is shared with the Cart/Checkout/Payment flow (built in
// parallel by another agent) — both sides need order-fetching functions.
// Keep exports named and additive; do not remove or overwrite the other
// agent's exports (e.g. anything related to placing an order/payment).

export interface OrderQuery {
  page?: number;
  size?: number;
}

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== "") {
      params[key] = value as string | number;
    }
  }
  return params;
}

/** GET /api/orders?page=&size= — the current customer's own orders, paginated. */
export async function fetchMyOrders(query: OrderQuery = {}) {
  const r = await api.get<PageResponse<OrderListItem>>("/orders", {
    params: cleanParams(query),
  });
  return r.data;
}

/** GET /api/orders/{id} — full order detail (also used as the post-checkout confirmation screen). */
export async function fetchOrderById(id: number | string) {
  const r = await api.get<OrderDetail>(`/orders/${id}`);
  return r.data;
}

/**
 * POST /api/orders — places an order from the current cart. Idempotent on
 * `idempotencyKey`: CheckoutPage generates a fresh key (lib/uuid.ts's randomUUID())
 * per checkout attempt so a duplicate click/retry can't create two orders.
 * Returns the created order in PENDING_PAYMENT status.
 */
export async function createOrder(payload: CreateOrderRequest) {
  const r = await api.post<OrderDetail>("/orders", payload);
  return r.data;
}

/** GET /api/orders/{id}/invoice — a GST invoice PDF for the current customer's own order, only available once payment is confirmed. */
export async function fetchOrderInvoice(id: number | string) {
  const r = await api.get(`/orders/${id}/invoice`, { responseType: "blob" });
  return r.data as Blob;
}
