import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as configApi from '../api/configuration'
import type { SiteConfiguration, ThemeColors } from '../types'

interface SiteConfigContextValue {
  config: SiteConfiguration | null
  isLoading: boolean
  /** Re-fetch GET /configuration — call after an admin change so the storefront reflects it live. */
  refresh: () => Promise<void>
}

const SiteConfigContext = createContext<SiteConfigContextValue | undefined>(undefined)

// Applies the active theme's colors as CSS custom properties on the document
// root. Most of the storefront isn't wired to consume these yet — this is
// deliberately future-proofing so pieces that DO consume them (Navbar,
// Footer, etc.) can use var(--brand-primary) instead of hardcoded hex.
function applyThemeVariables(theme: ThemeColors | null | undefined) {
  if (!theme) return
  const root = document.documentElement.style
  if (theme.primaryColor) root.setProperty('--brand-primary', theme.primaryColor)
  if (theme.secondaryColor) root.setProperty('--brand-secondary', theme.secondaryColor)
  if (theme.accentColor) root.setProperty('--brand-accent', theme.accentColor)
  if (theme.backgroundColor) root.setProperty('--brand-background', theme.backgroundColor)
  if (theme.textColor) root.setProperty('--brand-text', theme.textColor)
  // Drives index.css's body-background rule — 'minimal' drops the page-wide
  // gold tint entirely (see AmbientBackground.tsx / Home.tsx, which also
  // skip rendering the full wash component for this theme).
  document.documentElement.dataset.ambient = theme.richAmbient === false ? 'minimal' : 'rich'
  // Drives index.css's [data-motif="studio"/"elan"] rules (display typeface, shimmer/glow
  // ornamentation, hero CTA shape) — a second, independent axis from richAmbient above: a
  // theme picks its background wash AND its visual language separately. Whitelisted rather
  // than a bare .toLowerCase() passthrough so an unrecognized future motif value falls back
  // to "signature" (matching every CSS rule's own default, unstyled state) instead of quietly
  // minting a new data-motif value with no matching CSS.
  const KNOWN_MOTIFS = new Set(['studio', 'elan'])
  const motif = theme.motif?.toLowerCase()
  document.documentElement.dataset.motif = motif && KNOWN_MOTIFS.has(motif) ? motif : 'signature'
}

/**
 * Loads the public site configuration (branding, active theme, login/register
 * visuals) once on mount. Public data, no auth required — this is a sibling
 * to AuthProvider, not nested inside/dependent on it, so it loads and works
 * for logged-out visitors too.
 */
export function SiteConfigProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<SiteConfiguration | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const data = await configApi.fetchPublicConfiguration()
      setConfig(data)
      applyThemeVariables(data.theme)
    } catch (err) {
      // The storefront must keep working with defaults/fallbacks even if
      // configuration fails to load — never let this crash the app.
      console.error('Failed to load site configuration', err)
      setConfig(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const value = useMemo<SiteConfigContextValue>(
    () => ({ config, isLoading, refresh: load }),
    [config, isLoading, load],
  )

  return <SiteConfigContext.Provider value={value}>{children}</SiteConfigContext.Provider>
}

export function useSiteConfig() {
  const ctx = useContext(SiteConfigContext)
  if (!ctx) throw new Error('useSiteConfig must be used within a SiteConfigProvider')
  return ctx
}
