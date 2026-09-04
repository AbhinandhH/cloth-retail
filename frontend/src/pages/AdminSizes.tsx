import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminSize } from '../types'

export default function AdminSizes() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminSize>>(
    () => ({
      title: 'Sizes',
      description: 'The full set of sizes available system-wide — group them under Size Groups to scope by category.',
      fetchList: (params) => adminMastersApi.fetchAdminSizes(params),
      create: async (payload) => {
        const created = await adminMastersApi.createSize(payload)
        refreshMaster('sizes')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateSize(id, payload)
        refreshMaster('sizes')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteSize(id)
        refreshMaster('sizes')
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
