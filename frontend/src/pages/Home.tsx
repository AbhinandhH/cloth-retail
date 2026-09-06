import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchProducts } from '../api/products'
import { getErrorMessage } from '../api/client'
import ProductCard from '../components/ProductCard'
import FilterFields from '../components/FilterFields'
import FilterSheet from '../components/FilterSheet'
import type { FilterValues } from '../components/FilterFields'
import { SkeletonImage } from '../components/Skeleton'
import EmptyState from '../components/EmptyState'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useCategories, useColors, useSizes } from '../context/MasterDataContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import type { ProductListItem } from '../types'

const PAGE_SIZE = 20
const RESULTS_ANCHOR_ID = 'shop-results'

/**
 * CSS/SVG recreation of the Loom Atelier monogram mark (interlocking "L"+"A"
 * serif letterforms with a flowing gold ribbon) — built from vector shapes
 * (SVG <text> + gradient-stroked paths), not a raster image, per the brief's
 * "create the logo using CSS/SVG for crisp quality at any resolution."
 * Rendered once, statically — no sway/animation of its own, so it stays
 * stable and undistorted; only the outer hero surface (see HERO_MELT_STYLES)
 * fades it out as part of the scroll-melt transition.
 */
function LoomAtelierMark({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 240 240" aria-hidden="true">
      <defs>
        <linearGradient id="loomMarkRibbon" x1="20" y1="200" x2="220" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#a9781f" />
          <stop offset="45%" stopColor="#f1d488" />
          <stop offset="55%" stopColor="#d4af37" />
          <stop offset="100%" stopColor="#b8860b" />
        </linearGradient>
      </defs>
      <text
        x="78"
        y="168"
        textAnchor="middle"
        fontFamily="'Playfair Display', Georgia, serif"
        fontSize="150"
        fontWeight="600"
        fill="#1c1712"
      >
        L
      </text>
      <text
        x="150"
        y="168"
        textAnchor="middle"
        fontFamily="'Playfair Display', Georgia, serif"
        fontSize="150"
        fontWeight="600"
        fill="#1c1712"
      >
        A
      </text>
      <path
        d="M28 188 C 70 150, 60 95, 118 78 C 165 64, 205 92, 196 60"
        fill="none"
        stroke="url(#loomMarkRibbon)"
        strokeWidth="5"
        strokeLinecap="round"
      />
      <path
        d="M40 205 C 85 172, 78 108, 138 92"
        fill="none"
        stroke="url(#loomMarkRibbon)"
        strokeWidth="2.5"
        strokeLinecap="round"
        opacity="0.6"
      />
    </svg>
  )
}

// Scroll-driven "melt" transition for the hero surface, using native CSS
// scroll-timeline animation (animation-timeline: view()) so it runs entirely
// on the compositor — no scroll listener, no JS per frame. The bottom-heavy
// "before" shape is a full rect; the "after" shape carves an irregular,
// drippy edge into the TOP of the box (mirroring the hero exiting the
// viewport top-first as the page scrolls down), combined with a rising blur
// and fade so it reads as dissolving rather than a hard cut. Gated behind
// both a feature check and prefers-reduced-motion — unsupported/reduced-
// motion browsers just get the hero scrolling away normally, no melt, which
// is a fully acceptable, non-broken fallback.
const HERO_MELT_STYLES = `
  @keyframes hero-melt-dissolve {
    0% {
      clip-path: polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%);
      filter: blur(0px);
      opacity: 1;
    }
    55% {
      filter: blur(3px);
      opacity: 0.85;
    }
    100% {
      clip-path: polygon(
        0% 34%, 9% 14%, 18% 42%, 27% 18%, 36% 46%, 45% 16%, 54% 44%,
        63% 12%, 72% 40%, 81% 20%, 90% 38%, 100% 16%,
        100% 100%, 0% 100%
      );
      filter: blur(14px);
      opacity: 0;
    }
  }
  @supports (animation-timeline: view()) {
    @media (prefers-reduced-motion: no-preference) {
      .hero-melt-surface {
        animation: hero-melt-dissolve linear both;
        animation-timeline: view();
        animation-range: exit;
      }
    }
  }
`

