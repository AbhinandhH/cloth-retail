import { api } from './client'
import type { Address, AddressRequest } from '../types'

/** GET /api/customer/addresses — the current customer's saved addresses. */
export function fetchAddresses() {
  return api.get<Address[]>('/customer/addresses').then((r) => r.data)
}

/** POST /api/customer/addresses — saves a new address, returns it (with id). */
export function createAddress(payload: AddressRequest) {
  return api.post<Address>('/customer/addresses', payload).then((r) => r.data)
}

export function updateAddress(id: number | string, payload: AddressRequest) {
  return api.put<Address>(`/customer/addresses/${id}`, payload).then((r) => r.data)
}

export function deleteAddress(id: number | string) {
  return api.delete<void>(`/customer/addresses/${id}`).then((r) => r.data)
}
