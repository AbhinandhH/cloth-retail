import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function AdminHome() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/admin/login', { replace: true })
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
      <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
        <span className="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-rose-600 text-lg font-bold text-white">
          A
        </span>
        <h1 className="mt-4 text-xl font-semibold text-white">Admin Portal</h1>
        <p className="mt-2 text-sm text-zinc-400">
          Signed in as <span className="text-zinc-200">{user?.fullName ?? user?.email}</span>.
        </p>
        <Link
          to="/admin/products"
          className="mt-6 block w-full rounded-lg bg-[var(--brand-primary,#e11d48)] py-2.5 text-sm font-semibold text-white hover:opacity-90"
        >
          Manage products
        </Link>
        <Link
          to="/admin/inventory"
          className="mt-3 block w-full rounded-lg border border-zinc-700 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
        >
          Inventory
        </Link>
        <Link
          to="/admin/masters"
          className="mt-3 block w-full rounded-lg border border-zinc-700 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
        >
          Masters
        </Link>
        <Link
          to="/admin/configuration"
          className="mt-3 block w-full rounded-lg border border-zinc-700 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
        >
          Site configuration
        </Link>
        <button
          onClick={handleLogout}
          className="mt-3 w-full rounded-lg border border-zinc-700 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
        >
          Log out
        </button>
      </div>
    </div>
  )
}
