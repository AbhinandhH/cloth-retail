import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminDamageReason } from '../types'

export default function AdminDamageReasons() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminDamageReason>>(
    () => ({
      title: 'Damage Reasons',
      description: 'Reasons available when marking inventory stock as damaged.',
      fetchList: (params) => adminMastersApi.fetchAdminDamageReasons(params),
      create: async (payload) => {
        const created = await adminMastersApi.createDamageReason(payload)
        refreshMaster('damageReasons')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateDamageReason(id, payload)
        refreshMaster('damageReasons')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteDamageReason(id)
        refreshMaster('damageReasons')
      },
      rowLabel: (row) => row.name,
      fields: [
        { key: 'name', label: 'Name', type: 'text', required: true },
        { key: 'code', label: 'Code', type: 'text', required: true },
        { key: 'description', label: 'Description', type: 'textarea' },
        { key: 'displayOrder', label: 'Display order', type: 'number', required: true },
        { key: 'active', label: 'Active', type: 'checkbox' },
      ],
      columns: [
        { key: 'name', label: 'Name' },
        { key: 'code', label: 'Code' },
        { key: 'description', label: 'Description', render: (row) => row.description ?? '—' },
        { key: 'displayOrder', label: 'Order' },
        { key: 'active', label: 'Status', render: (row) => (row.active ? 'Active' : 'Inactive') },
        { key: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleDateString() },
      ],
    }),
    [refreshMaster],
  )

  return <MasterCrudPage config={config} backTo="/admin/masters" />
}
