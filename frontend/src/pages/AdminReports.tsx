import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCategories } from '../context/MasterDataContext'
import {
  exportReportPdf,
  fetchCategorySales,
  fetchCustomerSales,
  fetchGstReport,
  fetchProductSales,
  fetchSalesSummary,
  fetchStockReport,
} from '../api/reports'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { downloadBlob } from '../lib/downloadBlob'
import ProductCombobox from '../components/ProductCombobox'
import CustomerCombobox from '../components/CustomerCombobox'
import type {
  CategorySalesRow,
  CustomerSalesRow,
  GstReportRow,
  InventoryVariantRow,
  ProductSalesRow,
  SalesSummaryRow,
  StockStatus,
} from '../types'

const PAGE_SIZE = 20

type ReportKey = 'sales-summary' | 'product-sales' | 'category-sales' | 'customer-sales' | 'stock' | 'gst'

const REPORT_TABS: { key: ReportKey; label: string }[] = [
  { key: 'sales-summary', label: 'Sales Summary' },
  { key: 'product-sales', label: 'Product-wise' },
  { key: 'category-sales', label: 'Category-wise' },
  { key: 'customer-sales', label: 'Customer-wise' },
  { key: 'stock', label: 'Stock' },
  { key: 'gst', label: 'GST / Tax' },
]

type StockStatusFilter = '' | StockStatus

const STOCK_STATUS_OPTIONS: { value: StockStatusFilter; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'LOW_STOCK', label: 'Low stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
]

