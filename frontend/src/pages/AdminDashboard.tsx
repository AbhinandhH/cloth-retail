import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchAdminDashboard } from '../api/adminDashboard'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import { orderStatusBadgeClasses, orderStatusLabel } from './AdminOrderDashboard'
import type { AdminDashboardData } from '../types'

/** Icon-only "back to admin home" affordance - matching AdminInventoryDashboard/AdminOrderDashboard's own copy of this. */
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

export default function AdminDashboard() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view the dashboard.</p>
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

  return <AdminDashboardContent />
}

function AdminDashboardContent() {
  const [dashboard, setDashboard] = useState<AdminDashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    fetchAdminDashboard()
      .then(setDashboard)
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Dashboard</h1>
          <p className="mt-1 text-sm text-zinc-500">Sales, orders, and stock at a glance.</p>
        </div>
      </div>

      {error && (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {error}
        </div>
      )}

      {/* KPI cards */}
      <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <SummaryCard
          label="Today's sales"
          value={loading ? undefined : formatPrice(dashboard?.todaysSales ?? 0)}
          sub={loading || !dashboard ? undefined : `${dashboard.todaysOrderCount} order${dashboard.todaysOrderCount === 1 ? '' : 's'}`}
          loading={loading}
        />
        <SummaryCard label="Total orders" value={loading ? undefined : String(dashboard?.totalOrders ?? 0)} loading={loading} />
        <SummaryCard
          label="Pending orders"
          value={loading ? undefined : String(dashboard?.pendingOrders ?? 0)}
          loading={loading}
          tone="amber"
          to="/admin/orders?orderStatus=CONFIRMED"
        />
        <SummaryCard
          label="Products in stock"
          value={loading ? undefined : String(dashboard?.productsInStock ?? 0)}
          loading={loading}
          to="/admin/inventory/stock"
        />
        <SummaryCard
          label="Low-stock products"
          value={loading ? undefined : String(dashboard?.lowStockCount ?? 0)}
          loading={loading}
          tone="amber"
          to="/admin/inventory/stock?stockStatus=LOW_STOCK"
        />
        <SummaryCard
          label="Out-of-stock products"
          value={loading ? undefined : String(dashboard?.outOfStockCount ?? 0)}
          loading={loading}
          tone="rose"
          to="/admin/inventory/stock?stockStatus=OUT_OF_STOCK"
        />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Panel title="Sales overview" subtitle="Last 14 days">
            {loading ? (
              <ChartSkeleton />
            ) : !dashboard || dashboard.salesOverview.every((p) => p.sales === 0) ? (
              <EmptyPanelText>No sales in the last 14 days.</EmptyPanelText>
            ) : (
              <SalesChart points={dashboard.salesOverview} />
            )}
          </Panel>
        </div>

        <Panel title="Top-selling products">
          {loading ? (
            <PanelSkeleton />
          ) : !dashboard || dashboard.topSellingProducts.length === 0 ? (
            <EmptyPanelText>No sales yet.</EmptyPanelText>
          ) : (
            <ul className="divide-y divide-zinc-100">
              {dashboard.topSellingProducts.map((p) => {
                const thumb = toMediaUrl(p.imageUrl)
                return (
                  <li key={p.sku} className="flex items-center gap-3 py-2.5">
                    <div className="h-10 w-10 shrink-0 overflow-hidden rounded-md bg-zinc-100">
                      {thumb && <img src={thumb} alt="" className="h-full w-full object-cover" />}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-zinc-800">{p.productName}</p>
                      <p className="text-xs text-zinc-400">{p.sku}</p>
                    </div>
                    <div className="shrink-0 text-right">
                      <p className="text-sm font-semibold text-zinc-900">{p.quantitySold} sold</p>
                      <p className="text-xs text-zinc-400">{formatPrice(p.revenue)}</p>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </Panel>
      </div>

      <div className="mt-4">
        <Panel title="Recent orders" action={<Link to="/admin/orders" className="text-xs font-medium text-zinc-500 hover:text-zinc-900">View all</Link>}>
          {loading ? (
            <PanelSkeleton />
          ) : !dashboard || dashboard.recentOrders.length === 0 ? (
            <EmptyPanelText>No orders yet.</EmptyPanelText>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] text-left text-sm">
                <thead>
                  <tr className="text-xs uppercase tracking-wide text-zinc-400">
                    <th className="pb-2 font-medium">Order</th>
                    <th className="pb-2 font-medium">Customer</th>
                    <th className="pb-2 font-medium">Status</th>
                    <th className="pb-2 pr-0 text-right font-medium">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {dashboard.recentOrders.map((o) => (
                    <tr key={o.id}>
                      <td className="py-2.5 pr-3">
                        <Link to={`/admin/orders/${o.id}`} className="font-medium text-zinc-800 hover:underline">
                          {o.orderNumber}
                        </Link>
                        <p className="text-xs text-zinc-400">{new Date(o.createdAt).toLocaleDateString()}</p>
                      </td>
                      <td className="py-2.5 pr-3 text-zinc-600">{o.customerName}</td>
                      <td className="py-2.5 pr-3">
                        <span className={`inline-block rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${orderStatusBadgeClasses(o.status)}`}>
                          {orderStatusLabel(o.status)}
                        </span>
                      </td>
                      <td className="py-2.5 pr-0 text-right font-semibold text-zinc-900">{formatPrice(o.totalAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>
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
  sub,
  loading,
  tone = 'default',
  to,
}: {
  label: string
  value: string | undefined
  sub?: string
  loading: boolean
  tone?: 'default' | 'amber' | 'rose'
  to?: string
}) {
  const valueClasses = tone === 'amber' ? 'text-amber-600' : tone === 'rose' ? 'text-rose-600' : 'text-zinc-900'
  const cardClassName = `rounded-lg border border-zinc-200 bg-white px-3 py-2.5 text-left ${to ? 'block transition-colors hover:border-zinc-300 hover:bg-zinc-50' : ''}`
  const content = (
    <>
      <p className="text-[11px] font-medium uppercase tracking-wide text-zinc-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${valueClasses}`}>
        {loading ? <span className="inline-block h-6 w-14 animate-pulse rounded bg-zinc-100 align-middle" /> : value}
      </p>
      {sub && <p className="mt-0.5 text-xs text-zinc-400">{sub}</p>}
    </>
  )
  if (to) {
    return (
      <Link to={to} className={cardClassName}>
        {content}
      </Link>
    )
  }
  return <div className={cardClassName}>{content}</div>
}

function Panel({ title, subtitle, action, children }: { title: string; subtitle?: string; action?: ReactNode; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-zinc-200 bg-white p-4">
      <div className="flex items-center justify-between gap-2">
        <div>
          <h3 className="text-sm font-semibold text-zinc-900">{title}</h3>
          {subtitle && <p className="text-xs text-zinc-400">{subtitle}</p>}
        </div>
        {action}
      </div>
      <div className="mt-3">{children}</div>
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

function ChartSkeleton() {
  return <div className="h-48 animate-pulse rounded bg-zinc-100" />
}

function EmptyPanelText({ children }: { children: ReactNode }) {
  return <p className="text-sm text-zinc-500">{children}</p>
}

/** A small, dependency-free bar chart - no charting library needed for 14 daily points. */
function SalesChart({ points }: { points: { date: string; sales: number; orderCount: number }[] }) {
  const width = 700
  const height = 180
  const padding = 24
  const max = Math.max(1, ...points.map((p) => p.sales))
  const barGap = 6
  const barWidth = (width - padding * 2 - barGap * (points.length - 1)) / points.length

  return (
    <svg viewBox={`0 0 ${width} ${height + 24}`} className="w-full" preserveAspectRatio="xMidYMid meet" role="img" aria-label="Sales over the last 14 days">
      {points.map((p, i) => {
        const barHeight = max === 0 ? 0 : Math.max(2, (p.sales / max) * height)
        const x = padding + i * (barWidth + barGap)
        const y = height - barHeight
        const isLast = i === points.length - 1
        const day = new Date(p.date + 'T00:00:00')
        return (
          <g key={p.date}>
            <rect
              x={x}
              y={y}
              width={barWidth}
              height={barHeight}
              rx={3}
              className={isLast ? 'fill-zinc-900' : 'fill-zinc-300'}
            >
              <title>
                {day.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}: {formatPrice(p.sales)} ({p.orderCount} order
                {p.orderCount === 1 ? '' : 's'})
              </title>
            </rect>
            {(i % 2 === 0 || isLast) && (
              <text x={x + barWidth / 2} y={height + 16} textAnchor="middle" className="fill-zinc-400 text-[9px]">
                {day.toLocaleDateString(undefined, { day: 'numeric', month: 'short' })}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}
