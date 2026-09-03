import { Link } from 'react-router-dom'
import { useSiteConfig } from '../context/SiteConfigContext'

export default function Footer() {
  const { config } = useSiteConfig()

  const businessName = config?.businessName ?? null
  const tagline = config?.tagline ?? 'Clothing that fits your life.'
  const footerText = config?.footerText ?? `© ${new Date().getFullYear()} ThreadCo. All rights reserved.`
  const contactEmail = config?.contactEmail ?? null
  const contactPhone = config?.contactPhone ?? null
  const instagramUrl = config?.instagramUrl ?? null
  const whatsappNumber = config?.whatsappNumber ?? null
  const facebookUrl = config?.facebookUrl ?? null

  const hasSocial = Boolean(instagramUrl || whatsappNumber || facebookUrl)

  return (
    <footer className="border-t border-zinc-200 bg-zinc-50">
      <div className="flex h-1 w-full">
        <div className="w-1/2 bg-[var(--brand-primary,#18181b)]" />
        <div className="w-1/2 bg-[var(--brand-secondary,#18181b)]" />
      </div>
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
          <div className="col-span-2 sm:col-span-1">
            <span className="text-lg font-bold tracking-tight text-zinc-900">
              {businessName ?? (
                <>
                  THREAD<span className="text-[var(--brand-primary,#e11d48)]">CO</span>
                </>
              )}
            </span>
            <p className="mt-2 text-sm text-zinc-500">{tagline}</p>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-zinc-900">Shop</h3>
            <ul className="mt-3 space-y-2 text-sm text-zinc-500">
              <li><Link to="/" className="hover:text-zinc-900">All products</Link></li>
              <li><Link to="/?category=new" className="hover:text-zinc-900">New arrivals</Link></li>
            </ul>
          </div>

          {(contactEmail || contactPhone) && (
            <div>
              <h3 className="text-sm font-semibold text-zinc-900">Support</h3>
              <ul className="mt-3 space-y-2 text-sm text-zinc-500">
                {contactEmail && (
                  <li><a href={`mailto:${contactEmail}`} className="hover:text-zinc-900">{contactEmail}</a></li>
                )}
                {contactPhone && (
                  <li><a href={`tel:${contactPhone}`} className="hover:text-zinc-900">{contactPhone}</a></li>
                )}
              </ul>
            </div>
          )}

          {hasSocial && (
            <div>
              <h3 className="text-sm font-semibold text-zinc-900">Follow</h3>
              <ul className="mt-3 space-y-2 text-sm text-zinc-500">
                {instagramUrl && (
                  <li>
                    <a href={instagramUrl} target="_blank" rel="noreferrer" className="hover:text-zinc-900">
                      Instagram
                    </a>
                  </li>
                )}
                {whatsappNumber && (
                  <li>
                    <a
                      href={`https://wa.me/${whatsappNumber.replace(/[^\d]/g, '')}`}
                      target="_blank"
                      rel="noreferrer"
                      className="hover:text-zinc-900"
                    >
                      WhatsApp
                    </a>
                  </li>
                )}
                {facebookUrl && (
                  <li>
                    <a href={facebookUrl} target="_blank" rel="noreferrer" className="hover:text-zinc-900">
                      Facebook
                    </a>
                  </li>
                )}
              </ul>
            </div>
          )}
        </div>

        <div className="mt-8 border-t border-zinc-200 pt-6 text-xs text-zinc-400">{footerText}</div>
      </div>
    </footer>
  )
}
