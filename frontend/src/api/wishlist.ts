import { api } from './client'
import type { Wishlist } from '../types'

/** GET /api/customer/wishlist — the full current set of wishlisted product ids. */
export function fetchWishlist() {
  return api.get<Wishlist>('/customer/wishlist').then((r) => r.data)
}

/** POST /api/customer/wishlist/{productId} — idempotent add. Returns the FULL updated set. */
export function addToWishlist(productId: number | string) {
  return api.post<Wishlist>(`/customer/wishlist/${productId}`).then((r) => r.data)
}

/** DELETE /api/customer/wishlist/{productId} — idempotent remove. Returns the FULL updated set. */
export function removeFromWishlist(productId: number | string) {
  return api.delete<Wishlist>(`/customer/wishlist/${productId}`).then((r) => r.data)
}
