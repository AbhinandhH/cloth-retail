import { useCallback, useEffect, useState } from 'react'
import { fetchWishlistProducts } from '../api/wishlist'
import { getErrorMessage } from '../api/client'
import ProductCard from '../components/ProductCard'
import EmptyState from '../components/EmptyState'
import ErrorState from '../components/ErrorState'
import { SkeletonImage } from '../components/Skeleton'
import type { ProductListItem } from '../types'

function HeartOutlineIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M12 20.5c-.3 0-.6-.1-.8-.3C7.4 17 3.5 13.4 3.5 9.4 3.5 6.6 5.7 4.5 8.4 4.5c1.5 0 2.9.7 3.6 1.9.7-1.2 2.1-1.9 3.6-1.9 2.7 0 4.9 2.1 4.9 4.9 0 4-3.9 7.6-7.7 10.8-.2.2-.5.3-.8.3z"
      />
    </svg>
  )
}

export default function WishlistPage() {
  const [products, setProducts] = useState<ProductListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    fetchWishlistProducts()
      .then(setProducts)
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="font-display text-2xl font-semibold text-zinc-900 sm:text-3xl">My Wishlist</h1>
      <p className="mt-1 text-sm text-zinc-500">
        {loading ? 'Loading…' : `${products.length} item${products.length === 1 ? '' : 's'} saved`}
      </p>

      <div className="mt-6">
        {error ? (
          <ErrorState message={error} onRetry={load} />
        ) : loading ? (
          <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <SkeletonImage key={i} />
            ))}
          </div>
        ) : products.length === 0 ? (
          <EmptyState
            icon={<HeartOutlineIcon />}
            title="Your wishlist is empty"
            message="Tap the heart on any product to save it here for later."
            ctaLabel="Browse products"
            ctaTo="/"
          />
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-4">
            {products.map((product, i) => (
              <ProductCard key={product.id} product={product} index={i} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
