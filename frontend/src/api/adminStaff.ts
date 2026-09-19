import { api } from './client'
import type { AdminStaffRow, ModulePermissionRow, User } from '../types'

export interface CreateAdminPayload {
  fullName: string
  email: string
  password: string
  role: 'ADMIN' | 'EMPLOYEE'
}

export function createAdmin(payload: CreateAdminPayload) {
  return api.post<User>('/admin/admins', payload).then((r) => r.data)
}

export function fetchStaff() {
  return api.get<AdminStaffRow[]>('/admin/admins').then((r) => r.data)
}

export function setStaffStatus(id: number | string, enabled: boolean) {
  return api.patch<void>(`/admin/admins/${id}/status`, { enabled }).then((r) => r.data)
}

export function fetchStaffPermissions(id: number | string) {
  return api.get<ModulePermissionRow[]>(`/admin/admins/${id}/permissions`).then((r) => r.data)
}

export function updateStaffPermissions(id: number | string, grants: ModulePermissionRow[]) {
  return api.put<ModulePermissionRow[]>(`/admin/admins/${id}/permissions`, { grants }).then((r) => r.data)
}

// --- Self-service profile (any admin-side account) -------------------------------------------------

export interface UpdateProfilePayload {
  fullName: string
  email: string
  currentPassword?: string
  newPassword?: string
}

export function fetchMyProfile() {
  return api.get<User>('/admin/profile').then((r) => r.data)
}

export function updateMyProfile(payload: UpdateProfilePayload) {
  return api.put<User>('/admin/profile', payload).then((r) => r.data)
}

export function fetchMyPermissions() {
  return api.get<ModulePermissionRow[]>('/admin/profile/permissions').then((r) => r.data)
}
