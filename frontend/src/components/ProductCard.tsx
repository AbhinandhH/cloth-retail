import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toMediaUrl, getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useInView } from '../hooks/useInView'
import { useAuth } from '../context/AuthContext'
import { useWishlist } from '../context/WishlistContext'
import type { ProductListItem } from '../types'

// Tasteful, capped stagger — a 20-item grid should feel like a gentle ripple,
// not a slow cascading reveal. index is just the item's position in whatever
// list is rendering it (Home's grid, the related-products strip, etc.).
const STAGGER_STEP_MS = 40
const STAGGER_MAX_MS = 320

function HeartIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      className="h-4 w-4"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={filled ? 0 : 1.75}
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 20.5c-.3 0-.6-.1-.8-.3C7.4 17 3.5 13.4 3.5 9.4 3.5 6.6 5.7 4.5 8.4 4.5c1.5 0 2.9.7 3.6 1.9.7-1.2 2.1-1.9 3.6-1.9 2.7 0 4.9 2.1 4.9 4.9 0 4-3.9 7.6-7.7 10.8-.2.2-.5.3-.8.3z"
      />
    </svg>
  )
}

/**
 * The heart lives inside the card's own <Link> (so it can sit right on top of
 * the product image, in the bottom-right corner per the design) — its click
 * handler stops propagation/default so it toggles the wishlist instead of
 * also navigating to the product page.
 */
function WishlistButton({ productId, inStock }: { productId: number | string; inStock: boolean }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const { isWishlisted, toggle } = useWishlist()
  const [isToggling, setIsToggling] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const wishlisted = isWishlisted(productId)

  const handleClick = async (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()

    // Guests get sent straight to login (with a return path), matching the
    // existing "Add to cart" pattern (see ProductDetail.tsx) — no separate
    // modal/toast system to build for this.
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    if (isToggling) return

    setIsToggling(true)
    setError(null)
    try {
      await toggle(productId)
    } catch (err) {
      setError(getErrorMessage(err))
      window.setTimeout(() => setError(null), 2500)
    } finally {
      setIsToggling(false)
    }
  }

  return (
    // Shifted up above the "Out of stock" bar (which spans the full width at
    // bottom-2) when it's showing, so the two never overlap.
    <div className={`absolute right-2 ${inStock ? 'bottom-2' : 'bottom-9'}`}>
      <button
        type="button"
        onClick={handleClick}
        disabled={isToggling}
        aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
        aria-pressed={wishlisted}
        // The icon itself is small (16px) but the button's own box is a full
        // 36px, per "proper mobile touch target even though the icon itself
        // is small".
        className={`flex h-9 w-9 items-center justify-center rounded-full bg-white/85 shadow-soft backdrop-blur-sm transition-transform duration-150 hover:scale-110 active:scale-95 disabled:cursor-wait disabled:opacity-70 ${
          wishlisted ? 'text-rose-600' : 'text-zinc-600'
        }`}
      >
        <HeartIcon filled={wishlisted} />
      </button>
      {error && (
        <p className="absolute right-0 top-full mt-1 whitespace-nowrap rounded-md bg-zinc-900/90 px-2 py-1 text-[10px] font-medium text-white shadow-soft">
          {error}
        </p>
      )}
    </div>
  )
}

export default function ProductCard({ product, index = 0 }: { product: ProductListItem; index?: number }) {
  const [ref, isInView] = useInView<HTMLDivElement>()
  const hasDiscount = product.discountPercent > 0
  const discountedPrice = hasDiscount
    ? product.minPrice * (1 - product.discountPercent / 100)
    : product.minPrice
  const priceRange = product.minPrice !== product.maxPrice
  const delayMs = Math.min(index * STAGGER_STEP_MS, STAGGER_MAX_MS)

  return (
    <div ref={ref} className={`reveal ${isInView ? 'is-visible' : ''}`} style={{ transitionDelay: `${delayMs}ms` }}>
      <Link
        to={`/product/${product.slug}`}
        className="group block rounded-xl p-1 transition-shadow duration-300 hover:shadow-soft"
      >
        <div className="relative aspect-[3/4] w-full overflow-hidden rounded-lg bg-zinc-100">
          <img
            src={toMediaUrl(product.primaryImageUrl) ?? undefined}
            alt={product.name}
            // Above-the-fold cards (first row or two) load eagerly - the
            // browser's own "is this near the viewport" heuristic behind
            // loading="lazy" has real, observed quirks where it doesn't
            // realize an image is already visible until an actual scroll
            // event fires, which is exactly the "images don't show up
            // until I scroll a little" symptom this sidesteps for content
            // that's visible immediately anyway.
            loading={index < 8 ? 'eager' : 'lazy'}
            className="h-full w-full object-cover transition duration-500 ease-out group-hover:scale-[1.03]"
            onError={(e) => {
              e.currentTarget.style.display = 'none'
            }}
          />
          {hasDiscount && (
            <span
              className="absolute left-2 top-2 rounded-full px-2 py-0.5 text-[10px] font-semibold sm:px-2.5 sm:text-xs"
              style={{ backgroundColor: 'var(--brand-text, #1c1712)', color: 'var(--brand-background, #ffffff)' }}
            >
              -{product.discountPercent}%
            </span>
          )}
          {!product.inStock && (
            <span className="absolute inset-x-2 bottom-2 rounded-full bg-zinc-900/85 py-1 text-center text-[10px] font-medium text-white backdrop-blur-sm sm:text-xs">
              Out of stock
            </span>
          )}
          <WishlistButton productId={product.id} inStock={product.inStock} />
        </div>

        <div className="px-1 pt-2.5 sm:pt-3">
          <p className="truncate text-[10px] font-medium uppercase tracking-wide text-zinc-500 sm:text-xs">{product.brand}</p>
          <h3 className="mt-0.5 truncate text-xs font-medium text-zinc-900 sm:text-sm">{product.name}</h3>

          <div className="mt-1 flex flex-wrap items-center gap-x-1.5 gap-y-0.5 sm:mt-1.5">
            <span className="text-xs font-semibold text-zinc-900 sm:text-sm">
              {priceRange ? `${formatPrice(discountedPrice)}+` : formatPrice(discountedPrice)}
            </span>
            {hasDiscount && (
              <span className="text-[10px] text-zinc-400 line-through sm:text-xs">{formatPrice(product.minPrice)}</span>
            )}
          </div>

          <span className="mt-1.5 hidden truncate rounded-full bg-zinc-100 px-2 py-0.5 text-[11px] font-medium text-zinc-600 sm:mt-2 sm:inline-block">
            {product.categoryName}
          </span>
        </div>
      </Link>
    </div>
  )
}
