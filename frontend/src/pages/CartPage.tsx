import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import ConfirmDialog from '../components/ConfirmDialog'
import type { CartItem } from '../types'

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
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="space-y-4">
          <div className="h-24 animate-pulse rounded-lg bg-zinc-100" />
          <div className="h-24 animate-pulse rounded-lg bg-zinc-100" />
        </div>
      </div>
    )
  }

  const items = cart?.items ?? []

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center sm:px-6 lg:px-8">
        <p className="text-lg font-medium text-zinc-900">Your cart is empty</p>
        <p className="mt-2 text-sm text-zinc-500">Find something you love and it'll show up here.</p>
        <Link
          to="/"
          className="mt-6 inline-block rounded-lg bg-[var(--brand-primary,#18181b)] px-6 py-2.5 text-sm font-semibold text-white ring-2 ring-offset-1 ring-[var(--brand-secondary,#18181b)] hover:opacity-90"
        >
          Continue shopping
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-zinc-900">Your cart</h1>
        <button
          type="button"
          onClick={() => setClearOpen(true)}
          className="text-sm font-medium text-rose-600 hover:text-rose-700"
        >
          Clear cart
        </button>
      </div>

      <ul className="mt-6 divide-y divide-zinc-200 border-y border-zinc-200">
        {items.map((item) => {
          const key = String(item.id)
          const nearLimit = item.availableQuantity > 0 && item.availableQuantity <= item.quantity + 2
          return (
            <li key={item.id} className="flex gap-4 py-4">
              <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-md bg-zinc-100">
                {item.imageUrl ? (
                  <img src={toMediaUrl(item.imageUrl) ?? undefined} alt={item.productName} className="h-full w-full object-cover" />
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
                  {!item.active && (
                    <p className="mt-1 text-xs font-medium text-rose-600">No longer available</p>
                  )}
                  {item.active && item.availableQuantity <= 0 && (
                    <p className="mt-1 text-xs font-medium text-rose-600">Out of stock</p>
                  )}
                  {item.active && item.availableQuantity > 0 && nearLimit && (
                    <p className="mt-1 text-xs font-medium text-amber-600">Only {item.availableQuantity} left</p>
                  )}
                </div>

                <div className="mt-2 flex items-center justify-between">
                  <div className="flex items-center rounded-md border border-zinc-300">
                    <button
                      type="button"
                      onClick={() => handleQuantityChange(item, item.quantity - 1)}
                      disabled={pendingQtyItemId === key || item.quantity <= 1}
                      className="px-2.5 py-1 text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
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
                      className="px-2.5 py-1 text-zinc-700 disabled:cursor-not-allowed disabled:opacity-40"
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
                {qtyErrors[key] && <p className="mt-1 text-xs text-rose-600">{qtyErrors[key]}</p>}

                <button
                  type="button"
                  onClick={() => setRemovingItem(item)}
                  className="mt-1 self-start text-xs font-medium text-zinc-500 hover:text-rose-600"
                >
                  Remove
                </button>
              </div>
            </li>
          )
        })}
      </ul>

      {/* Order summary */}
      <div className="mt-6 space-y-1.5 rounded-lg border border-zinc-200 p-4 text-sm">
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
      </div>

      <Link
        to="/checkout"
        className="mt-4 block w-full rounded-lg bg-[var(--brand-primary,#18181b)] py-3 text-center text-sm font-semibold text-white ring-2 ring-offset-1 ring-[var(--brand-secondary,#18181b)] hover:opacity-90"
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
