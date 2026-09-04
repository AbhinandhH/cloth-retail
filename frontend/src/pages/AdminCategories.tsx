import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminCategory } from '../types'

export default function AdminCategories() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminCategory>>(
    () => ({
      title: 'Categories',
      description: 'Top-level product categories shown across the storefront and product form.',
      fetchList: (params) => adminMastersApi.fetchAdminCategories(params),
      create: async (payload) => {
        const created = await adminMastersApi.createCategory(payload)
        refreshMaster('categories')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateCategory(id, payload)
        refreshMaster('categories')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteCategory(id)
        refreshMaster('categories')
      },
      rowLabel: (row) => row.name,
      fields: [
        { key: 'name', label: 'Name', type: 'text', required: true },
        { key: 'slug', label: 'Slug', type: 'text', required: true },
        { key: 'displayOrder', label: 'Display order', type: 'number', required: true },
        { key: 'active', label: 'Active', type: 'checkbox' },
      ],
      columns: [
        { key: 'name', label: 'Name' },
        { key: 'slug', label: 'Slug' },
        { key: 'displayOrder', label: 'Order' },
        { key: 'active', label: 'Status', render: (row) => (row.active ? 'Active' : 'Inactive') },
        { key: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleDateString() },
      ],
    }),
    [refreshMaster],
  )

  return <MasterCrudPage config={config} />
}
