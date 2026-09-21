import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useSiteConfig } from '../context/SiteConfigContext'
import * as configApi from '../api/configuration'
import { getErrorMessage } from '../api/client'
import type { AdminSiteConfiguration, SiteConfiguration, Theme } from '../types'
import ImageUploadField from './ImageUploadField'
import Swatch from './Swatch'
import TextField from './TextField'

type BrandingFormState = configApi.SiteConfigurationUpdate

function toFormState(config: AdminSiteConfiguration | SiteConfiguration | null): BrandingFormState {
  return {
    businessName: config?.businessName ?? null,
    tagline: config?.tagline ?? null,
    logoUrl: config?.logoUrl ?? null,
    faviconUrl: config?.faviconUrl ?? null,
    contactEmail: config?.contactEmail ?? null,
    contactPhone: config?.contactPhone ?? null,
    instagramUrl: config?.instagramUrl ?? null,
    whatsappNumber: config?.whatsappNumber ?? null,
    facebookUrl: config?.facebookUrl ?? null,
    footerText: config?.footerText ?? null,
    loginBackgroundImageUrl: config?.loginBackgroundImageUrl ?? null,
    loginPromoImageUrl: config?.loginPromoImageUrl ?? null,
    loginPromoText: config?.loginPromoText ?? null,
    registrationImageUrl: config?.registrationImageUrl ?? null,
    // Backend requires a value (1-1440) on save - default to its own 15-minute fallback rather
    // than null, so a first-ever save (or a config fetch that somehow omits it) doesn't fail
    // validation on a field the admin never touched.
    orderReservationTtlMinutes: config?.orderReservationTtlMinutes ?? 15,
    // Backend requires a non-null value on save - default to its own "off" fallback, same
    // reasoning as orderReservationTtlMinutes above.
    reserveStockOnlyAtPayment: config?.reserveStockOnlyAtPayment ?? false,
    // Always present on both the public and admin shapes (see SiteConfiguration type), but a
    // fallback still guards a first-ever save before any config has loaded.
    idleTimeoutMinutes: config?.idleTimeoutMinutes ?? 30,
  }
}

/**
 * Curated copy for a theme's motif (see Theme.java's own doc comment for what each one changes
 * visually) - no backend field for this since it's editorial copy about the *motif*, shared by
 * every theme that picks it, not a per-theme admin-editable field like the other Theme columns.
 */
const MOTIF_INFO: Record<string, { label: string; description: string; badgeClass: string } | undefined> = {
  STUDIO: {
    label: 'Studio',
    description: 'A calm, geometric look: a clean sans display face, no shimmer or hero glow, and a squared-off call-to-action.',
    badgeClass: 'bg-indigo-50 text-indigo-600',
  },
  ELAN: {
    label: 'Élan',
    description:
      'A refined editorial-inspired fashion theme combining warm neutrals, sophisticated typography, subtle contrast, and elegant interactions to create a premium boutique shopping experience.',
    badgeClass: 'bg-amber-50 text-amber-700',
  },
}

/** Icon-only "back to admin home" affordance - no text, matching the other admin screens' back links. */
function AdminHomeBackLink({ className = '' }: { className?: string }) {
  return (
    <Link
      to="/admin"
      aria-label="Back to admin home"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  )
}

