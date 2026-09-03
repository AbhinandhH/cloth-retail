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
          <span className="absolute left-2 top-2 rounded bg-rose-600 px-2 py-0.5 text-xs font-semibold text-white">
            -{product.discountPercent}%
          </span>
        )}
        {!product.inStock && (
          <span className="absolute inset-x-0 bottom-0 bg-zinc-900/80 py-1 text-center text-xs font-medium text-white">
            Out of stock
          </span>
        )}
      </div>

      <div className="p-3">
        <p className="truncate text-xs font-medium uppercase tracking-wide text-zinc-500">{product.brand}</p>
        <h3 className="mt-0.5 truncate text-sm font-medium text-zinc-900">{product.name}</h3>

        <div className="mt-1.5 flex items-center gap-2">
          <span className="text-sm font-semibold text-zinc-900">
            {priceRange ? `${formatPrice(discountedPrice)}+` : formatPrice(discountedPrice)}
          </span>
          {hasDiscount && (
            <span className="text-xs text-zinc-400 line-through">{formatPrice(product.minPrice)}</span>
          )}
        </div>

        <span className="mt-2 inline-block rounded-full bg-zinc-100 px-2 py-0.5 text-[11px] font-medium text-zinc-600">
          {product.categoryName}
        </span>
      </div>
    </Link>
  )
}
