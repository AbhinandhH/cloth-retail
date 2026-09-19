import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AdminConfigurationForm from '../components/AdminConfigurationForm'

export default function AdminConfiguration() {
  // Site configuration is store-governance, ADMIN-only (the store owner) - SUPER_ADMIN (the
  // software owner) has no store-level access at all, see RoleName's own doc comment.
  const { isStoreAdmin } = useAuth()

  if (!isStoreAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">
            You need the ADMIN role to view and edit site configuration.
          </p>
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

  return <AdminConfigurationForm />
}
