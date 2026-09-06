import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as addressesApi from '../api/addresses'
import { getErrorMessage } from '../api/client'
import BackButton from '../components/BackButton'
import EmptyState from '../components/EmptyState'
import ErrorState from '../components/ErrorState'
import { SkeletonBlock, SkeletonText } from '../components/Skeleton'
import TextField from '../components/customer/TextField'
import ConfirmDialog from '../components/customer/ConfirmDialog'
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

function formatAddress(addr: Address) {
  const parts = [
    addr.addressLine1,
    addr.addressLine2,
    `${addr.city}, ${addr.state} ${addr.postalCode}`,
    addr.country,
  ].filter(Boolean)
  return parts
}

function AddressIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z"
      />
    </svg>
  )
}

function AddressForm({
  form,
  onChange,
  onSubmit,
  onCancel,
  saving,
  error,
  submitLabel,
}: {
  form: AddressRequest
  onChange: <K extends keyof AddressRequest>(key: K, value: AddressRequest[K]) => void
  onSubmit: (e: FormEvent) => void
  onCancel: () => void
  saving: boolean
  error: string | null
  submitLabel: string
}) {
  return (
    <form onSubmit={onSubmit} className="space-y-4 rounded-xl border border-zinc-200 bg-white p-4 shadow-soft sm:p-5">
      <TextField label="Label (optional)" value={form.label} onChange={(v) => onChange('label', v)} />
      <TextField
        label="Address line 1"
        value={form.addressLine1}
        onChange={(v) => onChange('addressLine1', v ?? '')}
      />
      <TextField
        label="Address line 2 (optional)"
        value={form.addressLine2}
        onChange={(v) => onChange('addressLine2', v)}
      />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label="City" value={form.city} onChange={(v) => onChange('city', v ?? '')} />
        <TextField label="State" value={form.state} onChange={(v) => onChange('state', v ?? '')} />
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label="Postal code" value={form.postalCode} onChange={(v) => onChange('postalCode', v ?? '')} />
        <TextField label="Country" value={form.country} onChange={(v) => onChange('country', v ?? '')} />
      </div>
      <label className="flex min-h-[40px] items-center gap-2 text-sm text-zinc-700">
        <input
          type="checkbox"
          checked={form.isDefault}
          onChange={(e) => onChange('isDefault', e.target.checked)}
          className="h-4 w-4 rounded border-zinc-300"
        />
        Set as default address
      </label>

      {error && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
      )}

      <div className="flex justify-end gap-2 pt-1">
        <button
          type="button"
          onClick={onCancel}
          disabled={saving}
          className="min-h-[40px] rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-60"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={saving}
          className="min-h-[40px] rounded-full bg-zinc-900 px-5 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
        >
          {saving ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}

export default function Profile() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [addresses, setAddresses] = useState<Address[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [formMode, setFormMode] = useState<'none' | 'add' | number | string>('none')
  const [form, setForm] = useState<AddressRequest>(EMPTY_ADDRESS)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<Address | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    addressesApi
      .fetchAddresses()
      .then((data) => setAddresses(data))
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  const updateForm = <K extends keyof AddressRequest>(key: K, value: AddressRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const openAddForm = () => {
    setForm(EMPTY_ADDRESS)
    setSaveError(null)
    setFormMode('add')
  }

  const openEditForm = (addr: Address) => {
    setForm({
      label: addr.label,
      addressLine1: addr.addressLine1,
      addressLine2: addr.addressLine2,
      city: addr.city,
      state: addr.state,
      postalCode: addr.postalCode,
      country: addr.country,
      isDefault: addr.isDefault,
    })
    setSaveError(null)
    setFormMode(addr.id)
  }

  const closeForm = () => {
    setFormMode('none')
    setSaveError(null)
  }

  const formValid =
    form.addressLine1.trim() !== '' &&
    form.city.trim() !== '' &&
    form.state.trim() !== '' &&
    form.postalCode.trim() !== '' &&
    form.country.trim() !== ''

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (saving || !formValid || formMode === 'none') return
    setSaving(true)
    setSaveError(null)
    try {
      if (formMode === 'add') {
        await addressesApi.createAddress(form)
      } else {
        await addressesApi.updateAddress(formMode, form)
      }
      // Reload rather than patch the touched address in place: setting a
      // new default clears `isDefault` on every other address server-side
      // (see backend AddressService.create/update), so a local merge would
      // leave stale "Default" badges on the others until a manual refresh.
      load()
      setFormMode('none')
    } catch (err) {
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget || deleting) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await addressesApi.deleteAddress(deleteTarget.id)
      setAddresses((prev) => (prev ? prev.filter((a) => a.id !== deleteTarget.id) : prev))
      setDeleteTarget(null)
    } catch (err) {
      setDeleteError(getErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="flex items-center gap-1">
        <BackButton className="-ml-2" />
        <h1 className="font-display text-2xl font-semibold text-zinc-900 sm:text-3xl">My account</h1>
      </div>

      {/* Account info */}
      <section className="mt-6 rounded-xl border border-zinc-200 bg-white p-5 shadow-soft sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-zinc-900 text-lg font-semibold text-white">
              {(user?.fullName ?? user?.email ?? '?').charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="truncate text-base font-semibold text-zinc-900">{user?.fullName}</p>
              <p className="truncate text-sm text-zinc-500">{user?.email}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="min-h-[40px] rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
          >
            Log out
          </button>
        </div>
      </section>

      {/* Orders */}
      <section className="mt-4">
        <Link
          to="/orders"
          className="flex items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-white px-5 py-4 shadow-soft transition-all hover:-translate-y-0.5 hover:border-zinc-300 hover:shadow-elevated"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-zinc-100 text-zinc-500">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.75}
                  d="M9 2.25H7.5a2.25 2.25 0 00-2.25 2.25v13.5a2.25 2.25 0 002.25 2.25h9a2.25 2.25 0 002.25-2.25V4.5a2.25 2.25 0 00-2.25-2.25H15M9 2.25a2.25 2.25 0 002.25 2.25h1.5A2.25 2.25 0 0015 2.25M9 2.25a2.25 2.25 0 012.25-2.25h1.5A2.25 2.25 0 0115 2.25m0 0v.75m-6 4.5h6m-6 3.75h6m-6 3.75h4.5"
                />
              </svg>
            </div>
            <div>
              <p className="text-sm font-semibold text-zinc-900">Order history</p>
              <p className="text-xs text-zinc-500">View your past and current orders</p>
            </div>
          </div>
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 shrink-0 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </Link>
      </section>

      {/* Addresses */}
      <section className="mt-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-zinc-900">Saved addresses</h2>
          {formMode === 'none' && addresses && addresses.length > 0 && (
            <button
              type="button"
              onClick={openAddForm}
              className="text-sm font-medium text-zinc-900 hover:underline"
            >
              + Add address
            </button>
          )}
        </div>

        <div className="mt-3">
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 2 }).map((_, i) => (
                <div key={i} className="rounded-xl border border-zinc-200 bg-white p-4">
                  <SkeletonText width="w-24" />
                  <SkeletonBlock className="mt-2 h-10 w-full" />
                </div>
              ))}
            </div>
          ) : error ? (
            <ErrorState message={error} onRetry={load} />
          ) : addresses && addresses.length === 0 && formMode === 'none' ? (
            <EmptyState
              icon={<AddressIcon />}
              title="No saved addresses yet"
              message="Add an address to speed up checkout next time."
              ctaLabel="Add address"
              onCta={openAddForm}
            />
          ) : (
            <div className="space-y-3">
              {addresses?.map((addr) =>
                formMode === addr.id ? (
                  <AddressForm
                    key={addr.id}
                    form={form}
                    onChange={updateForm}
                    onSubmit={handleSubmit}
                    onCancel={closeForm}
                    saving={saving}
                    error={saveError}
                    submitLabel="Save changes"
                  />
                ) : (
                  <div
                    key={addr.id}
                    className="flex items-start justify-between gap-3 rounded-xl border border-zinc-200 bg-white p-4 shadow-soft sm:p-5"
                  >
                    <div className="min-w-0 text-sm">
                      {addr.label && <p className="font-medium text-zinc-900">{addr.label}</p>}
                      <p className="mt-0.5 leading-relaxed text-zinc-600">{formatAddress(addr).join(', ')}</p>
                      {addr.isDefault && (
                        <span className="mt-2 inline-block rounded-full bg-zinc-100 px-2 py-0.5 text-[11px] font-medium text-zinc-600">
                          Default
                        </span>
                      )}
                    </div>
                    <div className="flex shrink-0 items-center gap-1">
                      <button
                        type="button"
                        onClick={() => openEditForm(addr)}
                        aria-label="Edit address"
                        className="flex h-10 w-10 items-center justify-center rounded-full text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-900"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={1.75}
                            d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487z"
                          />
                        </svg>
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeleteTarget(addr)}
                        aria-label="Delete address"
                        className="flex h-10 w-10 items-center justify-center rounded-full text-zinc-500 transition-colors hover:bg-rose-50 hover:text-rose-600"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={1.75}
                            d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
                          />
                        </svg>
                      </button>
                    </div>
                  </div>
                ),
              )}

              {formMode === 'add' && (
                <AddressForm
                  form={form}
                  onChange={updateForm}
                  onSubmit={handleSubmit}
                  onCancel={closeForm}
                  saving={saving}
                  error={saveError}
                  submitLabel="Save address"
                />
              )}

              {formMode === 'none' && addresses && addresses.length === 0 && (
                <button
                  type="button"
                  onClick={openAddForm}
                  className="min-h-[40px] text-sm font-medium text-zinc-900 hover:underline"
                >
                  + Add address
                </button>
              )}
            </div>
          )}
        </div>
      </section>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete address"
        message={
          deleteTarget
            ? `Remove ${deleteTarget.label ? `"${deleteTarget.label}"` : 'this address'} from your saved addresses?`
            : ''
        }
        confirmLabel="Delete"
        danger
        confirming={deleting}
        error={deleteError}
        onConfirm={handleDelete}
        onCancel={() => {
          setDeleteTarget(null)
          setDeleteError(null)
        }}
      />
    </div>
  )
}
