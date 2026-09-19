import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/auth'
import { fetchMyPermissions } from '../api/adminStaff'
import { registerUnauthorizedHandler, setAccessToken } from '../api/client'
import type { AdminModule, AuthResponse, ModulePermissionRow, OtpChannel, User, VerificationStatusResponse } from '../types'

interface AuthContextValue {
  user: User | null
  accessToken: string | null
  isAuthChecking: boolean
  isAuthenticated: boolean
  /** Any admin-side account (ADMIN, SUPER_ADMIN, or EMPLOYEE) - the broad route-access gate. */
  isAdmin: boolean
  /** ADMIN or SUPER_ADMIN - the store-owner tier. Governance screens (Tax, Site Config, Notifications, Staff) are gated on this, never EMPLOYEE. SUPER_ADMIN (the software owner) inherits every ADMIN privilege on top of its own software-owner-only powers - see SecurityConfig's RoleHierarchy bean. */
  isStoreAdmin: boolean
  /** SUPER_ADMIN specifically - the software owner. Also always isStoreAdmin (see above) - this is for the software-owner-only screens layered on top (e.g. subscription billing), not for telling it apart from ADMIN on shared ones. */
  isSuperAdmin: boolean
  /** This account's own per-module grants - SUPER_ADMIN bypasses the permission table entirely via the role hierarchy, so it's never populated (or needed) for that role. */
  permissions: ModulePermissionRow[]
  hasModuleView: (module: AdminModule) => boolean
  hasModuleEdit: (module: AdminModule) => boolean
  login: (email: string, password: string) => Promise<User>
  loginAsAdmin: (email: string, password: string) => Promise<User>
  /** May come back already logged in, or still pending email/mobile OTP verification — see VerificationStatusResponse. */
  register: (payload: authApi.RegisterPayload) => Promise<VerificationStatusResponse>
  /** Logs the user in automatically once every required channel is verified (completed=true). */
  verifyOtp: (registrationId: number | string, channel: OtpChannel, code: string) => Promise<VerificationStatusResponse>
  resendOtp: (registrationId: number | string, channel: OtpChannel) => Promise<void>
  logout: () => Promise<void>
  /** Re-fetches this account's own permissions - call after an admin edits their own grants, or another admin's edit might affect the currently-viewed account. */
  refreshPermissions: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isAuthChecking, setIsAuthChecking] = useState(true)
  const [permissions, setPermissions] = useState<ModulePermissionRow[]>([])

  // Only ADMIN/EMPLOYEE accounts have rows in the permission table at all (SUPER_ADMIN never
  // touches the 8 operational modules the table covers) - fetching for a CUSTOMER/SUPER_ADMIN
  // session would just 403, so this is skipped for those.
  const loadPermissions = useCallback(async (roles: User['roles']) => {
    if (!roles.some((r) => r === 'ADMIN' || r === 'EMPLOYEE')) {
      setPermissions([])
      return
    }
    try {
      const rows = await fetchMyPermissions()
      setPermissions(rows)
    } catch {
      setPermissions([])
    }
  }, [])

  const applyAuthResponse = useCallback(
    (res: AuthResponse) => {
      setAccessToken(res.accessToken)
      setToken(res.accessToken)
      setUser(res.user)
      void loadPermissions(res.user.roles)
      return res.user
    },
    [loadPermissions],
  )

  const clearAuth = useCallback(() => {
    setAccessToken(null)
    setToken(null)
    setUser(null)
    setPermissions([])
  }, [])

  const refreshPermissions = useCallback(async () => {
    if (user) await loadPermissions(user.roles)
  }, [user, loadPermissions])

  // Silent session restore on app load via the httpOnly refresh cookie,
  // then hydrate the user profile via GET /auth/me so the navbar can show
  // a name immediately instead of waiting for the next login/register call.
  useEffect(() => {
    let cancelled = false
    authApi
      .refreshSession()
      .then(async (res) => {
        if (cancelled) return
        setAccessToken(res.accessToken)
        setToken(res.accessToken)
        const me = await authApi.getCurrentUser()
        if (!cancelled) {
          setUser(me)
          void loadPermissions(me.roles)
        }
      })
      .catch(() => {
        // No valid session cookie yet (fresh visitor) — this is normal, not an error.
        if (!cancelled) clearAuth()
      })
      .finally(() => {
        if (!cancelled) setIsAuthChecking(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    registerUnauthorizedHandler(() => clearAuth())
    return () => registerUnauthorizedHandler(null)
  }, [clearAuth])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await authApi.loginCustomer({ email, password })
      return applyAuthResponse(res)
    },
    [applyAuthResponse],
  )

  const loginAsAdmin = useCallback(
    async (email: string, password: string) => {
      const res = await authApi.loginAdmin({ email, password })
      return applyAuthResponse(res)
    },
    [applyAuthResponse],
  )

  // Shared by register() and verifyOtp() - both hit the same "is verification fully done yet"
  // decision server-side (VerificationStatusResponse), and only actually log the user in once it is.
  const applyVerificationStatus = useCallback(
    (res: VerificationStatusResponse) => {
      if (res.completed && res.auth) {
        applyAuthResponse(res.auth)
      }
      return res
    },
    [applyAuthResponse],
  )

  const register = useCallback(
    async (payload: authApi.RegisterPayload) => {
      const res = await authApi.registerCustomer(payload)
      return applyVerificationStatus(res)
    },
    [applyVerificationStatus],
  )

  const verifyOtp = useCallback(
    async (registrationId: number | string, channel: OtpChannel, code: string) => {
      const res = await authApi.verifyOtp(registrationId, channel, code)
      return applyVerificationStatus(res)
    },
    [applyVerificationStatus],
  )

  const resendOtp = useCallback(async (registrationId: number | string, channel: OtpChannel) => {
    await authApi.resendOtp(registrationId, channel)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }, [clearAuth])

  const hasModuleView = useCallback(
    (module: AdminModule) => permissions.some((p) => p.module === module && p.canView),
    [permissions],
  )
  const hasModuleEdit = useCallback(
    (module: AdminModule) => permissions.some((p) => p.module === module && p.canEdit),
    [permissions],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken: token,
      isAuthChecking,
      isAuthenticated: Boolean(user),
      isAdmin: Boolean(user?.roles?.some((r) => r === 'ADMIN' || r === 'SUPER_ADMIN' || r === 'EMPLOYEE')),
      isStoreAdmin: Boolean(user?.roles?.some((r) => r === 'ADMIN' || r === 'SUPER_ADMIN')),
      isSuperAdmin: Boolean(user?.roles?.includes('SUPER_ADMIN')),
      permissions,
      hasModuleView,
      hasModuleEdit,
      login,
      loginAsAdmin,
      register,
      verifyOtp,
      resendOtp,
      logout,
      refreshPermissions,
    }),
    [
      user,
      token,
      isAuthChecking,
      permissions,
      hasModuleView,
      hasModuleEdit,
      login,
      loginAsAdmin,
      register,
      verifyOtp,
      resendOtp,
      logout,
      refreshPermissions,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
