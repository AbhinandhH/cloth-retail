import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { markDamaged } from '../api/adminInventory'
import { getErrorMessage } from '../api/client'
import type { DamageReason, InventoryVariantRow } from '../types'

const DAMAGE_REASONS: { value: DamageReason; label: string }[] = [
  { value: 'DEFECTIVE', label: 'Defective' },
  { value: 'TRANSIT_DAMAGE', label: 'Transit damage' },
  { value: 'WAREHOUSE_DAMAGE', label: 'Warehouse damage' },
  { value: 'RETURN_DAMAGE', label: 'Return damage' },
  { value: 'OTHER', label: 'Other' },
]

interface MarkDamagedModalProps {
  /** The variant being marked damaged, or null to keep the modal closed/unmounted. */
  variant: InventoryVariantRow | null
  onClose: () => void
  /** Called after a successful submission, before onClose — caller should refetch the table. */
  onSuccess: () => void
}

/**
 * Centered modal for recording damaged stock. Same overlay/backdrop/body-scroll-lock
 * mechanics as FilterSheet, adapted to a centered card instead of a bottom sheet.
 */
export default function MarkDamagedModal({ variant, onClose, onSuccess }: MarkDamagedModalProps) {
  const [quantity, setQuantity] = useState('')
  const [reason, setReason] = useState<DamageReason>(DAMAGE_REASONS[0].value)
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const open = variant !== null

  useEffect(() => {
    if (open) {
      setQuantity('')
      setReason(DAMAGE_REASONS[0].value)
      setNotes('')
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

  const parsedQuantity = quantity.trim() === '' ? null : Number(quantity)
  const isValidQuantity = parsedQuantity !== null && Number.isInteger(parsedQuantity) && parsedQuantity > 0
  const exceedsAvailable = isValidQuantity && (parsedQuantity as number) > variant.availableQuantity
  const canSubmit = isValidQuantity && !exceedsAvailable && !saving

  const handleClose = () => {
    if (saving) return
    onClose()
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!canSubmit || parsedQuantity === null) return
    setSaving(true)
    setError(null)
    try {
      await markDamaged(variant.variantId, { quantity: parsedQuantity, reason, notes: notes.trim() || null })
      onSuccess()
      onClose()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label="Mark stock damaged">
      <div className="absolute inset-0 bg-black/40" onClick={handleClose} />
      <div className="relative mx-auto flex min-h-full max-w-md items-center px-4 py-8">
        <div className="w-full rounded-xl bg-white p-6 shadow-xl">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-zinc-900">Mark stock damaged</h2>
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
            <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Available quantity</p>
            <p className="text-2xl font-semibold text-zinc-900">{variant.availableQuantity}</p>
          </div>

          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div>
              <label htmlFor="damage-quantity" className="block text-sm font-medium text-zinc-900">
                Quantity
              </label>
              <input
                id="damage-quantity"
                type="number"
                min={1}
                max={variant.availableQuantity}
                step={1}
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              />
              {exceedsAvailable && (
                <p className="mt-1 text-xs text-rose-600">
                  Can&apos;t exceed the available quantity ({variant.availableQuantity}).
                </p>
              )}
            </div>

            <div>
              <label htmlFor="damage-reason" className="block text-sm font-medium text-zinc-900">
                Reason
              </label>
              <select
                id="damage-reason"
                value={reason}
                onChange={(e) => setReason(e.target.value as DamageReason)}
                className="mt-1 w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              >
                {DAMAGE_REASONS.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="damage-notes" className="block text-sm font-medium text-zinc-900">
                Notes <span className="font-normal text-zinc-400">(optional)</span>
              </label>
              <textarea
                id="damage-notes"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              />
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
                className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-500 disabled:opacity-50"
              >
                {saving ? 'Saving…' : 'Confirm damage'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
