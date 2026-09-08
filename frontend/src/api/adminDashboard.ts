import { api } from './client'
import type { AdminDashboardData } from '../types'

export function fetchAdminDashboard() {
  return api.get<AdminDashboardData>('/admin/dashboard').then((r) => r.data)
}
