import { api } from "./client";
import type {
  AdminReturnDetail,
  AdminReturnRow,
  PageResponse,
  ReturnRequestStatus,
  ReturnRequestType,
} from "../types";

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== "") {
      params[key] = value as string | number;
    }
  }
  return params;
}

export interface AdminReturnQuery {
  page?: number;
  size?: number;
  requestType?: ReturnRequestType | "";
  status?: ReturnRequestStatus | "";
  q?: string;
}

export function fetchAdminReturns(query: AdminReturnQuery = {}) {
  return api
    .get<PageResponse<AdminReturnRow>>("/admin/returns", { params: cleanParams(query) })
    .then((r) => r.data);
}

export function fetchAdminReturnDetail(id: number | string) {
  return api.get<AdminReturnDetail>(`/admin/returns/${id}`).then((r) => r.data);
}

export function approveExchange(id: number | string, note?: string | null) {
  return api.post<AdminReturnDetail>(`/admin/returns/${id}/approve-exchange`, { note }).then((r) => r.data);
}

export function approveDamageClaim(id: number | string, note?: string | null) {
  return api.post<AdminReturnDetail>(`/admin/returns/${id}/approve-damage`, { note }).then((r) => r.data);
}

export function rejectReturnRequest(id: number | string, note?: string | null) {
  return api.post<AdminReturnDetail>(`/admin/returns/${id}/reject`, { note }).then((r) => r.data);
}

export function uploadReturnEvidence(id: number | string, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return api
    .post<AdminReturnDetail>(`/admin/returns/${id}/evidence`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then((r) => r.data);
}

/** GET /admin/returns/{id}/evidence — fetch as a blob and build an object URL, same reasoning as the customer-side fetchMyEvidenceBlob. */
export function fetchAdminEvidenceBlob(id: number | string) {
  return api.get(`/admin/returns/${id}/evidence`, { responseType: "blob" }).then((r) => r.data as Blob);
}

export function initiateReturnRefund(id: number | string, payload: { amount: number; reason?: string | null }) {
  return api.post<AdminReturnDetail>(`/admin/returns/${id}/refund`, payload).then((r) => r.data);
}
