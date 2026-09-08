import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchInventoryDashboard } from '../api/adminInventory'
import { getErrorMessage } from '../api/client'
import type { InventoryDashboard } from '../types'

/** Icon-only "back to admin home" affordance - no text, matching AdminProductForm/AdminProductList's back links. */
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
  const navigate = useNavigate()

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

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Inventory</h1>
          <p className="mt-1 text-sm text-zinc-500">Stock levels, adjustments, and damage tracking.</p>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm font-medium text-zinc-600">
        <Link to="/admin/inventory/stock" className="hover:text-zinc-900">
          Stock
        </Link>
        <Link to="/admin/inventory/history" className="hover:text-zinc-900">
          Transaction &amp; damage history
        </Link>
      </div>
      <p className="mt-3 text-xs text-zinc-500">
        Stock adjustments and mark-damaged only affect products that already exist. To bring a brand-new
        product into inventory — with its own variants, sizes, colors, and starting stock — use{' '}
        <Link to="/admin/products/new" className="font-medium text-zinc-700 underline hover:text-zinc-900">
          Add product
        </Link>
        .
      </p>

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
          onClick={() => navigate('/admin/inventory/stock?stockStatus=LOW_STOCK')}
        />
        <SummaryCard
          label="Out of stock"
          value={dashboard?.outOfStockCount}
          loading={dashboardLoading}
          tone="rose"
          onClick={() => navigate('/admin/inventory/stock?stockStatus=OUT_OF_STOCK')}
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
