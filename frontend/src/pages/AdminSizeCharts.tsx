import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as adminMastersApi from '../api/adminMasters'
import { getErrorMessage } from '../api/client'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import ConfirmDialog from '../components/ConfirmDialog'
import SelectField from '../components/SelectField'
import TextField from '../components/TextField'
import type { AdminSizeChart, SizeChartRequest } from '../types'

/** Icon-only "back to masters" affordance - no text, matching the other admin screens' back links. */
function MastersBackLink({ className = '' }: { className?: string }) {
  return (
    <Link
      to="/admin/masters"
      aria-label="Back to masters"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  )
}

interface FormRow {
  sizeLabel: string
  /** Positionally aligned to FormState.columns - values[i] is this row's value for columns[i]. */
  values: string[]
}

interface FormState {
  name: string
  description: string
  displayOrder: string
  active: boolean
  columns: string[]
  rows: FormRow[]
}

function blankForm(): FormState {
  return { name: '', description: '', displayOrder: '0', active: true, columns: ['Chest', 'Waist'], rows: [] }
}

interface FieldErrors {
  name?: string
  columns?: string
  rows?: string
}

/** Mirrors the backend's own validation (SizeChartAdminRequest/SizeChartRowRequest) so the
 * admin sees the same messages before ever submitting, instead of just a disabled button. */
function validateForm(form: FormState): FieldErrors {
  const errors: FieldErrors = {}
  if (form.name.trim() === '') {
    errors.name = 'Enter a name for this size chart.'
  }
  if (form.columns.length === 0) {
    errors.columns = 'Add at least one measurement column.'
  } else if (form.columns.some((c) => c.trim() === '')) {
    errors.columns = "Column names can't be empty — remove any blank ones."
  }
  if (form.rows.some((r) => r.sizeLabel.trim() === '')) {
    errors.rows = 'Give every row a size label, e.g. "M" or "42".'
  }
  return errors
}

function formFromChart(chart: AdminSizeChart): FormState {
  return {
    name: chart.name,
    description: chart.description ?? '',
    displayOrder: String(chart.displayOrder),
    active: chart.active,
    columns: chart.columns.slice(),
    rows: chart.rows
      .slice()
      .sort((a, b) => a.displayOrder - b.displayOrder)
      .map((r) => ({ sizeLabel: r.sizeLabel, values: r.values.slice() })),
  }
}

export default function AdminSizeCharts() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage size charts.</p>
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

  return <AdminSizeChartsContent />
}

