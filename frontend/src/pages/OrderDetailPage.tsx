import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchOrderById } from '../api/orders'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { OrderStatusBadge } from './OrderHistoryPage'
import type { OrderDetail, PaymentStatus } from '../types'

const PAYMENT_STATUS_LABEL: Record<Exclude<PaymentStatus, null>, string> = {
  PENDING: 'Payment pending',
  SUCCESS: 'Payment successful',
  FAILED: 'Payment failed',
}

const PAYMENT_STATUS_BADGE: Record<Exclude<PaymentStatus, null>, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
  SUCCESS: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  FAILED: 'bg-rose-50 text-rose-700 border-rose-200',
}

function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  if (!status) return null
  return (
    <span className={`inline-block rounded-full border px-2 py-0.5 text-[11px] font-medium ${PAYMENT_STATUS_BADGE[status]}`}>
      {PAYMENT_STATUS_LABEL[status]}
    </span>
  )
}

function formatDateTime(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    if (!id) return
    setLoading(true)
    setError(null)
    fetchOrderById(id)
      .then((res) => setOrder(res))
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="space-y-3">
          <div className="h-8 w-2/3 animate-pulse rounded bg-zinc-100" />
          <div className="h-32 animate-pulse rounded-lg bg-zinc-100" />
          <div className="h-48 animate-pulse rounded-lg bg-zinc-100" />
        </div>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error ?? 'Order not found.'}
        </div>
        <Link to="/orders" className="mt-4 inline-block text-sm font-medium text-zinc-900 underline">
          &larr; Back to my orders
        </Link>
      </div>
    )
  }

  const isFreshConfirmation = order.status === 'CONFIRMED'
  const isPendingPayment = order.status === 'PENDING_PAYMENT'

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
      <Link to="/orders" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
        &larr; My orders
      </Link>

      {isFreshConfirmation && (
        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-5 py-5 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h1 className="mt-3 text-xl font-semibold text-emerald-900">Your order is confirmed!</h1>
          <p className="mt-1 text-sm text-emerald-700">Thanks for shopping with us — details are below.</p>
        </div>
      )}

      <div className="mt-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          {!isFreshConfirmation && <h1 className="text-2xl font-semibold text-zinc-900">Order {order.orderNumber}</h1>}
          {isFreshConfirmation && <p className="text-sm font-medium text-zinc-500">Order {order.orderNumber}</p>}
          <p className="mt-1 text-sm text-zinc-500">Placed {formatDateTime(order.createdAt)}</p>
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <OrderStatusBadge status={order.status} />
          <PaymentStatusBadge status={order.paymentStatus} />
        </div>
      </div>

      {isPendingPayment && (
        <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
          <p className="text-sm font-medium text-amber-800">Payment not completed</p>
          {order.reservationExpiresAt && (
            <p className="mt-1 text-sm text-amber-700">
              Your items are reserved until {formatDateTime(order.reservationExpiresAt)}. Complete payment before then
              or your reservation may be released.
            </p>
          )}
          <Link
            to={`/checkout/payment/${order.id}`}
            className="mt-3 inline-block rounded-lg bg-zinc-900 px-4 py-2 text-sm font-semibold text-white hover:bg-zinc-700"
          >
            Complete payment
          </Link>
        </div>
      )}

      {/* Line items */}
      <div className="mt-6 rounded-lg border border-zinc-200 bg-white">
        <div className="border-b border-zinc-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-zinc-900">Items</h2>
        </div>
        <ul className="divide-y divide-zinc-100">
          {order.items.map((item) => (
            <li key={item.id} className="flex items-center justify-between gap-3 px-4 py-3">
              <div className="min-w-0">
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
          ))}
        </ul>
      </div>

      {/* Totals */}
      <div className="mt-4 rounded-lg border border-zinc-200 bg-white px-4 py-3">
        <dl className="space-y-1.5 text-sm">
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
      </div>

      {/* Shipping & contact */}
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded-lg border border-zinc-200 bg-white px-4 py-3">
          <h2 className="text-sm font-semibold text-zinc-900">Shipping address</h2>
          <address className="mt-2 text-sm not-italic text-zinc-600">
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
        <div className="rounded-lg border border-zinc-200 bg-white px-4 py-3">
          <h2 className="text-sm font-semibold text-zinc-900">Contact</h2>
          <p className="mt-2 text-sm text-zinc-600">{order.contactName}</p>
          <p className="text-sm text-zinc-600">{order.contactPhone}</p>
        </div>
      </div>
    </div>
  )
}
