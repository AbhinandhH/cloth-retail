import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchMyOrders } from '../api/orders'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import BackButton from '../components/BackButton'
import EmptyState from '../components/EmptyState'
import ErrorState from '../components/ErrorState'
import { SkeletonBlock, SkeletonText } from '../components/Skeleton'
import type { OrderListItem, OrderStatus } from '../types'

const PAGE_SIZE = 10

// Mirrors the full 11-value backend OrderStatus (see types/index.ts) — every
// value the customer endpoints can actually return needs a label/badge here,
// not just the pre-fulfillment subset.
const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Pending payment',
  PAYMENT_PROCESSING: 'Processing payment',
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

const STATUS_BADGE: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'bg-amber-50 text-amber-700 border-amber-200',
  PAYMENT_PROCESSING: 'bg-amber-50 text-amber-700 border-amber-200',
  PAYMENT_FAILED: 'bg-rose-50 text-rose-700 border-rose-200',
  CONFIRMED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PROCESSING: 'bg-sky-50 text-sky-700 border-sky-200',
  PACKED: 'bg-sky-50 text-sky-700 border-sky-200',
  SHIPPED: 'bg-violet-50 text-violet-700 border-violet-200',
  DELIVERED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELLED: 'bg-rose-50 text-rose-700 border-rose-200',
  RETURNED: 'bg-amber-50 text-amber-700 border-amber-200',
  REFUNDED: 'bg-zinc-100 text-zinc-700 border-zinc-200',
}

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`inline-block rounded-full border px-2 py-0.5 text-[11px] font-medium ${STATUS_BADGE[status]}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  )
}

function formatDate(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString('en-IN', { dateStyle: 'medium' })
}

function BagIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
      />
    </svg>
  )
}

function OrderCardSkeleton() {
  return (
    <div className="rounded-xl border border-zinc-200 bg-white p-4 shadow-soft sm:p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1 space-y-2">
          <SkeletonText width="w-32" />
          <SkeletonText width="w-40" className="h-3" />
          <SkeletonBlock className="mt-2 h-5 w-24 rounded-full" />
        </div>
        <div className="shrink-0 space-y-2 text-right">
          <SkeletonText width="w-16" />
          <SkeletonText width="w-12" className="h-3" />
        </div>
      </div>
    </div>
  )
}

export default function OrderHistoryPage() {
  const [orders, setOrders] = useState<OrderListItem[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    fetchMyOrders({ page, size: PAGE_SIZE })
      .then((res) => {
        setOrders(res.content)
        setTotalPages(res.totalPages)
        setTotalElements(res.totalElements)
      })
      .catch((err) => {
        setError(getErrorMessage(err))
        setOrders([])
        setTotalPages(0)
        setTotalElements(0)
      })
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return []
    const windowSize = 5
    let start = Math.max(0, page - Math.floor(windowSize / 2))
    const end = Math.min(totalPages, start + windowSize)
    start = Math.max(0, end - windowSize)
    return Array.from({ length: end - start }, (_, i) => start + i)
  }, [page, totalPages])

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="flex items-center gap-1">
        <BackButton className="-ml-2" />
        <div>
          <h1 className="font-display text-2xl font-semibold text-zinc-900 sm:text-3xl">My orders</h1>
          <p className="mt-0.5 text-sm text-zinc-500">
            {loading ? 'Loading…' : `${totalElements} order${totalElements === 1 ? '' : 's'}`}
          </p>
        </div>
      </div>

      {loading ? (
        <div className="mt-6 space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <OrderCardSkeleton key={i} />
          ))}
        </div>
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : orders.length === 0 ? (
        <EmptyState
          icon={<BagIcon />}
          title="No orders yet"
          message="Once you place an order, it'll show up here."
          ctaLabel="Start shopping"
          ctaTo="/"
        />
      ) : (
        <ul className="mt-6 space-y-3">
          {orders.map((order, i) => (
            <li key={order.id} className="animate-fade-in-up" style={{ animationDelay: `${Math.min(i, 6) * 40}ms` }}>
              <Link
                to={`/orders/${order.id}`}
                className="flex items-start justify-between gap-3 rounded-xl border border-zinc-200 bg-white p-4 shadow-soft transition-all hover:-translate-y-0.5 hover:border-zinc-300 hover:shadow-elevated sm:p-5"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-zinc-900">{order.orderNumber}</p>
                  <p className="mt-0.5 text-xs text-zinc-500">
                    {formatDate(order.createdAt)} &middot; {order.itemCount} item{order.itemCount === 1 ? '' : 's'}
                  </p>
                  <div className="mt-2">
                    <OrderStatusBadge status={order.status} />
                  </div>
                </div>
                <div className="shrink-0 text-right">
                  <p className="text-sm font-semibold text-zinc-900">{formatPrice(order.totalAmount)}</p>
                  <span className="mt-1.5 inline-flex items-center gap-0.5 text-xs font-medium text-zinc-500">
                    View details
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="mt-8 flex items-center justify-center gap-1">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="min-h-[40px] rounded-full border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-40"
          >
            Prev
          </button>
          {pageNumbers.map((p) => (
            <button
              key={p}
              onClick={() => setPage(p)}
              className={`min-h-[40px] min-w-[40px] rounded-full px-3 py-1.5 text-sm font-medium transition-colors ${
                p === page ? 'bg-zinc-900 text-white' : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
              }`}
            >
              {p + 1}
            </button>
          ))}
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="min-h-[40px] rounded-full border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
