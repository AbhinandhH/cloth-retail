import { api } from "./client";
import type {
  EligibleOrderItem,
  PageResponse,
  ReturnRequestDetail,
  ReturnRequestSummary,
} from "../types";

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
