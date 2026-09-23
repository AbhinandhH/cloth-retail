import type { ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { useLockout } from '../context/LockoutContext'
import { useAuth } from '../context/AuthContext'

/**
 * Always-mounted global gate (same shape as IdleSessionWatcher) wrapping the whole <Routes> tree
 * in App.tsx. Renders a full-screen "unavailable" page instead of any route once the backend has
 * signalled a 402 (see LockoutContext / SubscriptionAccessFilter) - this blocks every one of the
 * ~40 page components from a single place, with no per-page changes.
 *
 * Two bypasses are required even while locked out, both because `isLockedOut` can go true before
 * we know who's asking: on every fresh page load, SiteConfigProvider fires an unauthenticated
 * GET /api/configuration before AuthProvider's own silent session-restore has had a chance to
 * resolve - if the site happens to be locked, that anonymous request 402s first regardless of who
 * the visitor turns out to be.
 *  - `isSuperAdmin`: once session-restore resolves (or a fresh login completes) and confirms this
 *    account is the software owner, every one of ITS OWN requests is exempted server-side anyway
 *    (see SubscriptionAccessFilter) - the splash would otherwise still show client-side from that
 *    earlier anonymous 402, even though nothing SUPER_ADMIN does is actually blocked.
 *  - `/admin/login`: must stay reachable regardless, or SUPER_ADMIN could never even attempt the
 *    login that proves the bypass above - the race above hits every fresh tab, logged in or not.
 *
 * Also holds off showing the splash while `isAuthChecking` is still in flight - otherwise an
 * already-logged-in SUPER_ADMIN would see it flash on every reload before session-restore has had
 * a chance to flip `isSuperAdmin` true and clear it again.
 */
export default function LockoutGate({ children }: { children: ReactNode }) {
  const { isLockedOut } = useLockout()
  const { isSuperAdmin, isAuthChecking } = useAuth()
  const location = useLocation()

  const bypass = isSuperAdmin || isAuthChecking || location.pathname === '/admin/login'
  if (!isLockedOut || bypass) return <>{children}</>

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center bg-zinc-50 px-4 text-center">
      <div className="animate-fade-in-up">
        <p className="font-display text-6xl text-zinc-200 sm:text-7xl">Closed</p>
        <h1 className="mt-4 font-display text-3xl text-zinc-900 sm:text-4xl">Store temporarily unavailable</h1>
        <p className="mx-auto mt-3 max-w-sm text-sm text-zinc-500">
          This store is temporarily unavailable. Please check back later, or contact the site owner if you believe
          this is an error.
        </p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="mt-8 inline-flex items-center justify-center rounded-xl bg-zinc-900 px-6 py-3 text-sm font-semibold text-white shadow-soft transition-opacity hover:opacity-90"
        >
          Try again
        </button>
      </div>
    </div>
  )
}
