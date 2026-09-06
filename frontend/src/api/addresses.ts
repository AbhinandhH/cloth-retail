import { api } from "./client";
import type { Address, AddressRequest } from "../types";

/** GET /api/customer/addresses — the current customer's saved addresses. */
export async function fetchAddresses() {
  const r = await api.get<Address[]>("/customer/addresses");
  return r.data;
}

/** POST /api/customer/addresses — saves a new address, returns it (with id). */
export async function createAddress(payload: AddressRequest) {
  const r = await api.post<Address>("/customer/addresses", payload);
  return r.data;
}

export async function updateAddress(
  id: number | string,
  payload: AddressRequest,
) {
  const r = await api.put<Address>(`/customer/addresses/${id}`, payload);
  return r.data;
}

export async function deleteAddress(id: number | string) {
  const r = await api.delete<void>(`/customer/addresses/${id}`);
  return r.data;
}
