import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import BackButton from '../components/BackButton'
import EmptyState from '../components/EmptyState'
import { SkeletonBlock, SkeletonText } from '../components/Skeleton'
import TaxIncludedNote from '../components/TaxIncludedNote'
import ConfirmDialog from '../components/customer/ConfirmDialog'
import type { CartItem } from '../types'

function CartIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M2.25 3h1.386c.51 0 .955.343 1.087.836l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 1.921-4.816 2.313-7.454a1.125 1.125 0 00-1.11-1.296H5.106M7.5 14.25L5.106 5.272M6 18.75a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z"
      />
    </svg>
  )
}

export default function CartPage() {
  const { cart, isLoading, updateQuantity, removeItem, clearCart } = useCart()

  const [removingItem, setRemovingItem] = useState<CartItem | null>(null)
  const [removing, setRemoving] = useState(false)
  const [removeError, setRemoveError] = useState<string | null>(null)

  const [clearing, setClearing] = useState(false)
  const [clearOpen, setClearOpen] = useState(false)
  const [clearError, setClearError] = useState<string | null>(null)

  // itemId -> error, so a failed quantity change on one line doesn't clobber
  // another line's message.
  const [qtyErrors, setQtyErrors] = useState<Record<string, string>>({})
  const [pendingQtyItemId, setPendingQtyItemId] = useState<string | null>(null)

  const handleQuantityChange = async (item: CartItem, nextQuantity: number) => {
    if (nextQuantity < 1 || nextQuantity > item.availableQuantity) return
    const key = String(item.id)
    setPendingQtyItemId(key)
    setQtyErrors((prev) => ({ ...prev, [key]: '' }))
    try {
      await updateQuantity(item.id, nextQuantity)
    } catch (err) {
      setQtyErrors((prev) => ({ ...prev, [key]: getErrorMessage(err) }))
    } finally {
      setPendingQtyItemId(null)
    }
  }

  const confirmRemove = async () => {
    if (!removingItem) return
    setRemoving(true)
    setRemoveError(null)
    try {
      await removeItem(removingItem.id)
      setRemovingItem(null)
    } catch (err) {
      setRemoveError(getErrorMessage(err))
    } finally {
      setRemoving(false)
    }
  }

  const confirmClear = async () => {
    setClearing(true)
    setClearError(null)
    try {
      await clearCart()
      setClearOpen(false)
    } catch (err) {
      setClearError(getErrorMessage(err))
    } finally {
      setClearing(false)
    }
  }

  if (isLoading && !cart) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="flex items-center gap-1">
          <BackButton className="-ml-2" />
          <SkeletonText width="w-28" />
        </div>
        <div className="mt-6 space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex gap-4 py-4">
              <SkeletonBlock className="h-20 w-20 shrink-0 rounded-lg" />
              <div className="flex-1 space-y-2">
                <SkeletonText width="w-3/4" />
                <SkeletonText width="w-1/3" />
                <SkeletonBlock className="mt-3 h-8 w-24" />
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  const items = cart?.items ?? []

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
        <BackButton className="-ml-2" />
        <EmptyState
          icon={<CartIcon />}
          title="Your cart is empty"
          message="Find something you love and it'll show up here."
          ctaLabel="Continue shopping"
          ctaTo="/"
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1">
          <BackButton className="-ml-2" />
          <h1 className="font-display text-2xl font-semibold text-zinc-900 sm:text-3xl">Your cart</h1>
        </div>
        <button
          type="button"
          onClick={() => setClearOpen(true)}
          className="min-h-[40px] rounded-full px-3 text-sm font-medium text-rose-800 transition-colors hover:bg-rose-50"
        >
          Clear cart
        </button>
      </div>

      <ul className="mt-6 divide-y divide-zinc-200 border-y border-zinc-200">
        {items.map((item) => {
          const key = String(item.id)
          const nearLimit = item.availableQuantity > 0 && item.availableQuantity <= item.quantity + 2
          return (
            <li key={item.id} className="flex gap-4 py-5">
              <div className="h-20 w-20 shrink-0 overflow-hidden rounded-lg bg-zinc-100 sm:h-24 sm:w-24">
                {item.imageUrl ? (
                  <img
                    src={toMediaUrl(item.imageUrl) ?? undefined}
                    alt={item.productName}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-xs text-zinc-400">No image</div>
                )}
              </div>

              <div className="flex flex-1 flex-col justify-between">
                <div>
                  <p className="text-sm font-medium text-zinc-900">{item.productName}</p>
                  <p className="mt-0.5 text-xs text-zinc-500">
                    {item.colorName} · {item.sizeName}
                  </p>
                  {!item.active && <p className="mt-1 text-xs font-medium text-rose-800">No longer available</p>}
                  {item.active && item.availableQuantity <= 0 && (
                    <p className="mt-1 text-xs font-medium text-rose-800">Out of stock</p>
                  )}
                  {item.active && item.availableQuantity > 0 && nearLimit && (
                    <p className="mt-1 text-xs font-medium text-amber-600">Only {item.availableQuantity} left</p>
                  )}
                </div>

                <div className="mt-2 flex items-center justify-between gap-3">
                  <div className="flex items-center rounded-full border border-zinc-300">
                    <button
                      type="button"
                      onClick={() => handleQuantityChange(item, item.quantity - 1)}
                      disabled={pendingQtyItemId === key || item.quantity <= 1}
                      className="flex h-10 w-10 items-center justify-center text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
                      aria-label="Decrease quantity"
                    >
                      −
                    </button>
                    <span className="min-w-[1.75rem] text-center text-sm font-medium text-zinc-900">
                      {item.quantity}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleQuantityChange(item, item.quantity + 1)}
                      disabled={pendingQtyItemId === key || item.quantity >= item.availableQuantity}
                      className="flex h-10 w-10 items-center justify-center text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
                      aria-label="Increase quantity"
                    >
                      +
                    </button>
                  </div>

                  <div className="text-right">
                    <p className="text-sm font-semibold text-zinc-900">{formatPrice(item.lineTotal)}</p>
                    {item.discountPercent > 0 && (
                      <p className="text-xs text-zinc-400 line-through">{formatPrice(item.unitPrice * item.quantity)}</p>
                    )}
                  </div>
                </div>
                {qtyErrors[key] && <p className="mt-1 text-xs text-rose-800">{qtyErrors[key]}</p>}

                <button
                  type="button"
                  onClick={() => setRemovingItem(item)}
                  className="mt-1 min-h-[40px] self-start text-xs font-medium text-zinc-500 hover:text-rose-800"
                >
                  Remove
                </button>
              </div>
            </li>
          )
        })}
      </ul>

      {/* Order summary */}
      <div className="mt-6 space-y-1.5 rounded-xl border border-zinc-200 bg-white p-4 text-sm shadow-soft sm:p-5">
        <div className="flex justify-between text-zinc-600">
          <span>Subtotal</span>
          <span>{formatPrice(cart?.subtotal ?? 0)}</span>
        </div>
        {(cart?.discountTotal ?? 0) > 0 && (
          <div className="flex justify-between text-emerald-600">
            <span>Discount</span>
            <span>−{formatPrice(cart?.discountTotal ?? 0)}</span>
          </div>
        )}
        <div className="mt-2 flex justify-between border-t border-zinc-200 pt-2 text-base font-semibold text-zinc-900">
          <span>Total</span>
          <span>{formatPrice(cart?.total ?? 0)}</span>
        </div>
        <TaxIncludedNote
          cgstPercent={cart?.cgstPercent ?? 0}
          cgstAmount={cart?.cgstAmount ?? 0}
          sgstPercent={cart?.sgstPercent ?? 0}
          sgstAmount={cart?.sgstAmount ?? 0}
        />
      </div>

      <Link
        to="/checkout"
        className="mt-4 block w-full btn-primary-radius bg-[var(--brand-primary,#18181b)] py-3.5 text-center text-sm font-semibold text-white transition-opacity hover:opacity-90"
      >
        Proceed to checkout
      </Link>

      <ConfirmDialog
        open={Boolean(removingItem)}
        title="Remove item"
        message={`Remove ${removingItem?.productName ?? 'this item'} from your cart?`}
        confirmLabel="Remove"
        danger
        confirming={removing}
        error={removeError}
        onConfirm={confirmRemove}
        onCancel={() => {
          setRemovingItem(null)
          setRemoveError(null)
        }}
      />

      <ConfirmDialog
        open={clearOpen}
        title="Clear cart"
        message="Remove all items from your cart? This can't be undone."
        confirmLabel="Clear cart"
        danger
        confirming={clearing}
        error={clearError}
        onConfirm={confirmClear}
        onCancel={() => {
          setClearOpen(false)
          setClearError(null)
        }}
      />
    </div>
  )
}
