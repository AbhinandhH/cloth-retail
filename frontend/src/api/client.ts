import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiErrorBody, RefreshResponse } from '../types'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL,
  withCredentials: true, // required so the httpOnly refresh cookie round-trips
})

// --- In-memory access token (never persisted to localStorage) ---
let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

// Called when a refresh attempt fails and we should treat the user as logged out.
// AuthContext registers this on mount.
type LogoutHandler = () => void
let onUnauthorized: LogoutHandler | null = null

export function registerUnauthorizedHandler(handler: LogoutHandler | null) {
  onUnauthorized = handler
}

// Attach bearer token to every outgoing request.
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
  /** Mark a request as requiring authentication so a failed refresh is treated
   * as an auth-required failure rather than a normal "browsing as guest" 401. */
  authRequired?: boolean
}

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = api
      .post<RefreshResponse>('/auth/refresh')
      .then((res) => {
        setAccessToken(res.data.accessToken)
        return res.data.accessToken
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorBody>) => {
    const original = error.config as RetriableConfig | undefined

    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !original.url?.includes('/auth/refresh')
    ) {
      original._retry = true
      const newToken = await refreshAccessToken()
      if (newToken) {
        original.headers = original.headers ?? {}
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      }
      // Refresh failed: treat as logged out. Public browsing requests should
      // just render logged-out state; only auth-required requests propagate
      // as an error a caller might redirect on.
      onUnauthorized?.()
    }

    return Promise.reject(error)
  },
)

/** Extract a user-friendly message from an API error, falling back for network errors. */
export function getErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiErrorBody | undefined
    if (data?.message) return data.message
    if (err.message) return err.message
  }
  return 'Something went wrong. Please try again.'
}

export function getFieldErrors(err: unknown): Record<string, string> {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiErrorBody | undefined
    if (data?.fieldErrors) {
      return Object.fromEntries(data.fieldErrors.map((fe) => [fe.field, fe.message]))
    }
  }
  return {}
}

// The backend's origin (no trailing /api), derived from the configured API
// base URL. Media URLs returned by the upload endpoint (e.g. "/media/x.png")
// are relative to this origin, not the frontend's own origin.
const backendOrigin = baseURL.replace(/\/api\/?$/, '')

export function getBackendOrigin() {
  return backendOrigin
}

/** Resolve a possibly-relative media URL (from SiteConfiguration/upload responses) against the backend origin. */
export function toMediaUrl(url: string | null | undefined): string | null {
  if (!url) return null
  if (/^https?:\/\//i.test(url) || url.startsWith('data:')) return url
  return `${backendOrigin}${url.startsWith('/') ? '' : '/'}${url}`
}
