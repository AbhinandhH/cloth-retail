import { useCallback, useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  addAdminOrderNote,
  cancelAdminOrder,
  fetchAdminOrderDetail,
  initiateAdminOrderRefund,
  saveAdminOrderShipment,
  updateAdminOrderStatus,
} from '../api/adminOrders'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import ConfirmDialog from '../components/ConfirmDialog'
import TextField from '../components/TextField'
import type { AdminOrderDetail as AdminOrderDetailType, AdminOrderPaymentStatus, AdminOrderStatus } from '../types'

// Terminal statuses — no further transitions expected. The "Cancel order"
// action is hidden once one of these is reached; everything else about the
// transition graph is driven entirely by `availableNextStatuses` from the
// backend (never hardcoded here).
const TERMINAL_STATUSES: AdminOrderStatus[] = [
  'CANCELLED',
  'DELIVERED',
  'RETURNED',
  'REFUNDED',
  'PAYMENT_FAILED',
]

const STATUS_LABELS: Record<AdminOrderStatus, string> = {
  PENDING_PAYMENT: 'Pending payment',
  PAYMENT_PROCESSING: 'Payment processing',
  PAYMENT_FAILED: 'Payment failed',
  CONFIRMED: 'Confirmed',
  PROCESSING: 'Processing',
  PACKED: 'Packed',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
  RETURNED: 'Returned',
  REFUNDED: 'Refunded',
}

// Order-status badge: solid pill. Kept visually distinct from the payment
// badge below (which uses a bordered/dotted style) so the two independent
// states never read as the same kind of thing.
const STATUS_BADGE_CLASSES: Record<AdminOrderStatus, string> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800',
  PAYMENT_PROCESSING: 'bg-amber-100 text-amber-800',
  PAYMENT_FAILED: 'bg-rose-100 text-rose-800',
  CONFIRMED: 'bg-sky-100 text-sky-800',
  PROCESSING: 'bg-indigo-100 text-indigo-800',
  PACKED: 'bg-indigo-100 text-indigo-800',
  SHIPPED: 'bg-violet-100 text-violet-800',
  DELIVERED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-zinc-200 text-zinc-700',
  RETURNED: 'bg-orange-100 text-orange-800',
  REFUNDED: 'bg-zinc-200 text-zinc-700',
}

function OrderStatusBadge({ status }: { status: AdminOrderStatus }) {
  return (
    <span className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${STATUS_BADGE_CLASSES[status]}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  )
}

const PAYMENT_STATUS_LABELS: Record<AdminOrderPaymentStatus, string> = {
  PENDING: 'Pending',
  SUCCESS: 'Success',
  FAILED: 'Failed',
}

// Deliberately a different shape (bordered rectangle + dot) from the
// order-status pill above, so the two badges never look like the same axis.
const PAYMENT_BADGE_CLASSES: Record<AdminOrderPaymentStatus, string> = {
  PENDING: 'border-amber-300 text-amber-700',
  SUCCESS: 'border-emerald-300 text-emerald-700',
  FAILED: 'border-rose-300 text-rose-700',
}

const PAYMENT_DOT_CLASSES: Record<AdminOrderPaymentStatus, string> = {
  PENDING: 'bg-amber-500',
  SUCCESS: 'bg-emerald-500',
  FAILED: 'bg-rose-500',
}

