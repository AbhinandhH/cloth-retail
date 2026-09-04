import { api } from './client'
import type { PaymentInitiateResponse, PaymentOutcome, PaymentSimulateResponse } from '../types'

/** POST /api/payments/initiate — starts a payment for the order, returns the mock gatewayReference. */
export function initiatePayment(orderId: number | string) {
  return api.post<PaymentInitiateResponse>('/payments/initiate', { orderId }).then((r) => r.data)
}

/** POST /api/payments/mock/simulate — dev-only: resolves a mock payment as SUCCESS or FAILURE. */
export function simulatePayment(gatewayReference: string, outcome: PaymentOutcome) {
  return api
    .post<PaymentSimulateResponse>('/payments/mock/simulate', { gatewayReference, outcome })
    .then((r) => r.data)
}
