import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/auth'
import { registerUnauthorizedHandler, setAccessToken } from '../api/client'
import type { AuthResponse, User } from '../types'

interface AuthContextValue {
  user: User | null
  accessToken: string | null
  isAuthChecking: boolean
  isAuthenticated: boolean
  isAdmin: boolean
  login: (email: string, password: string) => Promise<User>
  loginAsAdmin: (email: string, password: string) => Promise<User>
  register: (payload: authApi.RegisterPayload) => Promise<User>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isAuthChecking, setIsAuthChecking] = useState(true)

  const applyAuthResponse = useCallback((res: AuthResponse) => {
    setAccessToken(res.accessToken)
    setToken(res.accessToken)
    setUser(res.user)
    return res.user
  }, [])

  const clearAuth = useCallback(() => {
    setAccessToken(null)
    setToken(null)
    setUser(null)
  }, [])

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
        if (!cancelled) setUser(me)
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

  const register = useCallback(
    async (payload: authApi.RegisterPayload) => {
      const res = await authApi.registerCustomer(payload)
      return applyAuthResponse(res)
    },
    [applyAuthResponse],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }, [clearAuth])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken: token,
      isAuthChecking,
      isAuthenticated: Boolean(user),
      isAdmin: Boolean(user?.roles?.some((r) => r === 'ADMIN' || r === 'SUPER_ADMIN')),
      login,
      loginAsAdmin,
      register,
      logout,
    }),
    [user, token, isAuthChecking, login, loginAsAdmin, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
