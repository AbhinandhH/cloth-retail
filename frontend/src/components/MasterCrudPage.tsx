import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../api/client'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import ConfirmDialog from './ConfirmDialog'
import SelectField from './SelectField'
import TextField from './TextField'

export interface MasterFieldConfig {
  key: string
  label: string
  type: 'text' | 'textarea' | 'number' | 'checkbox' | 'color' | 'select'
  required?: boolean
  options?: { value: string; label: string }[] // for type: 'select'
}

export interface MasterColumnConfig<T> {
  key: string
  label: string
  render?: (row: T) => ReactNode
}

export interface MasterCrudConfig<T extends { id: number | string }> {
  title: string
  description: string
  fetchList: (params: { q?: string; active?: boolean }) => Promise<T[]>
  create: (payload: Record<string, unknown>) => Promise<T>
  update: (id: T['id'], payload: Record<string, unknown>) => Promise<T>
  remove: (id: T['id']) => Promise<void>
  fields: MasterFieldConfig[] // drives the add/edit form
  columns: MasterColumnConfig<T>[] // drives the table
  /** Row label used in confirm/success messages, e.g. (row) => row.name. Defaults to String(row.id). */
  rowLabel?: (row: T) => string
}

function defaultValueFor(field: MasterFieldConfig): unknown {
  if (field.type === 'checkbox') return field.key === 'active'
  return ''
}

function blankFormValues(fields: MasterFieldConfig[]): Record<string, unknown> {
  return Object.fromEntries(fields.map((f) => [f.key, defaultValueFor(f)]))
}

function valuesFromRow<T>(fields: MasterFieldConfig[], row: T): Record<string, unknown> {
  return Object.fromEntries(fields.map((f) => [f.key, (row as Record<string, unknown>)[f.key]]))
}

function buildPayload(fields: MasterFieldConfig[], values: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const f of fields) {
    const raw = values[f.key]
    if (f.type === 'number') {
      out[f.key] = raw === '' || raw === null || raw === undefined ? (f.required ? 0 : null) : Number(raw)
    } else if (f.type === 'checkbox') {
      out[f.key] = Boolean(raw)
    } else {
      const str = typeof raw === 'string' ? raw.trim() : raw
      out[f.key] = str === '' && !f.required ? null : str
    }
  }
  return out
}

