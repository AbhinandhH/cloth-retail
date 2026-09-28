export default function Field({
  id,
  label,
  type,
  autoComplete,
  value,
  onChange,
  error,
  required,
}: {
  id: string
  label: string
  type: string
  autoComplete?: string
  value: string
  onChange: (value: string) => void
  error?: string
  required?: boolean
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-zinc-700">
        {label}
      </label>
      <input
        id={id}
        type={type}
        required={required}
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1.5 w-full rounded-xl border border-zinc-200 bg-white px-3.5 py-2.5 text-sm text-zinc-900 transition-colors focus:border-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-900/10"
      />
      {error && <p className="mt-1.5 text-xs text-rose-600">{error}</p>}
    </div>
  )
}
