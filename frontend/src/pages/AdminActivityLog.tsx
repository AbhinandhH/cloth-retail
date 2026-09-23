import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchActivityLog } from '../api/adminActivityLog'
import type { ActivityLogQuery } from '../api/adminActivityLog'
import { getErrorMessage } from '../api/client'
import type { ActivityLogRow } from '../types'

const PAGE_SIZE = 20

const METHOD_OPTIONS = ['', 'POST', 'PUT', 'PATCH', 'DELETE'] as const

function methodBadgeClasses(method: string) {
  switch (method) {
    case 'POST':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    case 'DELETE':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    default:
      return 'bg-indigo-50 text-indigo-700 border-indigo-200'
  }
}

function formatDateTime(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

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

export default function AdminActivityLog() {
  const { isStoreAdmin } = useAuth()

  if (!isStoreAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view the activity log.</p>
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

  return <AdminActivityLogContent />
}

function AdminActivityLogContent() {
  const [page, setPage] = useState(0)
  const [module, setModule] = useState('')
  const [httpMethod, setHttpMethod] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')

  const [rows, setRows] = useState<ActivityLogRow[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setPage(0)
  }, [module, httpMethod, dateFrom, dateTo])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    const query: ActivityLogQuery = { page, size: PAGE_SIZE, module, httpMethod, dateFrom, dateTo }
    fetchActivityLog(query)
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
  }, [page, module, httpMethod, dateFrom, dateTo])

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
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Activity log</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Who changed what across the admin console, and when. Reads aren't logged, only changes.
          </p>
        </div>
      </div>

      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <input
          type="text"
          value={module}
          onChange={(e) => setModule(e.target.value)}
          placeholder="Module (e.g. products)…"
          className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500 sm:w-56"
        />
        <select
          value={httpMethod}
          onChange={(e) => setHttpMethod(e.target.value)}
          className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
        >
          {METHOD_OPTIONS.map((m) => (
            <option key={m} value={m}>
              {m === '' ? 'All actions' : m}
            </option>
          ))}
        </select>
        <div className="flex items-center gap-2 text-sm text-zinc-500">
          <label htmlFor="date-from">From</label>
          <input
            id="date-from"
            type="date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
            className="rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
          />
          <label htmlFor="date-to">To</label>
          <input
            id="date-to"
            type="date"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
            className="rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
          />
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
      )}

      <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Time</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Admin</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Module</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Action</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Path</th>
              <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 bg-white">
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={6} className="px-3 py-3">
                    <div className="h-4 animate-pulse rounded bg-zinc-100" />
                  </td>
                </tr>
              ))
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-3 py-10 text-center text-zinc-500">
                  No activity found.
                </td>
              </tr>
            ) : (
              rows.map((r) => (
                <tr key={r.id}>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(r.createdAt)}</td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-900">{r.actorName ?? '—'}</td>
                  <td className="whitespace-nowrap px-3 py-2 capitalize text-zinc-600">{r.module.replace(/-/g, ' ')}</td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <span className={`rounded-full border px-2 py-0.5 text-[10px] font-medium ${methodBadgeClasses(r.httpMethod)}`}>
                      {r.httpMethod}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 font-mono text-xs text-zinc-500">{r.path}</td>
                  <td
                    className={`whitespace-nowrap px-3 py-2 text-right font-semibold ${
                      r.statusCode >= 400 ? 'text-rose-600' : 'text-emerald-600'
                    }`}
                  >
                    {r.statusCode}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-xs text-zinc-500">{loading ? 'Loading…' : `${totalElements} record${totalElements === 1 ? '' : 's'}`}</p>
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