function PaymentStatusBadge({ status }: { status: AdminOrderPaymentStatus }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-xs font-semibold ${PAYMENT_BADGE_CLASSES[status]}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${PAYMENT_DOT_CLASSES[status]}`} />
      Payment: {PAYMENT_STATUS_LABELS[status]}
    </span>
  )
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function formatDateOnly(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString('en-IN', { dateStyle: 'medium' })
}

/** Normalizes a possibly-datetime ISO string down to yyyy-MM-dd for a <input type="date">. */
function toDateInputValue(value: string | null | undefined): string {
  if (!value) return ''
  return value.length >= 10 ? value.slice(0, 10) : value
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

/** Icon-only "back to orders" affordance - no text, matching the other admin screens' back links. */
function OrdersBackLink({ className = '' }: { className?: string }) {
  return (
    <Link
      to="/admin/orders"
      aria-label="Back to orders"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  )
}

export default function AdminOrderDetail() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view this order.</p>
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

  return <AdminOrderDetailContent />
}

function AdminOrderDetailContent() {
  const { id } = useParams<{ id: string }>()

  const [order, setOrder] = useState<AdminOrderDetailType | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const load = useCallback(() => {
    if (!id) return
    setLoading(true)
    setLoadError(null)
    fetchAdminOrderDetail(id)
      .then(setOrder)
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  // --- Status transition (and cancel) ---
  const [statusDraft, setStatusDraft] = useState<{ toStatus: AdminOrderStatus; reason: string } | null>(null)
  const [statusConfirmOpen, setStatusConfirmOpen] = useState(false)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [statusError, setStatusError] = useState<string | null>(null)

  const [cancelDraft, setCancelDraft] = useState<{ reason: string } | null>(null)
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false)
  const [cancelSubmitting, setCancelSubmitting] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)

  const submitStatusChange = () => {
    if (!id || !statusDraft) return
    setStatusSubmitting(true)
    setStatusError(null)
    updateAdminOrderStatus(id, { toStatus: statusDraft.toStatus, reason: statusDraft.reason || null })
      .then((updated) => {
        setOrder(updated)
        setStatusConfirmOpen(false)
        setStatusDraft(null)
      })
      .catch((err) => setStatusError(getErrorMessage(err)))
      .finally(() => setStatusSubmitting(false))
  }

  const submitCancel = () => {
    if (!id || !cancelDraft || !cancelDraft.reason.trim()) return
    setCancelSubmitting(true)
    setCancelError(null)
    cancelAdminOrder(id, cancelDraft.reason.trim())
      .then((updated) => {
        setOrder(updated)
        setCancelConfirmOpen(false)
        setCancelDraft(null)
      })
      .catch((err) => setCancelError(getErrorMessage(err)))
      .finally(() => setCancelSubmitting(false))
  }

  // --- Internal notes ---
  const [noteText, setNoteText] = useState('')
  const [noteSubmitting, setNoteSubmitting] = useState(false)
  const [noteError, setNoteError] = useState<string | null>(null)

  const submitNote = (e: FormEvent) => {
    e.preventDefault()
    if (!id || !noteText.trim()) return
    setNoteSubmitting(true)
    setNoteError(null)
    addAdminOrderNote(id, noteText.trim())
      .then(() => {
        setNoteText('')
        load()
      })
      .catch((err) => setNoteError(getErrorMessage(err)))
      .finally(() => setNoteSubmitting(false))
  }

  // --- Shipment (create-or-update) ---
  const [shipProvider, setShipProvider] = useState<string | null>(null)
  const [shipTracking, setShipTracking] = useState<string | null>(null)
  const [shipDate, setShipDate] = useState<string | null>(null)
  const [deliveryDate, setDeliveryDate] = useState<string | null>(null)
  const [shipNotes, setShipNotes] = useState<string | null>(null)
  const [shipSubmitting, setShipSubmitting] = useState(false)
  const [shipError, setShipError] = useState<string | null>(null)
  const [shipMessage, setShipMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!order) return
    setShipProvider(order.shipment?.provider ?? null)
    setShipTracking(order.shipment?.trackingNumber ?? null)
    setShipDate(toDateInputValue(order.shipment?.shipmentDate) || null)
    setDeliveryDate(toDateInputValue(order.shipment?.deliveryDate) || null)
    setShipNotes(order.shipment?.notes ?? null)
  }, [order?.shipment])

  const submitShipment = (e: FormEvent) => {
    e.preventDefault()
    if (!id) return
    setShipSubmitting(true)
    setShipError(null)
    setShipMessage(null)
    saveAdminOrderShipment(id, {
      provider: shipProvider,
      trackingNumber: shipTracking,
      shipmentDate: shipDate,
      deliveryDate: deliveryDate,
      notes: shipNotes,
    })
      .then(() => {
        setShipMessage('Shipment details saved.')
        load()
      })
      .catch((err) => setShipError(getErrorMessage(err)))
      .finally(() => setShipSubmitting(false))
  }

  // --- Refund ---
  const [refundAmount, setRefundAmount] = useState('')
  const [refundReason, setRefundReason] = useState<string | null>(null)
  const [refundConfirmOpen, setRefundConfirmOpen] = useState(false)
  const [refundSubmitting, setRefundSubmitting] = useState(false)
  const [refundError, setRefundError] = useState<string | null>(null)

  useEffect(() => {
    if (order?.payment && order.payment.status === 'SUCCESS' && !order.refund) {
      setRefundAmount(String(order.payment.amount))
    }
  }, [order?.payment, order?.refund])

  const submitRefund = () => {
    if (!id) return
    const amount = Number(refundAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      setRefundError('Enter a valid refund amount.')
      return
    }
    setRefundSubmitting(true)
    setRefundError(null)
    initiateAdminOrderRefund(id, { amount, reason: refundReason || null })
      .then(() => {
        setRefundConfirmOpen(false)
        load()
      })
      .catch((err) => setRefundError(getErrorMessage(err)))
      .finally(() => setRefundSubmitting(false))
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="space-y-3">
          <div className="h-8 w-1/2 animate-pulse rounded bg-zinc-100" />
          <div className="h-40 animate-pulse rounded-lg bg-zinc-100" />
          <div className="h-40 animate-pulse rounded-lg bg-zinc-100" />
        </div>
      </div>
    )
  }

  if (loadError || !order) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {loadError ?? 'Order not found.'}
        </div>
        <Link to="/admin/orders" className="mt-4 inline-block text-sm font-medium text-zinc-900 underline">
          &larr; Back to orders
        </Link>
      </div>
    )
  }

  const canCancel = !TERMINAL_STATUSES.includes(order.status)

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-2">
          <OrdersBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">Order {order.orderNumber}</h1>
            <p className="mt-1 text-sm text-zinc-500">
              Created {formatDateTime(order.createdAt)} &middot; Updated {formatDateTime(order.updatedAt)}
            </p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-2">
          <OrderStatusBadge status={order.status} />
          {order.payment && <PaymentStatusBadge status={order.payment.status} />}
        </div>
      </div>

      <div className="mt-6 space-y-6">
        {/* Items */}
        <SectionCard title="Items">
          <ul className="divide-y divide-zinc-100">
            {order.items.map((item) => {
              const imgSrc = toMediaUrl(item.imageUrl)
              return (
                <li key={item.id} className="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
                  <div className="h-14 w-14 shrink-0 overflow-hidden rounded-md border border-zinc-200 bg-zinc-50">
                    {imgSrc ? (
                      <img src={imgSrc} alt={item.productName} className="h-full w-full object-cover" />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center text-[10px] text-zinc-400">
                        No image
                      </div>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-zinc-900">{item.productName}</p>
                    <p className="mt-0.5 text-xs text-zinc-500">
                      {item.colorName} &middot; {item.sizeName} &middot; Qty {item.quantity}
                    </p>
                    <p className="mt-0.5 text-xs text-zinc-400">{item.sku}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-sm font-semibold text-zinc-900">{formatPrice(item.lineTotal)}</p>
                    <p className="mt-0.5 text-xs text-zinc-500">
                      {formatPrice(item.unitPrice)} each
                      {item.discountPercent > 0 ? ` · ${item.discountPercent}% off` : ''}
                    </p>
                  </div>
                </li>
              )
            })}
          </ul>

          <dl className="mt-4 space-y-1.5 border-t border-zinc-100 pt-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-zinc-500">Subtotal</dt>
              <dd className="text-zinc-900">{formatPrice(order.subtotal)}</dd>
            </div>
            {order.discountTotal > 0 && (
              <div className="flex justify-between">
                <dt className="text-zinc-500">Discount</dt>
                <dd className="text-emerald-600">&minus;{formatPrice(order.discountTotal)}</dd>
              </div>
            )}
            <div className="flex justify-between">
              <dt className="text-zinc-500">Shipping</dt>
              <dd className="text-zinc-900">{order.shippingCharge > 0 ? formatPrice(order.shippingCharge) : 'Free'}</dd>
            </div>
            <div className="mt-1.5 flex justify-between border-t border-zinc-200 pt-1.5 text-base font-semibold">
              <dt className="text-zinc-900">Total</dt>
              <dd className="text-zinc-900">{formatPrice(order.totalAmount)}</dd>
            </div>
          </dl>
        </SectionCard>

        {/* Customer (read-only) */}
        <SectionCard title="Customer">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Contact</p>
              <p className="mt-1 text-sm text-zinc-900">{order.customer.name}</p>
              <p className="text-sm text-zinc-600">{order.customer.phone}</p>
              <p className="text-sm text-zinc-600">{order.customer.email}</p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Shipping address</p>
              <address className="mt-1 text-sm not-italic text-zinc-600">
                {order.shippingAddress.addressLine1}
                <br />
                {order.shippingAddress.addressLine2 && (
                  <>
                    {order.shippingAddress.addressLine2}
                    <br />
                  </>
                )}
                {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}
                <br />
                {order.shippingAddress.country}
              </address>
            </div>
          </div>
        </SectionCard>

        {/* Payment */}
        <SectionCard title="Payment">
          {!order.payment ? (
            <p className="text-sm text-zinc-500">No payment recorded for this order.</p>
          ) : (
            <div className="space-y-4">
              <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm sm:grid-cols-4">
                <div>
                  <dt className="text-xs text-zinc-500">Status</dt>
                  <dd className="mt-0.5">
                    <PaymentStatusBadge status={order.payment.status} />
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Method</dt>
                  <dd className="mt-0.5 text-zinc-900">{order.payment.method}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Amount</dt>
                  <dd className="mt-0.5 font-medium text-zinc-900">{formatPrice(order.payment.amount)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Gateway reference</dt>
                  <dd className="mt-0.5 text-zinc-900">{order.payment.gatewayReference ?? '—'}</dd>
                </div>
              </dl>
              {order.payment.failureReason && (
                <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                  Failure reason: {order.payment.failureReason}
                </div>
              )}

              {order.refund ? (
                <div className="rounded-md border border-zinc-200 bg-zinc-50 px-3 py-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Refund</p>
                  <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm sm:grid-cols-4">
                    <div>
                      <dt className="text-xs text-zinc-500">Status</dt>
                      <dd className="mt-0.5 text-zinc-900">{order.refund.status}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-zinc-500">Amount</dt>
                      <dd className="mt-0.5 font-medium text-zinc-900">{formatPrice(order.refund.amount)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-zinc-500">Reference</dt>
                      <dd className="mt-0.5 text-zinc-900">{order.refund.reference ?? '—'}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-zinc-500">Date</dt>
                      <dd className="mt-0.5 text-zinc-900">{formatDateTime(order.refund.createdAt)}</dd>
                    </div>
                  </dl>
                </div>
              ) : order.payment.status === 'SUCCESS' ? (
                <div className="rounded-md border border-zinc-200 px-3 py-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Initiate refund</p>
                  <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <TextField
                      label="Amount (₹)"
                      type="number"
                      value={refundAmount}
                      onChange={(v) => setRefundAmount(v ?? '')}
                    />
                    <TextField label="Reason (optional)" value={refundReason} onChange={setRefundReason} />
                  </div>
                  {refundError && !refundConfirmOpen && (
                    <p className="mt-2 text-sm text-rose-600">{refundError}</p>
                  )}
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
              ) : null}
            </div>
          )}
        </SectionCard>

        {/* Shipment */}
        <SectionCard title="Shipment">
          <form onSubmit={submitShipment} className="space-y-4">
            {order.shipment ? (
              <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm sm:grid-cols-4">
                <div>
                  <dt className="text-xs text-zinc-500">Provider</dt>
                  <dd className="mt-0.5 text-zinc-900">{order.shipment.provider ?? '—'}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Tracking number</dt>
                  <dd className="mt-0.5 text-zinc-900">{order.shipment.trackingNumber ?? '—'}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Shipped</dt>
                  <dd className="mt-0.5 text-zinc-900">{formatDateOnly(order.shipment.shipmentDate)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-zinc-500">Delivered</dt>
                  <dd className="mt-0.5 text-zinc-900">{formatDateOnly(order.shipment.deliveryDate)}</dd>
                </div>
              </dl>
            ) : (
              <p className="text-sm text-zinc-500">No shipment recorded yet.</p>
            )}

            <div className="border-t border-zinc-100 pt-4">
              <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">
                {order.shipment ? 'Update shipment' : 'Add shipment'}
              </p>
              <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                <TextField label="Provider" value={shipProvider} onChange={setShipProvider} />
                <TextField label="Tracking number" value={shipTracking} onChange={setShipTracking} />
                <TextField label="Shipment date" type="date" value={shipDate} onChange={setShipDate} />
                <TextField label="Delivery date" type="date" value={deliveryDate} onChange={setDeliveryDate} />
              </div>
              <div className="mt-3">
                <TextField label="Notes" value={shipNotes} onChange={setShipNotes} textarea />
              </div>

              {shipError && (
                <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                  {shipError}
                </div>
              )}
              {shipMessage && (
                <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                  {shipMessage}
                </div>
              )}

              <button
                type="submit"
                disabled={shipSubmitting}
                className="mt-3 rounded-md bg-zinc-900 px-4 py-2 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-60"
              >
                {shipSubmitting ? 'Saving…' : 'Save shipment'}
              </button>
            </div>
          </form>
        </SectionCard>

        {/* Status actions */}
        <SectionCard title="Status actions">
          {order.availableNextStatuses.length === 0 && !canCancel ? (
            <p className="text-sm text-zinc-500">This order is in a terminal state — no further actions available.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {order.availableNextStatuses.map((next) => (
                <button
                  key={next}
                  type="button"
                  onClick={() => {
                    setStatusError(null)
                    setStatusDraft({ toStatus: next, reason: '' })
                  }}
                  className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
                >
                  Mark as {STATUS_LABELS[next] ?? next}
                </button>
              ))}
              {canCancel && (
                <button
                  type="button"
                  onClick={() => {
                    setCancelError(null)
                    setCancelDraft({ reason: '' })
                  }}
                  className="rounded-md border border-rose-200 px-3 py-1.5 text-sm font-medium text-rose-700 hover:bg-rose-50"
                >
                  Cancel order
                </button>
              )}
            </div>
          )}

          {/* Status change reason step */}
          {statusDraft && (
            <div className="mt-4 rounded-md border border-zinc-200 bg-zinc-50 px-3 py-3">
              <p className="text-sm font-medium text-zinc-900">
                Change status to {STATUS_LABELS[statusDraft.toStatus] ?? statusDraft.toStatus}
              </p>
              <div className="mt-2">
                <TextField
                  label="Reason (optional)"
                  value={statusDraft.reason}
                  onChange={(v) => setStatusDraft((d) => (d ? { ...d, reason: v ?? '' } : d))}
                />
              </div>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => setStatusDraft(null)}
                  className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-white"
                >
                  Discard
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setStatusError(null)
                    setStatusConfirmOpen(true)
                  }}
                  className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-semibold text-white hover:bg-zinc-700"
                >
                  Continue
                </button>
              </div>
            </div>
          )}

          {/* Cancel reason step (required) */}
          {cancelDraft && (
            <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-3">
              <p className="text-sm font-medium text-rose-900">Cancel this order</p>
              <div className="mt-2">
                <TextField
                  label="Reason (required)"
                  value={cancelDraft.reason}
                  onChange={(v) => setCancelDraft((d) => (d ? { ...d, reason: v ?? '' } : d))}
                />
              </div>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => setCancelDraft(null)}
                  className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-white"
                >
                  Discard
                </button>
                <button
                  type="button"
                  disabled={!cancelDraft.reason.trim()}
                  onClick={() => {
                    setCancelError(null)
                    setCancelConfirmOpen(true)
                  }}
                  className="rounded-md bg-rose-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-rose-500 disabled:opacity-50"
                >
                  Continue
                </button>
              </div>
            </div>
          )}
        </SectionCard>

        {/* Status history */}
        <SectionCard title="Status history">
          {order.statusHistory.length === 0 ? (
            <p className="text-sm text-zinc-500">No history recorded.</p>
          ) : (
            <ol className="space-y-3">
              {order.statusHistory.map((entry, i) => (
                <li key={i} className="flex items-start gap-3 text-sm">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-zinc-400" />
                  <div>
                    <p className="text-zinc-900">
                      {entry.previousStatus ? (STATUS_LABELS[entry.previousStatus] ?? entry.previousStatus) : 'Order placed'}
                      {entry.previousStatus && ' → '}
                      {entry.previousStatus && (STATUS_LABELS[entry.newStatus] ?? entry.newStatus)}
                    </p>
                    <p className="mt-0.5 text-xs text-zinc-500">
                      {entry.changedByName ?? 'System'} &middot; {formatDateTime(entry.createdAt)}
                    </p>
                    {entry.reason && <p className="mt-0.5 text-xs text-zinc-500">Reason: {entry.reason}</p>}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </SectionCard>

        {/* Internal notes */}
        <SectionCard
          title="Internal notes"
          right={
            <span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[10px] font-medium text-amber-700">
              Internal — not visible to customer
            </span>
          }
        >
          {order.notes.length === 0 ? (
            <p className="text-sm text-zinc-500">No notes yet.</p>
          ) : (
            <ul className="space-y-3">
              {order.notes.map((n) => (
                <li key={n.id} className="rounded-md border border-zinc-100 bg-zinc-50 px-3 py-2">
                  <p className="text-sm text-zinc-800">{n.note}</p>
                  <p className="mt-1 text-xs text-zinc-500">
                    {n.adminName} &middot; {formatDateTime(n.createdAt)}
                  </p>
                </li>
              ))}
            </ul>
          )}

          <form onSubmit={submitNote} className="mt-4 border-t border-zinc-100 pt-4">
            <TextField label="Add a note" value={noteText} onChange={(v) => setNoteText(v ?? '')} textarea />
            {noteError && <p className="mt-2 text-sm text-rose-600">{noteError}</p>}
            <button
              type="submit"
              disabled={noteSubmitting || !noteText.trim()}
              className="mt-3 rounded-md bg-zinc-900 px-4 py-2 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-60"
            >
              {noteSubmitting ? 'Adding…' : 'Add note'}
            </button>
          </form>
        </SectionCard>
      </div>

      {/* Status change confirmation */}
      <ConfirmDialog
        open={statusConfirmOpen}
        title="Change order status"
        message={
          statusDraft
            ? `Change status to "${STATUS_LABELS[statusDraft.toStatus] ?? statusDraft.toStatus}"?${
                statusDraft.reason ? ` Reason: ${statusDraft.reason}` : ''
              }`
            : ''
        }
        confirmLabel="Change status"
        confirming={statusSubmitting}
        error={statusError}
        onConfirm={submitStatusChange}
        onCancel={() => {
          setStatusConfirmOpen(false)
          setStatusError(null)
        }}
      />

      {/* Cancel confirmation */}
      <ConfirmDialog
        open={cancelConfirmOpen}
        title="Cancel order"
        message={cancelDraft ? `Cancel this order? Reason: ${cancelDraft.reason}` : ''}
        confirmLabel="Cancel order"
        danger
        confirming={cancelSubmitting}
        error={cancelError}
        onConfirm={submitCancel}
        onCancel={() => {
          setCancelConfirmOpen(false)
          setCancelError(null)
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
        <OrdersBackLink />
      </div>
    </div>
  )
}
