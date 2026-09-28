import { api } from "@/shared/api/client";
import type {
  CreateOrderRequest,
  EligibleOrderItem,
  OrderDetail,
  OrderListItem,
  PageResponse,
  ReturnRequestDetail,
  ReturnRequestSummary,
} from "@/shared/types";

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
 * `idempotencyKey`: CheckoutPage generates a fresh key (uuid.ts's randomUUID())
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

/** GET /returns/orders/{orderId}/eligible-items — which of this delivered order's items can still be exchanged/reported, and (for exchange) which replacement sizes are actually in stock right now. */
export function fetchEligibleItems(orderId: number | string) {
  return api
    .get<EligibleOrderItem[]>(`/returns/orders/${orderId}/eligible-items`)
    .then((r) => r.data);
}

export function requestSizeExchange(
  orderId: number | string,
  itemId: number | string,
  payload: { requestedVariantId: number | string; reason?: string | null },
) {
  return api
    .post<ReturnRequestDetail>(`/returns/orders/${orderId}/items/${itemId}/exchange`, payload)
    .then((r) => r.data);
}

export function reportDamagedProduct(
  orderId: number | string,
  itemId: number | string,
  payload: { reason: string },
) {
  return api
    .post<ReturnRequestDetail>(`/returns/orders/${orderId}/items/${itemId}/damage`, payload)
    .then((r) => r.data);
}

export function fetchMyReturnRequests(query: { page?: number; size?: number } = {}) {
  return api.get<PageResponse<ReturnRequestSummary>>("/returns", { params: query }).then((r) => r.data);
}

export function fetchMyReturnRequest(id: number | string) {
  return api.get<ReturnRequestDetail>(`/returns/${id}`).then((r) => r.data);
}

/** GET /returns/{id}/evidence — authenticated, ownership-checked; fetch as a blob and build an object URL, never a plain `<video src>` (no way to attach a bearer header to that). */
export function fetchMyEvidenceBlob(id: number | string) {
  return api.get(`/returns/${id}/evidence`, { responseType: "blob" }).then((r) => r.data as Blob);
}
