import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  createAdmin,
  fetchStaff,
  fetchStaffPermissions,
  setStaffStatus,
  updateStaffPermissions,
} from '../api/adminStaff'
import { getErrorMessage } from '../api/client'
import type { AdminModule, AdminStaffRow, ModulePermissionRow } from '../types'

const MODULE_LABELS: Record<AdminModule, string> = {
  DASHBOARD: 'Dashboard',
  PRODUCTS: 'Products',
  INVENTORY: 'Inventory',
  ORDERS: 'Orders',
  RETURNS: 'Returns',
  CUSTOMERS: 'Customers',
  MASTERS: 'Masters',
  REPORTS: 'Reports',
}
const MODULES: AdminModule[] = ['DASHBOARD', 'PRODUCTS', 'INVENTORY', 'ORDERS', 'RETURNS', 'CUSTOMERS', 'MASTERS', 'REPORTS']

function emptyGrants(): ModulePermissionRow[] {
  return MODULES.map((module) => ({ module, canView: false, canEdit: false }))
}

/** Icon-only "back to admin home" affordance, matching the other admin screens' back links. */
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

export default function AdminStaff() {
  // isStoreAdmin already covers SUPER_ADMIN (the software owner inherits every ADMIN privilege -
  // see SecurityConfig's RoleHierarchy bean), so anyone reaching this page gets the full staff
  // management UI - there's no reduced/read-only variant to branch on anymore.
  const { isStoreAdmin } = useAuth()

  if (!isStoreAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to manage staff accounts.</p>
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

  return <AdminStaffContent />
}

function AdminStaffContent() {
  const [rows, setRows] = useState<AdminStaffRow[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setLoadError(null)
    fetchStaff()
      .then(setRows)
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'ADMIN' | 'EMPLOYEE'>('EMPLOYEE')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    try {
      await createAdmin({ fullName, email, password, role })
      setFullName('')
      setEmail('')
      setPassword('')
      load()
    } catch (err) {
      setCreateError(getErrorMessage(err))
    } finally {
      setCreating(false)
    }
  }

  const [expandedId, setExpandedId] = useState<number | string | null>(null)
  const [grants, setGrants] = useState<ModulePermissionRow[]>(emptyGrants())
  const [permissionsLoading, setPermissionsLoading] = useState(false)
  const [permissionsError, setPermissionsError] = useState<string | null>(null)
  const [savingPermissions, setSavingPermissions] = useState(false)

  const toggleExpand = (row: AdminStaffRow) => {
    if (expandedId === row.id) {
      setExpandedId(null)
      return
    }
    setExpandedId(row.id)
    setPermissionsLoading(true)
    setPermissionsError(null)
    fetchStaffPermissions(row.id)
      .then(setGrants)
      .catch((err) => setPermissionsError(getErrorMessage(err)))
      .finally(() => setPermissionsLoading(false))
  }

  const toggleGrant = (module: AdminModule, field: 'canView' | 'canEdit') => {
    setGrants((prev) =>
      prev.map((g) => {
        if (g.module !== module) return g
        if (field === 'canEdit') {
          const canEdit = !g.canEdit
          return { ...g, canEdit, canView: canEdit ? true : g.canView }
        }
        const canView = !g.canView
        return { ...g, canView, canEdit: canView ? g.canEdit : false }
      }),
    )
  }

  const savePermissions = async (id: number | string) => {
    setSavingPermissions(true)
    setPermissionsError(null)
    try {
      const updated = await updateStaffPermissions(id, grants)
      setGrants(updated)
    } catch (err) {
      setPermissionsError(getErrorMessage(err))
    } finally {
      setSavingPermissions(false)
    }
  }

  const handleToggleStatus = async (row: AdminStaffRow) => {
    try {
      await setStaffStatus(row.id, !row.enabled)
      load()
    } catch (err) {
      setLoadError(getErrorMessage(err))
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Staff</h1>
          <p className="mt-1 text-sm text-zinc-500">Admin and employee accounts, and what each one can see and change.</p>
        </div>
      </div>

      <form onSubmit={handleCreate} className="mt-6 rounded-xl border border-zinc-200 p-4">
        <p className="text-sm font-medium text-zinc-900">Add admin or employee</p>
        {createError && (
          <div className="mt-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{createError}</div>
        )}
        <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <input
            type="text"
            required
            placeholder="Full name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
          />
          <input
            type="email"
            required
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
          />
          <input
            type="password"
            required
            minLength={8}
            placeholder="Password (min 8 characters)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
          />
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as 'ADMIN' | 'EMPLOYEE')}
            className="rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none"
          >
            <option value="EMPLOYEE">Employee</option>
            <option value="ADMIN">Admin</option>
          </select>
        </div>
        <button
          type="submit"
          disabled={creating}
          className="mt-4 rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {creating ? 'Creating…' : 'Create account'}
        </button>
      </form>

      {loadError && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{loadError}</div>
      )}

      <div className="mt-6 overflow-hidden rounded-lg border border-zinc-200">
        <table className="min-w-full divide-y divide-zinc-200 text-sm">
          <thead className="bg-zinc-50">
            <tr>
              <th className="px-3 py-2 text-left font-medium text-zinc-600">Name</th>
              <th className="px-3 py-2 text-left font-medium text-zinc-600">Email</th>
              <th className="px-3 py-2 text-left font-medium text-zinc-600">Role</th>
              <th className="px-3 py-2 text-left font-medium text-zinc-600">Status</th>
              <th className="px-3 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 bg-white">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-3 py-3">
                  <div className="h-4 animate-pulse rounded bg-zinc-100" />
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-3 py-10 text-center text-zinc-500">
                  No staff accounts yet.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <>
                  <tr key={row.id} className="hover:bg-zinc-50">
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-800">{row.fullName}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{row.email}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-700">{row.role}</td>
                    <td className="whitespace-nowrap px-3 py-2">
                      <span
                        className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${
                          row.enabled ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-zinc-200 bg-zinc-100 text-zinc-500'
                        }`}
                      >
                        {row.enabled ? 'ACTIVE' : 'DISABLED'}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2 text-right">
                      <button
                        type="button"
                        onClick={() => toggleExpand(row)}
                        className="mr-2 rounded-md border border-zinc-300 px-2 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50"
                      >
                        {expandedId === row.id ? 'Close' : 'Permissions'}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleToggleStatus(row)}
                        className="rounded-md border border-zinc-300 px-2 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50"
                      >
                        {row.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                  {expandedId === row.id && (
                    <tr key={`${row.id}-permissions`}>
                      <td colSpan={5} className="bg-zinc-50 px-3 py-4">
                        {permissionsError && (
                          <div className="mb-3 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                            {permissionsError}
                          </div>
                        )}
                        {permissionsLoading ? (
                          <div className="h-16 animate-pulse rounded bg-zinc-100" />
                        ) : (
                          <>
                            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                              {MODULES.map((module) => {
                                const grant = grants.find((g) => g.module === module)!
                                return (
                                  <div key={module} className="rounded-md border border-zinc-200 bg-white p-2">
                                    <p className="text-xs font-medium text-zinc-700">{MODULE_LABELS[module]}</p>
                                    <label className="mt-1 flex items-center gap-1.5 text-xs text-zinc-600">
                                      <input
                                        type="checkbox"
                                        checked={grant.canView}
                                        onChange={() => toggleGrant(module, 'canView')}
                                        className="h-3.5 w-3.5 rounded border-zinc-300"
                                      />
                                      View
                                    </label>
                                    <label className="mt-1 flex items-center gap-1.5 text-xs text-zinc-600">
                                      <input
                                        type="checkbox"
                                        checked={grant.canEdit}
                                        onChange={() => toggleGrant(module, 'canEdit')}
                                        className="h-3.5 w-3.5 rounded border-zinc-300"
                                      />
                                      Edit
                                    </label>
                                  </div>
                                )
                              })}
                            </div>
                            <button
                              type="button"
                              disabled={savingPermissions}
                              onClick={() => savePermissions(row.id)}
                              className="mt-3 rounded-md bg-zinc-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-zinc-800 disabled:opacity-60"
                            >
                              {savingPermissions ? 'Saving…' : 'Save permissions'}
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  )}
                </>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>
    </div>
  )
}
