import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import * as addressesApi from '../api/addresses'
import * as ordersApi from '../api/orders'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../lib/formatPrice'
import TextField from '../components/TextField'
import type { Address, AddressRequest } from '../types'

const EMPTY_ADDRESS: AddressRequest = {
  label: null,
  addressLine1: '',
  addressLine2: null,
  city: '',
  state: '',
  postalCode: '',
  country: '',
  isDefault: false,
}

export default function CheckoutPage() {
  const { cart } = useCart()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [addresses, setAddresses] = useState<Address[]>([])
  const [loadingAddresses, setLoadingAddresses] = useState(true)
  const [addressesError, setAddressesError] = useState<string | null>(null)

  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null)
  const [showNewForm, setShowNewForm] = useState(false)
  const [form, setForm] = useState<AddressRequest>(EMPTY_ADDRESS)

  const [contactPhone, setContactPhone] = useState('')

  const [placing, setPlacing] = useState(false)
  const [placeError, setPlaceError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    addressesApi
      .fetchAddresses()
      .then((data) => {
        if (cancelled) return
        setAddresses(data)
        if (data.length > 0) {
          const preferred = data.find((a) => a.isDefault) ?? data[0]
          setSelectedAddressId(String(preferred.id))
          setShowNewForm(false)
        } else {
          setShowNewForm(true)
        }
      })
      .catch((err) => {
        if (cancelled) return
        setAddressesError(getErrorMessage(err))
        setShowNewForm(true)
      })
      .finally(() => {
        if (!cancelled) setLoadingAddresses(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const updateForm = <K extends keyof AddressRequest>(key: K, value: AddressRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const formValid =
    form.addressLine1.trim() !== '' &&
    form.city.trim() !== '' &&
    form.state.trim() !== '' &&
    form.postalCode.trim() !== '' &&
    form.country.trim() !== ''

  const canPlaceOrder = (showNewForm ? formValid : Boolean(selectedAddressId)) && (cart?.items.length ?? 0) > 0

  const handlePlaceOrder = async (e: FormEvent) => {
    e.preventDefault()
    if (placing || !canPlaceOrder) return
    // Disable immediately (before any await) so a double-click/tap can't fire
    // two requests — the backend is idempotent on the key below too, but a
    // snappy UI shouldn't rely on that alone.
    setPlacing(true)
    setPlaceError(null)

    try {
      let shippingAddressId: string | number
      if (showNewForm) {
        const created = await addressesApi.createAddress(form)
        shippingAddressId = created.id
      } else {
        shippingAddressId = selectedAddressId!
      }

      const order = await ordersApi.createOrder({
        idempotencyKey: crypto.randomUUID(),
        shippingAddressId,
        contactPhone: contactPhone.trim() || undefined,
      })
      navigate(`/checkout/payment/${order.id}`)
    } catch (err) {
      setPlaceError(getErrorMessage(err))
      setPlacing(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-semibold text-zinc-900">Checkout</h1>

      <form onSubmit={handlePlaceOrder} className="mt-6 space-y-6">
        {/* Shipping address */}
        <section>
          <h2 className="text-sm font-semibold text-zinc-900">Shipping address</h2>

          {loadingAddresses ? (
            <div className="mt-2 h-16 animate-pulse rounded-md bg-zinc-100" />
          ) : (
            <>
              {addressesError && (
                <p className="mt-2 text-xs text-rose-600">{addressesError}</p>
              )}

              {addresses.length > 0 && (
                <div className="mt-3 space-y-2">
                  {addresses.map((addr) => (
                    <label
                      key={addr.id}
                      className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 text-sm ${
                        !showNewForm && selectedAddressId === String(addr.id)
                          ? 'border-zinc-900 ring-1 ring-zinc-900'
                          : 'border-zinc-200'
                      }`}
                    >
                      <input
                        type="radio"
                        name="address"
                        className="mt-1"
                        checked={!showNewForm && selectedAddressId === String(addr.id)}
                        onChange={() => {
                          setSelectedAddressId(String(addr.id))
                          setShowNewForm(false)
                        }}
                      />
                      <span>
                        {addr.label && <span className="block font-medium text-zinc-900">{addr.label}</span>}
                        <span className="block text-zinc-600">
                          {addr.addressLine1}
                          {addr.addressLine2 ? `, ${addr.addressLine2}` : ''}, {addr.city}, {addr.state}{' '}
                          {addr.postalCode}, {addr.country}
                        </span>
                        {addr.isDefault && (
                          <span className="mt-1 inline-block rounded bg-zinc-100 px-1.5 py-0.5 text-[11px] font-medium text-zinc-600">
                            Default
                          </span>
                        )}
                      </span>
                    </label>
                  ))}

                  <button
                    type="button"
                    onClick={() => setShowNewForm((v) => !v)}
                    className="text-sm font-medium text-zinc-900 hover:underline"
                  >
                    {showNewForm ? 'Use a saved address instead' : '+ Add new address'}
                  </button>
                </div>
              )}

              {showNewForm && (
                <div className="mt-3 space-y-4 rounded-lg border border-zinc-200 p-4">
                  <TextField label="Label (optional)" value={form.label} onChange={(v) => updateForm('label', v)} />
                  <TextField
                    label="Address line 1"
                    value={form.addressLine1}
                    onChange={(v) => updateForm('addressLine1', v ?? '')}
                  />
                  <TextField
                    label="Address line 2 (optional)"
                    value={form.addressLine2}
                    onChange={(v) => updateForm('addressLine2', v)}
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <TextField label="City" value={form.city} onChange={(v) => updateForm('city', v ?? '')} />
                    <TextField label="State" value={form.state} onChange={(v) => updateForm('state', v ?? '')} />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <TextField
                      label="Postal code"
                      value={form.postalCode}
                      onChange={(v) => updateForm('postalCode', v ?? '')}
                    />
                    <TextField label="Country" value={form.country} onChange={(v) => updateForm('country', v ?? '')} />
                  </div>
                  <label className="flex items-center gap-2 text-sm text-zinc-700">
                    <input
                      type="checkbox"
                      checked={form.isDefault}
                      onChange={(e) => updateForm('isDefault', e.target.checked)}
                    />
                    Set as default address
                  </label>
                </div>
              )}
            </>
          )}
        </section>

        {/* Contact */}
        <section>
          <h2 className="text-sm font-semibold text-zinc-900">Contact</h2>
          <p className="mt-1 text-xs text-zinc-500">Signed in as {user?.fullName ?? user?.email}</p>
          <div className="mt-2">
            <TextField
              label="Contact phone (optional)"
              type="tel"
              value={contactPhone}
              onChange={(v) => setContactPhone(v ?? '')}
            />
          </div>
        </section>

        {/* Order summary */}
        <section className="space-y-1.5 rounded-lg border border-zinc-200 p-4 text-sm">
          <p className="mb-1 text-xs text-zinc-400">
            Informational only — the final total is recalculated when the order is placed.
          </p>
          <div className="flex justify-between text-zinc-600">
            <span>Subtotal</span>
            <span>{formatPrice(cart?.subtotal ?? 0)}</span>
          </div>
          {(cart?.discountTotal ?? 0) > 0 && (
            <div className="flex justify-between text-emerald-600">
              <span>Discount</span>
              <span>−{formatPrice(cart?.discountTotal ?? 0)}</span>
            </div>
          )}
          <div className="mt-2 flex justify-between border-t border-zinc-200 pt-2 text-base font-semibold text-zinc-900">
            <span>Total</span>
            <span>{formatPrice(cart?.total ?? 0)}</span>
          </div>
        </section>

        {placeError && (
          <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {placeError}
          </div>
        )}

        <button
          type="submit"
          disabled={placing || !canPlaceOrder}
          className="w-full rounded-lg bg-[var(--brand-primary,#18181b)] py-3 text-sm font-semibold text-white ring-2 ring-offset-1 ring-[var(--brand-secondary,#18181b)] hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {placing ? 'Placing order…' : 'Place order'}
        </button>
      </form>
    </div>
  )
}
