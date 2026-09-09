import { api } from './client'
import type { AuthResponse, OtpChannel, RefreshResponse, User, VerificationStatusResponse } from '../types'

export interface RegisterPayload {
  fullName: string
  email: string
  mobileNumber: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

/** May come back already logged in (completed=true, auth populated) or pending OTP verification (completed=false, auth null) — see VerificationStatusResponse. */
export function registerCustomer(payload: RegisterPayload) {
  return api.post<VerificationStatusResponse>('/auth/register', payload).then((r) => r.data)
}

export function verifyOtp(registrationId: number | string, channel: OtpChannel, code: string) {
  return api.post<VerificationStatusResponse>('/auth/otp/verify', { registrationId, channel, code }).then((r) => r.data)
}

export function resendOtp(registrationId: number | string, channel: OtpChannel) {
  return api.post('/auth/otp/resend', { registrationId, channel })
}

export function loginCustomer(payload: LoginPayload) {
  return api.post<AuthResponse>('/auth/login', payload).then((r) => r.data)
}

export function loginAdmin(payload: LoginPayload) {
  return api.post<AuthResponse>('/admin/auth/login', payload).then((r) => r.data)
}

export function refreshSession() {
  return api.post<RefreshResponse>('/auth/refresh').then((r) => r.data)
}

export function getCurrentUser() {
  return api.get<User>('/auth/me').then((r) => r.data)
}

export function logout() {
  return api.post('/auth/logout')
}
