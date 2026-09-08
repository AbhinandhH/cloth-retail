import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as wishlistApi from '../api/wishlist'
import { useAuth } from './AuthContext'

interface WishlistContextValue {
  isLoading: boolean
  /** True once the initial load (or a guest clear) has settled - lets a ProductCard avoid a flash of "not wishlisted" before the real state is known. */
  isReady: boolean
  isWishlisted: (productId: number | string) => boolean
  /** Adds if not present, removes if present - the one thing a heart-icon click needs. Throws on failure (mirrors CartContext's addItem/etc — caller shows its own error state). */
  toggle: (productId: number | string) => Promise<void>
}

const WishlistContext = createContext<WishlistContextValue | undefined>(undefined)

/**
 * Mirrors CartContext's shape exactly: loads on auth, no-ops for guests, and
 * every mutation replaces local state with the FULL set the backend returns
 * rather than computing membership locally. Stores ids as strings internally
 * so a numeric id from the API and a numeric-or-string id from a ProductCard
 * prop always compare equal, regardless of which JS type either happens to be.
 */
export function WishlistProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isAuthChecking } = useAuth()
  const [productIds, setProductIds] = useState<Set<string>>(new Set())
  const [isLoading, setIsLoading] = useState(false)
  const [isReady, setIsReady] = useState(false)

  const load = useCallback(async () => {
    setIsLoading(true)
    try {
      const data = await wishlistApi.fetchWishlist()
      setProductIds(new Set(data.productIds.map(String)))
    } catch {
      // A failed refetch shouldn't wipe out whatever we already knew — same
      // "don't let a network blip erase good local state" reasoning as
      // CartContext's own load().
    } finally {
      setIsLoading(false)
      setIsReady(true)
    }
  }, [])

  useEffect(() => {
    if (isAuthChecking) return
    if (isAuthenticated) {
      load()
    } else {
      setProductIds(new Set())
      setIsReady(true)
    }
  }, [isAuthenticated, isAuthChecking, load])

  const isWishlisted = useCallback((productId: number | string) => productIds.has(String(productId)), [productIds])

  const toggle = useCallback(
    async (productId: number | string) => {
      const key = String(productId)
      const data = productIds.has(key)
        ? await wishlistApi.removeFromWishlist(productId)
        : await wishlistApi.addToWishlist(productId)
      setProductIds(new Set(data.productIds.map(String)))
    },
    [productIds],
  )

  const value = useMemo<WishlistContextValue>(
    () => ({ isLoading, isReady, isWishlisted, toggle }),
    [isLoading, isReady, isWishlisted, toggle],
  )

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>
}

export function useWishlist() {
  const ctx = useContext(WishlistContext)
  if (!ctx) throw new Error('useWishlist must be used within a WishlistProvider')
  return ctx
}
