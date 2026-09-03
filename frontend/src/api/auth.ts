import { api } from './client'
import type { AuthResponse, RefreshResponse, User } from '../types'

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

export function registerCustomer(payload: RegisterPayload) {
  return api.post<AuthResponse>('/auth/register', payload).then((r) => r.data)
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
