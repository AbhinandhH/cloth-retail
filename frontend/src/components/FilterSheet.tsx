import { useEffect, useState } from 'react'
import FilterFields from './FilterFields'
import type { FilterValues } from './FilterFields'
import type { Category, Color, Size } from '../types'

interface FilterSheetProps {
  open: boolean
  onClose: () => void
  categories: Category[]
  sizes: Size[]
  colors: Color[]
  /** Currently-applied (committed) values — the sheet's draft starts here each time it opens. */
  committed: FilterValues
  onApply: (values: FilterValues) => void
}

const emptyValues: FilterValues = { categoryId: '', sizeId: '', colorId: '', minPrice: '', maxPrice: '' }

/**
 * Mobile-only bottom sheet for filters (lg:hidden — desktop keeps the sticky
 * sidebar). Edits are buffered locally and only take effect on "Show
 * results," so the product grid behind it doesn't refetch/jump while the
 * sheet is still open.
 */
export default function FilterSheet({ open, onClose, categories, sizes, colors, committed, onApply }: FilterSheetProps) {
  const [draft, setDraft] = useState<FilterValues>(committed)

  // Re-seed the draft from whatever's currently applied every time the sheet opens.
  useEffect(() => {
    if (open) setDraft(committed)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  useEffect(() => {
    if (!open) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [open])

  if (!open) return null

  const updateDraft: <K extends keyof FilterValues>(key: K, value: FilterValues[K]) => void = (key, value) =>
    setDraft((prev) => ({ ...prev, [key]: value }))

  const activeDraftCount = Object.values(draft).filter(Boolean).length

  return (
    <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Filters">
      <div className="absolute inset-0 bg-zinc-950/40" onClick={onClose} />
      <div className="animate-[sheet-slide-up_0.25s_ease-out] absolute inset-x-0 bottom-0 flex max-h-[85dvh] flex-col rounded-t-2xl bg-white shadow-elevated">
        <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
          <h2 className="font-display text-lg text-zinc-900">Filters</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close filters"
            className="flex h-9 w-9 items-center justify-center rounded-full text-zinc-500 hover:bg-zinc-100"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-4 py-5">
          <FilterFields categories={categories} sizes={sizes} colors={colors} values={draft} onChange={updateDraft} />
        </div>

        <div className="flex items-center gap-3 border-t border-zinc-200 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
          <button
            type="button"
            onClick={() => setDraft(emptyValues)}
            disabled={activeDraftCount === 0}
            className="text-sm font-medium text-zinc-600 hover:text-zinc-900 disabled:opacity-40"
          >
            Clear all
          </button>
          <button
            type="button"
            onClick={() => onApply(draft)}
            className="ml-auto flex-1 rounded-full bg-[var(--brand-primary,#18181b)] py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90"
          >
            Show results
          </button>
        </div>
      </div>
    </div>
  )
}
