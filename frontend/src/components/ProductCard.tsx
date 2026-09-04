import { Link } from 'react-router-dom'
import { formatPrice } from '../lib/formatPrice'
import type { ProductListItem } from '../types'

export default function ProductCard({ product }: { product: ProductListItem }) {
  const hasDiscount = product.discountPercent > 0
  const discountedPrice = hasDiscount
    ? product.minPrice * (1 - product.discountPercent / 100)
    : product.minPrice
  const priceRange = product.minPrice !== product.maxPrice

  return (
    <Link
      to={`/product/${product.slug}`}
      className="group block overflow-hidden rounded-lg border border-zinc-200 bg-white transition hover:shadow-md"
    >
      <div className="relative aspect-[3/4] w-full overflow-hidden bg-zinc-100">
        <img
          src={product.primaryImageUrl}
          alt={product.name}
          loading="lazy"
          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
          onError={(e) => {
            e.currentTarget.style.display = 'none'
          }}
        />
        {hasDiscount && (
          <span className="absolute left-1.5 top-1.5 rounded bg-rose-600 px-1.5 py-0.5 text-[10px] font-semibold text-white sm:left-2 sm:top-2 sm:px-2 sm:text-xs">
            -{product.discountPercent}%
          </span>
        )}
        {!product.inStock && (
          <span className="absolute inset-x-0 bottom-0 bg-zinc-900/80 py-1 text-center text-[10px] font-medium text-white sm:text-xs">
            Out of stock
          </span>
        )}
      </div>

      <div className="p-2 sm:p-3">
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
  )
}
