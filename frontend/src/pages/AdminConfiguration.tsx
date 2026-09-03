import { useCallback, useEffect, useId, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import * as configApi from '../api/configuration'
import { getErrorMessage } from '../api/client'
import type { AdminSiteConfiguration, SiteConfiguration, Theme } from '../types'
import ImageUploadField from '../components/ImageUploadField'

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
  }
}

export default function AdminConfiguration() {
  const { user } = useAuth()
  const isSuperAdmin = Boolean(user?.roles?.includes('SUPER_ADMIN'))

  if (!isSuperAdmin) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Super Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">
            You need the SUPER_ADMIN role to view and edit site configuration.
          </p>
          <Link
            to="/admin"
            className="mt-6 inline-block rounded-lg border border-zinc-700 px-4 py-2 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
          >
            Back to admin home
          </Link>
        </div>
      </div>
    )
  }

  return <AdminConfigurationForm />
}

function AdminConfigurationForm() {
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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Site configuration</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Theme, branding, and login/registration visuals for the storefront.
          </p>
        </div>
        <Link to="/admin" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
          &larr; Admin home
        </Link>
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
              return (
                <div
                  key={theme.id}
                  className={`rounded-xl border p-4 ${
                    isActive ? 'border-zinc-900 ring-1 ring-zinc-900' : 'border-zinc-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold text-zinc-900">{theme.name}</span>
                    {isActive && (
                      <span className="rounded-full bg-zinc-900 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-white">
                        Active
                      </span>
                    )}
                  </div>
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

        <div className="mt-6 flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-zinc-900 px-6 py-2.5 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-60"
          >
            {saving ? 'Saving…' : 'Save configuration'}
          </button>
        </div>
      </form>
    </div>
  )
}

function Swatch({ color, label }: { color: string | null; label: string }) {
  if (!color) return null
  return (
    <span
      title={`${label}: ${color}`}
      className="h-6 w-6 rounded-full border border-zinc-200"
      style={{ backgroundColor: color }}
    />
  )
}

function TextField({
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
