/**
 * A small reusable confirmation modal for irreversible actions (delete).
 * Unlike window.confirm, it can show a backend-specific error inline (e.g. a
 * 409 "cannot delete" message) instead of failing silently, and it can show
 * a busy state while the confirm action is in flight.
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-6 shadow-xl">
        <h2 className="text-base font-semibold text-zinc-900">{title}</h2>
        <p className="mt-2 text-sm text-zinc-600">{message}</p>

        {error && (
          <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {error}
          </div>
        )}

        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={confirming}
            className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-60"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirming}
            className={`rounded-md px-4 py-2 text-sm font-semibold text-white disabled:opacity-60 ${
              danger ? 'bg-rose-600 hover:bg-rose-500' : 'bg-zinc-900 hover:bg-zinc-700'
            }`}
          >
            {confirming ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
