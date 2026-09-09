import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/auth'
import { registerUnauthorizedHandler, setAccessToken } from '../api/client'
import type { AuthResponse, OtpChannel, User, VerificationStatusResponse } from '../types'

interface AuthContextValue {
  user: User | null
  accessToken: string | null
  isAuthChecking: boolean
  isAuthenticated: boolean
  isAdmin: boolean
  login: (email: string, password: string) => Promise<User>
  loginAsAdmin: (email: string, password: string) => Promise<User>
  /** May come back already logged in, or still pending email/mobile OTP verification — see VerificationStatusResponse. */
  register: (payload: authApi.RegisterPayload) => Promise<VerificationStatusResponse>
  /** Logs the user in automatically once every required channel is verified (completed=true). */
  verifyOtp: (registrationId: number | string, channel: OtpChannel, code: string) => Promise<VerificationStatusResponse>
  resendOtp: (registrationId: number | string, channel: OtpChannel) => Promise<void>
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
      verifyOtp,
      resendOtp,
      logout,
    }),
    [user, token, isAuthChecking, login, loginAsAdmin, register, verifyOtp, resendOtp, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
