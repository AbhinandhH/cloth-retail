import { api } from './client'
import type { PaymentConfig, PaymentInitiateResponse, PaymentOutcome, PaymentSimulateResponse } from '../types'

/** GET /api/payments/config — public, unauthenticated. Which checkout UI to render (mock dev panel vs real Razorpay checkout). */
export function fetchPaymentConfig() {
  return api.get<PaymentConfig>('/payments/config').then((r) => r.data)
}

/** POST /api/payments/initiate — starts a payment for the order, returns the gateway's own reference (mock's fake one, or Razorpay's real order id). */
export function initiatePayment(orderId: number | string) {
  return api.post<PaymentInitiateResponse>('/payments/initiate', { orderId }).then((r) => r.data)
}

/** POST /api/payments/mock/simulate — dev-only: resolves a mock payment as SUCCESS or FAILURE. */
export function simulatePayment(gatewayReference: string, outcome: PaymentOutcome) {
  return api
    .post<PaymentSimulateResponse>('/payments/mock/simulate', { gatewayReference, outcome })
    .then((r) => r.data)
}
