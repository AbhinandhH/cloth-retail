import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../api/client'
import BackButton from '../components/BackButton'
import type { OtpChannel } from '../types'

interface VerifyOtpState {
  registrationId: number | string
  email: string
  mobileNumber: string
  emailVerificationRequired: boolean
  mobileVerificationRequired: boolean
}

const RESEND_COOLDOWN_SECONDS = 30

/** One channel's code entry + resend, rendered by the parent for whichever channel(s) are still outstanding. */
function OtpChannelSection({
  channel,
  destination,
  registrationId,
  onVerified,
}: {
  channel: OtpChannel
  destination: string
  registrationId: number | string
  onVerified: (channel: OtpChannel, completed: boolean) => void
}) {
  const { verifyOtp, resendOtp } = useAuth()
  const [code, setCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resending, setResending] = useState(false)
  const [resendMessage, setResendMessage] = useState<string | null>(null)
  const [cooldown, setCooldown] = useState(0)

  useEffect(() => {
    if (cooldown <= 0) return
    const timer = window.setInterval(() => setCooldown((c) => Math.max(0, c - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [cooldown])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const res = await verifyOtp(registrationId, channel, code)
      const stillRequired = channel === 'EMAIL' ? res.emailVerificationRequired : res.mobileVerificationRequired
      onVerified(channel, !stillRequired)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleResend = async () => {
    setResendMessage(null)
    setError(null)
    setResending(true)
    try {
      await resendOtp(registrationId, channel)
      setResendMessage('A new code has been sent.')
      setCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setResending(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3 rounded-xl border border-zinc-200 p-4">
      <div>
        <p className="text-sm font-medium text-zinc-900">
          {channel === 'EMAIL' ? 'Verify your email' : 'Verify your mobile number'}
        </p>
        <p className="mt-0.5 text-xs text-zinc-500">Enter the code sent to {destination}.</p>
      </div>

      {error && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">{error}</div>
      )}
      {resendMessage && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
          {resendMessage}
        </div>
      )}

      <input
        type="text"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={8}
        value={code}
        onChange={(e) => setCode(e.target.value)}
        placeholder="6-digit code"
        className="w-full rounded-xl border border-zinc-200 bg-white px-3.5 py-2.5 text-center text-lg tracking-[0.3em] text-zinc-900 focus:border-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-900/10"
      />

      <div className="flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={handleResend}
          disabled={resending || cooldown > 0}
          className="text-xs font-medium text-zinc-600 hover:text-zinc-900 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {cooldown > 0 ? `Resend code (${cooldown}s)` : resending ? 'Sending…' : 'Resend code'}
        </button>
        <button
          type="submit"
          disabled={submitting || code.trim() === ''}
          className="rounded-xl bg-[var(--brand-primary,#18181b)] px-5 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitting ? 'Verifying…' : 'Verify'}
        </button>
      </div>
    </form>
  )
}

export default function VerifyOtp() {
  const location = useLocation()
  const navigate = useNavigate()
  const state = location.state as VerifyOtpState | null

  const [emailRequired, setEmailRequired] = useState(state?.emailVerificationRequired ?? false)
  const [mobileRequired, setMobileRequired] = useState(state?.mobileVerificationRequired ?? false)

  // Reached without the state a fresh registration hands off (e.g. a page refresh, or someone
  // navigating here directly) - there's no registrationId/channel info to verify against, so send them
  // back to sign up again rather than showing a broken form.
  useEffect(() => {
    if (!state) {
      navigate('/register', { replace: true })
    }
  }, [state, navigate])

  if (!state) return null

  // `completed` here means "this one channel is now verified", not "the whole account is
  // activated" - the account actually going live (and AuthContext.verifyOtp logging the user in
  // as a side effect) happens once neither channel is required any more, handled below.
  const handleChannelVerified = (channel: OtpChannel, completed: boolean) => {
    if (channel === 'EMAIL') setEmailRequired(!completed)
    else setMobileRequired(!completed)
  }

  const allDone = !emailRequired && !mobileRequired
  if (allDone) {
    navigate('/', { replace: true })
    return null
  }

  return (
    <div className="relative mx-auto flex min-h-[70dvh] max-w-md flex-col justify-center px-4 py-16 sm:px-6">
      <BackButton className="absolute left-3 top-3 z-10 sm:left-4 sm:top-4" />
      <div className="animate-fade-in-up">
        <h1 className="font-display text-3xl text-zinc-900">Verify your account</h1>
        <p className="mt-2 text-sm text-zinc-500">
          We've sent a verification code to confirm it's really you before your account goes live.
        </p>

        <div className="mt-8 space-y-4">
          {emailRequired && (
            <OtpChannelSection
              channel="EMAIL"
              destination={state.email}
              registrationId={state.registrationId}
              onVerified={handleChannelVerified}
            />
          )}
          {mobileRequired && (
            <OtpChannelSection
              channel="MOBILE"
              destination={state.mobileNumber}
              registrationId={state.registrationId}
              onVerified={handleChannelVerified}
            />
          )}
        </div>
      </div>
    </div>
  )
}