function AdminSizeChartsContent() {
  const [rows, setRows] = useState<AdminSizeChart[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [activeFilter, setActiveFilter] = useState<'' | 'true' | 'false'>('')

  const [rowMessage, setRowMessage] = useState<string | null>(null)
  const [rowError, setRowError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<AdminSizeChart | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<AdminSizeChart | null>(null)
  const [form, setForm] = useState<FormState>(blankForm())
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminMastersApi
      .fetchAdminSizeCharts({ q: debouncedSearch || undefined, active: activeFilter === '' ? undefined : activeFilter === 'true' })
      .then(setRows)
      .catch((err) => {
        setError(getErrorMessage(err))
        setRows([])
      })
      .finally(() => setLoading(false))
  }, [debouncedSearch, activeFilter])

  useEffect(() => {
    load()
  }, [load])

  const openAdd = () => {
    setEditing(null)
    setForm(blankForm())
    setSaveError(null)
    setFieldErrors({})
    setFormOpen(true)
  }

  const openEdit = (chart: AdminSizeChart) => {
    setEditing(chart)
    setForm(formFromChart(chart))
    setSaveError(null)
    setFieldErrors({})
    setFormOpen(true)
  }

  const closeForm = () => {
    if (saving) return
    setFormOpen(false)
    setEditing(null)
  }

  const addColumn = () => {
    setForm((prev) => ({
      ...prev,
      columns: [...prev.columns, ''],
      rows: prev.rows.map((r) => ({ ...r, values: [...r.values, ''] })),
    }))
    setFieldErrors((prev) => ({ ...prev, columns: undefined }))
  }

  const updateColumn = (index: number, value: string) => {
    setForm((prev) => {
      const next = prev.columns.slice()
      next[index] = value
      return { ...prev, columns: next }
    })
    setFieldErrors((prev) => ({ ...prev, columns: undefined }))
  }

  const removeColumn = (index: number) => {
    setForm((prev) => ({
      ...prev,
      columns: prev.columns.filter((_, i) => i !== index),
      rows: prev.rows.map((r) => ({ ...r, values: r.values.filter((_, i) => i !== index) })),
    }))
  }

  const addRow = () => {
    setForm((prev) => ({ ...prev, rows: [...prev.rows, { sizeLabel: '', values: prev.columns.map(() => '') }] }))
  }

  const updateRowLabel = (rowIndex: number, value: string) => {
    setForm((prev) => {
      const next = prev.rows.slice()
      next[rowIndex] = { ...next[rowIndex], sizeLabel: value }
      return { ...prev, rows: next }
    })
    setFieldErrors((prev) => ({ ...prev, rows: undefined }))
  }

  const updateRowValue = (rowIndex: number, colIndex: number, value: string) => {
    setForm((prev) => {
      const next = prev.rows.slice()
      const values = next[rowIndex].values.slice()
      values[colIndex] = value
      next[rowIndex] = { ...next[rowIndex], values }
      return { ...prev, rows: next }
    })
  }

  const removeRow = (index: number) => {
    setForm((prev) => ({ ...prev, rows: prev.rows.filter((_, i) => i !== index) }))
  }

  const handleSubmit = async () => {
    const errors = validateForm(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    setSaveError(null)
    try {
      const payload: SizeChartRequest = {
        name: form.name.trim(),
        description: form.description.trim() === '' ? null : form.description.trim(),
        displayOrder: form.displayOrder.trim() === '' ? 0 : Number(form.displayOrder),
        active: form.active,
        columns: form.columns.map((c) => c.trim()),
        rows: form.rows.map((r) => ({ sizeLabel: r.sizeLabel.trim(), values: r.values })),
      }
      if (editing) {
        await adminMastersApi.updateSizeChart(editing.id, payload)
        setRowMessage(`"${form.name}" was updated.`)
      } else {
        await adminMastersApi.createSizeChart(payload)
        setRowMessage(`"${form.name}" was added.`)
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

  const toggleActive = async (chart: AdminSizeChart) => {
    setRowMessage(null)
    setRowError(null)
    try {
      const payload: SizeChartRequest = {
        name: chart.name,
        description: chart.description,
        displayOrder: chart.displayOrder,
        active: !chart.active,
        columns: chart.columns,
        rows: chart.rows
          .slice()
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map((r) => ({ sizeLabel: r.sizeLabel, values: r.values })),
      }
      await adminMastersApi.updateSizeChart(chart.id, payload)
      setRowMessage(`"${chart.name}" is now ${chart.active ? 'inactive' : 'active'}.`)
      load()
    } catch (err) {
      setRowError(getErrorMessage(err))
    }
  }

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await adminMastersApi.deleteSizeChart(deleteTarget.id)
      setRowMessage(`"${deleteTarget.name}" was deleted.`)
      setDeleteTarget(null)
      load()
    } catch (err) {
      setDeleteError(getErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-2">
          <MastersBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">Size Charts</h1>
            <p className="mt-1 text-sm text-zinc-500">
              Reusable measurement tables — assign one to a product from its Details tab.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={openAdd}
          className="shrink-0 rounded-xl bg-zinc-900 px-5 py-3 text-sm font-semibold text-white transition-all duration-150 hover:bg-zinc-800 active:scale-[0.97]"
        >
          + Add
        </button>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-[1fr_200px]">
        <input
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search…"
          className="w-full rounded-lg border border-zinc-300 px-4 py-2.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
        />
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

      <div className="mt-6 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr className="text-left text-xs font-semibold uppercase tracking-wide text-zinc-500">
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Columns</th>
              <th className="px-4 py-3">Rows</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 bg-white">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-zinc-500">
                  Loading…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-zinc-500">
                  Nothing found.
                </td>
              </tr>
            ) : (
              rows.map((chart) => (
                <tr key={chart.id} className="hover:bg-zinc-50">
                  <td className="px-4 py-3">
                    <p className="font-medium text-zinc-900">{chart.name}</p>
                    {chart.description && <p className="text-xs text-zinc-400">{chart.description}</p>}
                  </td>
                  <td className="px-4 py-3 text-zinc-600">{chart.columns.join(', ')}</td>
                  <td className="px-4 py-3 text-zinc-600">{chart.rows.length}</td>
                  <td className="px-4 py-3 text-zinc-600">{chart.active ? 'Active' : 'Inactive'}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <button type="button" onClick={() => openEdit(chart)} className="text-xs font-medium text-zinc-700 hover:underline">
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => toggleActive(chart)}
                        className={`text-xs font-medium hover:underline ${chart.active ? 'text-amber-700' : 'text-emerald-700'}`}
                      >
                        {chart.active ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setDeleteError(null)
                          setDeleteTarget(chart)
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

      {formOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/50 px-4" role="dialog" aria-modal="true">
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl border border-zinc-200 bg-white p-6 shadow-xl">
            <h2 className="text-base font-semibold text-zinc-900">{editing ? 'Edit size chart' : 'Add size chart'}</h2>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <TextField
                label="Name"
                value={form.name}
                onChange={(v) => {
                  setForm((p) => ({ ...p, name: v ?? '' }))
                  setFieldErrors((p) => ({ ...p, name: undefined }))
                }}
                error={fieldErrors.name}
              />
              <TextField
                label="Display order"
                type="number"
                value={form.displayOrder}
                onChange={(v) => setForm((p) => ({ ...p, displayOrder: v ?? '' }))}
              />
            </div>

            <div className="mt-4">
              <TextField
                label="Description"
                value={form.description}
                onChange={(v) => setForm((p) => ({ ...p, description: v ?? '' }))}
                textarea
              />
            </div>

            <label className="mt-4 flex items-center gap-2 text-sm font-medium text-zinc-900">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm((p) => ({ ...p, active: e.target.checked }))}
                className="h-4 w-4"
              />
              Active
            </label>

            {/* Measurement columns - custom per chart, e.g. Chest/Waist/Length for tops or
                just Foot Length for footwear. Editing a column here reshapes every row below
                it, in the same position. */}
            <div className="mt-6">
              <h3 className="text-sm font-semibold text-zinc-900">Measurement columns</h3>
              <p className="mt-0.5 text-xs text-zinc-500">
                E.g. "Chest", "Waist", "Length" — every row below gets one value per column.
              </p>
              <div className="mt-2 space-y-2">
                {form.columns.map((col, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <input
                      type="text"
                      value={col}
                      onChange={(e) => updateColumn(i, e.target.value)}
                      placeholder={`Column ${i + 1}`}
                      className="flex-1 rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
                    />
                    <button
                      type="button"
                      onClick={() => removeColumn(i)}
                      title="Remove column"
                      className="shrink-0 text-rose-500 hover:text-rose-700"
                    >
                      &times;
                    </button>
                  </div>
                ))}
                <button type="button" onClick={addColumn} className="text-xs font-medium text-zinc-700 hover:underline">
                  + Add column
                </button>
                {fieldErrors.columns && <p className="text-xs text-rose-600">{fieldErrors.columns}</p>}
              </div>
            </div>

            {/* Rows - one per size, values aligned by position to the columns above. */}
            <div className="mt-6">
              <h3 className="text-sm font-semibold text-zinc-900">Rows</h3>
              <p className="mt-0.5 text-xs text-zinc-500">One row per size — the label shown is free text (e.g. "M" or "42").</p>
              <div className="mt-2 overflow-x-auto rounded-md border border-zinc-200">
                <table className="min-w-full divide-y divide-zinc-200 text-sm">
                  <thead className="bg-zinc-50">
                    <tr className="text-left text-xs font-semibold uppercase tracking-wide text-zinc-500">
                      <th className="px-3 py-2">Size</th>
                      {form.columns.map((col, i) => (
                        <th key={i} className="px-3 py-2">
                          {col || `Column ${i + 1}`}
                        </th>
                      ))}
                      <th className="px-3 py-2" />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100 bg-white">
                    {form.rows.length === 0 ? (
                      <tr>
                        <td colSpan={form.columns.length + 2} className="px-3 py-4 text-center text-zinc-400">
                          No rows yet.
                        </td>
                      </tr>
                    ) : (
                      form.rows.map((row, rIdx) => (
                        <tr key={rIdx}>
                          <td className="px-2 py-1.5">
                            <input
                              type="text"
                              value={row.sizeLabel}
                              onChange={(e) => updateRowLabel(rIdx, e.target.value)}
                              placeholder="e.g. M"
                              className="w-20 rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
                            />
                          </td>
                          {row.values.map((val, cIdx) => (
                            <td key={cIdx} className="px-2 py-1.5">
                              <input
                                type="text"
                                value={val}
                                onChange={(e) => updateRowValue(rIdx, cIdx, e.target.value)}
                                placeholder="e.g. 38–40&quot;"
                                className="w-28 rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
                              />
                            </td>
                          ))}
                          <td className="px-2 py-1.5">
                            <button
                              type="button"
                              onClick={() => removeRow(rIdx)}
                              title="Remove row"
                              className="text-rose-500 hover:text-rose-700"
                            >
                              &times;
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
              <button type="button" onClick={addRow} className="mt-2 text-xs font-medium text-zinc-700 hover:underline">
                + Add row
              </button>
              {fieldErrors.rows && <p className="mt-1 text-xs text-rose-600">{fieldErrors.rows}</p>}
            </div>

            {saveError && (
              <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{saveError}</div>
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
                onClick={handleSubmit}
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
        title="Delete size chart"
        message={
          deleteTarget
            ? `Delete "${deleteTarget.name}"? This cannot be undone. Any product using it must be reassigned first.`
            : ''
        }
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

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <MastersBackLink />
      </div>
    </div>
  )
}
