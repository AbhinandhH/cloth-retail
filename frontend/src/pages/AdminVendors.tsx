import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminVendor } from '../types'

export default function AdminVendors() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminVendor>>(
    () => ({
      title: 'Vendors',
      description: 'Suppliers and manufacturers used for purchasing and inventory sourcing.',
      fetchList: (params) => adminMastersApi.fetchAdminVendors(params),
      create: async (payload) => {
        const created = await adminMastersApi.createVendor(payload)
        refreshMaster('vendors')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateVendor(id, payload)
        refreshMaster('vendors')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteVendor(id)
        refreshMaster('vendors')
      },
      rowLabel: (row) => row.name,
      fields: [
        { key: 'name', label: 'Name', type: 'text', required: true },
        { key: 'contactName', label: 'Contact name', type: 'text' },
        { key: 'contactEmail', label: 'Contact email', type: 'text' },
        { key: 'contactPhone', label: 'Contact phone', type: 'text' },
        { key: 'active', label: 'Active', type: 'checkbox' },
      ],
      columns: [
        { key: 'name', label: 'Name' },
        { key: 'contactName', label: 'Contact name', render: (row) => row.contactName ?? '—' },
        { key: 'contactEmail', label: 'Contact email', render: (row) => row.contactEmail ?? '—' },
        { key: 'contactPhone', label: 'Contact phone', render: (row) => row.contactPhone ?? '—' },
        { key: 'active', label: 'Status', render: (row) => (row.active ? 'Active' : 'Inactive') },
        { key: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleDateString() },
      ],
    }),
    [refreshMaster],
  )

  return <MasterCrudPage config={config} />
}
