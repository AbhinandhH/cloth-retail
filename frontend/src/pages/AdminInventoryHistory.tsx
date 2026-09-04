import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchDamages, fetchTransactions } from '../api/adminInventory'
import type { DamageQuery, TransactionQuery } from '../api/adminInventory'
import { getErrorMessage } from '../api/client'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { InventoryDamageRow, InventoryTransactionRow, InventoryTransactionType } from '../types'

const PAGE_SIZE = 20

type Tab = 'transactions' | 'damages'

const TRANSACTION_TYPE_OPTIONS: { value: '' | InventoryTransactionType; label: string }[] = [
  { value: '', label: 'All types' },
  { value: 'PURCHASE_IN', label: 'Purchase in' },
  { value: 'SALE_OUT', label: 'Sale out' },
  { value: 'RETURN_IN', label: 'Return in' },
  { value: 'ADJUSTMENT', label: 'Adjustment' },
  { value: 'CANCEL_REVERSAL', label: 'Cancel reversal' },
  { value: 'DAMAGE', label: 'Damage' },
]

function typeBadgeClasses(type: InventoryTransactionType) {
  switch (type) {
    case 'PURCHASE_IN':
    case 'RETURN_IN':
    case 'CANCEL_REVERSAL':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    case 'SALE_OUT':
    case 'DAMAGE':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    default:
      return 'bg-zinc-100 text-zinc-700 border-zinc-200'
  }
}

function formatDateTime(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminInventoryHistory() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view inventory history.</p>
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

  return <AdminInventoryHistoryContent />
}

function AdminInventoryHistoryContent() {
  const [tab, setTab] = useState<Tab>('transactions')

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Inventory history</h1>
          <p className="mt-1 text-sm text-zinc-500">Stock movements and damage records.</p>
        </div>
        <div className="flex items-center gap-4">
          <Link to="/admin/inventory" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
            &larr; Inventory
          </Link>
          <Link to="/admin" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
            Admin home
          </Link>
        </div>
      </div>

      <div className="mt-6 flex gap-1 border-b border-zinc-200">
        <button
          type="button"
          onClick={() => setTab('transactions')}
          className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
            tab === 'transactions' ? 'border-zinc-900 text-zinc-900' : 'border-transparent text-zinc-500 hover:text-zinc-700'
          }`}
        >
          Stock movements
        </button>
        <button
          type="button"
          onClick={() => setTab('damages')}
          className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
            tab === 'damages' ? 'border-zinc-900 text-zinc-900' : 'border-transparent text-zinc-500 hover:text-zinc-700'
          }`}
        >
          Damage history
        </button>
      </div>

      <div className="mt-6">{tab === 'transactions' ? <TransactionsPanel /> : <DamagesPanel />}</div>
    </div>
  )
}

