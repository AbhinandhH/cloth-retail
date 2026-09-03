import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchCategories, fetchColors, fetchProducts, fetchSizes } from '../api/products'
import { getErrorMessage } from '../api/client'
import ProductCard from '../components/ProductCard'
import FilterSelect from '../components/FilterSelect'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { Category, Color, ProductListItem, Size } from '../types'

const PAGE_SIZE = 20

export default function Home() {
  const [searchParams, setSearchParams] = useSearchParams()

  const [categories, setCategories] = useState<Category[]>([])
  const [sizes, setSizes] = useState<Size[]>([])
  const [colors, setColors] = useState<Color[]>([])

  const [products, setProducts] = useState<ProductListItem[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Local text input state, decoupled from the URL so typing feels instant;
  // the debounced value is what actually drives the URL/query.
  const [searchInput, setSearchInput] = useState(searchParams.get('q') ?? '')
  const debouncedSearch = useDebouncedValue(searchInput, 400)

  const page = Number(searchParams.get('page') ?? '0') || 0
  const categoryId = searchParams.get('categoryId') ?? ''
  const sizeId = searchParams.get('sizeId') ?? ''
  const colorId = searchParams.get('colorId') ?? ''
  const minPrice = searchParams.get('minPrice') ?? ''
  const maxPrice = searchParams.get('maxPrice') ?? ''
  const q = searchParams.get('q') ?? ''

  // Load filter option lists once.
  useEffect(() => {
    fetchCategories().then(setCategories).catch(() => setCategories([]))
    fetchSizes().then(setSizes).catch(() => setSizes([]))
    fetchColors().then(setColors).catch(() => setColors([]))
  }, [])

  // Push the debounced search text into the URL (resetting to page 0).
  useEffect(() => {
    if (debouncedSearch === q) return
    const next = new URLSearchParams(searchParams)
    if (debouncedSearch) next.set('q', debouncedSearch)
    else next.delete('q')
    next.delete('page')
    setSearchParams(next, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch])

  // Fetch products whenever the URL-driven query changes.
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchProducts({ page, size: PAGE_SIZE, categoryId, sizeId, colorId, minPrice, maxPrice, q })
      .then((res) => {
        if (cancelled) return
        setProducts(res.content)
        setTotalPages(res.totalPages)
        setTotalElements(res.totalElements)
      })
      .catch((err) => {
        if (cancelled) return
        setError(getErrorMessage(err))
        setProducts([])
        setTotalPages(0)
        setTotalElements(0)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [page, categoryId, sizeId, colorId, minPrice, maxPrice, q])

  const updateFilter = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page')
    setSearchParams(next, { replace: true })
  }

  const goToPage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams)
    if (nextPage > 0) next.set('page', String(nextPage))
    else next.delete('page')
    setSearchParams(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const clearFilters = () => {
    setSearchInput('')
    setSearchParams(new URLSearchParams())
  }

  const hasActiveFilters = Boolean(categoryId || sizeId || colorId || minPrice || maxPrice || q)

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return []
    const windowSize = 5
    let start = Math.max(0, page - Math.floor(windowSize / 2))
    const end = Math.min(totalPages, start + windowSize)
    start = Math.max(0, end - windowSize)
    return Array.from({ length: end - start }, (_, i) => start + i)
  }, [page, totalPages])

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Search */}
      <div className="mb-4">
        <label htmlFor="search" className="sr-only">Search products</label>
        <input
          id="search"
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search by name or SKU…"
          className="w-full rounded-lg border border-zinc-300 px-4 py-2.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[220px_1fr]">
        {/* Filters */}
        <aside className="lg:sticky lg:top-20 lg:self-start">
          <details className="rounded-lg border border-zinc-200 lg:border-0" open>
            <summary className="cursor-pointer select-none px-4 py-3 text-sm font-semibold text-zinc-900 lg:hidden">
              Filters
            </summary>
            <div className="space-y-5 px-4 pb-4 lg:p-0">
              <FilterSelect
                label="Category"
                value={categoryId}
                onChange={(v) => updateFilter('categoryId', v)}
                options={categories.map((c) => ({ value: String(c.id), label: c.name }))}
              />
              <FilterSelect
                label="Size"
                value={sizeId}
                onChange={(v) => updateFilter('sizeId', v)}
                options={sizes.map((s) => ({ value: String(s.id), label: s.name }))}
              />
              <FilterSelect
                label="Color"
                value={colorId}
                onChange={(v) => updateFilter('colorId', v)}
                options={colors.map((c) => ({ value: String(c.id), label: c.name }))}
              />
              <div>
                <p className="mb-2 text-sm font-medium text-zinc-900">Price</p>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min={0}
                    inputMode="decimal"
                    placeholder="Min"
                    value={minPrice}
                    onChange={(e) => updateFilter('minPrice', e.target.value)}
                    className="w-full rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
                  />
                  <span className="text-zinc-400">–</span>
                  <input
                    type="number"
                    min={0}
                    inputMode="decimal"
                    placeholder="Max"
                    value={maxPrice}
                    onChange={(e) => updateFilter('maxPrice', e.target.value)}
                    className="w-full rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
                  />
                </div>
              </div>

              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="text-sm font-medium text-rose-600 hover:text-rose-700"
                >
                  Clear all filters
                </button>
              )}
            </div>
          </details>
        </aside>

        {/* Results */}
        <div>
          <div className="mb-3 flex items-center justify-between">
            <p className="text-sm text-zinc-500">
              {loading ? 'Loading…' : `${totalElements} product${totalElements === 1 ? '' : 's'}`}
            </p>
          </div>

          {error && (
            <div className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          )}

          {loading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="aspect-[3/4] animate-pulse rounded-lg bg-zinc-100" />
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="rounded-lg border border-dashed border-zinc-300 px-4 py-16 text-center">
              <p className="text-zinc-500">No products match your filters.</p>
              {hasActiveFilters && (
                <button onClick={clearFilters} className="mt-3 text-sm font-medium text-zinc-900 underline">
                  Clear filters
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-1">
              <button
                onClick={() => goToPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
              >
                Prev
              </button>
              {pageNumbers.map((p) => (
                <button
                  key={p}
                  onClick={() => goToPage(p)}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                    p === page ? 'bg-zinc-900 text-white' : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
                  }`}
                >
                  {p + 1}
                </button>
              ))}
              <button
                onClick={() => goToPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