// Full-bleed, page-wide ambient theme — a single cohesive marble/linen-like
// surface (layered gold-tinted radial washes + a faint film-grain overlay for
// tactile, "printed" quality) that sits behind the ENTIRE homepage rather than
// being boxed inside the hero card, per the "add image as the theme" brief.
// `position: fixed` so it holds still while the page scrolls over it (a
// parallax depth cue) — safe here because Layout.tsx's wrapper is a plain
// flex div with no transform/filter/backdrop-blur that would turn it into a
// containing block for descendants' `fixed` positioning (see Layout.tsx).
// The `.home-ambient-glow` blob drifts continuously as the user scrolls the
// whole document (scroll(root) timeline, not view()) — the "flowing" scroll
// effect distinct from the hero's own exit-melt and each card's own reveal.
const HOME_AMBIENT_STYLES = `
  .home-ambient-bg {
    position: fixed;
    inset: 0;
    z-index: -1;
    pointer-events: none;
    overflow: hidden;
    background:
      radial-gradient(1100px 620px at 12% -8%, rgba(212,175,55,0.16), transparent 60%),
      radial-gradient(900px 720px at 108% 12%, rgba(212,175,55,0.12), transparent 65%),
      radial-gradient(820px 900px at 50% 115%, rgba(28,23,18,0.05), transparent 70%),
      linear-gradient(165deg, #faf6ee 0%, #f4ebd9 45%, #ede1c8 72%, #f7f0e1 100%);
  }
  .home-ambient-bg::after {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0.035;
    mix-blend-mode: multiply;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  }
  .home-ambient-glow {
    position: absolute;
    top: -10%;
    left: 50%;
    width: 140vmax;
    height: 140vmax;
    transform: translate(-50%, 0);
    background: radial-gradient(circle, rgba(212,175,55,0.10), transparent 55%);
  }
  @keyframes home-ambient-flow {
    0% { transform: translate(-52%, -2%) rotate(0deg); }
    100% { transform: translate(-48%, 4%) rotate(4deg); }
  }
  @supports (animation-timeline: scroll()) {
    @media (prefers-reduced-motion: no-preference) {
      .home-ambient-glow {
        animation: home-ambient-flow linear both;
        animation-timeline: scroll(root);
      }
    }
  }
`

/**
 * Premium, typography-forward brand intro. Renders immediately with sensible
 * fallback copy — never gated on SiteConfig's own load — then updates in
 * place once /configuration resolves, so it's never blocked or janky. Its own
 * background is transparent now: the page-wide .home-ambient-bg shows
 * through directly, so the hero reads as part of one continuous themed
 * surface instead of a separate boxed card.
 */
function ShopHero({ businessName, tagline }: { businessName: string | null; tagline: string | null }) {
  const scrollToResults = () => {
    document.getElementById(RESULTS_ANCHOR_ID)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <section className="hero-melt-surface relative -mx-4 mb-8 overflow-hidden px-4 py-14 sm:-mx-6 sm:px-6 sm:py-20 lg:-mx-8 lg:px-8 lg:py-28">
      <style>{HERO_MELT_STYLES}</style>

      <div className="pointer-events-none absolute inset-0" aria-hidden="true">
        <div
          className="absolute -left-12 -top-16 h-64 w-64 rounded-full sm:h-80 sm:w-80"
          style={{ background: 'radial-gradient(circle, rgba(212,175,55,0.14), transparent 70%)' }}
        />
        <div
          className="absolute -right-16 -bottom-10 h-72 w-72 rounded-full sm:h-96 sm:w-96"
          style={{ background: 'radial-gradient(circle, rgba(212,175,55,0.10), transparent 70%)' }}
        />
      </div>

      <LoomAtelierMark className="pointer-events-none absolute left-1/2 top-1/2 h-[280px] w-[280px] -translate-x-1/2 -translate-y-1/2 opacity-[0.055] sm:h-[360px] sm:w-[360px] lg:h-[440px] lg:w-[440px]" />

      <div className="relative mx-auto max-w-2xl text-center">
        <p className="animate-fade-in-up text-[11px] font-semibold uppercase tracking-[0.25em] text-[#a9781f] sm:text-xs">
          New season
        </p>
        <h1
          className="animate-fade-in-up mt-2 font-display text-hero text-[#1c1712]"
          style={{ animationDelay: '80ms' }}
        >
          {businessName || (
            <>
              THREAD<span style={{ color: '#a9781f' }}>CO</span>
            </>
          )}
        </h1>
        <div
          className="animate-scale-in mx-auto mt-4 h-px w-14 sm:w-16"
          style={{ background: 'linear-gradient(90deg, transparent, #b8860b, transparent)', animationDelay: '160ms' }}
        />
        {tagline && (
          <p
            className="animate-fade-in-up mt-4 text-sm text-[#6b5f4f] sm:text-base"
            style={{ animationDelay: '120ms' }}
          >
            {tagline}
          </p>
        )}
        <button
          type="button"
          onClick={scrollToResults}
          className="animate-fade-in-up mt-6 inline-flex items-center gap-1.5 rounded-full bg-[#1c1712] px-6 py-2.5 text-sm font-semibold text-[#f5f1e8] ring-1 ring-[#b8860b]/50 transition-transform hover:scale-[1.02] sm:mt-7"
          style={{ animationDelay: '200ms' }}
        >
          Shop the collection
          <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>
    </section>
  )
}

