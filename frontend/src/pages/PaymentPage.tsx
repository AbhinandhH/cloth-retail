import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as ordersApi from '../api/orders'
import * as paymentsApi from '../api/payments'
import { getErrorMessage } from '../api/client'
import { useCart } from '../context/CartContext'
import { formatPrice } from '../lib/formatPrice'
import { loadRazorpayCheckout, openRazorpayCheckout } from '../lib/razorpay'
import BackButton from '../components/BackButton'
import ErrorState from '../components/ErrorState'
import { SkeletonBlock, SkeletonText } from '../components/Skeleton'
import type { OrderDetail, PaymentConfig, PaymentInitiateResponse } from '../types'

export default function PaymentPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const { refresh: refreshCart } = useCart()

  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [loadingOrder, setLoadingOrder] = useState(true)
  const [orderError, setOrderError] = useState<string | null>(null)

  // Which checkout UI to render - the dev/simulation panel below, or the real Razorpay flow
  // further down. Loaded alongside the order so the page never flashes the wrong one.
  const [paymentConfig, setPaymentConfig] = useState<PaymentConfig | null>(null)
  const [loadingConfig, setLoadingConfig] = useState(true)

  const [payment, setPayment] = useState<PaymentInitiateResponse | null>(null)
  const [initiating, setInitiating] = useState(false)
  const [initiateError, setInitiateError] = useState<string | null>(null)

  const [simulating, setSimulating] = useState(false)
  const [failureMessage, setFailureMessage] = useState<string | null>(null)

  // Real (Razorpay) checkout flow state - unused when paymentConfig.provider is "mock".
  const [checkoutLoading, setCheckoutLoading] = useState(false)
  const [checkoutError, setCheckoutError] = useState<string | null>(null)
  const [dismissed, setDismissed] = useState(false)

  // Guards against a real, reproduced backend deadlock: the /payments/initiate
  // endpoint has no idempotency protection against two concurrent calls for
  // the same order, so an accidental double-fire (StrictMode's dev double-
  // invoke, a double-tap, or two tabs on the same payment page) can start two
  // requests where one succeeds silently while the other's failure is what
  // renders — hiding that payment actually went through.
  const initiateInFlightRef = useRef(false)
  const autoInitiatedOrderRef = useRef<string | undefined>(undefined)

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
    if (initiateInFlightRef.current) return
    initiateInFlightRef.current = true
    setInitiating(true)
    setInitiateError(null)
    setFailureMessage(null)
    paymentsApi
      .initiatePayment(orderId)
      .then((data) => setPayment(data))
      .catch((err) => setInitiateError(getErrorMessage(err)))
      .finally(() => {
        initiateInFlightRef.current = false
        setInitiating(false)
      })
  }, [orderId])

  useEffect(() => {
    loadOrder()
  }, [loadOrder])

  useEffect(() => {
    paymentsApi
      .fetchPaymentConfig()
      .then((data) => {
        setPaymentConfig(data)
        // Preload the checkout script ahead of the "Pay" click so opening it feels instant -
        // failures are swallowed here and surfaced instead when the button is actually clicked
        // and loading is retried.
        if (data.provider === 'razorpay') {
          loadRazorpayCheckout().catch(() => {})
        }
      })
      // Config failing to load shouldn't block the page - fall back to treating it as the mock
      // provider (the safer default: no real money can move without real credentials anyway).
      .catch(() => setPaymentConfig({ provider: 'mock', keyId: null, reserveStockOnlyAtPayment: false }))
      .finally(() => setLoadingConfig(false))
  }, [])

  // When true, stock is reserved (and this order becomes visible in "My Orders") only once the
  // customer clicks "Pay" below - see SiteConfiguration.reserveStockOnlyAtPayment. Auto-initiating
  // on mount (the default mode's behavior, further down) would defeat that entirely, since it
  // reserves stock the instant this page loads, before any click.
  const deferredMode = paymentConfig?.reserveStockOnlyAtPayment === true

  const payWithRazorpay = (paymentOverride?: PaymentInitiateResponse) => {
    const activePayment = paymentOverride ?? payment
    if (!activePayment || !paymentConfig?.keyId || checkoutLoading) return
    setCheckoutError(null)
    setDismissed(false)
    setCheckoutLoading(true)
    loadRazorpayCheckout()
      .then(() => {
        openRazorpayCheckout({
          key: paymentConfig.keyId!,
          amount: Math.round(activePayment.amount * 100),
          currency: 'INR',
          name: 'Loom Atelier Studio',
          order_id: activePayment.gatewayReference,
          prefill: {
            name: order?.contactName,
            contact: order?.contactPhone,
          },
          handler: () => {
            // The gateway's webhook (not this callback) is what actually confirms the payment -
            // see PaymentWebhookService's own doc comment on why. This just takes the customer to
            // the order page, which reflects whatever status that webhook has landed by the time
            // it loads (usually near-instant in practice). Kick off a cart refresh here too - the
            // webhook has often already landed by the time this fires, so the navbar badge can
            // drop immediately instead of waiting on OrderDetailPage's own (authoritative) refresh.
            refreshCart()
            navigate(`/orders/${activePayment.orderId}`)
          },
          modal: {
            ondismiss: () => {
              setCheckoutLoading(false)
              setDismissed(true)
            },
          },
        })
        setCheckoutLoading(false)
      })
      .catch((err) => {
        setCheckoutError(getErrorMessage(err))
        setCheckoutLoading(false)
      })
  }

  // Auto-initiate once per order on mount - but ONLY in the default (non-deferred) mode. In
  // deferred mode, initiating here (merely landing on this page) would reserve stock before the
  // customer has clicked anything, defeating the whole point of the config flag - see
  // handlePayClick below, which is what initiates in that mode instead, on explicit click.
  // Critically, this must wait for `paymentConfig` to actually finish loading before deciding:
  // `deferredMode` reads `paymentConfig?.reserveStockOnlyAtPayment`, which is `undefined` (falsy)
  // on the very first render since that config fetch is a separate, async effect - without the
  // `loadingConfig` guard, this effect would fire and auto-initiate on that first render, before
  // the fetched config had a chance to flip `deferredMode` to true, defeating deferred mode every
  // time regardless of the admin setting. Tracking orderId (not just relying on the effect's own
  // dependency identity) means React StrictMode's mount -> cleanup -> mount dev cycle is a no-op
  // the second time through, while the Retry/Retry payment buttons below still call initiate()
  // directly and are unaffected by this guard.
  useEffect(() => {
    if (!orderId) return
    if (loadingConfig) return
    if (deferredMode) return
    if (autoInitiatedOrderRef.current === orderId) return
    autoInitiatedOrderRef.current = orderId
    initiate()
  }, [orderId, initiate, deferredMode, loadingConfig])

  // Deferred mode's "Pay {order total}" button: initiates payment (which is what actually
  // reserves stock in this mode, at PaymentServiceImpl.initiate) and, for the real Razorpay
  // provider, immediately chains into opening the checkout modal using the freshly-returned
  // payment data - not stale `payment` state, which wouldn't be updated yet in the same tick.
  // For the mock provider, succeeding just reveals the existing post-initiate panel below
  // (its render condition already covers that - no further chaining needed).
  const handlePayClick = () => {
    if (!orderId) return
    if (initiateInFlightRef.current) return
    initiateInFlightRef.current = true
    setInitiating(true)
    setInitiateError(null)
    setFailureMessage(null)
    paymentsApi
      .initiatePayment(orderId)
      .then((data) => {
        setPayment(data)
        if (paymentConfig?.provider === 'razorpay') {
          payWithRazorpay(data)
        }
      })
      .catch((err) => setInitiateError(getErrorMessage(err)))
      .finally(() => {
        initiateInFlightRef.current = false
        setInitiating(false)
      })
  }

  const handleSimulate = async (outcome: 'SUCCESS' | 'FAILURE') => {
    if (!payment || simulating) return
    setSimulating(true)
    setFailureMessage(null)
    try {
      const result = await paymentsApi.simulatePayment(payment.gatewayReference, outcome)
      if (result.paymentStatus === 'SUCCESS') {
        refreshCart()
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

  if (loadingOrder || loadingConfig) {
    return (
      <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
        <BackButton className="-ml-2" />
        <div className="mt-4 space-y-3">
          <SkeletonText width="w-40" />
          <SkeletonBlock className="h-16 w-full rounded-xl" />
          <SkeletonBlock className="h-56 w-full rounded-xl" />
        </div>
      </div>
    )
  }

  if (orderError || !order) {
    return (
      <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
        <BackButton className="-ml-2" />
        <ErrorState
          title="Order not found"
          message={orderError ?? 'This order could not be loaded.'}
          onRetry={loadOrder}
        />
        <div className="text-center">
          <Link to="/cart" className="text-sm font-medium text-zinc-900 hover:underline">
            Back to cart
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6 sm:px-6 lg:px-8">
      <div className="flex items-center gap-1">
        <BackButton className="-ml-2" />
        <h1 className="font-display text-2xl font-semibold text-zinc-900 sm:text-3xl">Payment</h1>
      </div>
      <p className="mt-1 pl-1 text-sm text-zinc-500">Order {order.orderNumber}</p>

      <div className="mt-6 flex items-center justify-between rounded-xl border border-zinc-200 bg-white p-4 text-sm shadow-soft sm:p-5">
        <span className="text-zinc-600">
          {order.items.length} item{order.items.length === 1 ? '' : 's'}
        </span>
        <span className="text-base font-semibold text-zinc-900">{formatPrice(order.totalAmount)}</span>
      </div>

      {paymentConfig?.provider === 'razorpay' ? (
        <div className="mt-6 rounded-2xl border border-zinc-200 bg-white p-5 shadow-soft">
          {initiating && (
            <p className="flex items-center gap-2 text-sm text-zinc-500">
              <span className="h-3.5 w-3.5 animate-pulse rounded-full bg-zinc-300" />
              Preparing payment…
            </p>
          )}

          {initiateError && (
            <div className="space-y-2">
              <p className="text-sm text-rose-700">{initiateError}</p>
              <button
                type="button"
                onClick={initiate}
                className="min-h-[40px] rounded-full border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
              >
                Retry
              </button>
            </div>
          )}

          {deferredMode && !payment && !initiating && !initiateError && (
            <>
              <p className="text-sm font-medium text-zinc-900">Amount due: {formatPrice(order.totalAmount)}</p>
              <p className="mt-1 text-xs text-zinc-500">
                Nothing is reserved until you pay. You'll be taken to Razorpay's secure checkout to
                complete payment by card, UPI, or netbanking.
              </p>
              <button
                type="button"
                onClick={handlePayClick}
                className="mt-4 min-h-[44px] w-full rounded-full bg-zinc-900 px-4 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
              >
                {`Pay ${formatPrice(order.totalAmount)}`}
              </button>
            </>
          )}

          {payment && !initiateError && (
            <>
              <p className="text-sm font-medium text-zinc-900">Amount due: {formatPrice(payment.amount)}</p>
              <p className="mt-1 text-xs text-zinc-500">
                You'll be taken to Razorpay's secure checkout to complete payment by card, UPI, or netbanking.
              </p>

              {checkoutError && <p className="mt-3 text-sm text-rose-700">{checkoutError}</p>}
              {dismissed && !checkoutError && (
                <p className="mt-3 text-sm text-zinc-500">Payment window closed. You can try again below.</p>
              )}

              <button
                type="button"
                onClick={() => payWithRazorpay()}
                disabled={checkoutLoading}
                className="mt-4 min-h-[44px] w-full rounded-full bg-zinc-900 px-4 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
              >
                {checkoutLoading ? 'Opening…' : `Pay ${formatPrice(payment.amount)}`}
              </button>
              <Link
                to="/cart"
                className="mt-2 flex min-h-[40px] items-center justify-center rounded-full border border-zinc-300 px-3 text-center text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
              >
                Return to cart
              </Link>
            </>
          )}
        </div>
      ) : (
        /* Dev/simulation payment panel — deliberately styled so it can never be
           mistaken for a real card/UPI entry screen: dashed amber border,
           hazard-style badge, explicit "no real payment" copy. */
        <div className="mt-6 rounded-2xl border-2 border-dashed border-amber-400 bg-amber-50 p-5">
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-amber-400 px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-amber-950">
              Development Mode
            </span>
          </div>
          <p className="mt-2.5 text-sm text-amber-900">
            This is a simulated payment gateway for development. No real payment is processed.
          </p>

          {initiating && (
            <p className="mt-4 flex items-center gap-2 text-sm text-amber-800">
              <span className="h-3.5 w-3.5 animate-pulse rounded-full bg-amber-400" />
              Preparing payment…
            </p>
          )}

          {initiateError && (
            <div className="mt-4 space-y-2">
              <p className="text-sm text-rose-700">{initiateError}</p>
              <button
                type="button"
                onClick={initiate}
                className="min-h-[40px] rounded-full border border-amber-400 bg-white px-4 text-sm font-medium text-amber-900 transition-colors hover:bg-amber-100"
              >
                Retry
              </button>
            </div>
          )}

          {deferredMode && !payment && !initiating && !initiateError && (
            <>
              <p className="mt-4 text-sm font-medium text-amber-900">Amount due: {formatPrice(order.totalAmount)}</p>
              <p className="mt-1 text-xs text-amber-700">Nothing is reserved until you pay.</p>
              <button
                type="button"
                onClick={handlePayClick}
                className="mt-4 min-h-[40px] w-full rounded-full bg-zinc-900 px-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
              >
                {`Pay ${formatPrice(order.totalAmount)}`}
              </button>
            </>
          )}

          {payment && !initiateError && (
            <>
              <p className="mt-4 break-all text-xs text-amber-700">Reference: {payment.gatewayReference}</p>
              <p className="mt-1 text-sm font-medium text-amber-900">Amount due: {formatPrice(payment.amount)}</p>

              {failureMessage ? (
                <div className="mt-4 space-y-3">
                  <p className="text-sm font-medium text-rose-700">{failureMessage}</p>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={initiate}
                      disabled={initiating}
                      className="min-h-[40px] flex-1 rounded-full bg-zinc-900 px-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
                    >
                      Retry payment
                    </button>
                    <Link
                      to="/cart"
                      className="flex min-h-[40px] flex-1 items-center justify-center rounded-full border border-zinc-300 px-3 text-center text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
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
                    className="min-h-[40px] rounded-full bg-emerald-600 px-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
                  >
                    {simulating ? 'Processing…' : 'Simulate Successful Payment'}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSimulate('FAILURE')}
                    disabled={simulating}
                    className="min-h-[40px] rounded-full bg-rose-600 px-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
                  >
                    {simulating ? 'Processing…' : 'Simulate Failed Payment'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
