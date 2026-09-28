export default function Swatch({ color, label }: { color: string | null; label: string }) {
  if (!color) return null
  return (
    <span
      title={`${label}: ${color}`}
      className="h-6 w-6 rounded-full border border-zinc-200"
      style={{ backgroundColor: color }}
    />
  )
}
