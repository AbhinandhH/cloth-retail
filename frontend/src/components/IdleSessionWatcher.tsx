import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'

const ACTIVITY_EVENTS = ['mousedown', 'mousemove', 'keydown', 'wheel', 'scroll', 'touchstart'] as const
const DEFAULT_IDLE_TIMEOUT_MINUTES = 30

/**
 * Auto-logs-out any authenticated session (customer, admin, employee, or super admin) after
 * idleTimeoutMinutes of no user activity - the admin-configurable value from Site Configuration
 * (see AdminConfigurationForm), loaded globally via SiteConfigContext so it's known even before
 * the user signs in.
 *
 * This is purely a client-side UX layer for immediate feedback while a tab is open and running -
 * AuthServiceImpl#refresh enforces the same window server-side against RefreshToken#lastUsedAt,
 * so a session can't be kept alive past the configured idle window just by skipping this timer
 * (e.g. a laptop asleep through it, or a tab backgrounded long enough for the browser to throttle
 * setTimeout). That server-side check is what actually matters for security; this just makes the
 * logout happen promptly instead of silently on the next failed request.
 */
export default function IdleSessionWatcher() {
  const { isAuthenticated, isAdmin, logout } = useAuth()
  const { config } = useSiteConfig()
  const navigate = useNavigate()
  const timerRef = useRef<number | undefined>(undefined)

  useEffect(() => {
    if (!isAuthenticated) return

    const idleTimeoutMs = (config?.idleTimeoutMinutes ?? DEFAULT_IDLE_TIMEOUT_MINUTES) * 60_000

    const handleIdle = () => {
      const redirectTo = isAdmin ? '/admin/login' : '/login'
      void logout().finally(() => {
        navigate(redirectTo, { replace: true, state: { idleLogout: true } })
      })
    }

    const resetTimer = () => {
      window.clearTimeout(timerRef.current)
      timerRef.current = window.setTimeout(handleIdle, idleTimeoutMs)
    }

    ACTIVITY_EVENTS.forEach((event) => window.addEventListener(event, resetTimer, { passive: true }))
    resetTimer()

    return () => {
      window.clearTimeout(timerRef.current)
      ACTIVITY_EVENTS.forEach((event) => window.removeEventListener(event, resetTimer))
    }
  }, [isAuthenticated, isAdmin, config?.idleTimeoutMinutes, logout, navigate])

  return null
}
