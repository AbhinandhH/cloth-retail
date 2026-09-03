import { useId } from 'react'
import type { ChangeEvent } from 'react'

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
      'mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500',
  }
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-zinc-900">
        {label}
      </label>
      {textarea ? (
        <textarea rows={3} {...commonProps} />
      ) : (
        <input type={type} {...commonProps} />
      )}
    </div>
  )
}
