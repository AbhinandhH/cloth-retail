// Same theme-derived-token pattern as Navbar.tsx's own NAV_STYLES: a scoped
// custom property for the one thing index.css's global tokens don't already
// cover (a hover tint) so the Cancel button's arbitrary-value class can stay
// a single var() reference. --surface-elevated/--text-secondary/--border-subtle/
// --brand-text/--brand-primary below are index.css's own global tokens, used
// directly - no local redefinition needed for those.
const DIALOG_STYLES = `
  .confirm-dialog-surface {
    --dialog-hover-bg: color-mix(in srgb, var(--brand-text, #18181b) 5%, var(--brand-background, #ffffff));
  }
`

/**
 * Customer-facing fork of the admin ConfirmDialog — same prop contract, but
 * styled to the storefront's premium design language, tracking the
 * merchant's configured brand theme (index.css's --brand-* and --surface-*
 * tokens) the same way every other customer-facing surface does. Forked
 * (not shared) because the admin original has no className/override props,
 * so restyling it in place would leak into untested admin CRUD screens -
 * admin intentionally keeps its own stable design regardless of the
 * storefront theme. Admin's components/ConfirmDialog.tsx is never edited.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  danger = false,
  confirming = false,
  error,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
  confirming?: boolean
  error?: string | null
  onConfirm: () => void
  onCancel: () => void
}) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/50 px-4 animate-fade-in">
      <style>{DIALOG_STYLES}</style>
      <div className="confirm-dialog-surface w-full max-w-sm rounded-2xl bg-[var(--surface-elevated,#ffffff)] p-6 shadow-elevated animate-scale-in">
        <h2 className="text-base font-semibold text-[var(--brand-text,#18181b)]">{title}</h2>
        <p className="mt-2 text-sm text-[var(--text-secondary,#52525b)]">{message}</p>

        {/* Danger/error stay a fixed rose - a semantic "this is destructive/wrong" color that
            should read the same regardless of which brand theme is active, not a themed one. */}
        {error && (
          <div className="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {error}
          </div>
        )}

        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={confirming}
            className="rounded-full border border-[var(--border-subtle,#d4d4d8)] px-4 py-2 text-sm font-medium text-[var(--text-secondary,#3f3f46)] transition-colors hover:bg-[var(--dialog-hover-bg)] disabled:opacity-60"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirming}
            className={`rounded-full px-4 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60 ${
              danger ? 'bg-rose-600' : 'bg-[var(--brand-primary,#18181b)]'
            }`}
          >
            {confirming ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
