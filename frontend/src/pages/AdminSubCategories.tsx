import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useCategories, useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminSubCategory } from '../types'

/**
 * Spec calls this page out as "bespoke — not generic" because it needs a
 * parent-Category <select> and a category-name table column. Both of those
 * turned out to be exactly what MasterCrudPage's existing `type: 'select'`
 * field (with dynamically-loaded `options`) and a column `render` already
 * support — so this still goes through MasterCrudPage rather than
 * duplicating ~250 lines of list/search/filter/form/delete chrome for no
 * behavioral difference. The one non-generic bit (categoryId needs to be a
 * number in the request body, not the string a <select> naturally produces)
 * is handled by wrapping create/update below.
 */
export default function AdminSubCategories() {
  const refreshMaster = useRefreshMaster()
  const { data: categories, loading: categoriesLoading } = useCategories()

  const categoryOptions = useMemo(
    () => categories.map((c) => ({ value: String(c.id), label: c.name })),
    [categories],
  )

  const config = useMemo<MasterCrudConfig<AdminSubCategory>>(
    () => ({
      title: 'Sub-Categories',
      description: 'Second-level groupings nested under a parent category.',
      fetchList: (params) => adminMastersApi.fetchAdminSubCategories(params),
      create: async (payload) => {
        const created = await adminMastersApi.createSubCategory({ ...payload, categoryId: Number(payload.categoryId) })
        refreshMaster('categories')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateSubCategory(id, {
          ...payload,
          categoryId: Number(payload.categoryId),
        })
        refreshMaster('categories')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteSubCategory(id)
        refreshMaster('categories')
      },
      rowLabel: (row) => row.name,
      fields: [
        { key: 'name', label: 'Name', type: 'text', required: true },
        { key: 'slug', label: 'Slug', type: 'text', required: true },
        {
          key: 'categoryId',
          label: 'Parent category',
          type: 'select',
          required: true,
          options: categoryOptions,
        },
        { key: 'displayOrder', label: 'Display order', type: 'number', required: true },
        { key: 'active', label: 'Active', type: 'checkbox' },
      ],
      columns: [
        { key: 'name', label: 'Name' },
        { key: 'slug', label: 'Slug' },
        { key: 'categoryName', label: 'Category' },
        { key: 'displayOrder', label: 'Order' },
        { key: 'active', label: 'Status', render: (row) => (row.active ? 'Active' : 'Inactive') },
        { key: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleDateString() },
      ],
    }),
    [refreshMaster, categoryOptions],
  )

  if (categoriesLoading && categories.length === 0) {
    return <div className="flex min-h-[40vh] items-center justify-center text-sm text-zinc-500">Loading categories…</div>
  }

  return <MasterCrudPage config={config} />
}
