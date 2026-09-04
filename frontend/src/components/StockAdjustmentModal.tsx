import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { adjustStock } from '../api/adminInventory'
import { getErrorMessage } from '../api/client'
import type { InventoryVariantRow } from '../types'

interface StockAdjustmentModalProps {
  /** The variant being adjusted, or null to keep the modal closed/unmounted. */
  variant: InventoryVariantRow | null
  onClose: () => void
  /** Called after a successful adjustment, before onClose — caller should refetch the table. */
  onSuccess: () => void
}

/**
 * Centered modal for a manual stock adjustment. Same overlay/backdrop/body-scroll-lock
 * mechanics as FilterSheet, adapted to a centered card instead of a bottom sheet.
 */
export default function StockAdjustmentModal({ variant, onClose, onSuccess }: StockAdjustmentModalProps) {
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const open = variant !== null

  // Reset the form fresh every time a (possibly different) variant is opened.
  useEffect(() => {
    if (open) {
      setAmount('')
      setReason('')
      setError(null)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, variant?.variantId])

  useEffect(() => {
    if (!open) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [open])

  if (!variant) return null

  const parsedAmount = amount.trim() === '' ? null : Number(amount)
  const isValidAmount =
    parsedAmount !== null && Number.isFinite(parsedAmount) && Number.isInteger(parsedAmount) && parsedAmount !== 0
  const resultingQuantity = isValidAmount ? variant.stockQuantity + (parsedAmount as number) : null
  const wouldGoNegative = resultingQuantity !== null && resultingQuantity < 0
  const canSubmit = isValidAmount && !wouldGoNegative && reason.trim().length > 0 && !saving

  const handleClose = () => {
    if (saving) return
    onClose()
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!canSubmit || parsedAmount === null) return
    setSaving(true)
    setError(null)
    try {
      await adjustStock(variant.variantId, { quantityChange: parsedAmount, reason: reason.trim() })
      onSuccess()
      onClose()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label="Adjust stock">
      <div className="absolute inset-0 bg-black/40" onClick={handleClose} />
      <div className="relative mx-auto flex min-h-full max-w-md items-center px-4 py-8">
        <div className="w-full rounded-xl bg-white p-6 shadow-xl">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-zinc-900">Adjust stock</h2>
            <button
              type="button"
              onClick={handleClose}
              aria-label="Close"
              className="flex h-8 w-8 items-center justify-center rounded-full text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div className="mt-4 rounded-md bg-zinc-50 px-3 py-2 text-sm">
            <p className="font-medium text-zinc-900">{variant.productName}</p>
            <p className="mt-0.5 text-zinc-500">
              {variant.sku} &middot; {variant.colorName} &middot; {variant.sizeName}
            </p>
          </div>

          <div className="mt-4">
            <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Current quantity</p>
            <p className="text-2xl font-semibold text-zinc-900">{variant.stockQuantity}</p>
          </div>

          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div>
              <label htmlFor="adjust-amount" className="block text-sm font-medium text-zinc-900">
                Adjustment amount
              </label>
              <input
                id="adjust-amount"
                type="number"
                step={1}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="e.g. 10 or -3"
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              />
              <p className="mt-1 text-xs text-zinc-500">Enter a positive number to add stock, negative to remove.</p>
            </div>

            <div>
              <label htmlFor="adjust-reason" className="block text-sm font-medium text-zinc-900">
                Reason
              </label>
              <input
                id="adjust-reason"
                type="text"
                required
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="e.g. Recount correction"
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              />
            </div>

            <div
              className={`rounded-md border px-3 py-2 text-sm ${
                wouldGoNegative ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-zinc-200 bg-zinc-50 text-zinc-700'
              }`}
            >
              {isValidAmount ? (
                <>
                  New quantity will be: <span className="font-semibold">{resultingQuantity}</span>
                </>
              ) : (
                <>Enter an amount to preview the new quantity.</>
              )}
              {wouldGoNegative && <p className="mt-1 text-xs">Resulting quantity can&apos;t be negative.</p>}
            </div>

            {error && (
              <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
            )}

            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={handleClose}
                disabled={saving}
                className="rounded-lg border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!canSubmit}
                className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-50"
              >
                {saving ? 'Saving…' : 'Confirm adjustment'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
