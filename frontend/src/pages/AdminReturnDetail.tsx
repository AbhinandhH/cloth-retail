import { useCallback, useEffect, useRef, useState } from 'react'
import type { ChangeEvent, ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  approveDamageClaim,
  approveExchange,
  fetchAdminEvidenceBlob,
  fetchAdminReturnDetail,
  initiateReturnRefund,
  rejectReturnRequest,
  uploadReturnEvidence,
} from '../api/adminReturns'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import ConfirmDialog from '../components/ConfirmDialog'
import TextField from '../components/TextField'
import type { AdminReturnDetail as AdminReturnDetailType, EvidenceStatus, ReturnRequestStatus } from '../types'

const STATUS_LABELS: Record<ReturnRequestStatus, string> = {
  PENDING: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  REFUNDED: 'Refunded',
}

const STATUS_BADGE_CLASSES: Record<ReturnRequestStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-sky-100 text-sky-800',
  REJECTED: 'bg-rose-100 text-rose-800',
  REFUNDED: 'bg-emerald-100 text-emerald-800',
}

function StatusBadge({ status }: { status: ReturnRequestStatus }) {
  return (
    <span className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${STATUS_BADGE_CLASSES[status]}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  )
}

const EVIDENCE_LABELS: Record<EvidenceStatus, string> = {
  NOT_SUBMITTED: 'Video not submitted yet',
  SUBMITTED: 'Video submitted',
}

const EVIDENCE_BADGE_CLASSES: Record<EvidenceStatus, string> = {
  NOT_SUBMITTED: 'border-amber-300 text-amber-700',
  SUBMITTED: 'border-emerald-300 text-emerald-700',
}

function EvidenceBadge({ status }: { status: EvidenceStatus }) {
  return (
    <span className={`inline-flex items-center rounded-md border px-2.5 py-1 text-xs font-semibold ${EVIDENCE_BADGE_CLASSES[status]}`}>
      {EVIDENCE_LABELS[status] ?? status}
    </span>
  )
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function SectionCard({ title, right, children }: { title: string; right?: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-zinc-200 bg-white">
      <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
        <h2 className="text-sm font-semibold text-zinc-900">{title}</h2>
        {right}
      </div>
      <div className="px-4 py-4">{children}</div>
    </section>
  )
}

/** Icon-only "back to returns" affordance, matching AdminOrderDetail's own back link. */
function ReturnsBackLink({ className = '' }: { className?: string }) {
  return (
    <Link
      to="/admin/returns"
      aria-label="Back to returns"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  )
}

export default function AdminReturnDetail() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view this request.</p>
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

  return <AdminReturnDetailContent />
}

