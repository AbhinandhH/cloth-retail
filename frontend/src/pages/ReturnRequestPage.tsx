import { useEffect, useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import {
  fetchEligibleItems,
  fetchMyReturnRequest,
  reportDamagedProduct,
  requestSizeExchange,
} from '../api/returns'
import { getErrorMessage, toMediaUrl } from '../api/client'
import BackButton from '../components/BackButton'
import { SkeletonBlock, SkeletonText } from '../components/Skeleton'
import ConfirmDialog from '../components/customer/ConfirmDialog'
import TextField from '../components/customer/TextField'
import type { EligibleOrderItem, ReturnRequestDetail } from '../types'

type Mode = 'choose' | 'exchange' | 'damage'

function StatusLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-zinc-500">{label}</span>
      <span className="font-medium text-zinc-900">{value}</span>
    </div>
  )
}

/** Plain-language status text for a submitted damage claim - reflects evidenceStatus/status together, not just the raw enum values. */
function damageStatusText(request: ReturnRequestDetail): string {
  if (request.status === 'REJECTED') return `Rejected${request.adminNote ? `: ${request.adminNote}` : ''}`
  if (request.status === 'REFUNDED') return 'Approved — refund processed'
  if (request.status === 'APPROVED') return 'Approved — refund will be processed shortly'
  if (request.evidenceStatus === 'SUBMITTED') return 'Video received, under review'
  return 'Awaiting your WhatsApp video'
}

function exchangeStatusText(request: ReturnRequestDetail): string {
  if (request.status === 'REJECTED') return `Rejected${request.adminNote ? `: ${request.adminNote}` : ''}`
  if (request.status === 'APPROVED') return 'Approved — your replacement size is being processed'
  return 'Pending review'
}

