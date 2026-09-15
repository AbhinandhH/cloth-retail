import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as cartApi from '../api/cart'
import { getErrorMessage } from '../api/client'
import { useAuth } from './AuthContext'
import type { Cart } from '../types'

interface CartContextValue {
  cart: Cart | null
  isLoading: boolean
  error: string | null
  addItem: (variantId: number | string, quantity?: number) => Promise<Cart>
  updateQuantity: (itemId: number | string, quantity: number) => Promise<Cart>
  removeItem: (itemId: number | string) => Promise<Cart>
  clearCart: () => Promise<Cart>
  refresh: () => Promise<void>
}

const CartContext = createContext<CartContextValue | undefined>(undefined)

/**
 * Loads and mutates the authenticated customer's cart. Mirrors
 * AuthContext/SiteConfigContext's provider shape: every mutation calls its
 * endpoint and replaces local state with the FULL cart object the backend
 * returns (per the API contract every cart endpoint returns the complete
 * updated cart) — this context never computes totals itself.
 *
 * A no-op for guests: skips the load while unauthenticated and clears any
 * stale cart on logout, so it's safe to always mount (see main.tsx) even
 * though it's only meaningfully active for signed-in customers.
 */
export function CartProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isAuthChecking } = useAuth()
  const [cart, setCart] = useState<Cart | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await cartApi.fetchCart()
      setCart(data)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoading(false)
    }
  }, [])

  // Wait for the silent session-restore check to finish before deciding —
  // otherwise every fresh page load would briefly try (and fail) to load a
  // cart for what looks like a logged-out visitor.
  useEffect(() => {
    if (isAuthChecking) return
    if (isAuthenticated) {
      load()
    } else {
      setCart(null)
      setError(null)
    }
  }, [isAuthenticated, isAuthChecking, load])

  const addItem = useCallback(async (variantId: number | string, quantity = 1) => {
    const data = await cartApi.addCartItem(variantId, quantity)
    setCart(data)
    return data
  }, [])

  const updateQuantity = useCallback(async (itemId: number | string, quantity: number) => {
    const data = await cartApi.updateCartItem(itemId, quantity)
    setCart(data)
    return data
  }, [])

  const removeItem = useCallback(async (itemId: number | string) => {
    const data = await cartApi.removeCartItem(itemId)
    setCart(data)
    return data
  }, [])

  const clearCart = useCallback(async () => {
    const data = await cartApi.clearCart()
    setCart(data)
    return data
  }, [])

  const value = useMemo<CartContextValue>(
    () => ({ cart, isLoading, error, addItem, updateQuantity, removeItem, clearCart, refresh: load }),
    [cart, isLoading, error, addItem, updateQuantity, removeItem, clearCart, load],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within a CartProvider')
  return ctx
}
