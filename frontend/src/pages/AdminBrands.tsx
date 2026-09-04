import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminBrand } from '../types'

export default function AdminBrands() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminBrand>>(
    () => ({
      title: 'Brands',
      description: 'The brand options available when creating or editing a product.',
      fetchList: (params) => adminMastersApi.fetchAdminBrands(params),
      create: async (payload) => {
        const created = await adminMastersApi.createBrand(payload)
        refreshMaster('brands')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateBrand(id, payload)
        refreshMaster('brands')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteBrand(id)
        refreshMaster('brands')
      },
      rowLabel: (row) => row.name,
      fields: [
        { key: 'name', label: 'Name', type: 'text', required: true },
        { key: 'displayOrder', label: 'Display order', type: 'number', required: true },
        { key: 'active', label: 'Active', type: 'checkbox' },
      ],
      columns: [
        { key: 'name', label: 'Name' },
        { key: 'displayOrder', label: 'Order' },
        { key: 'active', label: 'Status', render: (row) => (row.active ? 'Active' : 'Inactive') },
        { key: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleDateString() },
      ],
    }),
    [refreshMaster],
  )

  return <MasterCrudPage config={config} />
}
