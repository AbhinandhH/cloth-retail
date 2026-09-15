import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { fetchProductBySlug, fetchProducts } from '../api/products'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useCategories } from '../context/MasterDataContext'
import BackButton from '../components/BackButton'
import ProductCard from '../components/ProductCard'
import WishlistButton from '../components/WishlistButton'
import ErrorState from '../components/ErrorState'
import { SkeletonBlock, SkeletonImage, SkeletonText } from '../components/Skeleton'
import type { ProductDetail as ProductDetailType, ProductListItem, ProductVariant } from '../types'

// Static, generic size-conversion reference — explicitly NOT per-product or
// per-category data (no backend field exists for that, and none should be
// added). Approximate inches, general guidance only.
const SIZE_GUIDE_ROWS = [
  { size: 'S', chest: '34–36"', waist: '28–30"' },
  { size: 'M', chest: '38–40"', waist: '32–34"' },
  { size: 'L', chest: '42–44"', waist: '36–38"' },
  { size: 'XL', chest: '46–48"', waist: '40–42"' },
  { size: 'XXL', chest: '50–52"', waist: '44–46"' },
]

export default function ProductDetail() {
  const { slug } = useParams<{ slug: string }>()
  const { isAuthenticated } = useAuth()
  const { addItem } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const { data: categories } = useCategories()
  const [product, setProduct] = useState<ProductDetailType | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [selectedSize, setSelectedSize] = useState<string | null>(null)
  const [selectedColor, setSelectedColor] = useState<string | null>(null)
  const [activeImage, setActiveImage] = useState(0)
  const [quantity, setQuantity] = useState(1)
  const [addingToCart, setAddingToCart] = useState(false)
  const [addedToCart, setAddedToCart] = useState(false)
  const [cartNotice, setCartNotice] = useState<string | null>(null)
  const [cartNoticeIsError, setCartNoticeIsError] = useState(false)

  const [relatedProducts, setRelatedProducts] = useState<ProductListItem[]>([])

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

  // Related products: same category, excluding the current product. There's
  // no categoryId on ProductDetail (only categoryName) and fetchProducts
  // filters by categoryId — so resolve the name against the categories master
  // list first. If it can't be resolved, or the category turns out to have no
  // other products, relatedProducts just stays empty and the section below is
  // omitted entirely (an empty strip here would look broken, not intentional).
  useEffect(() => {
    if (!product) return
    const category = categories.find((c) => c.name === product.categoryName)
    if (!category) {
      setRelatedProducts([])
      return
    }
    let cancelled = false
    fetchProducts({ categoryId: category.id, size: 8 })
      .then((res) => {
        if (cancelled) return
        setRelatedProducts(res.content.filter((p) => p.slug !== product.slug).slice(0, 4))
      })
      .catch(() => {
        if (!cancelled) setRelatedProducts([])
      })
    return () => {
      cancelled = true
    }
  }, [product, categories])

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

  // Every size of a color shares the exact same photos (images are grouped
  // by color on the backend now, not by size), so the gallery only needs to
  // reset when the color actually changes, not on every size tap.
  useEffect(() => {
    setActiveImage(0)
  }, [selectedVariant?.colorName])

  // Quantity/cart notice DO need to reset on any variant change, size
  // included — stock and a previously-shown error/notice are specific to the
  // exact size+color SKU.
  useEffect(() => {
    setQuantity(1)
    setCartNotice(null)
    setCartNoticeIsError(false)
    setAddedToCart(false)
  }, [selectedVariant?.id])

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
          <SkeletonImage className="aspect-[4/5] sm:aspect-square" />
          <div className="space-y-4">
            <SkeletonText width="w-1/3" />
            <SkeletonText width="w-2/3" className="h-7" />
            <SkeletonText width="w-1/4" />
            <SkeletonBlock className="h-9 w-40" />
            <SkeletonBlock className="h-24 w-full" />
          </div>
        </div>
      </div>
    )
  }

  if (error || !product) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
        <ErrorState title="Product not found" message={error ?? 'This product may have been removed.'} />
        <div className="-mt-6 text-center">
          <Link to="/" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
            &larr; Back to shop
          </Link>
        </div>
      </div>
    )
  }

  const variant = selectedVariant
  // Variant images may come back as backend-relative paths (local uploads, e.g. "/media/x.png")
  // or already-absolute URLs (seeded picsum.photos data) - toMediaUrl leaves the latter untouched.
  const images = variant?.images?.length ? variant.images.map((url) => toMediaUrl(url) ?? url) : []
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
      setAddedToCart(true)
    } catch (err) {
      setCartNotice(getErrorMessage(err))
      setCartNoticeIsError(true)
    } finally {
      setAddingToCart(false)
    }
  }

  const handleBuyNow = () => {
    navigate('/checkout')
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
        <div className="relative">
          <BackButton className="absolute left-2 top-2 z-10 bg-white/90 shadow-soft backdrop-blur-sm sm:left-3 sm:top-3" />
          <div className="relative aspect-[4/5] w-full overflow-hidden rounded-lg bg-zinc-100 sm:aspect-square">
            {images[activeImage] ? (
              <img
                key={activeImage}
                src={images[activeImage]}
                alt={`${product.name} — ${variant?.colorName}`}
                className="animate-fade-in h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-zinc-400">No image</div>
            )}
            {/* Same bottom-right placement as ProductCard's grid heart, so the affordance
                reads identically whether you're browsing the grid or a single product. */}
            {product && <WishlistButton productId={product.id} className="absolute bottom-3 right-3" />}
          </div>
          {images.length > 1 && (
            <div className="mt-3 flex snap-x snap-mandatory gap-2 overflow-x-auto pb-1">
              {images.map((img, i) => (
                <button
                  key={img + i}
                  onClick={() => setActiveImage(i)}
                  className={`h-16 w-16 flex-shrink-0 snap-start overflow-hidden rounded-md border-2 transition-colors sm:h-20 sm:w-20 ${
                    i === activeImage ? 'border-zinc-900' : 'border-transparent hover:border-zinc-300'
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
          <h1 className="mt-1 font-display text-2xl text-zinc-900 sm:text-3xl">{product.name}</h1>

          <div className="mt-3 flex items-center gap-2">
            <span className="text-xl font-bold text-zinc-900">{formatPrice(discountedPrice)}</span>
            {hasDiscount && (
              <>
                <span className="text-sm text-zinc-400 line-through">{formatPrice(price)}</span>
                <span className="rounded-full bg-rose-100 px-1.5 py-0.5 text-xs font-semibold text-rose-700">
                  -{variant?.discountPercent}%
                </span>
              </>
            )}
          </div>

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
                    className={`h-9 w-9 rounded-full border-2 transition-colors ${
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
                      className={`min-w-[2.75rem] rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
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

          {/* Size guide — static, general reference; not per-product data */}
          <details className="group mt-4 rounded-lg border border-zinc-200">
            <summary className="flex cursor-pointer list-none items-center justify-between px-4 py-3 text-sm font-medium text-zinc-900">
              <span>Size guide</span>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-4 w-4 text-zinc-500 transition-transform group-open:rotate-180"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </summary>
            <div className="px-4 pb-4">
              <p className="mb-2 text-xs text-zinc-500">
                General size guide (approximate, inches). Fit may vary by style.
              </p>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-zinc-700">
                  <thead>
                    <tr className="border-b border-zinc-200 text-zinc-500">
                      <th className="py-1.5 pr-3 font-medium">Size</th>
                      <th className="py-1.5 pr-3 font-medium">Chest</th>
                      <th className="py-1.5 font-medium">Waist</th>
                    </tr>
                  </thead>
                  <tbody>
                    {SIZE_GUIDE_ROWS.map((row) => (
                      <tr key={row.size} className="border-b border-zinc-100 last:border-0">
                        <td className="py-1.5 pr-3 font-medium text-zinc-900">{row.size}</td>
                        <td className="py-1.5 pr-3">{row.chest}</td>
                        <td className="py-1.5">{row.waist}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </details>

          {/* Stock availability */}
          <p className="mt-4 text-sm font-medium">
            {inStock ? (
              <span className="text-emerald-600">In stock</span>
            ) : (
              <span className="text-zinc-500">Out of stock</span>
            )}
          </p>

          {/* Description */}
          <div className="mt-6 space-y-4 border-t border-zinc-200 pt-6 text-sm text-zinc-700">
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

          {/* Add to cart */}
          {inStock && isAuthenticated && (
            <div className="mt-6 flex items-center gap-3">
              <span className="text-sm font-medium text-zinc-900">Qty</span>
              <div className="flex items-center rounded-full border border-zinc-300">
                <button
                  type="button"
                  onClick={() => {
                    setQuantity((q) => Math.max(1, q - 1))
                    setAddedToCart(false)
                  }}
                  disabled={quantity <= 1}
                  className="px-3 py-1.5 text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
                  aria-label="Decrease quantity"
                >
                  −
                </button>
                <span className="min-w-[2rem] text-center text-sm font-medium text-zinc-900">{quantity}</span>
                <button
                  type="button"
                  onClick={() => {
                    setQuantity((q) => Math.min(variant?.stockQuantity ?? 1, q + 1))
                    setAddedToCart(false)
                  }}
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
            onClick={addedToCart ? handleBuyNow : handleAddToCart}
            disabled={!inStock || addingToCart}
            className="mt-4 w-full btn-primary-radius bg-[var(--brand-primary,#18181b)] py-3 text-sm font-semibold text-white ring-2 ring-offset-1 ring-[var(--brand-secondary,#18181b)] transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:bg-zinc-300 disabled:text-zinc-500 disabled:ring-0"
          >
            {!inStock ? 'Out of stock' : addingToCart ? 'Adding…' : addedToCart ? 'Buy now' : 'Add to cart'}
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
        </div>
      </div>

      {/* Related products — omitted entirely when the category can't be
          resolved or has no other products, rather than showing an empty
          strip. */}
      {relatedProducts.length > 0 && (
        <section className="mt-14 border-t border-zinc-200 pt-8">
          <h2 className="font-display text-display text-zinc-900">You may also like</h2>
          <div className="mt-5 grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
            {relatedProducts.map((p, i) => (
              <ProductCard key={p.id} product={p} index={i} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
