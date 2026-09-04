import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchCategories, fetchColors, fetchSizes } from '../api/products'
import { fetchInventoryDashboard, fetchInventoryVariants } from '../api/adminInventory'
import type { VariantQuery } from '../api/adminInventory'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import StockAdjustmentModal from '../components/StockAdjustmentModal'
import MarkDamagedModal from '../components/MarkDamagedModal'
import type {
  Category,
  Color,
  InventoryDashboard,
  InventoryVariantRow,
  ProductStatus,
  Size,
  StockStatus,
} from '../types'

const PAGE_SIZE = 20

type StockStatusFilter = '' | StockStatus
type ProductStatusFilter = '' | ProductStatus
type SortField = 'name' | 'stock' | 'updatedAt'
type SortDir = 'asc' | 'desc'

const STOCK_STATUS_OPTIONS: { value: StockStatusFilter; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'LOW_STOCK', label: 'Low stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
]

const PRODUCT_STATUS_OPTIONS: { value: ProductStatusFilter; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'ARCHIVED', label: 'Archived' },
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

function availableQuantityClasses(row: InventoryVariantRow) {
  if (row.availableQuantity <= 0) return 'text-rose-600 font-semibold'
  if (row.lowStockThreshold !== null && row.availableQuantity <= row.lowStockThreshold) {
    return 'text-amber-600 font-semibold'
  }
  return 'text-zinc-900 font-semibold'
}

function formatDateTime(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminInventoryDashboard() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view inventory.</p>
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

  return <AdminInventoryDashboardContent />
}