function FieldInput({
  field,
  value,
  onChange,
  error,
}: {
  field: MasterFieldConfig
  value: unknown
  onChange: (value: unknown) => void
  error?: string
}) {
  switch (field.type) {
    case 'textarea':
      return (
        <TextField label={field.label} value={(value as string) ?? ''} onChange={(v) => onChange(v ?? '')} textarea />
      )
    case 'number':
      return (
        <TextField
          label={field.label}
          value={value === null || value === undefined ? '' : String(value)}
          onChange={(v) => onChange(v ?? '')}
          type="number"
        />
      )
    case 'checkbox':
      return (
        <label className="flex items-center gap-2 pt-6 text-sm font-medium text-zinc-900">
          <input
            type="checkbox"
            checked={Boolean(value)}
            onChange={(e) => onChange(e.target.checked)}
            className="h-4 w-4"
          />
          {field.label}
        </label>
      )
    case 'select':
      return (
        <SelectField
          label={field.label}
          value={(value as string) ?? ''}
          onChange={onChange}
          placeholder={`Select ${field.label.toLowerCase()}`}
          required={field.required}
          options={field.options ?? []}
          error={error}
        />
      )
    case 'color':
      return (
        <div>
          <label className="block text-sm font-medium text-zinc-900">{field.label}</label>
          <div className="mt-1 flex items-center gap-2">
            <input
              type="color"
              value={(value as string) || '#000000'}
              onChange={(e) => onChange(e.target.value)}
              className="h-9 w-12 shrink-0 rounded border border-zinc-300"
            />
            <input
              type="text"
              value={(value as string) ?? ''}
              onChange={(e) => onChange(e.target.value)}
              placeholder="#RRGGBB"
              className="flex-1 rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
          </div>
          {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
        </div>
      )
    default:
      return <TextField label={field.label} value={(value as string) ?? ''} onChange={(v) => onChange(v ?? '')} />
  }
}

/**
 * Generic list+form admin page for a "master data" table: search, active/
 * inactive filter, client-side-paginated-free table (these lists are small),
 * add/edit via an inline modal panel, and delete via ConfirmDialog surfacing
 * the backend's 409 message. Table columns and form fields are entirely
 * driven by `config` — this component makes no assumptions about a specific
 * master's fields beyond `id` and (for the built-in activate/deactivate
 * action) `active`.
 */
export default function MasterCrudPage<T extends { id: number | string; active?: boolean }>({
  config,
  backTo = '/admin/masters',
}: {
  config: MasterCrudConfig<T>
  backTo?: string
}) {
  const { isAdmin } = useAuth()

  const [rows, setRows] = useState<T[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [activeFilter, setActiveFilter] = useState<'' | 'true' | 'false'>('')

  const [rowMessage, setRowMessage] = useState<string | null>(null)
  const [rowError, setRowError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<T['id'] | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<T | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<T | null>(null)
  const [formValues, setFormValues] = useState<Record<string, unknown>>({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const rowLabel = useCallback((row: T) => (config.rowLabel ? config.rowLabel(row) : String(row.id)), [config])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    config
      .fetchList({ q: debouncedSearch || undefined, active: activeFilter === '' ? undefined : activeFilter === 'true' })
      .then(setRows)
      .catch((err) => {
        setError(getErrorMessage(err))
        setRows([])
      })
      .finally(() => setLoading(false))
    // config.fetchList is recreated per-render by the calling page (useMemo there keys it) —
    // depending on it directly is intentional so a config swap reloads.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch, activeFilter, config.fetchList])

  useEffect(() => {
    load()
  }, [load])

  const openAdd = () => {
    setEditing(null)
    setFormValues(blankFormValues(config.fields))
    setSaveError(null)
    setFormOpen(true)
  }

  const openEdit = (row: T) => {
    setEditing(row)
    setFormValues(valuesFromRow(config.fields, row))
    setSaveError(null)
    setFormOpen(true)
  }

  const closeForm = () => {
    if (saving) return
    setFormOpen(false)
    setEditing(null)
  }

  const handleFormSubmit = async () => {
    setSaving(true)
    setSaveError(null)
    try {
      const payload = buildPayload(config.fields, formValues)
      if (editing) {
        await config.update(editing.id, payload)
        setRowMessage(`"${rowLabel(editing)}" was updated.`)
      } else {
        const created = await config.create(payload)
        setRowMessage(`"${rowLabel(created)}" was added.`)
      }
      setFormOpen(false)
      setEditing(null)
      load()
    } catch (err) {
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const toggleActive = async (row: T) => {
    setBusyId(row.id)
    setRowMessage(null)
    setRowError(null)
    try {
      const payload = buildPayload(config.fields, { ...valuesFromRow(config.fields, row), active: !row.active })
      await config.update(row.id, payload)
      setRowMessage(`"${rowLabel(row)}" is now ${row.active ? 'inactive' : 'active'}.`)
      load()
    } catch (err) {
      setRowError(getErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await config.remove(deleteTarget.id)
      setRowMessage(`"${rowLabel(deleteTarget)}" was deleted.`)
      setDeleteTarget(null)
      load()
    } catch (err) {
      setDeleteError(getErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  const hasActiveField = useMemo(() => config.fields.some((f) => f.key === 'active'), [config.fields])

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage {config.title.toLowerCase()}.</p>
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

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">{config.title}</h1>
          <p className="mt-1 text-sm text-zinc-500">{config.description}</p>
        </div>
        <div className="flex items-center gap-4">
          <Link to={backTo} className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
            &larr; Masters
          </Link>
          <button
            type="button"
            onClick={openAdd}
            className="rounded-lg bg-zinc-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-zinc-700"
          >
            + Add
          </button>
        </div>
      </div>

      {/* Toolbar */}
      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-[1fr_200px]">
        <div>
          <label htmlFor="master-search" className="sr-only">
            Search
          </label>
          <input
            id="master-search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search…"
            className="w-full rounded-lg border border-zinc-300 px-4 py-2.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
        </div>
        <SelectField
          label="Status"
          value={activeFilter}
          onChange={(v) => setActiveFilter(v as '' | 'true' | 'false')}
          placeholder="All statuses"
          options={[
            { value: 'true', label: 'Active only' },
            { value: 'false', label: 'Inactive only' },
          ]}
        />
      </div>

      {rowMessage && (
        <div className="mt-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {rowMessage}
        </div>
      )}
      {rowError && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{rowError}</div>
      )}
      {error && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
      )}

      {/* Table */}
      <div className="mt-6 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr className="text-left text-xs font-semibold uppercase tracking-wide text-zinc-500">
              {config.columns.map((col) => (
                <th key={col.key} className="px-4 py-3">
                  {col.label}
                </th>
              ))}
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 bg-white">
            {loading ? (
              <tr>
                <td colSpan={config.columns.length + 1} className="px-4 py-10 text-center text-zinc-500">
                  Loading…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={config.columns.length + 1} className="px-4 py-10 text-center text-zinc-500">
                  Nothing found.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="hover:bg-zinc-50">
                  {config.columns.map((col) => (
                    <td key={col.key} className="px-4 py-3 text-zinc-700">
                      {col.render ? col.render(row) : String((row as Record<string, unknown>)[col.key] ?? '—')}
                    </td>
                  ))}
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <button
                        type="button"
                        onClick={() => openEdit(row)}
                        className="text-xs font-medium text-zinc-700 hover:underline"
                      >
                        Edit
                      </button>
                      {hasActiveField && (
                        <button
                          type="button"
                          disabled={busyId === row.id}
                          onClick={() => toggleActive(row)}
                          className={`text-xs font-medium hover:underline disabled:opacity-50 ${
                            row.active ? 'text-amber-700' : 'text-emerald-700'
                          }`}
                        >
                          {row.active ? 'Deactivate' : 'Activate'}
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => {
                          setDeleteError(null)
                          setDeleteTarget(row)
                        }}
                        className="text-xs font-medium text-rose-600 hover:underline"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-sm text-zinc-500">{loading ? '' : `${rows.length} record${rows.length === 1 ? '' : 's'}`}</p>

      {/* Add/edit panel */}
      {formOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/50 px-4" role="dialog" aria-modal="true">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-zinc-200 bg-white p-6 shadow-xl">
            <h2 className="text-base font-semibold text-zinc-900">
              {editing ? `Edit ${config.title.replace(/s$/, '')}` : `Add ${config.title.replace(/s$/, '')}`}
            </h2>

            <div className="mt-4 space-y-4">
              {config.fields.map((field) => (
                <FieldInput
                  key={field.key}
                  field={field}
                  value={formValues[field.key]}
                  onChange={(v) => setFormValues((prev) => ({ ...prev, [field.key]: v }))}
                />
              ))}
            </div>

            {saveError && (
              <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                {saveError}
              </div>
            )}

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={closeForm}
                disabled={saving}
                className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleFormSubmit}
                disabled={saving}
                className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-semibold text-white hover:bg-zinc-700 disabled:opacity-60"
              >
                {saving ? 'Saving…' : editing ? 'Save' : 'Add'}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        title={`Delete ${config.title.replace(/s$/, '').toLowerCase()}`}
        message={deleteTarget ? `Delete "${rowLabel(deleteTarget)}"? This cannot be undone.` : ''}
        confirmLabel="Delete"
        danger
        confirming={deleting}
        error={deleteError}
        onConfirm={handleDeleteConfirm}
        onCancel={() => {
          setDeleteTarget(null)
          setDeleteError(null)
        }}
      />
    </div>
  )
}
