import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import * as adminMastersApi from '../api/adminMasters'
import { getErrorMessage } from '../api/client'
import { useCategories, useRefreshMaster, useSizes } from '../context/MasterDataContext'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import ConfirmDialog from '../components/ConfirmDialog'
import SelectField from '../components/SelectField'
import TextField from '../components/TextField'
import type { AdminSizeGroup, SizeGroupRequest } from '../types'

interface FormState {
  name: string
  description: string
  displayOrder: string
  active: boolean
  categoryIds: string[]
  /** Ordered — the array order is the group's size display order. */
  sizeIds: string[]
}

function blankForm(): FormState {
  return { name: '', description: '', displayOrder: '0', active: true, categoryIds: [], sizeIds: [] }
}

function formFromGroup(group: AdminSizeGroup): FormState {
  return {
    name: group.name,
    description: group.description ?? '',
    displayOrder: String(group.displayOrder),
    active: group.active,
    categoryIds: group.categoryIds.map(String),
    sizeIds: group.sizes
      .slice()
      .sort((a, b) => a.displayOrder - b.displayOrder)
      .map((s) => String(s.id)),
  }
}

export default function AdminSizeGroups() {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage size groups.</p>
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

  return <AdminSizeGroupsContent />
}

function AdminSizeGroupsContent() {
  const refreshMaster = useRefreshMaster()
  const { data: categories } = useCategories()
  const { data: sizes } = useSizes()

  const [rows, setRows] = useState<AdminSizeGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 400)
  const [activeFilter, setActiveFilter] = useState<'' | 'true' | 'false'>('')

  const [rowMessage, setRowMessage] = useState<string | null>(null)
  const [rowError, setRowError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<AdminSizeGroup | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<AdminSizeGroup | null>(null)
  const [form, setForm] = useState<FormState>(blankForm())
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminMastersApi
      .fetchAdminSizeGroups({ q: debouncedSearch || undefined, active: activeFilter === '' ? undefined : activeFilter === 'true' })
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
    setFormOpen(true)
  }

  const openEdit = (group: AdminSizeGroup) => {
    setEditing(group)
    setForm(formFromGroup(group))
    setSaveError(null)
    setFormOpen(true)
  }

  const closeForm = () => {
    if (saving) return
    setFormOpen(false)
    setEditing(null)
  }

  const toggleCategory = (id: string) => {
    setForm((prev) => ({
      ...prev,
      categoryIds: prev.categoryIds.includes(id) ? prev.categoryIds.filter((c) => c !== id) : [...prev.categoryIds, id],
    }))
  }

  const toggleSize = (id: string) => {
    setForm((prev) => ({
      ...prev,
      sizeIds: prev.sizeIds.includes(id) ? prev.sizeIds.filter((s) => s !== id) : [...prev.sizeIds, id],
    }))
  }

  const moveSize = (index: number, direction: -1 | 1) => {
    setForm((prev) => {
      const target = index + direction
      if (target < 0 || target >= prev.sizeIds.length) return prev
      const next = prev.sizeIds.slice()
      ;[next[index], next[target]] = [next[target], next[index]]
      return { ...prev, sizeIds: next }
    })
  }

  const handleSubmit = async () => {
    setSaving(true)
    setSaveError(null)
    try {
      const payload: SizeGroupRequest = {
        name: form.name.trim(),
        description: form.description.trim() === '' ? null : form.description.trim(),
        displayOrder: form.displayOrder.trim() === '' ? 0 : Number(form.displayOrder),
        active: form.active,
        categoryIds: form.categoryIds.map(Number),
        sizeIds: form.sizeIds.map(Number),
      }
      if (editing) {
        await adminMastersApi.updateSizeGroup(editing.id, payload)
        setRowMessage(`"${form.name}" was updated.`)
      } else {
        await adminMastersApi.createSizeGroup(payload)
        setRowMessage(`"${form.name}" was added.`)
      }
      refreshMaster('sizes')
      setFormOpen(false)
      setEditing(null)
      load()
    } catch (err) {
      setSaveError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const toggleActive = async (group: AdminSizeGroup) => {
    setRowMessage(null)
    setRowError(null)
    try {
      const payload: SizeGroupRequest = {
        name: group.name,
        description: group.description,
        displayOrder: group.displayOrder,
        active: !group.active,
        categoryIds: group.categoryIds,
        sizeIds: group.sizes.slice().sort((a, b) => a.displayOrder - b.displayOrder).map((s) => s.id),
      }
      await adminMastersApi.updateSizeGroup(group.id, payload)
      setRowMessage(`"${group.name}" is now ${group.active ? 'inactive' : 'active'}.`)
      refreshMaster('sizes')
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
      await adminMastersApi.deleteSizeGroup(deleteTarget.id)
      setRowMessage(`"${deleteTarget.name}" was deleted.`)
      setDeleteTarget(null)
      refreshMaster('sizes')
      load()
    } catch (err) {
      setDeleteError(getErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  const orderedSelectedSizes = useMemo(
    () => form.sizeIds.map((id) => sizes.find((s) => String(s.id) === id)).filter((s): s is NonNullable<typeof s> => Boolean(s)),
    [form.sizeIds, sizes],
  )

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Size Groups</h1>
          <p className="mt-1 text-sm text-zinc-500">
            Scope which sizes apply to which categories — e.g. shoe sizes vs. apparel sizes.
          </p>
        </div>
        <div className="flex items-center gap-4">
          <Link to="/admin/masters" className="text-sm font-medium text-zinc-600 hover:text-zinc-900">
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
              <th className="px-4 py-3">Categories</th>
              <th className="px-4 py-3">Sizes</th>
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
              rows.map((group) => (
                <tr key={group.id} className="hover:bg-zinc-50">
                  <td className="px-4 py-3">
                    <p className="font-medium text-zinc-900">{group.name}</p>
                    {group.description && <p className="text-xs text-zinc-400">{group.description}</p>}
                  </td>
                  <td className="px-4 py-3 text-zinc-600">{group.categoryIds.length}</td>
                  <td className="px-4 py-3 text-zinc-600">{group.sizes.length}</td>
                  <td className="px-4 py-3 text-zinc-600">{group.active ? 'Active' : 'Inactive'}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <button type="button" onClick={() => openEdit(group)} className="text-xs font-medium text-zinc-700 hover:underline">
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => toggleActive(group)}
                        className={`text-xs font-medium hover:underline ${group.active ? 'text-amber-700' : 'text-emerald-700'}`}
                      >
                        {group.active ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setDeleteError(null)
                          setDeleteTarget(group)
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
          <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl border border-zinc-200 bg-white p-6 shadow-xl">
            <h2 className="text-base font-semibold text-zinc-900">{editing ? 'Edit size group' : 'Add size group'}</h2>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <TextField label="Name" value={form.name} onChange={(v) => setForm((p) => ({ ...p, name: v ?? '' }))} />
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

            {/* Categories multi-select */}
            <div className="mt-6">
              <h3 className="text-sm font-semibold text-zinc-900">Categories</h3>
              <p className="mt-0.5 text-xs text-zinc-500">Which categories this size group applies to.</p>
              <div className="mt-2 grid max-h-40 grid-cols-2 gap-x-4 gap-y-1 overflow-y-auto rounded-md border border-zinc-200 p-3 sm:grid-cols-3">
                {categories.length === 0 ? (
                  <p className="text-xs text-zinc-400">No categories available.</p>
                ) : (
                  categories.map((c) => (
                    <label key={c.id} className="flex items-center gap-2 text-sm text-zinc-700">
                      <input
                        type="checkbox"
                        checked={form.categoryIds.includes(String(c.id))}
                        onChange={() => toggleCategory(String(c.id))}
                        className="h-4 w-4"
                      />
                      {c.name}
                    </label>
                  ))
                )}
              </div>
            </div>

            {/* Sizes ordered multi-select */}
            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <h3 className="text-sm font-semibold text-zinc-900">Available sizes</h3>
                <p className="mt-0.5 text-xs text-zinc-500">Check to add to this group.</p>
                <div className="mt-2 max-h-56 space-y-1 overflow-y-auto rounded-md border border-zinc-200 p-3">
                  {sizes.length === 0 ? (
                    <p className="text-xs text-zinc-400">No sizes available.</p>
                  ) : (
                    sizes.map((s) => (
                      <label key={s.id} className="flex items-center gap-2 text-sm text-zinc-700">
                        <input
                          type="checkbox"
                          checked={form.sizeIds.includes(String(s.id))}
                          onChange={() => toggleSize(String(s.id))}
                          className="h-4 w-4"
                        />
                        {s.name}
                      </label>
                    ))
                  )}
                </div>
              </div>
              <div>
                <h3 className="text-sm font-semibold text-zinc-900">Selected order</h3>
                <p className="mt-0.5 text-xs text-zinc-500">Reorder with the arrows — this is the display order.</p>
                <div className="mt-2 max-h-56 space-y-1 overflow-y-auto rounded-md border border-zinc-200 p-3">
                  {orderedSelectedSizes.length === 0 ? (
                    <p className="text-xs text-zinc-400">No sizes selected yet.</p>
                  ) : (
                    orderedSelectedSizes.map((s, index) => (
                      <div key={s.id} className="flex items-center justify-between gap-2 rounded-md bg-zinc-50 px-2 py-1 text-sm">
                        <span className="text-zinc-800">{s.name}</span>
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            title="Move earlier"
                            onClick={() => moveSize(index, -1)}
                            disabled={index === 0}
                            className="text-zinc-500 hover:text-zinc-800 disabled:opacity-30"
                          >
                            &uarr;
                          </button>
                          <button
                            type="button"
                            title="Move later"
                            onClick={() => moveSize(index, 1)}
                            disabled={index === orderedSelectedSizes.length - 1}
                            className="text-zinc-500 hover:text-zinc-800 disabled:opacity-30"
                          >
                            &darr;
                          </button>
                          <button
                            type="button"
                            title="Remove"
                            onClick={() => toggleSize(String(s.id))}
                            className="text-rose-500 hover:text-rose-700"
                          >
                            &times;
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
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
                disabled={saving || !form.name.trim()}
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
        title="Delete size group"
        message={deleteTarget ? `Delete "${deleteTarget.name}"? This cannot be undone.` : ''}
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
