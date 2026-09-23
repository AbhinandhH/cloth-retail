import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as subscriptionApi from '../api/adminSubscription'
import { getErrorMessage } from '../api/client'
import { loadRazorpayCheckout, openRazorpayCheckout } from '../lib/razorpay'
import { formatPrice } from '../lib/formatPrice'
import TextField from '../components/TextField'
import type { SubscriptionPaymentRow, SubscriptionStatusResponse } from '../types'

/** Icon-only "back to admin home" affordance - matches the other governance screens (Tax, etc). */
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

function StatusPill({ status }: { status: SubscriptionStatusResponse }) {
  if (status.locked) {
    return (
      <span className="inline-flex items-center rounded-full bg-rose-100 px-2.5 py-1 text-xs font-medium text-rose-700">
        Locked - site unavailable
      </span>
    )
  }
  if (status.paid) {
    return (
      <span className="inline-flex items-center rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-medium text-emerald-700">
        Paid
      </span>
    )
  }
  if (status.dueDate) {
    return (
      <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-700">
        Unpaid
      </span>
    )
  }
  return (
    <span className="inline-flex items-center rounded-full bg-zinc-100 px-2.5 py-1 text-xs font-medium text-zinc-600">
      Not configured
    </span>
  )
}

function PaymentStatusBadge({ status }: { status: SubscriptionPaymentRow['status'] }) {
  const styles: Record<SubscriptionPaymentRow['status'], string> = {
    SUCCESS: 'bg-emerald-100 text-emerald-700',
    PENDING: 'bg-amber-100 text-amber-700',
    FAILED: 'bg-rose-100 text-rose-700',
  }
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${styles[status]}`}>{status}</span>
}

export default function AdminSubscriptionBilling() {
  // Governance, store-owner tier - EMPLOYEE never sees this. SUPER_ADMIN additionally gets the
  // settings form and manual override below (see isSuperAdmin branches).
  const { isStoreAdmin, isSuperAdmin } = useAuth()

  const [status, setStatus] = useState<SubscriptionStatusResponse | null>(null)
  const [payments, setPayments] = useState<SubscriptionPaymentRow[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [monthlyAmount, setMonthlyAmount] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [savingSettings, setSavingSettings] = useState(false)
  const [settingsError, setSettingsError] = useState<string | null>(null)
  const [settingsMessage, setSettingsMessage] = useState<string | null>(null)

  const [markingPaid, setMarkingPaid] = useState(false)
  const [markPaidError, setMarkPaidError] = useState<string | null>(null)

  const [paying, setPaying] = useState(false)
  const [payError, setPayError] = useState<string | null>(null)
  const [payMessage, setPayMessage] = useState<string | null>(null)

  const loadAll = useCallback(async () => {
    if (!isStoreAdmin) return
    setLoading(true)
    setLoadError(null)
    try {
      const [statusData, paymentsData] = await Promise.all([
        subscriptionApi.fetchSubscriptionStatus(),
        subscriptionApi.fetchSubscriptionPayments(),
      ])
      setStatus(statusData)
      setPayments(paymentsData)
      setMonthlyAmount(statusData.monthlyAmount != null ? String(statusData.monthlyAmount) : '')
      setDueDate(statusData.dueDate ?? '')
    } catch (err) {
      setLoadError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [isStoreAdmin])

  useEffect(() => {
    loadAll()
  }, [loadAll])

  if (!isStoreAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view subscription billing.</p>
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

  const handleSaveSettings = async (e: FormEvent) => {
    e.preventDefault()
    setSettingsError(null)
    setSettingsMessage(null)
    const amount = Number(monthlyAmount)
    if (!monthlyAmount || Number.isNaN(amount) || amount <= 0) {
      setSettingsError('Enter a monthly amount greater than 0.')
      return
    }
    if (!dueDate) {
      setSettingsError('Choose a due date.')
      return
    }
    setSavingSettings(true)
    try {
      const saved = await subscriptionApi.updateSubscriptionSettings(amount, dueDate)
      setStatus(saved)
      setSettingsMessage('Billing settings saved. This cycle is now marked unpaid.')
    } catch (err) {
      setSettingsError(getErrorMessage(err))
    } finally {
      setSavingSettings(false)
    }
  }

  const handleMarkPaid = async () => {
    setMarkPaidError(null)
    setMarkingPaid(true)
    try {
      const saved = await subscriptionApi.markSubscriptionPaid()
      setStatus(saved)
      loadAll()
    } catch (err) {
      setMarkPaidError(getErrorMessage(err))
    } finally {
      setMarkingPaid(false)
    }
  }

  const handlePayNow = async () => {
    setPayError(null)
    setPayMessage(null)
    setPaying(true)
    try {
      const initiation = await subscriptionApi.initiateSubscriptionPayment()
      if (!initiation.keyId) {
        setPayMessage(
          `Payment initiated (reference: ${initiation.gatewayOrderId}). Online checkout isn't configured yet - contact the site owner to confirm payment another way.`,
        )
        return
      }
      await loadRazorpayCheckout()
      openRazorpayCheckout({
        key: initiation.keyId,
        amount: Math.round(initiation.amount * 100),
        currency: 'INR',
        name: 'Subscription billing',
        order_id: initiation.gatewayOrderId,
        handler: () => {
          setPayMessage('Payment submitted. It will reflect here once confirmed - refresh in a moment.')
          loadAll()
        },
      })
    } catch (err) {
      setPayError(getErrorMessage(err))
    } finally {
      setPaying(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Subscription billing</h1>
          <p className="mt-1 text-sm text-zinc-500">
            {isSuperAdmin
              ? "Set the monthly amount and due date owed by this store's owner. If it goes unpaid past the due date, the site becomes inaccessible to everyone except you."
              : 'Your subscription with the software owner. If this goes unpaid past the due date, the site becomes inaccessible until it is settled.'}
          </p>
        </div>
      </div>

      {loading ? (
        <div className="mt-8 flex min-h-[20vh] items-center justify-center text-sm text-zinc-500">Loading…</div>
      ) : loadError ? (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{loadError}</div>
      ) : status ? (
        <>
          <div className="mt-8 rounded-xl border border-zinc-200 p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-zinc-900">Current cycle</p>
              <StatusPill status={status} />
            </div>
            <dl className="mt-3 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-xs text-zinc-500">Monthly amount</dt>
                <dd className="mt-0.5 font-medium text-zinc-900">
                  {status.monthlyAmount != null ? formatPrice(status.monthlyAmount) : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-zinc-500">Due date</dt>
                <dd className="mt-0.5 font-medium text-zinc-900">{status.dueDate ?? '—'}</dd>
              </div>
              {status.dueDate && !status.paid && (
                <div className="col-span-2">
                  <dt className="text-xs text-zinc-500">Days until due</dt>
                  <dd className="mt-0.5 font-medium text-zinc-900">
                    {status.daysUntilDue != null && status.daysUntilDue < 0
                      ? `${Math.abs(status.daysUntilDue)} day(s) overdue`
                      : `${status.daysUntilDue ?? '—'} day(s)`}
                  </dd>
                </div>
              )}
            </dl>

            {!isSuperAdmin && !status.paid && status.monthlyAmount != null && (
              <div className="mt-4 border-t border-zinc-100 pt-4">
                {payError && (
                  <div className="mb-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{payError}</div>
                )}
                {payMessage && (
                  <div className="mb-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                    {payMessage}
                  </div>
                )}
                <button
                  type="button"
                  onClick={handlePayNow}
                  disabled={paying}
                  className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {paying ? 'Starting payment…' : `Pay ${formatPrice(status.monthlyAmount)} now`}
                </button>
              </div>
            )}
          </div>

          {isSuperAdmin && (
            <div className="mt-6 rounded-xl border border-zinc-200 p-4">
              <p className="text-sm font-medium text-zinc-900">Update billing settings</p>
              <p className="mt-0.5 text-xs text-zinc-500">
                Saving resets this cycle to unpaid - the store owner will need to pay again, or you can mark it paid
                manually below.
              </p>

              {settingsError && (
                <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                  {settingsError}
                </div>
              )}
              {settingsMessage && !settingsError && (
                <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                  {settingsMessage}
                </div>
              )}

              <form onSubmit={handleSaveSettings} className="mt-4 space-y-4">
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <TextField label="Monthly amount (₹)" type="number" value={monthlyAmount} onChange={(v) => setMonthlyAmount(v ?? '')} />
                  <TextField label="Due date" type="date" value={dueDate} onChange={(v) => setDueDate(v ?? '')} />
                </div>
                <button
                  type="submit"
                  disabled={savingSettings}
                  className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {savingSettings ? 'Saving…' : 'Save billing settings'}
                </button>
              </form>

              <div className="mt-4 border-t border-zinc-100 pt-4">
                {markPaidError && (
                  <div className="mb-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                    {markPaidError}
                  </div>
                )}
                <button
                  type="button"
                  onClick={handleMarkPaid}
                  disabled={markingPaid || status.paid}
                  className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {markingPaid ? 'Marking as paid…' : status.paid ? 'Already paid' : 'Mark as paid manually'}
                </button>
                <p className="mt-2 text-xs text-zinc-500">
                  Use this if the store owner paid another way (bank transfer, etc.), or to unlock the site
                  immediately without waiting on the payment gateway.
                </p>
              </div>
            </div>
          )}

          <div className="mt-6 rounded-xl border border-zinc-200 p-4">
            <p className="text-sm font-medium text-zinc-900">Payment history</p>
            {payments.length === 0 ? (
              <p className="mt-2 text-sm text-zinc-500">No payment attempts yet.</p>
            ) : (
              <ul className="mt-3 divide-y divide-zinc-100">
                {payments.map((p) => (
                  <li key={p.id} className="flex items-center justify-between py-2 text-sm">
                    <div>
                      <p className="font-medium text-zinc-900">{formatPrice(p.amount)}</p>
                      <p className="text-xs text-zinc-500">{new Date(p.createdAt).toLocaleString()}</p>
                    </div>
                    <PaymentStatusBadge status={p.status} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      ) : null}

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
