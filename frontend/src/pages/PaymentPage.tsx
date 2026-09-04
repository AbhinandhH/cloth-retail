import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as ordersApi from '../api/orders'
import * as paymentsApi from '../api/payments'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import type { OrderDetail, PaymentInitiateResponse } from '../types'

export default function PaymentPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()

  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [loadingOrder, setLoadingOrder] = useState(true)
  const [orderError, setOrderError] = useState<string | null>(null)

  const [payment, setPayment] = useState<PaymentInitiateResponse | null>(null)
  const [initiating, setInitiating] = useState(false)
  const [initiateError, setInitiateError] = useState<string | null>(null)

  const [simulating, setSimulating] = useState(false)
  const [failureMessage, setFailureMessage] = useState<string | null>(null)

  const loadOrder = useCallback(() => {
    if (!orderId) return
    setLoadingOrder(true)
    setOrderError(null)
    ordersApi
      .fetchOrderById(orderId)
      .then((data) => setOrder(data))
      .catch((err) => setOrderError(getErrorMessage(err)))
      .finally(() => setLoadingOrder(false))
  }, [orderId])

  const initiate = useCallback(() => {
    if (!orderId) return
    setInitiating(true)
    setInitiateError(null)
    setFailureMessage(null)
    paymentsApi
      .initiatePayment(orderId)
      .then((data) => setPayment(data))
      .catch((err) => setInitiateError(getErrorMessage(err)))
      .finally(() => setInitiating(false))
  }, [orderId])

  useEffect(() => {
    loadOrder()
  }, [loadOrder])

  useEffect(() => {
    initiate()
  }, [initiate])

  const handleSimulate = async (outcome: 'SUCCESS' | 'FAILURE') => {
    if (!payment || simulating) return
    setSimulating(true)
    setFailureMessage(null)
    try {
      const result = await paymentsApi.simulatePayment(payment.gatewayReference, outcome)
      if (result.paymentStatus === 'SUCCESS') {
        navigate(`/orders/${result.orderId}`)
      } else {
        setFailureMessage('Payment failed. You can retry the payment or return to your cart.')
      }
    } catch (err) {
      setFailureMessage(getErrorMessage(err))
    } finally {
      setSimulating(false)
    }
  }

  if (loadingOrder) {
    return (
      <div className="mx-auto max-w-md px-4 py-10 sm:px-6 lg:px-8">
        <div className="h-40 animate-pulse rounded-lg bg-zinc-100" />
      </div>
    )
  }

  if (orderError || !order) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center sm:px-6 lg:px-8">
        <p className="text-lg font-medium text-zinc-900">Order not found</p>
        <p className="mt-2 text-sm text-zinc-500">{orderError ?? 'This order could not be loaded.'}</p>
        <Link to="/cart" className="mt-6 inline-block text-sm font-medium text-rose-600 hover:text-rose-700">
          &larr; Back to cart
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-semibold text-zinc-900">Payment</h1>
      <p className="mt-1 text-sm text-zinc-500">Order {order.orderNumber}</p>

      <div className="mt-6 rounded-lg border border-zinc-200 p-4 text-sm">
        <div className="flex justify-between text-zinc-600">
          <span>{order.items.length} item{order.items.length === 1 ? '' : 's'}</span>
          <span className="text-base font-semibold text-zinc-900">{formatPrice(order.totalAmount)}</span>
        </div>
      </div>

      {/* Dev/simulation payment panel — deliberately styled so it can never be
          mistaken for a real card/UPI entry screen. */}
      <div className="mt-6 rounded-xl border-2 border-dashed border-amber-400 bg-amber-50 p-5">
        <div className="flex items-center gap-2">
          <span className="rounded bg-amber-400 px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-amber-950">
            Development Mode
          </span>
        </div>
        <p className="mt-2 text-sm text-amber-900">
          This is a simulated payment gateway for development. No real payment is processed.
        </p>

        {initiating && <p className="mt-4 text-sm text-amber-800">Preparing payment…</p>}

        {initiateError && (
          <div className="mt-4 space-y-2">
            <p className="text-sm text-rose-700">{initiateError}</p>
            <button
              type="button"
              onClick={initiate}
              className="rounded-md border border-amber-400 bg-white px-3 py-1.5 text-sm font-medium text-amber-900 hover:bg-amber-100"
            >
              Retry
            </button>
          </div>
        )}

        {payment && !initiateError && (
          <>
            <p className="mt-3 break-all text-xs text-amber-700">Reference: {payment.gatewayReference}</p>
            <p className="mt-1 text-sm font-medium text-amber-900">Amount due: {formatPrice(payment.amount)}</p>

            {failureMessage ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm font-medium text-rose-700">{failureMessage}</p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={initiate}
                    disabled={initiating}
                    className="flex-1 rounded-md bg-zinc-900 px-3 py-2 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-60"
                  >
                    Retry payment
                  </button>
                  <Link
                    to="/cart"
                    className="flex-1 rounded-md border border-zinc-300 px-3 py-2 text-center text-sm font-medium text-zinc-700 hover:bg-zinc-50"
                  >
                    Return to cart
                  </Link>
                </div>
              </div>
            ) : (
              <div className="mt-4 flex flex-col gap-2">
                <button
                  type="button"
                  onClick={() => handleSimulate('SUCCESS')}
                  disabled={simulating}
                  className="rounded-md bg-emerald-600 px-3 py-2 text-sm font-semibold text-white hover:bg-emerald-500 disabled:opacity-60"
                >
                  {simulating ? 'Processing…' : 'Simulate Successful Payment'}
                </button>
                <button
                  type="button"
                  onClick={() => handleSimulate('FAILURE')}
                  disabled={simulating}
                  className="rounded-md bg-rose-600 px-3 py-2 text-sm font-semibold text-white hover:bg-rose-500 disabled:opacity-60"
                >
                  {simulating ? 'Processing…' : 'Simulate Failed Payment'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
