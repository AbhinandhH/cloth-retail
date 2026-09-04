import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminMaterial } from '../types'

export default function AdminMaterials() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminMaterial>>(
    () => ({
      title: 'Materials',
      description: 'The fabric/material options available when creating or editing a product.',
      fetchList: (params) => adminMastersApi.fetchAdminMaterials(params),
      create: async (payload) => {
        const created = await adminMastersApi.createMaterial(payload)
        refreshMaster('materials')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateMaterial(id, payload)
        refreshMaster('materials')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteMaterial(id)
        refreshMaster('materials')
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