function AdminReturnDetailContent() {
  const { id } = useParams<{ id: string }>()

  const [request, setRequest] = useState<AdminReturnDetailType | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const load = useCallback(() => {
    if (!id) return
    setLoading(true)
    setLoadError(null)
    fetchAdminReturnDetail(id)
      .then(setRequest)
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  // --- Evidence video preview (authenticated - can't use a plain <video src>) ---
  const [evidenceUrl, setEvidenceUrl] = useState<string | null>(null)
  const [evidenceLoadError, setEvidenceLoadError] = useState<string | null>(null)
  const evidenceUrlRef = useRef<string | null>(null)

  useEffect(() => {
    evidenceUrlRef.current = null
    setEvidenceUrl(null)
    setEvidenceLoadError(null)
    if (!id || !request?.evidenceVideoAvailable) return
    let cancelled = false
    fetchAdminEvidenceBlob(id)
      .then((blob) => {
        if (cancelled) return
        const url = URL.createObjectURL(blob)
        evidenceUrlRef.current = url
        setEvidenceUrl(url)
      })
      .catch((err) => {
        if (!cancelled) setEvidenceLoadError(getErrorMessage(err))
      })
    return () => {
      cancelled = true
      if (evidenceUrlRef.current) {
        URL.revokeObjectURL(evidenceUrlRef.current)
        evidenceUrlRef.current = null
      }
    }
  }, [id, request?.evidenceVideoAvailable])

  // --- Evidence upload ---
  const [uploadSubmitting, setUploadSubmitting] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  const handleFileSelected = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file || !id) return
    setUploadSubmitting(true)
    setUploadError(null)
    uploadReturnEvidence(id, file)
      .then((updated) => setRequest(updated))
      .catch((err) => setUploadError(getErrorMessage(err)))
      .finally(() => setUploadSubmitting(false))
  }

  // --- Decision (approve exchange / approve damage / reject) ---
  const [decisionNote, setDecisionNote] = useState('')
  const [pendingAction, setPendingAction] = useState<'approve-exchange' | 'approve-damage' | 'reject' | null>(null)
  const [decisionSubmitting, setDecisionSubmitting] = useState(false)
  const [decisionError, setDecisionError] = useState<string | null>(null)

  const submitDecision = () => {
    if (!id || !pendingAction) return
    setDecisionSubmitting(true)
    setDecisionError(null)
    const note = decisionNote.trim() || undefined
    const call =
      pendingAction === 'approve-exchange'
        ? approveExchange(id, note)
        : pendingAction === 'approve-damage'
          ? approveDamageClaim(id, note)
          : rejectReturnRequest(id, note)
    call
      .then((updated) => {
        setRequest(updated)
        setPendingAction(null)
        setDecisionNote('')
      })
      .catch((err) => setDecisionError(getErrorMessage(err)))
      .finally(() => setDecisionSubmitting(false))
  }

  // --- Refund ---
  const [refundAmount, setRefundAmount] = useState('')
  const [refundReason, setRefundReason] = useState<string | null>(null)
  const [refundConfirmOpen, setRefundConfirmOpen] = useState(false)
  const [refundSubmitting, setRefundSubmitting] = useState(false)
  const [refundError, setRefundError] = useState<string | null>(null)

  const submitRefund = () => {
    if (!id) return
    const amount = Number(refundAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      setRefundError('Enter a valid refund amount.')
      return
    }
    setRefundSubmitting(true)
    setRefundError(null)
    initiateReturnRefund(id, { amount, reason: refundReason || null })
      .then((updated) => {
        setRequest(updated)
        setRefundConfirmOpen(false)
      })
      .catch((err) => setRefundError(getErrorMessage(err)))
      .finally(() => setRefundSubmitting(false))
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="space-y-3">
          <div className="h-8 w-1/2 animate-pulse rounded bg-zinc-100" />
          <div className="h-40 animate-pulse rounded-lg bg-zinc-100" />
          <div className="h-40 animate-pulse rounded-lg bg-zinc-100" />
        </div>
      </div>
    )
  }

  if (loadError || !request) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {loadError ?? 'Return request not found.'}
        </div>
        <Link to="/admin/returns" className="mt-4 inline-block text-sm font-medium text-zinc-900 underline">
          &larr; Back to returns
        </Link>
      </div>
    )
  }

  const isExchange = request.requestType === 'SIZE_EXCHANGE'
  const isDamage = request.requestType === 'DAMAGED_PRODUCT'
  const canDecide = request.status === 'PENDING'
  const canRefund = isDamage && request.status === 'APPROVED'

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-2">
          <ReturnsBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">
              {isExchange ? 'Size exchange' : 'Damaged product'} request #{request.id}
            </h1>
            <p className="mt-1 text-sm text-zinc-500">
              Order{' '}
              <Link to={`/admin/orders/${request.orderId}`} className="font-medium text-zinc-700 underline">
                {request.orderNumber}
              </Link>{' '}
              &middot; Created {formatDateTime(request.createdAt)}
            </p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-2">
          <StatusBadge status={request.status} />
          {isDamage && <EvidenceBadge status={request.evidenceStatus} />}
        </div>
      </div>

      <div className="mt-6 space-y-6">
        {/* Request details */}
        <SectionCard title="Request">
          <dl className="grid grid-cols-1 gap-x-4 gap-y-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-xs text-zinc-500">Customer</dt>
              <dd className="mt-0.5 text-zinc-900">{request.customerName}</dd>
              <dd className="text-zinc-600">{request.customerPhone ?? '—'}</dd>
              <dd className="text-zinc-600">{request.customerEmail}</dd>
            </div>
            <div>
              <dt className="text-xs text-zinc-500">Product</dt>
              <dd className="mt-0.5 text-zinc-900">{request.productName}</dd>
              <dd className="text-zinc-600">{request.sku}</dd>
            </div>
            <div>
              <dt className="text-xs text-zinc-500">Original size</dt>
              <dd className="mt-0.5 text-zinc-900">{request.originalSizeName}</dd>
            </div>
            {isExchange && (
              <div>
                <dt className="text-xs text-zinc-500">Requested size</dt>
                <dd className="mt-0.5 text-zinc-900">{request.requestedSizeName ?? '—'}</dd>
              </div>
            )}
            <div className="sm:col-span-2">
              <dt className="text-xs text-zinc-500">Reason</dt>
              <dd className="mt-0.5 text-zinc-900">{request.reason ?? '—'}</dd>
            </div>
          </dl>
        </SectionCard>

        {/* Evidence (damage only) */}
        {isDamage && (
          <SectionCard title="Video evidence">
            <p className="text-xs text-zinc-500">
              Reference code the customer was asked to include in WhatsApp:{' '}
              <span className="font-mono font-medium text-zinc-800">{request.evidenceReferenceCode ?? '—'}</span>
            </p>

            {request.evidenceVideoAvailable ? (
              evidenceLoadError ? (
                <p className="mt-3 text-sm text-rose-600">{evidenceLoadError}</p>
              ) : evidenceUrl ? (
                <video controls src={evidenceUrl} className="mt-3 max-h-96 w-full rounded-md border border-zinc-200 bg-black" />
              ) : (
                <div className="mt-3 h-48 w-full animate-pulse rounded-md bg-zinc-100" />
              )
            ) : (
              <p className="mt-3 text-sm text-zinc-500">
                No video has been attached yet. Once you receive the customer's WhatsApp video (matched by the
                reference code above), upload it here.
              </p>
            )}

            <div className="mt-4 border-t border-zinc-100 pt-4">
              <label className="inline-flex cursor-pointer items-center rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50">
                {uploadSubmitting ? 'Uploading…' : request.evidenceVideoAvailable ? 'Replace video' : 'Upload video'}
                <input
                  type="file"
                  accept="video/mp4,video/webm,video/quicktime"
                  className="hidden"
                  disabled={uploadSubmitting}
                  onChange={handleFileSelected}
                />
              </label>
              {uploadError && <p className="mt-2 text-sm text-rose-600">{uploadError}</p>}
            </div>
          </SectionCard>
        )}

        {/* Decision */}
        <SectionCard title="Decision">
          {!canDecide ? (
            <p className="text-sm text-zinc-500">
              This request has already been reviewed
              {request.reviewedByName ? ` by ${request.reviewedByName}` : ''}
              {request.reviewedAt ? ` on ${formatDateTime(request.reviewedAt)}` : ''}.
            </p>
          ) : (
            <>
              <TextField label="Note (optional)" value={decisionNote} onChange={(v) => setDecisionNote(v ?? '')} textarea />
              <div className="mt-3 flex flex-wrap gap-2">
                {isExchange && (
                  <button
                    type="button"
                    onClick={() => {
                      setDecisionError(null)
                      setPendingAction('approve-exchange')
                    }}
                    className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-semibold text-white hover:bg-zinc-700"
                  >
                    Approve exchange
                  </button>
                )}
                {isDamage && (
                  <button
                    type="button"
                    disabled={request.evidenceStatus !== 'SUBMITTED'}
                    title={request.evidenceStatus !== 'SUBMITTED' ? 'Upload the video evidence before approving' : undefined}
                    onClick={() => {
                      setDecisionError(null)
                      setPendingAction('approve-damage')
                    }}
                    className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-semibold text-white hover:bg-zinc-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Approve damage claim
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => {
                    setDecisionError(null)
                    setPendingAction('reject')
                  }}
                  className="rounded-md border border-rose-200 px-3 py-1.5 text-sm font-medium text-rose-700 hover:bg-rose-50"
                >
                  Reject
                </button>
              </div>
              {decisionError && !pendingAction && <p className="mt-2 text-sm text-rose-600">{decisionError}</p>}
            </>
          )}

          {request.adminNote && (
            <div className="mt-4 rounded-md border border-zinc-100 bg-zinc-50 px-3 py-2">
              <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Admin note</p>
              <p className="mt-1 text-sm text-zinc-800">{request.adminNote}</p>
            </div>
          )}
        </SectionCard>

        {/* Refund (damage + approved only) */}
        {isDamage && (canRefund || request.refund) && (
          <SectionCard title="Refund">
            {request.refund ? (
              <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm sm:grid-cols-4">
                <div>
                  <dt className="text-xs text-zinc-500">Status</dt>
                  <dd className="mt-0.5 text-zinc-900">{request.refund.status}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Amount</dt>
                  <dd className="mt-0.5 font-medium text-zinc-900">{formatPrice(request.refund.amount)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Reference</dt>
                  <dd className="mt-0.5 text-zinc-900">{request.refund.reference ?? '—'}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Date</dt>
                  <dd className="mt-0.5 text-zinc-900">{formatDateTime(request.refund.createdAt)}</dd>
                </div>
              </dl>
            ) : (
              <div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <TextField label="Amount (₹)" type="number" value={refundAmount} onChange={(v) => setRefundAmount(v ?? '')} />
                  <TextField label="Reason (optional)" value={refundReason} onChange={setRefundReason} />
                </div>
                {refundError && !refundConfirmOpen && <p className="mt-2 text-sm text-rose-600">{refundError}</p>}
                <button
                  type="button"
                  onClick={() => {
                    setRefundError(null)
                    setRefundConfirmOpen(true)
                  }}
                  className="mt-3 rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
                >
                  Initiate refund
                </button>
              </div>
            )}
          </SectionCard>
        )}
      </div>

      {/* Decision confirmation */}
      <ConfirmDialog
        open={pendingAction !== null}
        title={
          pendingAction === 'approve-exchange'
            ? 'Approve exchange'
            : pendingAction === 'approve-damage'
              ? 'Approve damage claim'
              : 'Reject request'
        }
        message={
          pendingAction === 'approve-exchange'
            ? 'Approving swaps stock immediately: the original size is restocked and the replacement size is deducted.'
            : pendingAction === 'approve-damage'
              ? 'This confirms the damage claim is valid. A refund can be initiated afterwards.'
              : 'This request will be marked rejected and cannot proceed further.'
        }
        confirmLabel={pendingAction === 'reject' ? 'Reject' : 'Approve'}
        danger={pendingAction === 'reject'}
        confirming={decisionSubmitting}
        error={decisionError}
        onConfirm={submitDecision}
        onCancel={() => {
          setPendingAction(null)
          setDecisionError(null)
        }}
      />

      {/* Refund confirmation */}
      <ConfirmDialog
        open={refundConfirmOpen}
        title="Initiate refund"
        message={`Initiate a refund of ${refundAmount ? formatPrice(Number(refundAmount) || 0) : ''}?${
          refundReason ? ` Reason: ${refundReason}` : ''
        }`}
        confirmLabel="Initiate refund"
        confirming={refundSubmitting}
        error={refundError}
        onConfirm={submitRefund}
        onCancel={() => {
          setRefundConfirmOpen(false)
          setRefundError(null)
        }}
      />

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <ReturnsBackLink />
      </div>
    </div>
  )
}
