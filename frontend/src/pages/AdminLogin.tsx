import { useState } from 'react'
import type { FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { getErrorMessage, getFieldErrors, toMediaUrl } from '../api/client'

export default function AdminLogin() {
  const { loginAsAdmin } = useAuth()
  const { config } = useSiteConfig()
  const navigate = useNavigate()
  const location = useLocation()

  const backgroundUrl = toMediaUrl(config?.loginBackgroundImageUrl)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // Pre-filled from IdleSessionWatcher's redirect state when this page was reached via an
  // idle-triggered auto-logout, rather than the user navigating here directly.
  const [formError, setFormError] = useState<string | null>(
    (location.state as { idleLogout?: boolean } | null)?.idleLogout
      ? "You've been logged out due to inactivity. Please sign in again."
      : null,
  )
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})
    try {
      await loginAsAdmin(email, password)
      navigate('/admin', { replace: true })
    } catch (err) {
      setFormError(getErrorMessage(err))
      setFieldErrors(getFieldErrors(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4"
      style={backgroundUrl ? { backgroundImage: `url(${backgroundUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : undefined}
    >
      <div className="w-full max-w-sm rounded-xl border border-zinc-800 bg-zinc-900 p-8 shadow-xl">
        <div className="mb-6 text-center">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--brand-primary,#e11d48)] text-lg font-bold text-white ring-2 ring-offset-2 ring-offset-zinc-900 ring-[var(--brand-secondary,#e11d48)]">
            A
          </span>
          <h1 className="mt-4 text-xl font-semibold text-white">Admin Portal</h1>
          <p className="mt-1 text-sm text-zinc-400">Restricted access. Staff sign-in only.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          {formError && (
            <div className="rounded-md border border-rose-900 bg-rose-950 px-3 py-2 text-sm text-rose-300">
              {formError}
            </div>
          )}

          <div>
            <label htmlFor="admin-email" className="block text-sm font-medium text-zinc-300">
              Email
            </label>
            <input
              id="admin-email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
            />
            {fieldErrors.email && <p className="mt-1 text-xs text-rose-400">{fieldErrors.email}</p>}
          </div>

          <div>
            <label htmlFor="admin-password" className="block text-sm font-medium text-zinc-300">
              Password
            </label>
            <input
              id="admin-password"
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-white focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
            />
            {fieldErrors.password && <p className="mt-1 text-xs text-rose-400">{fieldErrors.password}</p>}
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-[var(--brand-primary,#e11d48)] py-2.5 text-sm font-semibold text-white ring-2 ring-offset-2 ring-offset-zinc-900 ring-[var(--brand-secondary,#e11d48)] hover:opacity-90 disabled:opacity-60"
          >
            {submitting ? 'Signing in…' : 'Sign in to Admin Portal'}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-zinc-500">
          Not staff? <a href="/" className="hover:underline">Return to storefront</a>
        </p>
      </div>
    </div>
  )
}
