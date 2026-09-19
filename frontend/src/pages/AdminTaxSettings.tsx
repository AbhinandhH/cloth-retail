import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as taxApi from '../api/tax'
import { getErrorMessage } from '../api/client'
import TextField from '../components/TextField'

/** Icon-only "back to admin home" affordance - no text, matching the other admin screens' back links. */
function AdminHomeBackLink({ className = '' }: { className?: string }) {
  return (
    <Link
      to="/admin"
      aria-label="Back to admin home"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  )
}

export default function AdminTaxSettings() {
  // Tax settings are store-governance, ADMIN-only (the store owner) - SUPER_ADMIN (the software
  // owner) has no store-level access at all, see RoleName's own doc comment.
  const { isStoreAdmin } = useAuth()

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [cgstPercent, setCgstPercent] = useState('0')
  const [sgstPercent, setSgstPercent] = useState('0')

  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!isStoreAdmin) return
    taxApi
      .fetchTaxSettings()
      .then((data) => {
        setCgstPercent(String(data.cgstPercent))
        setSgstPercent(String(data.sgstPercent))
      })
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [isStoreAdmin])

  if (!isStoreAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage tax settings.</p>
          <Link
            to="/admin"
            className="mt-6 inline-block rounded-lg border border-zinc-700 px-4 py-2 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
          >
            Back to admin home
          </Link>
        </div>
      </div>
    )
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSaveError(null)
    setSaveMessage(null)
    setSaving(true)
    try {
      const saved = await taxApi.updateTaxSettings({
        cgstPercent: Number(cgstPercent) || 0,
        sgstPercent: Number(sgstPercent) || 0,
      })
      setCgstPercent(String(saved.cgstPercent))
      setSgstPercent(String(saved.sgstPercent))
      setSaveMessage('Tax settings saved.')
    } catch (err) {
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const total = (Number(cgstPercent) || 0) + (Number(sgstPercent) || 0)

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Tax</h1>
          <p className="mt-1 text-sm text-zinc-500">
            CGST and SGST rates applied to every new order's goods total. Changes only affect orders placed after
            saving - an order already placed keeps whatever rate was in effect when it was created.
          </p>
        </div>
      </div>

      {loading ? (
        <div className="mt-8 flex min-h-[20vh] items-center justify-center text-sm text-zinc-500">Loading…</div>
      ) : loadError ? (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{loadError}</div>
      ) : (
        <div className="mt-8 rounded-xl border border-zinc-200 p-4">
          <p className="text-sm font-medium text-zinc-900">GST rates</p>
          <p className="mt-0.5 text-xs text-zinc-500">
            Standard intra-state GST split - CGST and SGST together add up to the total GST rate (e.g. 9% + 9% for an
            18% slab). Applied to each order's product total after any discount, before shipping.
          </p>

          {saveError && (
            <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{saveError}</div>
          )}
          {saveMessage && !saveError && (
            <div className="mt-3 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {saveMessage}
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <TextField label="CGST %" type="number" value={cgstPercent} onChange={(v) => setCgstPercent(v ?? '0')} />
              <TextField label="SGST %" type="number" value={sgstPercent} onChange={(v) => setSgstPercent(v ?? '0')} />
            </div>
            <p className="text-xs text-zinc-500">Total GST: {total}%</p>

            <button
              type="submit"
              disabled={saving}
              className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {saving ? 'Saving…' : 'Save tax settings'}
            </button>
          </form>
        </div>
      )}

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
