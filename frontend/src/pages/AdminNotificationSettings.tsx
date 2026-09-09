import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as adminNotificationsApi from '../api/adminNotifications'
import { getErrorMessage } from '../api/client'
import Switch from '../components/Switch'
import type { NotificationSettings } from '../types'

/** Icon-only "back to admin home" affordance, matching the other admin screens' back links. */
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

function NotificationSettingsForm() {
  const [settings, setSettings] = useState<NotificationSettings | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)

  useEffect(() => {
    adminNotificationsApi
      .fetchNotificationSettings()
      .then(setSettings)
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  const toggle = async (patch: Partial<NotificationSettings>) => {
    if (!settings) return
    const next = { ...settings, ...patch }
    setSettings(next)
    setSaveError(null)
    setSaveMessage(null)
    setSaving(true)
    try {
      const saved = await adminNotificationsApi.updateNotificationSettings(next)
      setSettings(saved)
      setSaveMessage('Saved.')
    } catch (err) {
      setSettings(settings)
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <div className="mt-6 text-sm text-zinc-500">Loading…</div>
  }

  if (loadError || !settings) {
    return (
      <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
        {loadError ?? 'Failed to load notification settings.'}
      </div>
    )
  }

  return (
    <div className="mt-6 space-y-4">
      {saveError && (
        <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{saveError}</div>
      )}
      {saveMessage && !saveError && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {saveMessage}
        </div>
      )}

      <div className="flex items-center justify-between gap-4 rounded-xl border border-zinc-200 p-4">
        <div>
          <p className="text-sm font-medium text-zinc-900">Email verification</p>
          <p className="mt-0.5 text-xs text-zinc-500">
            When on, a new customer must confirm a code emailed to them before their account is usable. Requires
            SMTP credentials to be configured (spring.mail.* in application.yml / env vars) - turning this on
            without them will make signup fail.
          </p>
        </div>
        <Switch
          label="Email verification"
          checked={settings.emailVerificationEnabled}
          disabled={saving}
          onChange={(checked) => toggle({ emailVerificationEnabled: checked })}
        />
      </div>

      <div className="flex items-center justify-between gap-4 rounded-xl border border-zinc-200 p-4">
        <div>
          <p className="text-sm font-medium text-zinc-900">Mobile verification</p>
          <p className="mt-0.5 text-xs text-zinc-500">
            When on, a customer who gives a mobile number at signup must confirm a code sent to it. Mobile number
            stays optional either way - a customer who leaves it blank is never asked to verify one.
          </p>
        </div>
        <Switch
          label="Mobile verification"
          checked={settings.mobileVerificationEnabled}
          disabled={saving}
          onChange={(checked) => toggle({ mobileVerificationEnabled: checked })}
        />
      </div>
    </div>
  )
}

export default function AdminNotificationSettings() {
  const { user } = useAuth()
  const isSuperAdmin = Boolean(user?.roles?.includes('SUPER_ADMIN'))

  if (!isSuperAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Super Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the SUPER_ADMIN role to manage notification settings.</p>
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

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Notifications</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Turn the email/mobile OTP verification services on or off for new customer signups. Changes apply
            immediately, no restart needed.
          </p>
        </div>
      </div>

      <NotificationSettingsForm />
    </div>
  )
}
