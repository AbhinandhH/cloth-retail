import { api } from '@/shared/api/client'
import type { AdminDashboardData } from '@/shared/types'

export function fetchAdminDashboard() {
  return api.get<AdminDashboardData>('/admin/dashboard').then((r) => r.data)
}
