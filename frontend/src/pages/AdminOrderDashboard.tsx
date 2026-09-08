import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchAdminOrderDashboard, fetchAdminOrders } from '../api/adminOrders'
import type { AdminOrderQuery } from '../api/adminOrders'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { AdminOrderDashboardData, AdminOrderStatus, AdminOrderSummary, PaymentStatus } from '../types'

const PAGE_SIZE = 20

type OrderStatusFilter = '' | AdminOrderStatus
type PaymentStatusFilter = '' | 'PENDING' | 'SUCCESS' | 'FAILED'
type SortField = 'createdAt' | 'totalAmount' | 'status'
type SortDir = 'asc' | 'desc'

const ORDER_STATUS_OPTIONS: { value: OrderStatusFilter; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING_PAYMENT', label: 'Pending payment' },
  { value: 'PAYMENT_PROCESSING', label: 'Payment processing' },
  { value: 'PAYMENT_FAILED', label: 'Payment failed' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'PACKED', label: 'Packed' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'RETURNED', label: 'Returned' },
  { value: 'REFUNDED', label: 'Refunded' },
]

const PAYMENT_STATUS_OPTIONS: { value: PaymentStatusFilter; label: string }[] = [
  { value: '', label: 'All payment statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'SUCCESS', label: 'Success' },
  { value: 'FAILED', label: 'Failed' },
]

/** Icon-only "back to admin home" affordance - no text, matching the other admin screens' back links. */
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

export function orderStatusBadgeClasses(status: AdminOrderStatus) {
  switch (status) {
    case 'PENDING_PAYMENT':
    case 'PAYMENT_PROCESSING':
      return 'bg-amber-50 text-amber-700 border-amber-200'
    case 'PAYMENT_FAILED':
    case 'CANCELLED':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    case 'CONFIRMED':
    case 'PROCESSING':
    case 'PACKED':
      return 'bg-blue-50 text-blue-700 border-blue-200'
    case 'SHIPPED':
      return 'bg-violet-50 text-violet-700 border-violet-200'
    case 'DELIVERED':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    case 'RETURNED':
    case 'REFUNDED':
      return 'bg-zinc-100 text-zinc-600 border-zinc-200'
    default:
      return 'bg-zinc-100 text-zinc-600 border-zinc-200'
  }
}

export function orderStatusLabel(status: AdminOrderStatus) {
  return status.replace(/_/g, ' ')
}

function paymentStatusBadgeClasses(status: PaymentStatus) {
  switch (status) {
    case 'SUCCESS':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    case 'FAILED':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    case 'PENDING':
      return 'bg-amber-50 text-amber-700 border-amber-200'
    default:
      return 'bg-zinc-100 text-zinc-500 border-zinc-200'
  }
}

function formatDateTime(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminOrderDashboard() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view orders.</p>
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

  return <AdminOrderDashboardContent />
}

function AdminOrderDashboardContent() {
  // --- Dashboard summary ---
  const [dashboard, setDashboard] = useState<AdminOrderDashboardData | null>(null)
  const [dashboardLoading, setDashboardLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState<string | null>(null)

  const loadDashboard = useCallback(() => {
    setDashboardLoading(true)
    setDashboardError(null)
    fetchAdminOrderDashboard()
      .then(setDashboard)
      .catch((err) => setDashboardError(getErrorMessage(err)))
      .finally(() => setDashboardLoading(false))
  }, [])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  // --- Order table state ---
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [orderStatus, setOrderStatus] = useState<OrderStatusFilter>('')
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatusFilter>('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('')
  const [sort, setSort] = useState<SortField>('createdAt')
  const [dir, setDir] = useState<SortDir>('desc')

  const [rows, setRows] = useState<AdminOrderSummary[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [tableLoading, setTableLoading] = useState(true)
  const [tableError, setTableError] = useState<string | null>(null)

  // Reset to page 0 whenever a filter/sort/search changes.
  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, orderStatus, paymentStatus, dateFrom, dateTo, paymentMethod, sort, dir])

  const loadTable = useCallback(() => {
    setTableLoading(true)
    setTableError(null)
    const query: AdminOrderQuery = {
      page,
      size: PAGE_SIZE,
      q: debouncedSearch,
      orderStatus,
      paymentStatus,
      dateFrom,
      dateTo,
      paymentMethod,
      sort,
      dir,
    }
    fetchAdminOrders(query)
      .then((res) => {
        setRows(res.content)
        setTotalPages(res.totalPages)
        setTotalElements(res.totalElements)
      })
      .catch((err) => {
        setTableError(getErrorMessage(err))
        setRows([])
        setTotalPages(0)
        setTotalElements(0)
      })
      .finally(() => setTableLoading(false))
  }, [page, debouncedSearch, orderStatus, paymentStatus, dateFrom, dateTo, paymentMethod, sort, dir])

  useEffect(() => {
    loadTable()
  }, [loadTable])

  const refreshAll = useCallback(() => {
    loadDashboard()
    loadTable()
  }, [loadDashboard, loadTable])

  const toggleSort = (field: SortField) => {
    if (sort === field) {
      setDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSort(field)
      setDir(field === 'createdAt' ? 'desc' : 'asc')
    }
  }

  const sortIndicator = (field: SortField) => {
    if (sort !== field) return null
    return dir === 'asc' ? ' ↑' : ' ↓'
  }

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return []
    const windowSize = 5
    let start = Math.max(0, page - Math.floor(windowSize / 2))
    const end = Math.min(totalPages, start + windowSize)
    start = Math.max(0, end - windowSize)
    return Array.from({ length: end - start }, (_, i) => start + i)
  }, [page, totalPages])

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-2">
          <AdminHomeBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">Orders</h1>
            <p className="mt-1 text-sm text-zinc-500">Track, search, and filter customer orders.</p>
          </div>
        </div>
        <button
          type="button"
          onClick={refreshAll}
          className="mt-2 shrink-0 text-sm font-medium text-zinc-600 hover:text-zinc-900"
        >
          Refresh
        </button>
      </div>

      {dashboardError && (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {dashboardError}
        </div>
      )}

      {/* Status-bucket summary cards */}
      <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
        <SummaryCard
          label="Total"
          value={dashboard?.totalOrders}
          loading={dashboardLoading}
          onClick={() => setOrderStatus('')}
        />
        <SummaryCard
          label="Pending"
          value={dashboard?.pendingCount}
          loading={dashboardLoading}
          tone="amber"
          onClick={() => setOrderStatus('PENDING_PAYMENT')}
        />
        <SummaryCard
          label="Processing"
          value={dashboard?.processingCount}
          loading={dashboardLoading}
          tone="blue"
          onClick={() => setOrderStatus('PROCESSING')}
        />
        <SummaryCard
          label="Packed"
          value={dashboard?.packedCount}
          loading={dashboardLoading}
          tone="blue"
          onClick={() => setOrderStatus('PACKED')}
        />
        <SummaryCard
          label="Shipped"
          value={dashboard?.shippedCount}
          loading={dashboardLoading}
          tone="violet"
          onClick={() => setOrderStatus('SHIPPED')}
        />
        <SummaryCard
          label="Delivered"
          value={dashboard?.deliveredCount}
          loading={dashboardLoading}
          tone="emerald"
          onClick={() => setOrderStatus('DELIVERED')}
        />
        <SummaryCard
          label="Cancelled"
          value={dashboard?.cancelledCount}
          loading={dashboardLoading}
          tone="rose"
          onClick={() => setOrderStatus('CANCELLED')}
        />
        <SummaryCard
          label="Payment failed"
          value={dashboard?.paymentFailedCount}
          loading={dashboardLoading}
          tone="rose"
          onClick={() => setOrderStatus('PAYMENT_FAILED')}
        />
      </div>

      {/* Recent orders mini-list — only shown when the dashboard response includes it */}
      {dashboard && dashboard.recentOrders && dashboard.recentOrders.length > 0 && (
        <div className="mt-6 rounded-lg border border-zinc-200 bg-white p-4">
          <h3 className="text-sm font-semibold text-zinc-900">Recent orders</h3>
          {/* Fixed per-column widths (rather than flex-shrink/truncate) so the row has a real
              total width - on a narrow viewport that's wider than the screen, which is what
              makes it a horizontally scrollable strip instead of just squeezing/clipping. */}
          <div className="mt-2 overflow-x-auto">
            <ul className="min-w-[640px] divide-y divide-zinc-100">
              {dashboard.recentOrders.map((o) => (
                <li key={o.id} className="flex items-center gap-3 py-2 text-sm">
                  <Link to={`/admin/orders/${o.id}`} className="w-36 shrink-0 truncate font-medium text-zinc-800 hover:underline">
                    {o.orderNumber}
                  </Link>
                  <span className="w-32 shrink-0 truncate text-zinc-500">{o.customerName}</span>
                  <span className="w-40 shrink-0 text-zinc-500">{formatDateTime(o.createdAt)}</span>
                  <span className="w-20 shrink-0 text-right text-zinc-900">{formatPrice(o.totalAmount)}</span>
                  <span
                    className={`w-28 shrink-0 rounded-full border px-1.5 py-0.5 text-center text-[10px] font-medium ${orderStatusBadgeClasses(o.status)}`}
                  >
                    {orderStatusLabel(o.status)}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {/* Order table */}
      <section className="mt-8 border-t border-zinc-200 pt-6">
        <h2 className="text-lg font-semibold text-zinc-900">All orders</h2>

        <div className="mt-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="lg:w-72">
            <label htmlFor="order-search" className="sr-only">
              Search orders
            </label>
            <input
              id="order-search"
              type="search"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by order # or customer…"
              className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:flex lg:flex-wrap lg:items-center">
            <select
              value={orderStatus}
              onChange={(e) => setOrderStatus(e.target.value as OrderStatusFilter)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              {ORDER_STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <select
              value={paymentStatus}
              onChange={(e) => setPaymentStatus(e.target.value as PaymentStatusFilter)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              {PAYMENT_STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <input
              type="text"
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value)}
              placeholder="Payment method"
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            />
            <label className="flex items-center gap-1.5 text-xs text-zinc-500">
              From
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
              />
            </label>
            <label className="flex items-center gap-1.5 text-xs text-zinc-500">
              To
              <input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
              />
            </label>
          </div>
        </div>

        {tableError && (
          <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {tableError}
          </div>
        )}

        <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
          <table className="min-w-full divide-y divide-zinc-200 text-sm">
            <thead className="bg-zinc-50">
              <tr>
                <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Order #</th>
                <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Customer</th>
                <th
                  className="cursor-pointer select-none whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600"
                  onClick={() => toggleSort('createdAt')}
                >
                  Date{sortIndicator('createdAt')}
                </th>
                <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Items</th>
                <th
                  className="cursor-pointer select-none whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600"
                  onClick={() => toggleSort('totalAmount')}
                >
                  Total{sortIndicator('totalAmount')}
                </th>
                <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Payment</th>
                <th
                  className="cursor-pointer select-none whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600"
                  onClick={() => toggleSort('status')}
                >
                  Status{sortIndicator('status')}
                </th>
                <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Updated</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 bg-white">
              {tableLoading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={8} className="px-3 py-3">
                      <div className="h-4 animate-pulse rounded bg-zinc-100" />
                    </td>
                  </tr>
                ))
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-3 py-10 text-center text-zinc-500">
                    No orders match your filters.
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.id} className="hover:bg-zinc-50">
                    <td className="whitespace-nowrap px-3 py-2">
                      <Link to={`/admin/orders/${row.id}`} className="font-medium text-zinc-900 hover:underline">
                        {row.orderNumber}
                      </Link>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2">
                      <div className="text-zinc-800">{row.customerName}</div>
                      <div className="text-xs text-zinc-400">{row.customerContact}</div>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(row.createdAt)}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{row.itemCount}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">
                      {formatPrice(row.totalAmount)}
                    </td>
                    <td className="whitespace-nowrap px-3 py-2">
                      <span
                        className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${paymentStatusBadgeClasses(row.paymentStatus)}`}
                      >
                        {row.paymentStatus ?? 'NO PAYMENT'}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2">
                      <span
                        className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${orderStatusBadgeClasses(row.status)}`}
                      >
                        {orderStatusLabel(row.status)}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(row.updatedAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex items-center justify-between">
          <p className="text-xs text-zinc-500">
            {tableLoading ? 'Loading…' : `${totalElements} order${totalElements === 1 ? '' : 's'}`}
          </p>
          {totalPages > 1 && (
            <div className="flex items-center gap-1">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
              >
                Prev
              </button>
              {pageNumbers.map((p) => (
                <button
                  key={p}
                  onClick={() => setPage(p)}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                    p === page ? 'bg-zinc-900 text-white' : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
                  }`}
                >
                  {p + 1}
                </button>
              ))}
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </section>

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}

function SummaryCard({
  label,
  value,
  loading,
  tone = 'default',
  onClick,
}: {
  label: string
  value: number | undefined
  loading: boolean
  tone?: 'default' | 'amber' | 'rose' | 'blue' | 'violet' | 'emerald'
  onClick?: () => void
}) {
  const valueClasses =
    tone === 'amber'
      ? 'text-amber-600'
      : tone === 'rose'
        ? 'text-rose-600'
        : tone === 'blue'
          ? 'text-blue-600'
          : tone === 'violet'
            ? 'text-violet-600'
            : tone === 'emerald'
              ? 'text-emerald-600'
              : 'text-zinc-900'
  const cardClassName = `rounded-lg border border-zinc-200 bg-white px-3 py-2.5 text-left ${
    onClick ? 'cursor-pointer hover:border-zinc-300 hover:bg-zinc-50' : ''
  }`
  const content = (
    <>
      <p className="text-[11px] font-medium uppercase tracking-wide text-zinc-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${valueClasses}`}>
        {loading ? <span className="inline-block h-5 w-8 animate-pulse rounded bg-zinc-100 align-middle" /> : value ?? 0}
      </p>
    </>
  )
  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={cardClassName}>
        {content}
      </button>
    )
  }
  return <div className={cardClassName}>{content}</div>
}
