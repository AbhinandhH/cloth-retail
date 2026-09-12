import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as adminNotificationsApi from '../api/adminNotifications'
import * as adminSmtpApi from '../api/adminSmtp'
import { getErrorMessage } from '../api/client'
import Switch from '../components/Switch'
import TextField from '../components/TextField'
import type { NotificationSettings, SmtpSettings } from '../types'

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

  // Message content is edited locally and only sent on an explicit save (unlike the toggles
  // below, which save immediately on click) - typing into a text field shouldn't fire a request
  // per keystroke.
  const [emailSubject, setEmailSubject] = useState('')
  const [messageTemplate, setMessageTemplate] = useState('')
  const [contentSaving, setContentSaving] = useState(false)
  const [contentError, setContentError] = useState<string | null>(null)
  const [contentMessage, setContentMessage] = useState<string | null>(null)

  useEffect(() => {
    adminNotificationsApi
      .fetchNotificationSettings()
      .then((data) => {
        setSettings(data)
        setEmailSubject(data.emailSubject)
        setMessageTemplate(data.messageTemplate)
      })
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

  const handleSaveContent = async (e: FormEvent) => {
    e.preventDefault()
    if (!settings) return
    setContentError(null)
    setContentMessage(null)
    setContentSaving(true)
    try {
      const saved = await adminNotificationsApi.updateNotificationSettings({
        ...settings,
        emailSubject,
        messageTemplate,
      })
      setSettings(saved)
      setContentMessage('Message content saved.')
    } catch (err) {
      setContentError(getErrorMessage(err))
    } finally {
      setContentSaving(false)
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
    <div className="space-y-4">
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
            When on, a new customer must confirm a code emailed to them before their account is usable. Set up the
            SMTP details below first - turning this on without them will make signup fail.
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

      <div className="rounded-xl border border-zinc-200 p-4">
        <p className="text-sm font-medium text-zinc-900">Message content</p>
        <p className="mt-0.5 text-xs text-zinc-500">
          Shared by both channels (SMS has no subject line). Use{' '}
          <code className="rounded bg-zinc-100 px-1 py-0.5">{'{code}'}</code> where the OTP itself should appear
          and, optionally,{' '}
          <code className="rounded bg-zinc-100 px-1 py-0.5">{'{ttlMinutes}'}</code> for how long it's valid.
        </p>

        {contentError && (
          <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {contentError}
          </div>
        )}
        {contentMessage && !contentError && (
          <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {contentMessage}
          </div>
        )}

        <form onSubmit={handleSaveContent} className="mt-4 space-y-4">
          <TextField label="Email subject" value={emailSubject} onChange={(v) => setEmailSubject(v ?? '')} />
          <TextField
            label="Message"
            textarea
            value={messageTemplate}
            onChange={(v) => setMessageTemplate(v ?? '')}
          />

          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Preview</p>
            <p className="mt-1 rounded-md bg-zinc-50 px-3 py-2 text-sm text-zinc-700">
              {messageTemplate.replace('{code}', '123456').replace('{ttlMinutes}', '10') || '—'}
            </p>
          </div>

          <button
            type="submit"
            disabled={contentSaving}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {contentSaving ? 'Saving…' : 'Save message content'}
          </button>
        </form>
      </div>
    </div>
  )
}

function SmtpSettingsForm() {
  const [settings, setSettings] = useState<SmtpSettings | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [host, setHost] = useState('')
  const [port, setPort] = useState('587')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fromAddress, setFromAddress] = useState('')
  const [useStarttls, setUseStarttls] = useState(true)

  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)

  const [testEmail, setTestEmail] = useState('')
  const [testing, setTesting] = useState(false)
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null)

  useEffect(() => {
    adminSmtpApi
      .fetchSmtpSettings()
      .then((data) => {
        setSettings(data)
        setHost(data.host ?? '')
        setPort(String(data.port))
        setUsername(data.username ?? '')
        setFromAddress(data.fromAddress ?? '')
        setUseStarttls(data.useStarttls)
      })
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSaveError(null)
    setSaveMessage(null)
    setSaving(true)
    try {
      const saved = await adminSmtpApi.updateSmtpSettings({
        host: host.trim() === '' ? null : host.trim(),
        port: Number(port) || 587,
        username: username.trim() === '' ? null : username.trim(),
        password: password.trim() === '' ? undefined : password,
        fromAddress: fromAddress.trim() === '' ? null : fromAddress.trim(),
        useStarttls,
      })
      setSettings(saved)
      // Never keep a submitted password sitting in the field/state longer than needed - the
      // backend never echoes it back either (see SmtpSettings.passwordConfigured).
      setPassword('')
      setSaveMessage('SMTP settings saved.')
    } catch (err) {
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleTest = async () => {
    setTestResult(null)
    setTesting(true)
    try {
      const result = await adminSmtpApi.sendTestEmail(testEmail)
      setTestResult(result)
    } catch (err) {
      setTestResult({ success: false, message: getErrorMessage(err) })
    } finally {
      setTesting(false)
    }
  }

  if (loading) {
    return <div className="text-sm text-zinc-500">Loading…</div>
  }

  if (loadError || !settings) {
    return (
      <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
        {loadError ?? 'Failed to load SMTP settings.'}
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-zinc-200 p-4">
      <p className="text-sm font-medium text-zinc-900">SMTP settings</p>
      <p className="mt-0.5 text-xs text-zinc-500">
        For Gmail: host <code className="rounded bg-zinc-100 px-1 py-0.5">smtp.gmail.com</code>, port 587, and an
        App Password (not your normal Gmail password) from Google Account → Security → 2-Step Verification → App
        passwords.
      </p>

      {saveError && (
        <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{saveError}</div>
      )}
      {saveMessage && !saveError && (
        <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {saveMessage}
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-4 space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField label="Host" value={host} onChange={(v) => setHost(v ?? '')} />
          <TextField label="Port" type="number" value={port} onChange={(v) => setPort(v ?? '587')} />
          <TextField label="Username" value={username} onChange={(v) => setUsername(v ?? '')} />
          <TextField
            label={settings.passwordConfigured ? 'Password (leave blank to keep current)' : 'Password'}
            type="password"
            value={password}
            onChange={(v) => setPassword(v ?? '')}
          />
          <TextField
            label="From address (optional)"
            value={fromAddress}
            onChange={(v) => setFromAddress(v ?? '')}
          />
        </div>

        <label className="flex items-center gap-2 text-sm text-zinc-700">
          <input
            type="checkbox"
            checked={useStarttls}
            onChange={(e) => setUseStarttls(e.target.checked)}
            className="h-4 w-4 rounded border-zinc-300"
          />
          Use STARTTLS
        </label>

        <button
          type="submit"
          disabled={saving}
          className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {saving ? 'Saving…' : 'Save SMTP settings'}
        </button>
      </form>

      <div className="mt-6 border-t border-zinc-100 pt-4">
        <p className="text-sm font-medium text-zinc-900">Send a test email</p>
        <p className="mt-0.5 text-xs text-zinc-500">
          Confirms the settings above actually work, without going through a full signup.
        </p>
        {testResult && (
          <div
            className={`mt-2 rounded-md border px-3 py-2 text-sm ${
              testResult.success
                ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                : 'border-rose-200 bg-rose-50 text-rose-700'
            }`}
          >
            {testResult.message}
          </div>
        )}
        <div className="mt-2 flex flex-col gap-2 sm:flex-row">
          <input
            type="email"
            value={testEmail}
            onChange={(e) => setTestEmail(e.target.value)}
            placeholder="you@example.com"
            className="flex-1 rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
          <button
            type="button"
            onClick={handleTest}
            disabled={testing || testEmail.trim() === ''}
            className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {testing ? 'Sending…' : 'Send test email'}
          </button>
        </div>
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
            Turn the email/mobile OTP verification services on or off for new customer signups, and configure the
            SMTP account emails are sent from. Changes apply immediately, no restart needed.
          </p>
        </div>
      </div>

      <div className="mt-6 space-y-6">
        <NotificationSettingsForm />
        <SmtpSettingsForm />
      </div>
    </div>
  )
}