export default function Home() {
  const [searchParams, setSearchParams] = useSearchParams()

  const { data: categories } = useCategories()
  const { data: sizes } = useSizes()
  const { data: colors } = useColors()
  const { config } = useSiteConfig()

  const [products, setProducts] = useState<ProductListItem[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filterSheetOpen, setFilterSheetOpen] = useState(false)

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

  // Commits every filter field in one URLSearchParams update (used by the mobile
  // sheet's "Show results") — calling updateFilter() five times in a row would each
  // read the same stale `searchParams` closure and clobber each other's changes.
  const applyFilters = (values: FilterValues) => {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(values)) {
      if (value) next.set(key, value)
      else next.delete(key)
    }
    next.delete('page')
    setSearchParams(next)
    setFilterSheetOpen(false)
  }

  const filterValues: FilterValues = { categoryId, sizeId, colorId, minPrice, maxPrice }
  const activeFilterCount = Object.values(filterValues).filter(Boolean).length

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
    <>
      <style>{HOME_AMBIENT_STYLES}</style>
      <div className="home-ambient-bg" aria-hidden="true">
        <div className="home-ambient-glow" />
      </div>
      <div className="relative mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <ShopHero businessName={config?.businessName ?? null} tagline={config?.tagline ?? null} />

      {/* Search + mobile filter trigger */}
      <div className="mb-4 flex items-center gap-2">
        <div className="flex-1">
          <label htmlFor="search" className="sr-only">Search products</label>
          <input
            id="search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by name or SKU…"
            className="w-full rounded-full border border-zinc-300 px-4 py-2.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
        </div>
        <button
          type="button"
          onClick={() => setFilterSheetOpen(true)}
          className="relative flex h-[42px] shrink-0 items-center gap-1.5 rounded-full border border-zinc-300 px-3.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50 lg:hidden"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h18M6 8h12M10 12h4" />
          </svg>
          Filters
          {activeFilterCount > 0 && (
            <span className="flex h-4 w-4 items-center justify-center rounded-full bg-[var(--brand-primary,#18181b)] text-[10px] font-semibold text-white">
              {activeFilterCount}
            </span>
          )}
        </button>
      </div>

      <FilterSheet
        open={filterSheetOpen}
        onClose={() => setFilterSheetOpen(false)}
        categories={categories}
        sizes={sizes}
        colors={colors}
        committed={filterValues}
        onApply={applyFilters}
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[220px_1fr]">
        {/* Desktop filter sidebar — mobile uses the FilterSheet above instead */}
        <aside className="hidden lg:sticky lg:top-20 lg:block lg:self-start">
          <FilterFields
            categories={categories}
            sizes={sizes}
            colors={colors}
            values={filterValues}
            onChange={(key, value) => updateFilter(key, value)}
          />
          {hasActiveFilters && (
            <button onClick={clearFilters} className="mt-5 text-sm font-medium text-rose-600 hover:text-rose-700">
              Clear all filters
            </button>
          )}
        </aside>

        {/* Results */}
        <div id={RESULTS_ANCHOR_ID} className="scroll-mt-20">
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
            <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <SkeletonImage key={i} />
              ))}
            </div>
          ) : products.length === 0 ? (
            <EmptyState
              icon={
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.75}
                    d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 104.35 4.35a7.5 7.5 0 0012.3 12.3z"
                  />
                </svg>
              }
              title="No products match your filters"
              message={hasActiveFilters ? 'Try adjusting or clearing your filters.' : 'Check back soon for new arrivals.'}
              ctaLabel={hasActiveFilters ? 'Clear filters' : undefined}
              onCta={hasActiveFilters ? clearFilters : undefined}
            />
          ) : (
            <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-4">
              {products.map((product, i) => (
                <ProductCard key={product.id} product={product} index={i} />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-1">
              <button
                onClick={() => goToPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="rounded-full border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
              >
                Prev
              </button>
              {pageNumbers.map((p) => (
                <button
                  key={p}
                  onClick={() => goToPage(p)}
                  className={`rounded-full px-3 py-1.5 text-sm font-medium ${
                    p === page ? 'bg-zinc-900 text-white' : 'border border-zinc-300 text-zinc-700 hover:bg-zinc-50'
                  }`}
                >
                  {p + 1}
                </button>
              ))}
              <button
                onClick={() => goToPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-full border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </div>
      </div>
    </>
  )
}