export default function ReturnRequestPage() {
  const { orderId, itemId } = useParams<{ orderId: string; itemId: string }>()
  const [searchParams] = useSearchParams()
  const initialType = searchParams.get('type') === 'damage' ? 'damage' : searchParams.get('type') === 'exchange' ? 'exchange' : null

  const [mode, setMode] = useState<Mode>(initialType ?? 'choose')

  const [item, setItem] = useState<EligibleOrderItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    if (!orderId) return
    setLoading(true)
    setLoadError(null)
    fetchEligibleItems(orderId)
      .then((items) => {
        const match = items.find((i) => String(i.orderItemId) === String(itemId))
        setItem(match ?? null)
      })
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [orderId, itemId])

  // --- Exchange flow ---
  const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null)
  const [exchangeReason, setExchangeReason] = useState<string | null>(null)
  const [exchangeConfirmOpen, setExchangeConfirmOpen] = useState(false)
  const [exchangeSubmitting, setExchangeSubmitting] = useState(false)
  const [exchangeError, setExchangeError] = useState<string | null>(null)
  const [exchangeResult, setExchangeResult] = useState<ReturnRequestDetail | null>(null)

  const submitExchange = () => {
    if (!orderId || !itemId || !selectedVariantId) return
    setExchangeSubmitting(true)
    setExchangeError(null)
    requestSizeExchange(orderId, itemId, { requestedVariantId: selectedVariantId, reason: exchangeReason })
      .then((result) => {
        setExchangeResult(result)
        setExchangeConfirmOpen(false)
      })
      .catch((err) => setExchangeError(getErrorMessage(err)))
      .finally(() => setExchangeSubmitting(false))
  }

  // --- Damage flow ---
  const [damageReason, setDamageReason] = useState<string | null>(null)
  const [damageConfirmOpen, setDamageConfirmOpen] = useState(false)
  const [damageSubmitting, setDamageSubmitting] = useState(false)
  const [damageError, setDamageError] = useState<string | null>(null)
  const [damageResult, setDamageResult] = useState<ReturnRequestDetail | null>(null)
  const [refreshingStatus, setRefreshingStatus] = useState(false)

  const submitDamage = () => {
    if (!orderId || !itemId || !damageReason?.trim()) return
    setDamageSubmitting(true)
    setDamageError(null)
    reportDamagedProduct(orderId, itemId, { reason: damageReason.trim() })
      .then((result) => {
        setDamageResult(result)
        setDamageConfirmOpen(false)
      })
      .catch((err) => setDamageError(getErrorMessage(err)))
      .finally(() => setDamageSubmitting(false))
  }

  const refreshDamageStatus = () => {
    if (!damageResult) return
    setRefreshingStatus(true)
    fetchMyReturnRequest(damageResult.id)
      .then(setDamageResult)
      .catch((err) => setDamageError(getErrorMessage(err)))
      .finally(() => setRefreshingStatus(false))
  }

  // A revisit (page reload, or navigating back into this URL later) for an item that already has
  // an active request - load and show that request's live status instead of a dead end, so
  // "see the request status" holds beyond the single session it was created in.
  useEffect(() => {
    if (!item?.alreadyHasActiveRequest || !item.activeRequestId) return
    fetchMyReturnRequest(item.activeRequestId)
      .then((request) => {
        if (request.requestType === 'SIZE_EXCHANGE') {
          setMode('exchange')
          setExchangeResult(request)
        } else {
          setMode('damage')
          setDamageResult(request)
        }
      })
      .catch(() => {
        // Leave the dead-end "already in progress" message as the fallback if this fails.
      })
  }, [item?.alreadyHasActiveRequest, item?.activeRequestId])

  const selectedReplacement = useMemo(
    () => item?.replacementSizes.find((s) => String(s.variantId) === selectedVariantId) ?? null,
    [item, selectedVariantId],
  )

  if (loading) {
    return (
      <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
        <BackButton className="-ml-2" />
        <div className="mt-4 space-y-3">
          <SkeletonText width="w-40" />
          <SkeletonBlock className="h-24 w-full rounded-xl" />
          <SkeletonBlock className="h-40 w-full rounded-xl" />
        </div>
      </div>
    )
  }

  if (loadError || !item) {
    return (
      <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
        <BackButton className="-ml-2" />
        <p className="mt-4 text-sm text-rose-600">{loadError ?? 'This item is not eligible for return or exchange.'}</p>
        <Link to={`/orders/${orderId}`} className="mt-4 inline-block text-sm font-medium text-zinc-900 underline">
          Back to order
        </Link>
      </div>
    )
  }

  if (item.alreadyHasActiveRequest && !exchangeResult && !damageResult) {
    return (
      <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
        <BackButton className="-ml-2" />
        <h1 className="mt-2 font-display text-2xl font-semibold text-zinc-900">Request already in progress</h1>
        <p className="mt-2 text-sm text-zinc-600">
          You already have an active return/exchange request for this item. You can't submit another one until it's
          resolved.
        </p>
        <Link to={`/orders/${orderId}`} className="mt-4 inline-block text-sm font-medium text-zinc-900 underline">
          Back to order
        </Link>
      </div>
    )
  }

  const itemImage = toMediaUrl(item.imageUrl)

  return (
    <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
      <div className="flex items-center gap-1">
        <BackButton className="-ml-2" />
        <h1 className="font-display text-2xl font-semibold text-zinc-900 sm:text-3xl">Return / Exchange</h1>
      </div>

      {/* Item summary */}
      <div className="mt-4 flex items-center gap-3 rounded-xl border border-zinc-200 bg-white p-3 shadow-soft">
        <div className="h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-zinc-100">
          {itemImage ? (
            <img src={itemImage} alt={item.productName} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-[10px] text-zinc-400">No image</div>
          )}
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-zinc-900">{item.productName}</p>
          <p className="mt-0.5 text-xs text-zinc-500">
            {item.colorName} &middot; {item.sizeName} &middot; Qty {item.quantity}
          </p>
          <p className="mt-0.5 text-xs text-zinc-400">{item.sku}</p>
        </div>
      </div>

      {mode === 'choose' && (
        <div className="mt-6 space-y-3">
          <button
            type="button"
            onClick={() => setMode('exchange')}
            className="w-full rounded-xl border border-zinc-200 bg-white p-4 text-left shadow-soft transition-colors hover:border-zinc-300"
          >
            <p className="text-sm font-semibold text-zinc-900">Request size exchange</p>
            <p className="mt-1 text-xs text-zinc-500">Wrong size, or need a different one — no refund, just a swap.</p>
          </button>
          <button
            type="button"
            onClick={() => setMode('damage')}
            className="w-full rounded-xl border border-zinc-200 bg-white p-4 text-left shadow-soft transition-colors hover:border-zinc-300"
          >
            <p className="text-sm font-semibold text-zinc-900">Report damaged product</p>
            <p className="mt-1 text-xs text-zinc-500">Item arrived damaged — video evidence required for a refund.</p>
          </button>
        </div>
      )}

      {/* --- Exchange flow --- */}
      {mode === 'exchange' && !exchangeResult && (
        <div className="mt-6 space-y-4">
          <div>
            <p className="text-sm font-semibold text-zinc-900">Choose a replacement size</p>
            {item.replacementSizes.length === 0 ? (
              <p className="mt-2 text-sm text-zinc-500">No other sizes are currently in stock for this item.</p>
            ) : (
              <div className="mt-2 flex flex-wrap gap-2">
                {item.replacementSizes.map((s) => (
                  <button
                    key={String(s.variantId)}
                    type="button"
                    onClick={() => setSelectedVariantId(String(s.variantId))}
                    className={`min-h-[40px] rounded-full border px-4 text-sm font-medium transition-colors ${
                      selectedVariantId === String(s.variantId)
                        ? 'border-zinc-900 bg-zinc-900 text-white'
                        : 'border-zinc-300 text-zinc-700 hover:border-zinc-400'
                    }`}
                  >
                    {s.sizeName}
                  </button>
                ))}
              </div>
            )}
          </div>

          <TextField label="Reason (optional)" value={exchangeReason} onChange={setExchangeReason} textarea />

          {exchangeError && !exchangeConfirmOpen && <p className="text-sm text-rose-600">{exchangeError}</p>}

          <button
            type="button"
            disabled={!selectedVariantId}
            onClick={() => {
              setExchangeError(null)
              setExchangeConfirmOpen(true)
            }}
            className="w-full btn-primary-radius bg-[var(--brand-primary,#18181b)] py-3.5 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Submit exchange request
          </button>
        </div>
      )}

      {mode === 'exchange' && exchangeResult && (
        <div className="mt-6 rounded-xl border border-zinc-200 bg-white p-4 shadow-soft">
          <p className="text-sm font-semibold text-zinc-900">Exchange request submitted</p>
          <div className="mt-3 space-y-2">
            <StatusLine label="Requested size" value={exchangeResult.requestedSizeName ?? '—'} />
            <StatusLine label="Status" value={exchangeStatusText(exchangeResult)} />
          </div>
          <Link to={`/orders/${orderId}`} className="mt-4 inline-block text-sm font-medium text-zinc-900 underline">
            Back to order
          </Link>
        </div>
      )}

      {/* --- Damage flow --- */}
      {mode === 'damage' && !damageResult && (
        <div className="mt-6 space-y-4">
          <TextField label="What's wrong with the item?" value={damageReason} onChange={setDamageReason} textarea />

          {damageError && !damageConfirmOpen && <p className="text-sm text-rose-600">{damageError}</p>}

          <button
            type="button"
            disabled={!damageReason?.trim()}
            onClick={() => {
              setDamageError(null)
              setDamageConfirmOpen(true)
            }}
            className="w-full btn-primary-radius bg-[var(--brand-primary,#18181b)] py-3.5 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Report damaged product
          </button>
        </div>
      )}

      {mode === 'damage' && damageResult && (
        <div className="mt-6 space-y-4">
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
            <p className="text-sm font-semibold text-amber-900">Video evidence required</p>
            <p className="mt-1.5 text-sm text-amber-800">
              Please send a clear video showing the damaged product through WhatsApp. Your refund request will be
              reviewed by our admin team after the video is received.
            </p>
            {damageResult.evidenceReferenceCode && (
              <p className="mt-2 text-xs text-amber-700">
                Reference code (please include it in your message):{' '}
                <span className="font-mono font-semibold">{damageResult.evidenceReferenceCode}</span>
              </p>
            )}
            {damageResult.whatsappLink && (
              <a
                href={damageResult.whatsappLink}
                target="_blank"
                rel="noreferrer"
                className="mt-3 flex min-h-[44px] w-full items-center justify-center gap-2 rounded-full bg-emerald-600 px-4 text-sm font-semibold text-white transition-opacity hover:opacity-90"
              >
                Send video via WhatsApp
              </a>
            )}
          </div>

          <div className="rounded-xl border border-zinc-200 bg-white p-4 shadow-soft">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-zinc-900">Request status</p>
              <button
                type="button"
                onClick={refreshDamageStatus}
                disabled={refreshingStatus}
                className="text-xs font-medium text-zinc-500 hover:text-zinc-900 disabled:opacity-60"
              >
                {refreshingStatus ? 'Refreshing…' : 'Refresh'}
              </button>
            </div>
            <div className="mt-3 space-y-2">
              <StatusLine
                label="Video evidence"
                value={damageResult.evidenceStatus === 'SUBMITTED' ? 'Received' : 'Pending'}
              />
              <StatusLine label="Status" value={damageStatusText(damageResult)} />
            </div>
          </div>

          <Link to={`/orders/${orderId}`} className="inline-block text-sm font-medium text-zinc-900 underline">
            Back to order
          </Link>
        </div>
      )}

      <ConfirmDialog
        open={exchangeConfirmOpen}
        title="Submit exchange request"
        message={
          selectedReplacement
            ? `Request an exchange for size ${selectedReplacement.sizeName}?`
            : 'Submit this exchange request?'
        }
        confirmLabel="Submit"
        confirming={exchangeSubmitting}
        error={exchangeError}
        onConfirm={submitExchange}
        onCancel={() => {
          setExchangeConfirmOpen(false)
          setExchangeError(null)
        }}
      />

      <ConfirmDialog
        open={damageConfirmOpen}
        title="Report damaged product"
        message="Submit this damage report? You'll be asked to send video evidence via WhatsApp next."
        confirmLabel="Submit"
        confirming={damageSubmitting}
        error={damageError}
        onConfirm={submitDamage}
        onCancel={() => {
          setDamageConfirmOpen(false)
          setDamageError(null)
        }}
      />
    </div>
  )
}
