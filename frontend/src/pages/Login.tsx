import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { getErrorMessage, getFieldErrors, toMediaUrl } from '../api/client'
import BackButton from '../components/BackButton'

export default function Login() {
  const { login } = useAuth()
  const { config } = useSiteConfig()
  const navigate = useNavigate()
  const location = useLocation()

  const backgroundUrl = toMediaUrl(config?.loginBackgroundImageUrl)
  const promoUrl = toMediaUrl(config?.loginPromoImageUrl)
  const promoText = config?.loginPromoText ?? null
  const hasPromo = Boolean(promoUrl || promoText)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch (err) {
      setFormError(getErrorMessage(err))
      setFieldErrors(getFieldErrors(err))
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass =
    'mt-1.5 w-full rounded-xl border border-zinc-200 bg-white px-3.5 py-2.5 text-sm text-zinc-900 transition-colors focus:border-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-900/10'

  return (
    <div
      className={hasPromo ? 'relative flex min-h-[70dvh] flex-col md:flex-row' : 'relative min-h-[70dvh]'}
      style={backgroundUrl ? { backgroundImage: `url(${backgroundUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : undefined}
    >
      <BackButton className="absolute left-3 top-3 z-10 sm:left-4 sm:top-4" />

      {hasPromo && (
        <div className="hidden flex-col items-center justify-center gap-6 bg-zinc-900 p-10 text-center text-white md:flex md:w-1/2">
          {promoUrl && (
            <img src={promoUrl} alt="" className="max-h-[50vh] w-full max-w-sm rounded-2xl object-cover shadow-elevated" />
          )}
          {promoText && <p className="max-w-sm font-display text-xl leading-relaxed text-white/90">{promoText}</p>}
        </div>
      )}

      <div
        className={
          hasPromo
            ? 'flex flex-1 flex-col justify-center px-4 py-16 sm:px-6 md:w-1/2'
            : 'mx-auto flex min-h-[70dvh] max-w-md flex-col justify-center px-4 py-16 sm:px-6'
        }
      >
        <div className={`animate-fade-in-up ${hasPromo ? 'mx-auto w-full max-w-md' : ''}`}>
          <h1 className="font-display text-3xl text-zinc-900">Welcome back</h1>
          <p className="mt-2 text-sm text-zinc-500">Log in to continue to your account.</p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
            {formError && (
              <div className="rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700">
                {formError}
              </div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-zinc-700">Email</label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClass}
              />
              {fieldErrors.email && <p className="mt-1.5 text-xs text-rose-600">{fieldErrors.email}</p>}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-zinc-700">Password</label>
              <input
                id="password"
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClass}
              />
              {fieldErrors.password && <p className="mt-1.5 text-xs text-rose-600">{fieldErrors.password}</p>}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-xl bg-[var(--brand-primary,#18181b)] py-3 text-sm font-semibold text-white shadow-soft ring-1 ring-inset ring-[var(--brand-secondary,#18181b)]/20 transition-opacity hover:opacity-90 disabled:opacity-60"
            >
              {submitting ? 'Logging in…' : 'Log in'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-zinc-500">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="font-medium text-zinc-900 hover:underline">
              Sign up
            </Link>
          </p>
          <p className="mt-2 text-center text-xs text-zinc-400">
            <Link to="/admin/login" className="hover:underline">Admin portal</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
