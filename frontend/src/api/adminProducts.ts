import { api } from './client'
import type {
  AdminProductDetail,
  AdminProductListResponse,
  AdminVendor,
  Brand,
  Material,
  ProductAdminRequest,
  ProductStatus,
  SubCategory,
} from '../types'

export interface AdminProductQuery {
  page?: number
  size?: number
  q?: string
  categoryId?: string | number
  status?: string
  sort?: string
}

export function fetchAdminProducts(query: AdminProductQuery) {
  const params: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value
    }
  }
  return api.get<AdminProductListResponse>('/admin/products', { params }).then((r) => r.data)
}

export function fetchAdminProduct(id: number | string) {
  return api.get<AdminProductDetail>(`/admin/products/${id}`).then((r) => r.data)
}

export function createAdminProduct(payload: ProductAdminRequest) {
  return api.post<AdminProductDetail>('/admin/products', payload).then((r) => r.data)
}

export function updateAdminProduct(id: number | string, payload: ProductAdminRequest) {
  return api.put<AdminProductDetail>(`/admin/products/${id}`, payload).then((r) => r.data)
}

export function updateAdminProductStatus(id: number | string, status: ProductStatus) {
  return api.patch<AdminProductDetail>(`/admin/products/${id}/status`, { status }).then((r) => r.data)
}

export function deleteAdminProduct(id: number | string) {
  return api.delete<void>(`/admin/products/${id}`).then((r) => r.data)
}

/** Admin master-data lookups needed by the product form — not the public/active-only lists. */
export function fetchBrands() {
  return api.get<Brand[]>('/admin/brands').then((r) => r.data)
}

export function fetchMaterials() {
  return api.get<Material[]>('/admin/materials').then((r) => r.data)
}

export function fetchVendors() {
  return api.get<AdminVendor[]>('/admin/vendors').then((r) => r.data)
}

/** Public endpoint (no admin-specific sub-categories list exists) — filterable by categoryId. */
export function fetchSubCategories(categoryId?: string | number) {
  const params = categoryId ? { categoryId } : undefined
  return api.get<SubCategory[]>('/sub-categories', { params }).then((r) => r.data)
}
