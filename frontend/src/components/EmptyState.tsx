import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface EmptyStateProps {
  icon: ReactNode
  title: string
  message?: string
  ctaLabel?: string
  ctaTo?: string
  onCta?: () => void
}

/**
 * Shared "nothing here" treatment for empty cart/orders/search-results/product
 * grids — replaces the plain-text placeholders that existed per-page before.
 */
export default function EmptyState({ icon, title, message, ctaLabel, ctaTo, onCta }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center px-4 py-16 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--surface-elevated)] text-[var(--text-secondary)]">{icon}</div>
      <p className="mt-5 text-base font-medium text-[var(--brand-text,#18181b)]">{title}</p>
      {message && <p className="mt-1.5 max-w-xs text-sm text-[var(--text-secondary)]">{message}</p>}
      {ctaLabel && (ctaTo || onCta) && (
        <>
          {ctaTo ? (
            <Link
              to={ctaTo}
              className="mt-6 btn-primary-radius bg-[var(--brand-primary,#18181b)] px-6 py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90"
            >
              {ctaLabel}
            </Link>
          ) : (
            <button
              type="button"
              onClick={onCta}
              className="mt-6 btn-primary-radius bg-[var(--brand-primary,#18181b)] px-6 py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90"
            >
              {ctaLabel}
            </button>
          )}
        </>
      )}
    </div>
  )
}
