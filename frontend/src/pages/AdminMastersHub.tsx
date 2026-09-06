import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

interface MasterLink {
  to: string
  name: string
  description: string
}

const PRODUCT_MASTERS: MasterLink[] = [
  { to: '/admin/categories', name: 'Categories', description: 'Top-level product categories.' },
  { to: '/admin/sub-categories', name: 'Sub-Categories', description: 'Second-level groupings nested under a category.' },
  { to: '/admin/colors', name: 'Colors', description: 'Color swatches available for variants.' },
  { to: '/admin/sizes', name: 'Sizes', description: 'The full set of sizes available system-wide.' },
  { to: '/admin/size-groups', name: 'Size Groups', description: 'Scope which sizes apply to which categories.' },
  { to: '/admin/brands', name: 'Brands', description: 'Brand options for products.' },
  { to: '/admin/materials', name: 'Materials', description: 'Fabric/material options for products.' },
  { to: '/admin/vendors', name: 'Vendors', description: 'Suppliers used for purchasing and sourcing.' },
]

const INVENTORY_MASTERS: MasterLink[] = [
  { to: '/admin/damage-reasons', name: 'Damage Reasons', description: 'Reasons available when marking stock damaged.' },
]

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

function MasterCard({ link }: { link: MasterLink }) {
  return (
    <Link
      to={link.to}
      className="flex items-center justify-between rounded-lg border border-zinc-200 bg-white px-4 py-3.5 hover:border-zinc-300 hover:bg-zinc-50"
    >
      <div>
        <p className="text-sm font-semibold text-zinc-900">{link.name}</p>
        <p className="mt-0.5 text-xs text-zinc-500">{link.description}</p>
      </div>
      <span className="text-zinc-400">&rarr;</span>
    </Link>
  )
}

export default function AdminMastersHub() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage master data.</p>
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
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Master Data</h1>
          <p className="mt-1 text-sm text-zinc-500">Shared reference data used across products and inventory.</p>
        </div>
      </div>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500">Product Masters</h2>
        <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
          {PRODUCT_MASTERS.map((link) => (
            <MasterCard key={link.to} link={link} />
          ))}
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500">Inventory Masters</h2>
        <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
          {INVENTORY_MASTERS.map((link) => (
            <MasterCard key={link.to} link={link} />
          ))}
        </div>
      </section>

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
