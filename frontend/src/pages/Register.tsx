import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { getErrorMessage, getFieldErrors, toMediaUrl } from '../api/client'
import Field from '../components/Field'
import BackButton from '../components/BackButton'

export default function Register() {
  const { register } = useAuth()
  const { config } = useSiteConfig()
  const navigate = useNavigate()

  const promoUrl = toMediaUrl(config?.registrationImageUrl)
  const hasPromo = Boolean(promoUrl)

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [mobileNumber, setMobileNumber] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const passwordsMismatch = confirmPassword.length > 0 && password !== confirmPassword

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFormError(null)
    setFieldErrors({})

    if (password !== confirmPassword) {
      setFormError('Passwords do not match.')
      return
    }

    setSubmitting(true)
    try {
      const res = await register({ fullName, email, mobileNumber, password })
      if (res.completed) {
        navigate('/', { replace: true })
      } else {
        navigate('/verify', {
          replace: true,
          state: {
            registrationId: res.registrationId,
            email,
            mobileNumber,
            emailVerificationRequired: res.emailVerificationRequired,
            mobileVerificationRequired: res.mobileVerificationRequired,
          },
        })
      }
    } catch (err) {
      setFormError(getErrorMessage(err))
      setFieldErrors(getFieldErrors(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={hasPromo ? 'relative flex min-h-[70dvh] flex-col md:flex-row' : 'relative min-h-[70dvh]'}>
      <BackButton className="absolute left-3 top-3 z-10 sm:left-4 sm:top-4" />

      {hasPromo && (
        <div className="hidden items-center justify-center bg-zinc-900 p-10 md:flex md:w-1/2">
          <img src={promoUrl!} alt="" className="max-h-[60vh] w-full max-w-sm rounded-2xl object-cover shadow-elevated" />
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
          <h1 className="font-display text-3xl text-zinc-900">Create an account</h1>
          <p className="mt-2 text-sm text-zinc-500">Join us to start shopping.</p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
            {formError && (
              <div className="rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700">
                {formError}
              </div>
            )}

            <Field
              id="fullName"
              label="Full name"
              type="text"
              autoComplete="name"
              value={fullName}
              onChange={setFullName}
              error={fieldErrors.fullName}
              required
            />
            <Field
              id="email"
              label="Email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={setEmail}
              error={fieldErrors.email}
              required
            />
            <Field
              id="mobileNumber"
              label="Mobile number (optional)"
              type="tel"
              autoComplete="tel"
              value={mobileNumber}
              onChange={setMobileNumber}
              error={fieldErrors.mobileNumber}
            />
            <Field
              id="password"
              label="Password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={setPassword}
              error={fieldErrors.password}
              required
            />
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-zinc-700">
                Confirm password
              </label>
              <input
                id="confirmPassword"
                type="password"
                required
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="mt-1.5 w-full rounded-xl border border-zinc-200 bg-white px-3.5 py-2.5 text-sm text-zinc-900 transition-colors focus:border-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-900/10"
              />
              {passwordsMismatch && <p className="mt-1.5 text-xs text-rose-600">Passwords do not match.</p>}
            </div>

            <button
              type="submit"
              disabled={submitting || passwordsMismatch}
              className="w-full rounded-xl bg-[var(--brand-primary,#18181b)] py-3 text-sm font-semibold text-white shadow-soft ring-1 ring-inset ring-[var(--brand-secondary,#18181b)]/20 transition-opacity hover:opacity-90 disabled:opacity-60"
            >
              {submitting ? 'Creating account…' : 'Sign up'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-zinc-500">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-zinc-900 hover:underline">
              Log in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
