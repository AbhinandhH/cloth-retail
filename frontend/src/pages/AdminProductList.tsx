import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as adminProductsApi from '../api/adminProducts'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useCategories } from '../context/MasterDataContext'
import ConfirmDialog from '../components/ConfirmDialog'
import SelectField from '../components/SelectField'
import type { AdminProductListItem, ProductStatus } from '../types'

const PAGE_SIZE = 20

const STATUS_OPTIONS: { value: ProductStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'ARCHIVED', label: 'Archived' },
]

const STATUS_BADGE: Record<ProductStatus, string> = {
  DRAFT: 'bg-zinc-100 text-zinc-600 border-zinc-200',
  ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  INACTIVE: 'bg-amber-50 text-amber-700 border-amber-200',
  ARCHIVED: 'bg-zinc-700 text-zinc-100 border-zinc-800',
}

/** Icon-only "back to admin home" affordance - no text, matching AdminProductForm's back link. */
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

function StatusBadge({ status }: { status: ProductStatus }) {
  const label = STATUS_OPTIONS.find((o) => o.value === status)?.label ?? status
  return (
    <span className={`inline-block rounded-full border px-2 py-0.5 text-[11px] font-medium ${STATUS_BADGE[status]}`}>
      {label}
    </span>
  )
}

export default function AdminProductList() {
  const { isAdmin } = useAuth()
  const navigate = useNavigate()

  const { data: categories } = useCategories()
  const [products, setProducts] = useState<AdminProductListItem[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [categoryId, setCategoryId] = useState('')
  const [status, setStatus] = useState('')

  // Row-level action state.
  const [busyId, setBusyId] = useState<number | string | null>(null)
  const [rowMessage, setRowMessage] = useState<string | null>(null)
  const [rowError, setRowError] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AdminProductListItem | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  // Reset to page 0 whenever a filter changes.
  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, categoryId, status])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminProductsApi
      .fetchAdminProducts({ page, size: PAGE_SIZE, q: debouncedSearch, categoryId, status })
      .then((res) => {
        setProducts(res.content)
        setTotalPages(res.totalPages)
        setTotalElements(res.totalElements)
      })
      .catch((err) => {
        setError(getErrorMessage(err))
        setProducts([])
        setTotalPages(0)
        setTotalElements(0)
      })
      .finally(() => setLoading(false))
  }, [page, debouncedSearch, categoryId, status])

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

  const handleStatusChange = async (product: AdminProductListItem, nextStatus: ProductStatus) => {
    const verb = nextStatus === 'ARCHIVED' ? 'archive' : nextStatus === 'ACTIVE' ? 'activate' : 'deactivate'
    if (!window.confirm(`Are you sure you want to ${verb} "${product.name}"? This can be changed again later.`)) {
      return
    }
    setBusyId(product.id)
    setRowMessage(null)
    setRowError(null)
    try {
      await adminProductsApi.updateAdminProductStatus(product.id, nextStatus)
      setRowMessage(`"${product.name}" is now ${nextStatus.toLowerCase()}.`)
      load()
    } catch (err) {
      setRowError(getErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await adminProductsApi.deleteAdminProduct(deleteTarget.id)
      setDeleteTarget(null)
      setRowMessage(`"${deleteTarget.name}" was deleted.`)
      load()
    } catch (err) {
      setDeleteError(getErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage products.</p>
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

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-2">
          <AdminHomeBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">Products</h1>
            <p className="mt-1 text-sm text-zinc-500">Manage the storefront catalog — products, variants, and stock.</p>
          </div>
        </div>
        <Link
          to="/admin/products/new"
          className="shrink-0 rounded-xl bg-zinc-900 px-5 py-3 text-sm font-semibold text-white transition-all duration-150 hover:bg-zinc-800 active:scale-[0.97]"
        >
          + Add product
        </Link>
      </div>

      {/* Toolbar */}
      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-[1fr_200px_180px]">
        <div>
          <label htmlFor="product-search" className="sr-only">
            Search products
          </label>
          <input
            id="product-search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by name or SKU…"
            className="w-full rounded-lg border border-zinc-300 px-4 py-2.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
        </div>
        <SelectField
          label="Category"
          value={categoryId}
          onChange={setCategoryId}
          placeholder="All categories"
          options={categories.map((c) => ({ value: String(c.id), label: c.name }))}
        />
        <SelectField
          label="Status"
          value={status}
          onChange={setStatus}
          placeholder="All statuses"
          options={STATUS_OPTIONS.map((o) => ({ value: o.value, label: o.label }))}
        />
      </div>

      {rowMessage && (
        <div className="mt-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {rowMessage}
        </div>
      )}
      {rowError && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {rowError}
        </div>
      )}
      {error && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {error}
        </div>
      )}

      {/* Table */}
      <div className="mt-6 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr className="text-left text-xs font-semibold uppercase tracking-wide text-zinc-500">
              <th className="px-4 py-3">Product</th>
              <th className="px-4 py-3">Base SKU</th>
              <th className="px-4 py-3">Category</th>
              <th className="px-4 py-3">Brand</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3 text-right">Variants</th>
              <th className="px-4 py-3 text-right">Stock</th>
              <th className="px-4 py-3">Updated</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 bg-white">
            {loading ? (
              <tr>
                <td colSpan={9} className="px-4 py-10 text-center text-zinc-500">
                  Loading…
                </td>
              </tr>
            ) : products.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-4 py-10 text-center text-zinc-500">
                  No products found.
                </td>
              </tr>
            ) : (
              products.map((product) => {
                const thumb = toMediaUrl(product.primaryImageUrl)
                return (
                  <tr
                    key={product.id}
                    className="cursor-pointer hover:bg-zinc-50"
                    onClick={() => navigate(`/admin/products/${product.id}`)}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        {thumb ? (
                          <img src={thumb} alt="" className="h-10 w-10 rounded-md border border-zinc-200 object-cover" />
                        ) : (
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md border border-dashed border-zinc-300 text-[9px] text-zinc-400">
                            No image
                          </div>
                        )}
                        <span className="font-medium text-zinc-900">{product.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-zinc-600">{product.baseSku ?? '—'}</td>
                    <td className="px-4 py-3 text-zinc-600">{product.categoryName}</td>
                    <td className="px-4 py-3 text-zinc-600">{product.brandName ?? '—'}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={product.status} />
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-600">{product.variantCount}</td>
                    <td className="px-4 py-3 text-right text-zinc-600">{product.totalStock}</td>
                    <td className="px-4 py-3 text-zinc-500">
                      {product.updatedAt ? new Date(product.updatedAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <div className="flex flex-wrap items-center gap-2">
                        {product.status !== 'ACTIVE' && (
                          <button
                            type="button"
                            disabled={busyId === product.id}
                            onClick={() => handleStatusChange(product, 'ACTIVE')}
                            className="text-xs font-medium text-emerald-700 hover:underline disabled:opacity-50"
                          >
                            Activate
                          </button>
                        )}
                        {product.status === 'ACTIVE' && (
                          <button
                            type="button"
                            disabled={busyId === product.id}
                            onClick={() => handleStatusChange(product, 'INACTIVE')}
                            className="text-xs font-medium text-amber-700 hover:underline disabled:opacity-50"
                          >
                            Deactivate
                          </button>
                        )}
                        {product.status !== 'ARCHIVED' && (
                          <button
                            type="button"
                            disabled={busyId === product.id}
                            onClick={() => handleStatusChange(product, 'ARCHIVED')}
                            className="text-xs font-medium text-zinc-600 hover:underline disabled:opacity-50"
                          >
                            Archive
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => {
                            setDeleteError(null)
                            setDeleteTarget(product)
                          }}
                          className="text-xs font-medium text-rose-600 hover:underline"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-sm text-zinc-500">
          {loading ? '' : `${totalElements} product${totalElements === 1 ? '' : 's'}`}
        </p>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-8 flex items-center justify-center gap-1">
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

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete product"
        message={
          deleteTarget
            ? `Delete "${deleteTarget.name}"? This cannot be undone.`
            : ''
        }
        confirmLabel="Delete"
        danger
        confirming={deleting}
        error={deleteError}
        onConfirm={handleDeleteConfirm}
        onCancel={() => {
          setDeleteTarget(null)
          setDeleteError(null)
        }}
      />

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
