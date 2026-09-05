/**
 * Customer-facing fork of the admin ConfirmDialog — same prop contract, but
 * styled to the storefront's premium design language. Forked (not shared)
 * because the admin original has no className/override props, so restyling
 * it in place would leak into untested admin CRUD screens. Admin's
 * components/ConfirmDialog.tsx is never edited.
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
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-elevated animate-scale-in">
        <h2 className="text-base font-semibold text-zinc-900">{title}</h2>
        <p className="mt-2 text-sm text-zinc-600">{message}</p>

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
            className="rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-60"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirming}
            className={`rounded-full px-4 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60 ${
              danger ? 'bg-rose-600' : 'bg-zinc-900'
            }`}
          >
            {confirming ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
