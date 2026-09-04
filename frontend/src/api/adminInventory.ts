import { api } from './client'
import type {
  InventoryDamageRow,
  InventoryDashboard,
  InventoryTransactionRow,
  InventoryVariantRow,
  MarkDamagedResult,
  PageResponse,
  StockAdjustResult,
} from '../types'

function cleanParams<T extends object>(query: T) {
  const params: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value as string | number
    }
  }
  return params
}

export interface VariantQuery {
  page?: number
  size?: number
  q?: string
  categoryId?: string | number
  colorId?: string | number
  sizeId?: string | number
  stockStatus?: string
  productStatus?: string
  sort?: 'name' | 'stock' | 'updatedAt'
  dir?: 'asc' | 'desc'
}

export function fetchInventoryVariants(query: VariantQuery) {
  return api
    .get<PageResponse<InventoryVariantRow>>('/admin/inventory/variants', { params: cleanParams(query) })
    .then((r) => r.data)
}

export function fetchInventoryDashboard() {
  return api.get<InventoryDashboard>('/admin/inventory/dashboard').then((r) => r.data)
}

export interface AdjustStockPayload {
  quantityChange: number
  reason: string
}

export function adjustStock(variantId: number | string, payload: AdjustStockPayload) {
  return api
    .post<StockAdjustResult>(`/admin/inventory/variants/${variantId}/adjust`, payload)
    .then((r) => r.data)
}

export interface MarkDamagedPayload {
  quantity: number
  /** The DamageReason master's id — was a `reason` enum string, now `reasonId`. */
  reasonId: number
  notes?: string | null
}

export function markDamaged(variantId: number | string, payload: MarkDamagedPayload) {
  return api
    .post<MarkDamagedResult>(`/admin/inventory/variants/${variantId}/damage`, payload)
    .then((r) => r.data)
}

export interface TransactionQuery {
  page?: number
  size?: number
  variantId?: string | number
  type?: string
  dateFrom?: string
  dateTo?: string
}

export function fetchTransactions(query: TransactionQuery) {
  return api
    .get<PageResponse<InventoryTransactionRow>>('/admin/inventory/transactions', { params: cleanParams(query) })
    .then((r) => r.data)
}

export interface DamageQuery {
  page?: number
  size?: number
  variantId?: string | number
}

export function fetchDamages(query: DamageQuery) {
  return api
    .get<PageResponse<InventoryDamageRow>>('/admin/inventory/damages', { params: cleanParams(query) })
    .then((r) => r.data)
}