function AdminInventoryDashboardContent() {
  // --- Filter option lists (public read endpoints) ---
  const [categories, setCategories] = useState<Category[]>([])
  const [sizes, setSizes] = useState<Size[]>([])
  const [colors, setColors] = useState<Color[]>([])

  useEffect(() => {
    fetchCategories().then(setCategories).catch(() => setCategories([]))
    fetchSizes().then(setSizes).catch(() => setSizes([]))
    fetchColors().then(setColors).catch(() => setColors([]))
  }, [])

  // --- Dashboard summary ---
  const [dashboard, setDashboard] = useState<InventoryDashboard | null>(null)
  const [dashboardLoading, setDashboardLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState<string | null>(null)

  const loadDashboard = useCallback(() => {
    setDashboardLoading(true)
    setDashboardError(null)
    fetchInventoryDashboard()
      .then(setDashboard)
      .catch((err) => setDashboardError(getErrorMessage(err)))
      .finally(() => setDashboardLoading(false))
  }, [])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  // --- Stock table state ---
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [categoryId, setCategoryId] = useState('')
  const [colorId, setColorId] = useState('')
  const [sizeId, setSizeId] = useState('')
  const [stockStatus, setStockStatus] = useState<StockStatusFilter>('')
  const [productStatus, setProductStatus] = useState<ProductStatusFilter>('')
  const [sort, setSort] = useState<SortField>('name')
  const [dir, setDir] = useState<SortDir>('asc')

  const [rows, setRows] = useState<InventoryVariantRow[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [tableLoading, setTableLoading] = useState(true)
  const [tableError, setTableError] = useState<string | null>(null)

  // Reset to page 0 whenever a filter/sort/search changes.
  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, categoryId, colorId, sizeId, stockStatus, productStatus, sort, dir])

  const loadTable = useCallback(() => {
    setTableLoading(true)
    setTableError(null)
    const query: VariantQuery = {
      page,
      size: PAGE_SIZE,
      q: debouncedSearch,
      categoryId,
      colorId,
      sizeId,
      stockStatus,
      productStatus,
      sort,
      dir,
    }
    fetchInventoryVariants(query)
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
  }, [page, debouncedSearch, categoryId, colorId, sizeId, stockStatus, productStatus, sort, dir])

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
      setDir('asc')
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

  // --- Modals ---
  const [adjustingVariant, setAdjustingVariant] = useState<InventoryVariantRow | null>(null)
  const [damagingVariant, setDamagingVariant] = useState<InventoryVariantRow | null>(null)

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Inventory</h1>
          <p className="mt-1 text-sm text-zinc-500">Stock levels, adjustments, and damage tracking.</p>
        </div>
        <div className="flex items-center gap-4">
          <Link to="/admin/inventory/history" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
            Transaction &amp; damage history
          </Link>
          <Link to="/admin" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
            &larr; Admin home
          </Link>
        </div>
      </div>

      {dashboardError && (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {dashboardError}
        </div>
      )}

      {/* Summary cards */}
      <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <SummaryCard label="Total products" value={dashboard?.totalProducts} loading={dashboardLoading} />
        <SummaryCard label="Total variants" value={dashboard?.totalVariants} loading={dashboardLoading} />
        <SummaryCard label="Available stock" value={dashboard?.totalAvailableStock} loading={dashboardLoading} />
        <SummaryCard
          label="Low stock"
          value={dashboard?.lowStockCount}
          loading={dashboardLoading}
          tone="amber"
          onClick={() => setStockStatus('LOW_STOCK')}
        />
        <SummaryCard
          label="Out of stock"
          value={dashboard?.outOfStockCount}
          loading={dashboardLoading}
          tone="rose"
          onClick={() => setStockStatus('OUT_OF_STOCK')}
        />
        <SummaryCard label="Total damaged" value={dashboard?.totalDamagedStock} loading={dashboardLoading} tone="rose" />
      </div>

      {/* Recent activity panels */}
      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <RecentPanel title="Recently added products">
          {dashboardLoading ? (
            <PanelSkeleton />
          ) : !dashboard || dashboard.recentProducts.length === 0 ? (
            <EmptyPanelText>No recent products.</EmptyPanelText>
          ) : (
            <ul className="divide-y divide-zinc-100">
              {dashboard.recentProducts.map((p) => (
                <li key={p.id} className="flex items-center justify-between py-2 text-sm">
                  <span className="truncate pr-2 text-zinc-800">{p.name}</span>
                  <span className="shrink-0 rounded-full border border-zinc-200 bg-zinc-50 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-zinc-500">
                    {p.status}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </RecentPanel>

        <RecentPanel title="Recent stock movements">
          {dashboardLoading ? (
            <PanelSkeleton />
          ) : !dashboard || dashboard.recentTransactions.length === 0 ? (
            <EmptyPanelText>No recent transactions.</EmptyPanelText>
          ) : (
            <ul className="divide-y divide-zinc-100">
              {dashboard.recentTransactions.map((t) => (
                <li key={t.id} className="py-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="truncate pr-2 text-zinc-800">{t.productName}</span>
                    <span className={`shrink-0 font-semibold ${t.quantity >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                      {t.quantity >= 0 ? '+' : ''}
                      {t.quantity}
                    </span>
                  </div>
                  <p className="mt-0.5 text-xs text-zinc-400">
                    {t.sku} &middot; {t.type}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </RecentPanel>

        <RecentPanel title="Recent damage records">
          {dashboardLoading ? (
            <PanelSkeleton />
          ) : !dashboard || dashboard.recentDamages.length === 0 ? (
            <EmptyPanelText>No recent damage records.</EmptyPanelText>
          ) : (
            <ul className="divide-y divide-zinc-100">
              {dashboard.recentDamages.map((d) => (
                <li key={d.id} className="py-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="truncate pr-2 text-zinc-800">{d.productName}</span>
                    <span className="shrink-0 font-semibold text-rose-600">-{d.quantity}</span>
                  </div>
                  <p className="mt-0.5 text-xs text-zinc-400">
                    {d.sku} &middot; {d.reason}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </RecentPanel>
      </div>

      {/* Stock table */}
      <section className="mt-8 border-t border-zinc-200 pt-6">
        <h2 className="text-lg font-semibold text-zinc-900">Stock</h2>

        <div className="mt-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="lg:w-72">
            <label htmlFor="stock-search" className="sr-only">
              Search products
            </label>
            <input
              id="stock-search"
              type="search"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by name or SKU…"
              className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:flex lg:items-center">
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              <option value="">All categories</option>
              {categories.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.name}
                </option>
              ))}
            </select>
            <select
              value={colorId}
              onChange={(e) => setColorId(e.target.value)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              <option value="">All colors</option>
              {colors.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.name}
                </option>
              ))}
            </select>
            <select
              value={sizeId}
              onChange={(e) => setSizeId(e.target.value)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              <option value="">All sizes</option>
              {sizes.map((s) => (
                <option key={s.id} value={String(s.id)}>
                  {s.name}
                </option>
              ))}
            </select>
            <select
              value={stockStatus}
              onChange={(e) => setStockStatus(e.target.value as StockStatusFilter)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              {STOCK_STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.value === '' ? 'All stock statuses' : opt.label}
                </option>
              ))}
            </select>
            <select
              value={productStatus}
              onChange={(e) => setProductStatus(e.target.value as ProductStatusFilter)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
            >
              {PRODUCT_STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.value === '' ? 'All product statuses' : opt.label}
                </option>
              ))}
            </select>
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
                <th className="cursor-pointer select-none whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600" onClick={() => toggleSort('name')}>
                  Product{sortIndicator('name')}
                </th>
                <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">SKU</th>
                <th className="whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600">Category</th>
                <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Price</th>
                <th className="cursor-pointer select-none whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600" onClick={() => toggleSort('stock')}>
                  Avail. / Reserved / Damaged / Total{sortIndicator('stock')}
                </th>
                <th className="cursor-pointer select-none whitespace-nowrap px-3 py-2 text-left font-medium text-zinc-600" onClick={() => toggleSort('updatedAt')}>
                  Updated{sortIndicator('updatedAt')}
                </th>
                <th className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 bg-white">
              {tableLoading ? (
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
                    No variants match your filters.
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.variantId}>
                    <td className="whitespace-nowrap px-3 py-2">
                      <div className="flex items-center gap-2">
                        <span
                          className="h-3 w-3 shrink-0 rounded-full border border-zinc-300"
                          style={{ backgroundColor: row.colorHex || undefined }}
                          title={row.colorName}
                        />
                        <span className="font-medium text-zinc-900">{row.productName}</span>
                        <span className="rounded border border-zinc-200 bg-zinc-50 px-1.5 py-0.5 text-[10px] font-medium text-zinc-500">
                          {row.sizeName}
                        </span>
                        <span
                          className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${stockStatusBadgeClasses(row.stockStatus)}`}
                        >
                          {row.stockStatus.replace('_', ' ')}
                        </span>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-600">{row.sku}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-600">{row.categoryName}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-900">{formatPrice(row.sellingPrice)}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right text-xs">
                      <span className={availableQuantityClasses(row)}>{row.availableQuantity}</span>
                      <span className="text-zinc-400"> / {row.reservedQuantity} / {row.damagedQuantity} / {row.stockQuantity}</span>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(row.updatedAt)}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setAdjustingVariant(row)}
                          className="rounded-md border border-zinc-300 px-2.5 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50"
                        >
                          Adjust
                        </button>
                        <button
                          type="button"
                          onClick={() => setDamagingVariant(row)}
                          className="rounded-md border border-rose-200 px-2.5 py-1 text-xs font-medium text-rose-700 hover:bg-rose-50"
                        >
                          Mark damaged
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex items-center justify-between">
          <p className="text-xs text-zinc-500">
            {tableLoading ? 'Loading…' : `${totalElements} variant${totalElements === 1 ? '' : 's'}`}
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

      <StockAdjustmentModal
        variant={adjustingVariant}
        onClose={() => setAdjustingVariant(null)}
        onSuccess={refreshAll}
      />
      <MarkDamagedModal
        variant={damagingVariant}
        onClose={() => setDamagingVariant(null)}
        onSuccess={refreshAll}
      />
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
  tone?: 'default' | 'amber' | 'rose'
  onClick?: () => void
}) {
  const valueClasses =
    tone === 'amber' ? 'text-amber-600' : tone === 'rose' ? 'text-rose-600' : 'text-zinc-900'
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

function RecentPanel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-zinc-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-zinc-900">{title}</h3>
      <div className="mt-2">{children}</div>
    </div>
  )
}

function PanelSkeleton() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="h-4 animate-pulse rounded bg-zinc-100" />
      ))}
    </div>
  )
}

function EmptyPanelText({ children }: { children: ReactNode }) {
  return <p className="text-sm text-zinc-500">{children}</p>
}
