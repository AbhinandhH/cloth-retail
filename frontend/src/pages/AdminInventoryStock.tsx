import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchInventoryVariants } from '../api/adminInventory'
import type { VariantQuery } from '../api/adminInventory'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useCategories, useColors, useSizes } from '../context/MasterDataContext'
import StockAdjustmentModal from '../components/StockAdjustmentModal'
import MarkDamagedModal from '../components/MarkDamagedModal'
import type { InventoryVariantRow, ProductStatus, StockStatus } from '../types'

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

/** Icon-only "back to inventory" affordance - no text, matching AdminProductForm/AdminProductList's back links. */
function InventoryBackLink({ className = '' }: { className?: string }) {
  return (
    <Link
      to="/admin/inventory"
      aria-label="Back to inventory"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  )
}

export default function AdminInventoryStock() {
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

  return <AdminInventoryStockContent />
}

function AdminInventoryStockContent() {
  const { data: categories } = useCategories()
  const { data: sizes } = useSizes()
  const { data: colors } = useColors()

  // The dashboard's "Low stock"/"Out of stock" summary cards link here with a
  // ?stockStatus= param so that jump-to-filtered-view behavior survives the
  // stock table moving to its own page - read once as the initial filter,
  // same as any other filter default.
  const [searchParams] = useSearchParams()
  const initialStockStatus = (searchParams.get('stockStatus') as StockStatusFilter | null) ?? ''

  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [categoryId, setCategoryId] = useState('')
  const [colorId, setColorId] = useState('')
  const [sizeId, setSizeId] = useState('')
  const [stockStatus, setStockStatus] = useState<StockStatusFilter>(initialStockStatus)
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

  const [adjustingVariant, setAdjustingVariant] = useState<InventoryVariantRow | null>(null)
  const [damagingVariant, setDamagingVariant] = useState<InventoryVariantRow | null>(null)

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <InventoryBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Stock</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Search, filter, and adjust stock levels for every existing product variant.
          </p>
        </div>
      </div>
      <p className="mt-3 text-xs text-zinc-500">
        Adjust and mark-damaged below only affect stock for products that already exist. To bring a brand-new
        product into inventory — with its own variants, sizes, colors, and starting stock — use{' '}
        <Link to="/admin/products/new" className="font-medium text-zinc-700 underline hover:text-zinc-900">
          Add product
        </Link>
        .
      </p>

      <div className="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
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

      <StockAdjustmentModal
        variant={adjustingVariant}
        onClose={() => setAdjustingVariant(null)}
        onSuccess={loadTable}
      />
      <MarkDamagedModal
        variant={damagingVariant}
        onClose={() => setDamagingVariant(null)}
        onSuccess={loadTable}
      />

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <InventoryBackLink />
      </div>
    </div>
  )
}