function TransactionsPanel() {
  const [page, setPage] = useState(0)
  const [skuInput, setSkuInput] = useState('')
  const debouncedSku = useDebouncedValue(skuInput, 400)
  const [type, setType] = useState<'' | InventoryTransactionType>('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')

  const [rows, setRows] = useState<InventoryTransactionRow[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setPage(0)
  }, [debouncedSku, type, dateFrom, dateTo])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    // The backend contract filters transactions by variantId, not a free-text
    // SKU search — client-side SKU substring filtering is applied to the
    // fetched page below since there's no dedicated `sku`/`q` query param.
    const query: TransactionQuery = { page, size: PAGE_SIZE, type, dateFrom, dateTo }
    fetchTransactions(query)
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
  }, [page, type, dateFrom, dateTo])

  useEffect(() => {
    load()
  }, [load])

  const filteredRows = useMemo(() => {
    const q = debouncedSku.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((r) => r.sku.toLowerCase().includes(q) || r.productName.toLowerCase().includes(q))
  }, [rows, debouncedSku])

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return []
    const windowSize = 5
    let start = Math.max(0, page - Math.floor(windowSize / 2))
    const end = Math.min(totalPages, start + windowSize)
    start = Math.max(0, end - windowSize)
    return Array.from({ length: end - start }, (_, i) => start + i)
  }, [page, totalPages])

  return (
    <div>
      <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <input
          type="search"
          value={skuInput}
          onChange={(e) => setSkuInput(e.target.value)}
          placeholder="Search by product/SKU on this page…"
          className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500 sm:w-64"
        />
        <select
          value={type}
          onChange={(e) => setType(e.target.value as '' | InventoryTransactionType)}
          className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
        >
          {TRANSACTION_TYPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
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
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Date</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Product / SKU</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Type</th>
              <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Qty change</th>
              <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Prev &rarr; New</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Reason</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Performed by</th>
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
            ) : filteredRows.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-3 py-10 text-center text-zinc-500">
                  No transactions found.
                </td>
              </tr>
            ) : (
              filteredRows.map((t) => (
                <tr key={t.id}>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(t.createdAt)}</td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <p className="font-medium text-zinc-900">{t.productName}</p>
                    <p className="text-xs text-zinc-400">{t.sku}</p>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <span className={`rounded-full border px-2 py-0.5 text-[10px] font-medium ${typeBadgeClasses(t.type)}`}>
                      {t.type.replace('_', ' ')}
                    </span>
                  </td>
                  <td className={`whitespace-nowrap px-3 py-2 text-right font-semibold ${t.quantity >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {t.quantity >= 0 ? '+' : ''}
                    {t.quantity}
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-600">
                    {t.previousQuantity} &rarr; {t.newQuantity}
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-600">{t.reason ?? '—'}</td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-600">{t.performedByName ?? '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-xs text-zinc-500">
          {loading ? 'Loading…' : `${totalElements} transaction${totalElements === 1 ? '' : 's'}`}
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
    </div>
  )
}

function DamagesPanel() {
  const [page, setPage] = useState(0)
  const [skuInput, setSkuInput] = useState('')
  const debouncedSku = useDebouncedValue(skuInput, 400)

  const [rows, setRows] = useState<InventoryDamageRow[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setPage(0)
  }, [debouncedSku])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    // The backend contract filters damages by variantId, not a free-text SKU
    // search — client-side SKU/product substring filtering is applied below.
    const query: DamageQuery = { page, size: PAGE_SIZE }
    fetchDamages(query)
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
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  const filteredRows = useMemo(() => {
    const q = debouncedSku.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((r) => r.sku.toLowerCase().includes(q) || r.productName.toLowerCase().includes(q))
  }, [rows, debouncedSku])

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return []
    const windowSize = 5
    let start = Math.max(0, page - Math.floor(windowSize / 2))
    const end = Math.min(totalPages, start + windowSize)
    start = Math.max(0, end - windowSize)
    return Array.from({ length: end - start }, (_, i) => start + i)
  }, [page, totalPages])

  return (
    <div>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          type="search"
          value={skuInput}
          onChange={(e) => setSkuInput(e.target.value)}
          placeholder="Search by product/SKU on this page…"
          className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500 sm:w-64"
        />
      </div>

      {error && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
      )}

      <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Date</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Product / SKU</th>
              <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Qty</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Reason</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Notes</th>
              <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Reported by</th>
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
            ) : filteredRows.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-3 py-10 text-center text-zinc-500">
                  No damage records found.
                </td>
              </tr>
            ) : (
              filteredRows.map((d) => (
                <tr key={d.id}>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(d.createdAt)}</td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <p className="font-medium text-zinc-900">{d.productName}</p>
                    <p className="text-xs text-zinc-400">{d.sku}</p>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-right font-semibold text-rose-600">-{d.quantity}</td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-600">{d.reason}</td>
                  <td className="max-w-xs truncate px-3 py-2 text-zinc-600" title={d.notes ?? undefined}>
                    {d.notes ?? '—'}
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-600">{d.reportedByName ?? '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-xs text-zinc-500">
          {loading ? 'Loading…' : `${totalElements} record${totalElements === 1 ? '' : 's'}`}
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
    </div>
  )
}
