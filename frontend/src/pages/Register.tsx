import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { getErrorMessage, getFieldErrors, toMediaUrl } from '../api/client'

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
      await register({ fullName, email, mobileNumber, password })
      navigate('/', { replace: true })
    } catch (err) {
      setFormError(getErrorMessage(err))
      setFieldErrors(getFieldErrors(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={hasPromo ? 'flex min-h-[70vh] flex-col md:flex-row' : 'min-h-[70vh]'}>
      {hasPromo && (
        <div className="hidden items-center justify-center bg-zinc-900 p-10 md:flex md:w-1/2">
          <img src={promoUrl!} alt="" className="max-h-[60vh] w-full max-w-sm rounded-lg object-cover" />
        </div>
      )}
      <div className={hasPromo ? 'flex flex-1 flex-col justify-center px-4 py-12 sm:px-6 md:w-1/2' : 'mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4 py-12 sm:px-6'}>
        <div className={hasPromo ? 'mx-auto w-full max-w-md' : ''}>
      <h1 className="text-2xl font-semibold text-zinc-900">Create an account</h1>
      <p className="mt-1 text-sm text-zinc-500">Join us to start shopping.</p>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
        {formError && (
          <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
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
          label="Mobile number"
          type="tel"
          autoComplete="tel"
          value={mobileNumber}
          onChange={setMobileNumber}
          error={fieldErrors.mobileNumber}
          required
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
          <label htmlFor="confirmPassword" className="block text-sm font-medium text-zinc-900">
            Confirm password
          </label>
          <input
            id="confirmPassword"
            type="password"
            required
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
          {passwordsMismatch && <p className="mt-1 text-xs text-rose-600">Passwords do not match.</p>}
        </div>

        <button
          type="submit"
          disabled={submitting || passwordsMismatch}
          className="w-full rounded-lg bg-[var(--brand-primary,#18181b)] py-2.5 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
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

function Field({
  id,
  label,
  type,
  autoComplete,
  value,
  onChange,
  error,
  required,
}: {
  id: string
  label: string
  type: string
  autoComplete?: string
  value: string
  onChange: (value: string) => void
  error?: string
  required?: boolean
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-zinc-900">
        {label}
      </label>
      <input
        id={id}
        type={type}
        required={required}
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
      />
      {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
    </div>
  )
}
