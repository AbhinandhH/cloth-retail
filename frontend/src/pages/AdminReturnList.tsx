import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchAdminReturns } from '../api/adminReturns'
import type { AdminReturnQuery } from '../api/adminReturns'
import { getErrorMessage } from '../api/client'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { AdminReturnRow, EvidenceStatus, ReturnRequestStatus, ReturnRequestType } from '../types'

const PAGE_SIZE = 20

type TypeFilter = '' | ReturnRequestType
type StatusFilter = '' | ReturnRequestStatus

const TYPE_OPTIONS: { value: TypeFilter; label: string }[] = [
  { value: '', label: 'All types' },
  { value: 'SIZE_EXCHANGE', label: 'Size exchange' },
  { value: 'DAMAGED_PRODUCT', label: 'Damaged product' },
]

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING', label: 'Pending review' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'REFUNDED', label: 'Refunded' },
]

function statusBadgeClasses(status: ReturnRequestStatus) {
  switch (status) {
    case 'PENDING':
      return 'bg-amber-50 text-amber-700 border-amber-200'
    case 'APPROVED':
      return 'bg-sky-50 text-sky-700 border-sky-200'
    case 'REJECTED':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    case 'REFUNDED':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    default:
      return 'bg-zinc-100 text-zinc-600 border-zinc-200'
  }
}

function evidenceBadgeClasses(status: EvidenceStatus) {
  return status === 'SUBMITTED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'
}

function typeLabel(type: ReturnRequestType) {
  return type === 'SIZE_EXCHANGE' ? 'Size exchange' : 'Damaged product'
}

function formatDateTime(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

/** Icon-only "back to admin home" affordance, matching the other admin list screens. */
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

export default function AdminReturnList() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view return requests.</p>
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

  return <AdminReturnListContent />
}

function AdminReturnListContent() {
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [requestType, setRequestType] = useState<TypeFilter>('')
  const [status, setStatus] = useState<StatusFilter>('')

  const [rows, setRows] = useState<AdminReturnRow[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, requestType, status])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    const query: AdminReturnQuery = { page, size: PAGE_SIZE, q: debouncedSearch, requestType, status }
    fetchAdminReturns(query)
      .then((res) => {
        setRows(res.content)
        setTotalPages(res.totalPages)
        setTotalElements(res.totalElements)
      })
      .catch((err) => {
        setError(getErrorMessage(err))
        setRows([])
        setTotalPages(0)
        setTotalElements(0)
      })
      .finally(() => setLoading(false))
  }, [page, debouncedSearch, requestType, status])

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
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-2">
          <AdminHomeBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">Returns &amp; exchanges</h1>
            <p className="mt-1 text-sm text-zinc-500">Review size-exchange and damaged-product requests.</p>
          </div>
        </div>
        <button type="button" onClick={load} className="mt-2 shrink-0 text-sm font-medium text-zinc-600 hover:text-zinc-900">
          Refresh
        </button>
      </div>

      <div className="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="lg:w-72">
          <label htmlFor="return-search" className="sr-only">
            Search return requests
          </label>
          <input
            id="return-search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by order #, product, or customer…"
            className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap sm:items-center">
          <select
            value={requestType}
            onChange={(e) => setRequestType(e.target.value as TypeFilter)}
            className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
          >
            {TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as StatusFilter)}
            className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
      )}

      <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Order #</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Customer</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Product</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Type</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Status</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Evidence</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Date</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 bg-white">
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={7} className="px-3 py-3">
                    <div className="h-4 animate-pulse rounded bg-zinc-100" />
                  </td>
                </tr>
              ))
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-3 py-10 text-center text-zinc-500">
                  No return requests match your filters.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="hover:bg-zinc-50">
                  <td className="whitespace-nowrap px-3 py-2">
                    <Link to={`/admin/returns/${row.id}`} className="font-medium text-zinc-900 hover:underline">
                      {row.orderNumber}
                    </Link>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-800">{row.customerName}</td>
                  <td className="px-3 py-2">
                    <div className="max-w-[220px] truncate text-zinc-800">{row.productName}</div>
                    <div className="text-xs text-zinc-400">
                      {row.originalSizeName}
                      {row.requestedSizeName ? ` → ${row.requestedSizeName}` : ''}
                    </div>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-700">{typeLabel(row.requestType)}</td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <span className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${statusBadgeClasses(row.status)}`}>
                      {row.status}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2">
                    {row.requestType === 'DAMAGED_PRODUCT' ? (
                      <span className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${evidenceBadgeClasses(row.evidenceStatus)}`}>
                        {row.evidenceStatus === 'SUBMITTED' ? 'Submitted' : 'Awaiting video'}
                      </span>
                    ) : (
                      <span className="text-xs text-zinc-300">—</span>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(row.createdAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-xs text-zinc-500">
          {loading ? 'Loading…' : `${totalElements} request${totalElements === 1 ? '' : 's'}`}
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

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
