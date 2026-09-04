import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { fetchProductBySlug } from '../api/products'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import type { ProductDetail as ProductDetailType, ProductVariant } from '../types'

export default function ProductDetail() {
  const { slug } = useParams<{ slug: string }>()
  const { isAuthenticated } = useAuth()
  const { addItem } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const [product, setProduct] = useState<ProductDetailType | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [selectedSize, setSelectedSize] = useState<string | null>(null)
  const [selectedColor, setSelectedColor] = useState<string | null>(null)
  const [activeImage, setActiveImage] = useState(0)
  const [quantity, setQuantity] = useState(1)
  const [addingToCart, setAddingToCart] = useState(false)
  const [cartNotice, setCartNotice] = useState<string | null>(null)
  const [cartNoticeIsError, setCartNoticeIsError] = useState(false)

  useEffect(() => {
    if (!slug) return
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchProductBySlug(slug)
      .then((data) => {
        if (cancelled) return
        setProduct(data)
        const first = data.variants[0]
        setSelectedSize(first?.sizeName ?? null)
        setSelectedColor(first?.colorName ?? null)
        setActiveImage(0)
      })
      .catch((err) => {
        if (cancelled) return
        setError(getErrorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [slug])

  const sizes = useMemo(
    () => Array.from(new Set((product?.variants ?? []).map((v) => v.sizeName))),
    [product],
  )
  const colors = useMemo(() => {
    const map = new Map<string, string>()
    for (const v of product?.variants ?? []) map.set(v.colorName, v.colorHex)
    return Array.from(map.entries())
  }, [product])

  const selectedVariant: ProductVariant | undefined = useMemo(() => {
    if (!product) return undefined
    return (
      product.variants.find((v) => v.sizeName === selectedSize && v.colorName === selectedColor) ??
      product.variants.find((v) => v.colorName === selectedColor) ??
      product.variants[0]
    )
  }, [product, selectedSize, selectedColor])

  useEffect(() => {
    setActiveImage(0)
    setQuantity(1)
    setCartNotice(null)
    setCartNoticeIsError(false)
  }, [selectedVariant?.id])

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
          <div className="aspect-square animate-pulse rounded-lg bg-zinc-100" />
          <div className="space-y-4">
            <div className="h-6 w-1/3 animate-pulse rounded bg-zinc-100" />
            <div className="h-8 w-2/3 animate-pulse rounded bg-zinc-100" />
            <div className="h-5 w-1/4 animate-pulse rounded bg-zinc-100" />
          </div>
        </div>
      </div>
    )
  }

  if (error || !product) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center">
        <p className="text-lg font-medium text-zinc-900">Product not found</p>
        <p className="mt-2 text-sm text-zinc-500">{error ?? 'This product may have been removed.'}</p>
        <Link to="/" className="mt-6 inline-block text-sm font-medium text-rose-600 hover:text-rose-700">
          &larr; Back to shop
        </Link>
      </div>
    )
  }

  const variant = selectedVariant
  const images = variant?.images?.length ? variant.images : []
  const hasDiscount = (variant?.discountPercent ?? 0) > 0
  const price = variant?.sellingPrice ?? 0
  const discountedPrice = hasDiscount ? price * (1 - (variant?.discountPercent ?? 0) / 100) : price
  const inStock = (variant?.stockQuantity ?? 0) > 0

  const handleAddToCart = async () => {
    // Browsing/viewing a product is always public - only the purchase action
    // itself is auth-gated, per the requirement that Instagram-ad traffic
    // must be able to land directly on a product page without hitting a
    // login wall. An unauthenticated tap sends them to login and back here.
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    if (!variant) return
    setAddingToCart(true)
    setCartNotice(null)
    setCartNoticeIsError(false)
    try {
      await addItem(variant.id, quantity)
      setCartNotice('Added to cart.')
      setCartNoticeIsError(false)
    } catch (err) {
      setCartNotice(getErrorMessage(err))
      setCartNoticeIsError(true)
    } finally {
      setAddingToCart(false)
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <nav className="mb-4 text-sm text-zinc-500">
        <Link to="/" className="hover:text-zinc-800">Shop</Link>
        <span className="mx-1.5">/</span>
        <span className="text-zinc-700">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        {/* Gallery */}
        <div>
          <div className="aspect-square w-full overflow-hidden rounded-lg bg-zinc-100">
            {images[activeImage] ? (
              <img
                src={images[activeImage]}
                alt={`${product.name} — ${variant?.colorName}`}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-zinc-400">No image</div>
            )}
          </div>
          {images.length > 1 && (
            <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
              {images.map((img, i) => (
                <button
                  key={img + i}
                  onClick={() => setActiveImage(i)}
                  className={`h-16 w-16 flex-shrink-0 overflow-hidden rounded-md border-2 ${
                    i === activeImage ? 'border-zinc-900' : 'border-transparent'
                  }`}
                >
                  <img src={img} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Details */}
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-zinc-500">{product.brand}</p>
          <h1 className="mt-1 text-2xl font-semibold text-zinc-900">{product.name}</h1>

          <div className="mt-3 flex items-center gap-2">
            <span className="text-xl font-bold text-zinc-900">{formatPrice(discountedPrice)}</span>
            {hasDiscount && (
              <>
                <span className="text-sm text-zinc-400 line-through">{formatPrice(price)}</span>
                <span className="rounded bg-rose-100 px-1.5 py-0.5 text-xs font-semibold text-rose-700">
                  -{variant?.discountPercent}%
                </span>
              </>
            )}
          </div>

          <p className="mt-2 text-sm font-medium">
            {inStock ? (
              <span className="text-emerald-600">In stock</span>
            ) : (
              <span className="text-zinc-500">Out of stock</span>
            )}
          </p>

          {/* Color selector */}
          {colors.length > 0 && (
            <div className="mt-6">
              <p className="mb-2 text-sm font-medium text-zinc-900">
                Color{selectedColor ? `: ${selectedColor}` : ''}
              </p>
              <div className="flex flex-wrap gap-2">
                {colors.map(([name, hex]) => (
                  <button
                    key={name}
                    onClick={() => setSelectedColor(name)}
                    title={name}
                    aria-pressed={selectedColor === name}
                    className={`h-9 w-9 rounded-full border-2 ${
                      selectedColor === name ? 'border-zinc-900' : 'border-zinc-200'
                    }`}
                    style={{ backgroundColor: hex || '#e5e5e5' }}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Size selector */}
          {sizes.length > 0 && (
            <div className="mt-6">
              <p className="mb-2 text-sm font-medium text-zinc-900">Size{selectedSize ? `: ${selectedSize}` : ''}</p>
              <div className="flex flex-wrap gap-2">
                {sizes.map((size) => {
                  const variantForSize = product.variants.find(
                    (v) => v.sizeName === size && v.colorName === selectedColor,
                  )
                  const disabled = variantForSize ? variantForSize.stockQuantity <= 0 : false
                  return (
                    <button
                      key={size}
                      onClick={() => setSelectedSize(size)}
                      disabled={disabled}
                      className={`min-w-[2.75rem] rounded-md border px-3 py-1.5 text-sm font-medium ${
                        selectedSize === size
                          ? 'border-zinc-900 bg-zinc-900 text-white'
                          : 'border-zinc-300 text-zinc-700 hover:border-zinc-500'
                      } ${disabled ? 'cursor-not-allowed opacity-40' : ''}`}
                    >
                      {size}
                    </button>
                  )
                })}
              </div>
            </div>
          )}

          {inStock && isAuthenticated && (
            <div className="mt-8 flex items-center gap-3">
              <span className="text-sm font-medium text-zinc-900">Qty</span>
              <div className="flex items-center rounded-md border border-zinc-300">
                <button
                  type="button"
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  disabled={quantity <= 1}
                  className="px-3 py-1.5 text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
                  aria-label="Decrease quantity"
                >
                  −
                </button>
                <span className="min-w-[2rem] text-center text-sm font-medium text-zinc-900">{quantity}</span>
                <button
                  type="button"
                  onClick={() => setQuantity((q) => Math.min(variant?.stockQuantity ?? 1, q + 1))}
                  disabled={quantity >= (variant?.stockQuantity ?? 1)}
                  className="px-3 py-1.5 text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
                  aria-label="Increase quantity"
                >
                  +
                </button>
              </div>
            </div>
          )}

          <button
            type="button"
            onClick={handleAddToCart}
            disabled={!inStock || addingToCart}
            className="mt-4 w-full rounded-lg bg-[var(--brand-primary,#18181b)] py-3 text-sm font-semibold text-white ring-2 ring-offset-1 ring-[var(--brand-secondary,#18181b)] hover:opacity-90 disabled:cursor-not-allowed disabled:bg-zinc-300 disabled:text-zinc-500 disabled:ring-0"
          >
            {!inStock ? 'Out of stock' : addingToCart ? 'Adding…' : 'Add to cart'}
          </button>
          {cartNotice && (
            <p className={`mt-2 text-center text-xs ${cartNoticeIsError ? 'text-rose-600' : 'text-zinc-500'}`}>
              {cartNotice}
              {!cartNoticeIsError && (
                <>
                  {' '}
                  <Link to="/cart" className="font-medium text-zinc-900 hover:underline">
                    View cart
                  </Link>
                </>
              )}
            </p>
          )}

          <div className="mt-8 space-y-4 border-t border-zinc-200 pt-6 text-sm text-zinc-700">
            <p>{product.description}</p>
            <dl className="grid grid-cols-2 gap-y-2">
              <dt className="text-zinc-500">Category</dt>
              <dd>{product.categoryName}{product.subCategoryName ? ` / ${product.subCategoryName}` : ''}</dd>
              <dt className="text-zinc-500">Brand</dt>
              <dd>{product.brand}</dd>
              <dt className="text-zinc-500">Material</dt>
              <dd>{product.material}</dd>
              {variant?.sku && (
                <>
                  <dt className="text-zinc-500">SKU</dt>
                  <dd>{variant.sku}</dd>
                </>
              )}
            </dl>
          </div>
        </div>
      </div>
    </div>
  )
}