function stockStatusBadgeClasses(status: StockStatus) {
  switch (status) {
    case 'OUT_OF_STOCK':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    case 'LOW_STOCK':
      return 'bg-amber-50 text-amber-700 border-amber-200'
    default:
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
  }
}

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoIso(days: number) {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

/** "YYYY-MM-DD" -> start-of-day ISO instant, for the dateFrom API param. */
function toRangeStart(dateStr: string) {
  return new Date(`${dateStr}T00:00:00`).toISOString()
}

/** "YYYY-MM-DD" -> end-of-day ISO instant, for the dateTo API param. */
function toRangeEnd(dateStr: string) {
  return new Date(`${dateStr}T23:59:59.999`).toISOString()
}

function formatDate(value: string) {
  const d = new Date(value.length <= 10 ? `${value}T00:00:00` : value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** Icon-only "back to admin home" affordance, matching the other admin screens' back links. */
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

function Pagination({
  page,
  totalPages,
  totalElements,
  label,
  loading,
  onChange,
}: {
  page: number
  totalPages: number
  totalElements: number
  label: string
  loading: boolean
  onChange: (page: number) => void
}) {
  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return []
    const windowSize = 5
    let start = Math.max(0, page - Math.floor(windowSize / 2))
    const end = Math.min(totalPages, start + windowSize)
    start = Math.max(0, end - windowSize)
    return Array.from({ length: end - start }, (_, i) => start + i)
  }, [page, totalPages])

  return (
    <div className="mt-3 flex items-center justify-between">
      <p className="text-xs text-zinc-500">{loading ? 'Loading…' : `${totalElements} ${label}${totalElements === 1 ? '' : 's'}`}</p>
      {totalPages > 1 && (
        <div className="flex items-center gap-1">
          <button
            onClick={() => onChange(Math.max(0, page - 1))}
            disabled={page === 0}
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
          >
            Prev
          </button>
          {pageNumbers.map((p) => (
            <button
              key={p}
              onClick={() => onChange(p)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                p === page ? 'bg-zinc-900 text-white' : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
              }`}
            >
              {p + 1}
            </button>
          ))}
          <button
            onClick={() => onChange(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}

/** A small, dependency-free bar chart, same technique as AdminDashboard.tsx's own sales-overview chart. */
function RevenueChart({ points }: { points: SalesSummaryRow[] }) {
  const width = 700
  const height = 160
  const padding = 20
  const max = Math.max(1, ...points.map((p) => p.revenue))
  const barGap = 4
  const barWidth = points.length > 0 ? (width - padding * 2 - barGap * (points.length - 1)) / points.length : 0

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="w-full" preserveAspectRatio="xMidYMid meet" role="img" aria-label="Revenue over the selected range">
      {points.map((p, i) => {
        const barHeight = max === 0 ? 0 : Math.max(2, (p.revenue / max) * (height - padding))
        const x = padding + i * (barWidth + barGap)
        const y = height - barHeight
        return (
          <rect key={p.date} x={x} y={y} width={Math.max(1, barWidth)} height={barHeight} rx={2} className="fill-zinc-800">
            <title>
              {formatDate(p.date)}: {formatPrice(p.revenue)} ({p.orderCount} order{p.orderCount === 1 ? '' : 's'})
            </title>
          </rect>
        )
      })}
    </svg>
  )
}

function DownloadPdfButton({ onDownload }: { onDownload: () => Promise<void> }) {
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        disabled={downloading}
        onClick={async () => {
          setDownloading(true)
          setError(null)
          try {
            await onDownload()
          } catch (err) {
            setError(getErrorMessage(err))
          } finally {
            setDownloading(false)
          }
        }}
        className="flex items-center gap-1.5 rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-zinc-800 disabled:opacity-50"
      >
        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M12 4v12m0 0l-4-4m4 4l4-4M4 20h16" />
        </svg>
        {downloading ? 'Preparing…' : 'Download PDF'}
      </button>
      {error && <span className="text-xs text-rose-600">{error}</span>}
    </div>
  )
}

export default function AdminReports() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view reports.</p>
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

  return <AdminReportsContent />
}

function AdminReportsContent() {
  const { data: categories } = useCategories()
  const [activeTab, setActiveTab] = useState<ReportKey>('sales-summary')

  // Shared filters - not every tab uses every one.
  const [dateFrom, setDateFrom] = useState(daysAgoIso(29))
  const [dateTo, setDateTo] = useState(todayIso())
  const [productId, setProductId] = useState<number | string | null>(null)
  const [productLabel, setProductLabel] = useState<string | null>(null)
  const [customerId, setCustomerId] = useState<number | string | null>(null)
  const [customerLabel, setCustomerLabel] = useState<string | null>(null)
  const [categoryId, setCategoryId] = useState('')
  const [stockStatus, setStockStatus] = useState<StockStatusFilter>('')

  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const [summaryRows, setSummaryRows] = useState<SalesSummaryRow[]>([])
  const [summaryTotals, setSummaryTotals] = useState({ totalRevenue: 0, totalOrders: 0, avgOrderValue: 0 })
  const [productRows, setProductRows] = useState<ProductSalesRow[]>([])
  const [categoryRows, setCategoryRows] = useState<CategorySalesRow[]>([])
  const [customerRows, setCustomerRows] = useState<CustomerSalesRow[]>([])
  const [stockRows, setStockRows] = useState<InventoryVariantRow[]>([])
  const [gstRows, setGstRows] = useState<GstReportRow[]>([])
  const [gstTotals, setGstTotals] = useState({ totalCgst: 0, totalSgst: 0, totalRevenue: 0 })

  // Reset to page 0 whenever the active tab or a relevant filter changes.
  useEffect(() => {
    setPage(0)
  }, [activeTab, dateFrom, dateTo, productId, categoryId, customerId, stockStatus])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    const from = toRangeStart(dateFrom)
    const to = toRangeEnd(dateTo)

    switch (activeTab) {
      case 'sales-summary':
        fetchSalesSummary({ dateFrom: from, dateTo: to })
          .then((res) => {
            setSummaryRows(res.rows)
            setSummaryTotals({ totalRevenue: res.totalRevenue, totalOrders: res.totalOrders, avgOrderValue: res.avgOrderValue })
            setTotalElements(res.totalOrders)
            setTotalPages(1)
          })
          .catch((err) => setError(getErrorMessage(err)))
          .finally(() => setLoading(false))
        return
      case 'product-sales':
        fetchProductSales({ dateFrom: from, dateTo: to, productId: productId ?? undefined, page, size: PAGE_SIZE })
          .then((res) => {
            setProductRows(res.content)
            setTotalPages(res.totalPages)
            setTotalElements(res.totalElements)
          })
          .catch((err) => setError(getErrorMessage(err)))
          .finally(() => setLoading(false))
        return
      case 'category-sales':
        fetchCategorySales({
          dateFrom: from,
          dateTo: to,
          categoryId: categoryId || undefined,
          page,
          size: PAGE_SIZE,
        })
          .then((res) => {
            setCategoryRows(res.content)
            setTotalPages(res.totalPages)
            setTotalElements(res.totalElements)
          })
          .catch((err) => setError(getErrorMessage(err)))
          .finally(() => setLoading(false))
        return
      case 'customer-sales':
        fetchCustomerSales({ dateFrom: from, dateTo: to, customerId: customerId ?? undefined, page, size: PAGE_SIZE })
          .then((res) => {
            setCustomerRows(res.content)
            setTotalPages(res.totalPages)
            setTotalElements(res.totalElements)
          })
          .catch((err) => setError(getErrorMessage(err)))
          .finally(() => setLoading(false))
        return
      case 'stock':
        fetchStockReport({
          categoryId: categoryId || undefined,
          productId: productId ?? undefined,
          stockStatus: stockStatus || undefined,
          page,
          size: PAGE_SIZE,
        })
          .then((res) => {
            setStockRows(res.content)
            setTotalPages(res.totalPages)
            setTotalElements(res.totalElements)
          })
          .catch((err) => setError(getErrorMessage(err)))
          .finally(() => setLoading(false))
        return
      case 'gst':
        fetchGstReport({ dateFrom: from, dateTo: to, page, size: PAGE_SIZE })
          .then((res) => {
            setGstRows(res.page.content)
            setGstTotals({ totalCgst: res.totalCgst, totalSgst: res.totalSgst, totalRevenue: res.totalRevenue })
            setTotalPages(res.page.totalPages)
            setTotalElements(res.page.totalElements)
          })
          .catch((err) => setError(getErrorMessage(err)))
          .finally(() => setLoading(false))
        return
    }
  }, [activeTab, dateFrom, dateTo, productId, categoryId, customerId, stockStatus, page])

  useEffect(() => {
    load()
  }, [load])

  const handleDownload = async () => {
    const from = toRangeStart(dateFrom)
    const to = toRangeEnd(dateTo)
    let blob: Blob
    switch (activeTab) {
      case 'sales-summary':
        blob = await exportReportPdf('sales-summary', { dateFrom: from, dateTo: to })
        break
      case 'product-sales':
        blob = await exportReportPdf('product-sales', { dateFrom: from, dateTo: to, productId: productId ?? undefined })
        break
      case 'category-sales':
        blob = await exportReportPdf('category-sales', { dateFrom: from, dateTo: to, categoryId: categoryId || undefined })
        break
      case 'customer-sales':
        blob = await exportReportPdf('customer-sales', { dateFrom: from, dateTo: to, customerId: customerId ?? undefined })
        break
      case 'stock':
        blob = await exportReportPdf('stock', {
          categoryId: categoryId || undefined,
          productId: productId ?? undefined,
          stockStatus: stockStatus || undefined,
        })
        break
      case 'gst':
        blob = await exportReportPdf('gst', { dateFrom: from, dateTo: to })
        break
    }
    downloadBlob(blob, `${activeTab}-${todayIso()}.pdf`)
  }

  const showDateRange = activeTab !== 'stock'
  const showProductFilter = activeTab === 'product-sales' || activeTab === 'stock'
  const showCategoryFilter = activeTab === 'category-sales' || activeTab === 'stock'
  const showCustomerFilter = activeTab === 'customer-sales'
  const showStockStatusFilter = activeTab === 'stock'

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-2">
          <AdminHomeBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">Reports</h1>
            <p className="mt-1 text-sm text-zinc-500">Sales, customer, stock, and tax reports - filter, view, and export.</p>
          </div>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap gap-1 border-b border-zinc-200">
        {REPORT_TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={`rounded-t-md px-3 py-2 text-sm font-medium ${
              activeTab === tab.key
                ? 'border-b-2 border-zinc-900 text-zinc-900'
                : 'text-zinc-500 hover:text-zinc-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4 shadow-sm sm:p-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-wrap items-end gap-3">
            {showDateRange && (
              <>
                <label className="flex flex-col gap-1 text-xs text-zinc-500">
                  From
                  <input
                    type="date"
                    value={dateFrom}
                    max={dateTo}
                    onChange={(e) => setDateFrom(e.target.value)}
                    className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs text-zinc-500">
                  To
                  <input
                    type="date"
                    value={dateTo}
                    min={dateFrom}
                    max={todayIso()}
                    onChange={(e) => setDateTo(e.target.value)}
                    className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
                  />
                </label>
              </>
            )}
            {showCategoryFilter && (
              <label className="flex flex-col gap-1 text-xs text-zinc-500">
                Category
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
                >
                  <option value="">All categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </label>
            )}
            {showProductFilter && (
              <div className="w-56">
                <ProductCombobox
                  value={productId}
                  label={productLabel}
                  onChange={(id, label) => {
                    setProductId(id)
                    setProductLabel(label)
                  }}
                />
              </div>
            )}
            {showCustomerFilter && (
              <div className="w-56">
                <CustomerCombobox
                  value={customerId}
                  label={customerLabel}
                  onChange={(id, label) => {
                    setCustomerId(id)
                    setCustomerLabel(label)
                  }}
                />
              </div>
            )}
            {showStockStatusFilter && (
              <label className="flex flex-col gap-1 text-xs text-zinc-500">
                Stock status
                <select
                  value={stockStatus}
                  onChange={(e) => setStockStatus(e.target.value as StockStatusFilter)}
                  className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
                >
                  {STOCK_STATUS_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>
          <DownloadPdfButton onDownload={handleDownload} />
        </div>

        {error && (
          <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
        )}

        {/* --- Sales Summary --- */}
        {activeTab === 'sales-summary' && (
          <div className="mt-4">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <SummaryTile label="Total revenue" value={formatPrice(summaryTotals.totalRevenue)} />
              <SummaryTile label="Total orders" value={String(summaryTotals.totalOrders)} />
              <SummaryTile label="Avg order value" value={formatPrice(summaryTotals.avgOrderValue)} />
            </div>
            <div className="mt-5 rounded-lg border border-zinc-200 p-4">
              {loading ? <div className="h-40 animate-pulse rounded bg-zinc-100" /> : <RevenueChart points={summaryRows} />}
            </div>
            <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Date</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Orders</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Revenue</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {loading ? (
                    <SkeletonRows cols={3} />
                  ) : summaryRows.length === 0 ? (
                    <EmptyRow cols={3} />
                  ) : (
                    summaryRows.map((r) => (
                      <tr key={r.date} className="hover:bg-zinc-50">
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-700">{formatDate(r.date)}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{r.orderCount}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">{formatPrice(r.revenue)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* --- Product-wise Sales --- */}
        {activeTab === 'product-sales' && (
          <div className="mt-4">
            <div className="overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">SKU</th>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Product</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Units sold</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Revenue</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {loading ? (
                    <SkeletonRows cols={4} />
                  ) : productRows.length === 0 ? (
                    <EmptyRow cols={4} />
                  ) : (
                    productRows.map((r) => (
                      <tr key={r.sku} className="hover:bg-zinc-50">
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{r.sku}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-800">{r.productName}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{r.quantitySold}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">{formatPrice(r.revenue)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} label="product" loading={loading} onChange={setPage} />
          </div>
        )}

        {/* --- Category-wise Sales --- */}
        {activeTab === 'category-sales' && (
          <div className="mt-4">
            <div className="overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Category</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Units sold</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Revenue</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {loading ? (
                    <SkeletonRows cols={3} />
                  ) : categoryRows.length === 0 ? (
                    <EmptyRow cols={3} />
                  ) : (
                    categoryRows.map((r) => (
                      <tr key={r.categoryName} className="hover:bg-zinc-50">
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-800">{r.categoryName}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{r.quantitySold}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">{formatPrice(r.revenue)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} label="category" loading={loading} onChange={setPage} />
          </div>
        )}

        {/* --- Customer-wise Sales --- */}
        {activeTab === 'customer-sales' && (
          <div className="mt-4">
            <div className="overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Customer</th>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Email</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Orders</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Total spent</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {loading ? (
                    <SkeletonRows cols={4} />
                  ) : customerRows.length === 0 ? (
                    <EmptyRow cols={4} />
                  ) : (
                    customerRows.map((r) => (
                      <tr key={r.customerProfileId} className="hover:bg-zinc-50">
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-800">{r.fullName}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{r.email}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{r.orderCount}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">{formatPrice(r.totalSpent)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} label="customer" loading={loading} onChange={setPage} />
          </div>
        )}

        {/* --- Stock --- */}
        {activeTab === 'stock' && (
          <div className="mt-4">
            <div className="overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">SKU</th>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Product</th>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Variant</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Stock</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Reserved</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Available</th>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {loading ? (
                    <SkeletonRows cols={7} />
                  ) : stockRows.length === 0 ? (
                    <EmptyRow cols={7} />
                  ) : (
                    stockRows.map((r) => (
                      <tr key={r.variantId} className="hover:bg-zinc-50">
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{r.sku}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-800">{r.productName}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-600">
                          {r.colorName} / {r.sizeName}
                        </td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{r.stockQuantity}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{r.reservedQuantity}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">{r.availableQuantity}</td>
                        <td className="whitespace-nowrap px-3 py-2">
                          <span className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${stockStatusBadgeClasses(r.stockStatus)}`}>
                            {r.stockStatus.replace('_', ' ')}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} label="variant" loading={loading} onChange={setPage} />
          </div>
        )}

        {/* --- GST / Tax --- */}
        {activeTab === 'gst' && (
          <div className="mt-4">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <SummaryTile label="Total CGST" value={formatPrice(gstTotals.totalCgst)} />
              <SummaryTile label="Total SGST" value={formatPrice(gstTotals.totalSgst)} />
              <SummaryTile label="Total revenue" value={formatPrice(gstTotals.totalRevenue)} />
            </div>
            <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Order</th>
                    <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Date</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">CGST</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">SGST</th>
                    <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {loading ? (
                    <SkeletonRows cols={5} />
                  ) : gstRows.length === 0 ? (
                    <EmptyRow cols={5} />
                  ) : (
                    gstRows.map((r) => (
                      <tr key={r.orderNumber} className="hover:bg-zinc-50">
                        <td className="whitespace-nowrap px-3 py-2 font-medium text-zinc-900">{r.orderNumber}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDate(r.createdAt)}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{formatPrice(r.cgstAmount)}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{formatPrice(r.sgstAmount)}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">{formatPrice(r.totalAmount)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} label="order" loading={loading} onChange={setPage} />
          </div>
        )}
      </section>

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}

function SummaryTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-3">
      <p className="text-xs text-zinc-500">{label}</p>
      <p className="mt-1 text-lg font-semibold text-zinc-900">{value}</p>
    </div>
  )
}

function SkeletonRows({ cols }: { cols: number }) {
  return (
    <>
      {Array.from({ length: 6 }).map((_, i) => (
        <tr key={i}>
          <td colSpan={cols} className="px-3 py-3">
            <div className="h-4 animate-pulse rounded bg-zinc-100" />
          </td>
        </tr>
      ))}
    </>
  )
}

function EmptyRow({ cols }: { cols: number }) {
  return (
    <tr>
      <td colSpan={cols} className="px-3 py-10 text-center text-zinc-500">
        No records match your filters.
      </td>
    </tr>
  )
}
