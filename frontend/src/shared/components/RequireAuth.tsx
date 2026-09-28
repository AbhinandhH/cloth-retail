import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

interface RequireAuthProps {
  children: ReactNode
  /** Where to send unauthenticated users. Defaults to the customer login page. */
  redirectTo?: string
  /** When true, also requires the ADMIN/SUPER_ADMIN role (used by /admin/*). */
  requireAdmin?: boolean
}

/**
 * Wraps a route element that should only render for an authenticated user.
 * Not used by any customer route in this pass (cart/checkout aren't built
 * yet), but kept here so protecting a future route is a one-line change:
 *   <Route path="/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
 */
export default function RequireAuth({ children, redirectTo = '/login', requireAdmin = false }: RequireAuthProps) {
  const { isAuthenticated, isAuthChecking, isAdmin } = useAuth()
  const location = useLocation()

  if (isAuthChecking) {
    return <div className="flex min-h-[40vh] items-center justify-center text-sm text-zinc-500">Loading…</div>
  }

  if (!isAuthenticated || (requireAdmin && !isAdmin)) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />
  }

  return <>{children}</>
}
