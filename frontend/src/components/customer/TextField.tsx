import { useId } from 'react'
import type { ChangeEvent } from 'react'

/**
 * Customer-facing fork of the admin TextField — same prop contract, restyled.
 * Forked rather than shared for the same reason as customer/ConfirmDialog.tsx:
 * the admin original has no className/override props. Admin's
 * components/TextField.tsx is never edited.
 */
export default function TextField({
  label,
  value,
  onChange,
  type = 'text',
  textarea = false,
}: {
  label: string
  value: string | null
  onChange: (value: string | null) => void
  type?: string
  textarea?: boolean
}) {
  const id = useId()
  const commonProps = {
    id,
    value: value ?? '',
    onChange: (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      onChange(e.target.value === '' ? null : e.target.value),
    className:
      'mt-1.5 w-full rounded-lg border border-zinc-300 px-3.5 py-2.5 text-sm text-zinc-900 transition-colors focus:border-zinc-900 focus:outline-none focus:ring-1 focus:ring-zinc-900',
  }
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-zinc-900">
        {label}
      </label>
      {textarea ? <textarea rows={3} {...commonProps} /> : <input type={type} {...commonProps} />}
    </div>
  )
}
