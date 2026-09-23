import { api } from './client'
import type {
  AdminBrand,
  AdminCategory,
  AdminColor,
  AdminDamageReason,
  AdminMaterial,
  AdminSize,
  AdminSizeChart,
  AdminSizeGroup,
  AdminSubCategory,
  AdminVendor,
  DamageReasonOption,
  Size,
  SizeChartRequest,
  SizeGroupRequest,
} from '../types'

export interface MasterQuery {
  q?: string
  active?: boolean
}

function cleanParams(query: MasterQuery = {}) {
  const params: Record<string, string> = {}
  if (query.q) params.q = query.q
  if (query.active !== undefined) params.active = String(query.active)
  return params
}

/**
 * Typed CRUD wrapper factory — every admin master endpoint (categories,
 * colors, sizes, brands, materials, sub-categories, damage-reasons,
 * size-groups) follows the same list/create/update/delete shape against
 * `/api/admin/{basePath}[/{id}]`, differing only in the response (T) and
 * request (TReq) payload shapes.
 */
function masterCrud<T, TReq = Record<string, unknown>>(basePath: string) {
  return {
    list: (query: MasterQuery = {}) => api.get<T[]>(basePath, { params: cleanParams(query) }).then((r) => r.data),
    create: (payload: TReq) => api.post<T>(basePath, payload).then((r) => r.data),
    update: (id: number | string, payload: TReq) => api.put<T>(`${basePath}/${id}`, payload).then((r) => r.data),
    remove: (id: number | string) => api.delete<void>(`${basePath}/${id}`).then((r) => r.data),
  }
}

const categoriesCrud = masterCrud<AdminCategory>('/admin/categories')
export const fetchAdminCategories = categoriesCrud.list
export const createCategory = categoriesCrud.create
export const updateCategory = categoriesCrud.update
export const deleteCategory = categoriesCrud.remove

const colorsCrud = masterCrud<AdminColor>('/admin/colors')
export const fetchAdminColors = colorsCrud.list
export const createColor = colorsCrud.create
export const updateColor = colorsCrud.update
export const deleteColor = colorsCrud.remove

const sizesCrud = masterCrud<AdminSize>('/admin/sizes')
export const fetchAdminSizes = sizesCrud.list
export const createSize = sizesCrud.create
export const updateSize = sizesCrud.update
export const deleteSize = sizesCrud.remove

const brandsCrud = masterCrud<AdminBrand>('/admin/brands')
export const fetchAdminBrands = brandsCrud.list
export const createBrand = brandsCrud.create
export const updateBrand = brandsCrud.update
export const deleteBrand = brandsCrud.remove

const materialsCrud = masterCrud<AdminMaterial>('/admin/materials')
export const fetchAdminMaterials = materialsCrud.list
export const createMaterial = materialsCrud.create
export const updateMaterial = materialsCrud.update
export const deleteMaterial = materialsCrud.remove

const vendorsCrud = masterCrud<AdminVendor>('/admin/vendors')
export const fetchAdminVendors = vendorsCrud.list
export const createVendor = vendorsCrud.create
export const updateVendor = vendorsCrud.update
export const deleteVendor = vendorsCrud.remove

const subCategoriesCrud = masterCrud<AdminSubCategory>('/admin/sub-categories')
export const fetchAdminSubCategories = subCategoriesCrud.list
export const createSubCategory = subCategoriesCrud.create
export const updateSubCategory = subCategoriesCrud.update
export const deleteSubCategory = subCategoriesCrud.remove

const damageReasonsCrud = masterCrud<AdminDamageReason>('/admin/damage-reasons')
export const fetchAdminDamageReasons = damageReasonsCrud.list
export const createDamageReason = damageReasonsCrud.create
export const updateDamageReason = damageReasonsCrud.update
export const deleteDamageReason = damageReasonsCrud.remove

const sizeGroupsCrud = masterCrud<AdminSizeGroup, SizeGroupRequest>('/admin/size-groups')
export const fetchAdminSizeGroups = sizeGroupsCrud.list
export const createSizeGroup = sizeGroupsCrud.create
export const updateSizeGroup = sizeGroupsCrud.update
export const deleteSizeGroup = sizeGroupsCrud.remove

const sizeChartsCrud = masterCrud<AdminSizeChart, SizeChartRequest>('/admin/size-charts')
export const fetchAdminSizeCharts = sizeChartsCrud.list
export const createSizeChart = sizeChartsCrud.create
export const updateSizeChart = sizeChartsCrud.update
export const deleteSizeChart = sizeChartsCrud.remove

/** Public, no auth — sizes scoped to a category's size group (falls back to a full list backend-side). */
export function fetchAvailableSizesForCategory(categoryId: number | string) {
  return api.get<Size[]>(`/categories/${categoryId}/available-sizes`).then((r) => r.data)
}

/** Public, no auth, active-only. */
export function fetchPublicDamageReasons() {
  return api.get<DamageReasonOption[]>('/damage-reasons').then((r) => r.data)
}