export default function AdminConfigurationForm() {
  const { refresh: refreshSiteConfig } = useSiteConfig()

  const [themes, setThemes] = useState<Theme[]>([])
  const [activeThemeId, setActiveThemeId] = useState<number | string | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [activatingId, setActivatingId] = useState<number | string | null>(null)
  const [themeMessage, setThemeMessage] = useState<string | null>(null)
  const [themeError, setThemeError] = useState<string | null>(null)

  const [form, setForm] = useState<BrandingFormState>(toFormState(null))
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)

  const loadAll = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [themeList, config] = await Promise.all([
        configApi.fetchThemes(),
        configApi.fetchAdminConfiguration(),
      ])
      setThemes(themeList)
      setActiveThemeId(configApi.getActiveThemeId(config))
      setForm(toFormState(config))
    } catch (err) {
      setLoadError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadAll()
  }, [loadAll])

  const handleActivate = async (theme: Theme) => {
    setActivatingId(theme.id)
    setThemeMessage(null)
    setThemeError(null)
    try {
      await configApi.activateTheme(theme.id)
      setActiveThemeId(theme.id)
      setThemeMessage(`"${theme.name}" is now the active theme.`)
      await refreshSiteConfig()
    } catch (err) {
      setThemeError(getErrorMessage(err))
    } finally {
      setActivatingId(null)
    }
  }

  const updateField = <K extends keyof BrandingFormState>(key: K, value: BrandingFormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleSave = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setSaveMessage(null)
    setSaveError(null)
    try {
      const updated = await configApi.updateConfiguration(form)
      setForm(toFormState(updated))
      setSaveMessage('Configuration saved.')
      await refreshSiteConfig()
    } catch (err) {
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <div className="flex min-h-[40vh] items-center justify-center text-sm text-zinc-500">Loading configuration…</div>
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Site configuration</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Theme, branding, and login/registration visuals for the storefront.
          </p>
        </div>
      </div>

      {loadError && (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {loadError}
        </div>
      )}

      {/* Theme picker */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold text-zinc-900">Theme</h2>
        <p className="mt-1 text-sm text-zinc-500">Pick the color theme applied across the storefront.</p>

        {themeMessage && (
          <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {themeMessage}
          </div>
        )}
        {themeError && (
          <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {themeError}
          </div>
        )}

        {themes.length === 0 ? (
          <p className="mt-4 text-sm text-zinc-500">No themes available yet.</p>
        ) : (
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {themes.map((theme) => {
              const isActive = String(theme.id) === String(activeThemeId)
              const motifInfo = MOTIF_INFO[theme.motif]
              return (
                <div
                  key={theme.id}
                  className={`rounded-xl border p-4 ${
                    isActive ? 'border-zinc-900 ring-1 ring-zinc-900' : 'border-zinc-200'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-semibold text-zinc-900">{theme.name}</span>
                    <div className="flex items-center gap-1.5">
                      {motifInfo && (
                        <span
                          className={`rounded-full px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide ${motifInfo.badgeClass}`}
                          title={motifInfo.description}
                        >
                          {motifInfo.label}
                        </span>
                      )}
                      {!theme.richAmbient && (
                        <span
                          className="rounded-full bg-zinc-100 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-zinc-500"
                          title="Plain background with no page-wide ambient wash"
                        >
                          Classic
                        </span>
                      )}
                      {isActive && (
                        <span className="rounded-full bg-zinc-900 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-white">
                          Active
                        </span>
                      )}
                    </div>
                  </div>
                  {motifInfo && <p className="mt-1.5 text-xs leading-relaxed text-zinc-500">{motifInfo.description}</p>}
                  <div className="mt-3 flex items-center gap-2">
                    <Swatch color={theme.primaryColor} label="Primary" />
                    <Swatch color={theme.secondaryColor} label="Secondary" />
                    <Swatch color={theme.accentColor} label="Accent" />
                    <Swatch color={theme.backgroundColor} label="Background" />
                    <Swatch color={theme.textColor} label="Text" />
                  </div>
                  <button
                    type="button"
                    disabled={isActive || activatingId === theme.id}
                    onClick={() => handleActivate(theme)}
                    className="mt-4 w-full rounded-md border border-zinc-300 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:cursor-default disabled:opacity-50"
                  >
                    {isActive ? 'Currently active' : activatingId === theme.id ? 'Activating…' : 'Activate'}
                  </button>
                </div>
              )
            })}
          </div>
        )}
      </section>

      <form onSubmit={handleSave}>
        {/* Branding */}
        <section className="mt-10 border-t border-zinc-200 pt-8">
          <h2 className="text-lg font-semibold text-zinc-900">Branding</h2>
          <p className="mt-1 text-sm text-zinc-500">Business identity shown across the storefront and footer.</p>

          <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2">
            <TextField label="Business name" value={form.businessName} onChange={(v) => updateField('businessName', v)} />
            <TextField label="Tagline" value={form.tagline} onChange={(v) => updateField('tagline', v)} />
            <TextField label="Contact email" type="email" value={form.contactEmail} onChange={(v) => updateField('contactEmail', v)} />
            <TextField label="Contact phone" type="tel" value={form.contactPhone} onChange={(v) => updateField('contactPhone', v)} />
            <TextField label="Instagram URL" value={form.instagramUrl} onChange={(v) => updateField('instagramUrl', v)} />
            <TextField label="WhatsApp number" value={form.whatsappNumber} onChange={(v) => updateField('whatsappNumber', v)} />
            <TextField label="Facebook URL" value={form.facebookUrl} onChange={(v) => updateField('facebookUrl', v)} />
            <TextField label="Footer text" value={form.footerText} onChange={(v) => updateField('footerText', v)} />
          </div>

          <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2">
            <ImageUploadField
              label="Logo"
              value={form.logoUrl}
              onUploaded={(url) => updateField('logoUrl', url)}
              helpText="Shown in the navbar. Falls back to the business name text when unset."
            />
            <ImageUploadField
              label="Favicon"
              value={form.faviconUrl}
              onUploaded={(url) => updateField('faviconUrl', url)}
            />
          </div>
        </section>

        {/* Checkout / inventory */}
        <section className="mt-10 border-t border-zinc-200 pt-8">
          <h2 className="text-lg font-semibold text-zinc-900">Checkout &amp; inventory</h2>
          <p className="mt-1 text-sm text-zinc-500">
            How long a placed-but-unpaid order holds its stock before it's released back and the
            order is automatically cancelled.
          </p>

          <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2">
            <TextField
              label="Order reservation timeout (minutes)"
              type="number"
              value={form.orderReservationTtlMinutes != null ? String(form.orderReservationTtlMinutes) : null}
              onChange={(v) =>
                updateField('orderReservationTtlMinutes', v === null || v.trim() === '' ? null : Number(v))
              }
            />
          </div>

          <label className="mt-4 flex items-start gap-2 text-sm text-zinc-700">
            <input
              type="checkbox"
              checked={form.reserveStockOnlyAtPayment ?? false}
              onChange={(e) => updateField('reserveStockOnlyAtPayment', e.target.checked)}
              className="mt-0.5 h-4 w-4 rounded border-zinc-300"
            />
            <span>
              Lock stock and show the order only after the customer clicks Pay
              <span className="mt-0.5 block text-xs text-zinc-500">
                Off (default): stock is reserved and the order appears in "My Orders" as soon as
                the customer places it. On: nothing is reserved and the order stays hidden until
                the customer actually clicks "Pay" on the payment screen.
              </span>
            </span>
          </label>
        </section>

        {/* Session timeout */}
        <section className="mt-10 border-t border-zinc-200 pt-8">
          <h2 className="text-lg font-semibold text-zinc-900">Session timeout</h2>
          <p className="mt-1 text-sm text-zinc-500">
            Everyone signed in — customers, admins, and staff — is automatically logged out after
            this many minutes of inactivity. Applies immediately, no restart needed.
          </p>

          <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2">
            <TextField
              label="Idle timeout (minutes)"
              type="number"
              value={String(form.idleTimeoutMinutes)}
              // Unlike orderReservationTtlMinutes, this field is never null in the type (see
              // SiteConfiguration.idleTimeoutMinutes) - an emptied input falls back to the
              // default rather than an invalid null value.
              onChange={(v) => updateField('idleTimeoutMinutes', v === null || v.trim() === '' ? 30 : Number(v))}
            />
          </div>
        </section>

        {/* Login/registration visuals */}
        <section className="mt-10 border-t border-zinc-200 pt-8">
          <h2 className="text-lg font-semibold text-zinc-900">Login &amp; registration visuals</h2>
          <p className="mt-1 text-sm text-zinc-500">
            Optional artwork and copy for the login, admin login, and registration pages.
          </p>

          <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2">
            <ImageUploadField
              label="Login background image"
              value={form.loginBackgroundImageUrl}
              onUploaded={(url) => updateField('loginBackgroundImageUrl', url)}
              helpText="Used on the customer and admin login pages."
            />
            <ImageUploadField
              label="Login promo image"
              value={form.loginPromoImageUrl}
              onUploaded={(url) => updateField('loginPromoImageUrl', url)}
              helpText="Shown in a side panel on the login page, alongside the promo text below."
            />
            <ImageUploadField
              label="Registration image"
              value={form.registrationImageUrl}
              onUploaded={(url) => updateField('registrationImageUrl', url)}
            />
            <TextField
              label="Login promo text"
              value={form.loginPromoText}
              onChange={(v) => updateField('loginPromoText', v)}
              textarea
            />
          </div>
        </section>

        {saveMessage && (
          <div className="mt-6 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {saveMessage}
          </div>
        )}
        {saveError && (
          <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {saveError}
          </div>
        )}

        <div className="mt-6 flex justify-center sm:justify-end">
          <button
            type="submit"
            disabled={saving}
            className="w-full rounded-xl bg-zinc-900 px-7 py-3 text-sm font-semibold text-white transition-all duration-150 hover:bg-zinc-800 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
          >
            {saving ? 'Saving…' : 'Save configuration'}
          </button>
        </div>
      </form>

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
