// Thin loader for Razorpay's Checkout script - see backend RazorpayPaymentGateway for the
// server-side half of this integration. The script defines a global `Razorpay` constructor;
// there's no npm package for it (Razorpay ships this as a hosted script, not a module), so this
// is the standard integration path.

const CHECKOUT_SCRIPT_URL = 'https://checkout.razorpay.com/v1/checkout.js'

export interface RazorpayCheckoutOptions {
  key: string
  amount: number
  currency: string
  name: string
  order_id: string
  prefill?: { name?: string; contact?: string }
  handler: (response: { razorpay_payment_id: string; razorpay_order_id: string; razorpay_signature: string }) => void
  modal?: { ondismiss?: () => void }
}

interface RazorpayCheckoutInstance {
  open: () => void
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayCheckoutOptions) => RazorpayCheckoutInstance
  }
}

let loadPromise: Promise<void> | null = null

/** Loads checkout.js exactly once, even across repeated calls (e.g. a retried payment attempt). */
export function loadRazorpayCheckout(): Promise<void> {
  if (window.Razorpay) return Promise.resolve()
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = CHECKOUT_SCRIPT_URL
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      loadPromise = null
      reject(new Error('Failed to load the Razorpay checkout script'))
    }
    document.body.appendChild(script)
  })
  return loadPromise
}

/** Opens Razorpay's checkout modal. Throws if the script hasn't been loaded yet - always await loadRazorpayCheckout() first. */
export function openRazorpayCheckout(options: RazorpayCheckoutOptions) {
  if (!window.Razorpay) {
    throw new Error('Razorpay checkout script is not loaded')
  }
  new window.Razorpay(options).open()
}
