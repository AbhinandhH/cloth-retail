import { useMemo } from 'react'
import * as adminMastersApi from '../api/adminMasters'
import { useRefreshMaster } from '../context/MasterDataContext'
import MasterCrudPage from '../components/MasterCrudPage'
import type { MasterCrudConfig } from '../components/MasterCrudPage'
import type { AdminColor } from '../types'

export default function AdminColors() {
  const refreshMaster = useRefreshMaster()

  const config = useMemo<MasterCrudConfig<AdminColor>>(
    () => ({
      title: 'Colors',
      description: 'The color swatches available when creating product variants.',
      fetchList: (params) => adminMastersApi.fetchAdminColors(params),
      create: async (payload) => {
        const created = await adminMastersApi.createColor(payload)
        refreshMaster('colors')
        return created
      },
      update: async (id, payload) => {
        const updated = await adminMastersApi.updateColor(id, payload)
        refreshMaster('colors')
        return updated
      },
      remove: async (id) => {
        await adminMastersApi.deleteColor(id)
        refreshMaster('colors')
      },
      rowLabel: (row) => row.name,
      fields: [
        { key: 'name', label: 'Name', type: 'text', required: true },
        { key: 'hexCode', label: 'Hex code', type: 'color', required: true },
        { key: 'displayOrder', label: 'Display order', type: 'number', required: true },
        { key: 'active', label: 'Active', type: 'checkbox' },
      ],
      columns: [
        {
          key: 'name',
          label: 'Name',
          render: (row) => (
            <span className="flex items-center gap-2">
              <span
                className="h-4 w-4 shrink-0 rounded-full border border-zinc-300"
                style={{ backgroundColor: row.hexCode || undefined }}
              />
              {row.name}
            </span>
          ),
        },
        { key: 'hexCode', label: 'Hex' },
        { key: 'displayOrder', label: 'Order' },
        { key: 'active', label: 'Status', render: (row) => (row.active ? 'Active' : 'Inactive') },
        { key: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleDateString() },
      ],
    }),
    [refreshMaster],
  )

  return <MasterCrudPage config={config} />
}
