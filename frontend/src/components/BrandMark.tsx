import { toMediaUrl } from '../api/client'

export default function BrandMark({ businessName, logoUrl }: { businessName: string | null | undefined; logoUrl: string | null | undefined }) {
  const resolvedLogo = toMediaUrl(logoUrl)
  if (resolvedLogo) {
    return <img src={resolvedLogo} alt={businessName ?? 'Logo'} className="h-9 w-auto object-contain" />
  }
  if (businessName) {
    return <span className="text-xl font-bold tracking-tight text-zinc-900">{businessName}</span>
  }
  return (
    <span className="text-xl font-bold tracking-tight text-zinc-900">
      THREAD<span className="text-[var(--brand-primary,#e11d48)]">CO</span>
    </span>
  )
}
