import { api } from './client'
import type {
  SubscriptionPayInitiationResponse,
  SubscriptionPaymentRow,
  SubscriptionStatusResponse,
} from '../types'

export function fetchSubscriptionStatus() {
  return api.get<SubscriptionStatusResponse>('/admin/subscription/status').then((r) => r.data)
}

/** SUPER_ADMIN only - see AdminSubscriptionController. Resets paid=false server-side. */
export function updateSubscriptionSettings(monthlyAmount: number, dueDate: string) {
  return api
    .put<SubscriptionStatusResponse>('/admin/subscription/settings', { monthlyAmount, dueDate })
    .then((r) => r.data)
}

/** SUPER_ADMIN only - manual override, doesn't require Razorpay to be reachable. */
export function markSubscriptionPaid() {
  return api.post<SubscriptionStatusResponse>('/admin/subscription/mark-paid').then((r) => r.data)
}

export function initiateSubscriptionPayment() {
  return api.post<SubscriptionPayInitiationResponse>('/admin/subscription/pay').then((r) => r.data)
}

export function fetchSubscriptionPayments() {
  return api.get<SubscriptionPaymentRow[]>('/admin/subscription/payments').then((r) => r.data)
}
