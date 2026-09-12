import { Link } from 'react-router-dom'
import { toMediaUrl } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useInView } from '../hooks/useInView'
import WishlistButton from './WishlistButton'
import type { ProductListItem } from '../types'

// Tasteful, capped stagger — a 20-item grid should feel like a gentle ripple,
// not a slow cascading reveal. index is just the item's position in whatever
// list is rendering it (Home's grid, the related-products strip, etc.).
const STAGGER_STEP_MS = 40
const STAGGER_MAX_MS = 320

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
        // product-card-surface: a quiet-motif hook (see index.css) that drops the hover shadow
        // and, on Élan, tightens the radius - the "signature" theme's own rounded-xl/shadow-soft
        // classes below still apply as the default for every theme that isn't studio/elan.
        className="product-card-surface group block rounded-xl p-1 transition-shadow duration-300 hover:shadow-soft"
      >
        <div className="product-card-image relative aspect-[3/4] w-full overflow-hidden rounded-lg bg-[var(--surface-elevated)]">
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
          {/* Shifted up above the "Out of stock" bar (which spans the full width at
              bottom-2) when it's showing, so the two never overlap. */}
          <WishlistButton
            productId={product.id}
            size="sm"
            className={`absolute right-2 ${product.inStock ? 'bottom-2' : 'bottom-9'}`}
          />
        </div>

        <div className="px-1 pt-2.5 sm:pt-3">
          <p className="truncate text-[10px] font-medium uppercase tracking-wide text-[var(--text-secondary)] sm:text-xs">{product.brand}</p>
          <h3 className="mt-0.5 truncate text-xs font-medium text-[var(--brand-text,#18181b)] sm:text-sm">{product.name}</h3>

          <div className="mt-1 flex flex-wrap items-center gap-x-1.5 gap-y-0.5 sm:mt-1.5">
            <span className="text-xs font-semibold text-[var(--brand-text,#18181b)] sm:text-sm">
              {priceRange ? `${formatPrice(discountedPrice)}+` : formatPrice(discountedPrice)}
            </span>
            {hasDiscount && (
              <span className="text-[10px] text-[var(--text-secondary)] line-through sm:text-xs">{formatPrice(product.minPrice)}</span>
            )}
          </div>

          <span className="mt-1.5 hidden truncate rounded-full bg-[var(--surface-elevated)] px-2 py-0.5 text-[11px] font-medium text-[var(--text-secondary)] sm:mt-2 sm:inline-block">
            {product.categoryName}
          </span>
        </div>
      </Link>
    </div>
  )
}
