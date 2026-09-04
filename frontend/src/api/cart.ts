import { api } from './client'
import type { Cart } from '../types'

/** GET /api/cart — empty `items` if the customer has no cart yet (no separate create step). */
export function fetchCart() {
  return api.get<Cart>('/cart').then((r) => r.data)
}

/** POST /api/cart/items — adds (or increments) a variant. Returns the FULL updated cart. */
export function addCartItem(productVariantId: number | string, quantity: number) {
  return api.post<Cart>('/cart/items', { productVariantId, quantity }).then((r) => r.data)
}

/** PUT /api/cart/items/{itemId} — sets the line's absolute quantity. Returns the FULL updated cart. */
export function updateCartItem(itemId: number | string, quantity: number) {
  return api.put<Cart>(`/cart/items/${itemId}`, { quantity }).then((r) => r.data)
}

/** DELETE /api/cart/items/{itemId} — removes one line. Returns the FULL updated cart. */
export function removeCartItem(itemId: number | string) {
  return api.delete<Cart>(`/cart/items/${itemId}`).then((r) => r.data)
}

/** DELETE /api/cart — empties the cart. Returns the FULL (now-empty) cart. */
export function clearCart() {
  return api.delete<Cart>('/cart').then((r) => r.data)
}
