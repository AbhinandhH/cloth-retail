import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { getErrorMessage, getFieldErrors, toMediaUrl } from '../api/client'

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

  return (
    <div
      className={hasPromo ? 'flex min-h-[70vh] flex-col md:flex-row' : 'min-h-[70vh]'}
      style={backgroundUrl ? { backgroundImage: `url(${backgroundUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : undefined}
    >
      {hasPromo && (
        <div className="hidden flex-col items-center justify-center gap-4 bg-zinc-900 p-10 text-center text-white md:flex md:w-1/2">
          {promoUrl && <img src={promoUrl} alt="" className="max-h-[50vh] w-full max-w-sm rounded-lg object-cover" />}
          {promoText && <p className="max-w-sm text-lg font-medium">{promoText}</p>}
        </div>
      )}
      <div className={hasPromo ? 'flex flex-1 flex-col justify-center px-4 py-12 sm:px-6 md:w-1/2' : 'mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4 py-12 sm:px-6'}>
        <div className={hasPromo ? 'mx-auto w-full max-w-md' : ''}>
          <h1 className="text-2xl font-semibold text-zinc-900">Log in</h1>
          <p className="mt-1 text-sm text-zinc-500">Welcome back. Enter your details below.</p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
            {formError && (
              <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                {formError}
              </div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-zinc-900">Email</label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              />
              {fieldErrors.email && <p className="mt-1 text-xs text-rose-600">{fieldErrors.email}</p>}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-zinc-900">Password</label>
              <input
                id="password"
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
              />
              {fieldErrors.password && <p className="mt-1 text-xs text-rose-600">{fieldErrors.password}</p>}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-lg bg-zinc-900 py-2.5 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-60"
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
