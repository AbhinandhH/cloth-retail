import { api } from './client'
import type { Category, Color, ProductDetail, ProductListResponse, Size } from '../types'

export interface ProductQuery {
  page?: number
  size?: number
  categoryId?: string | number
  subCategoryId?: string | number
  sizeId?: string | number
  colorId?: string | number
  minPrice?: number | string
  maxPrice?: number | string
  q?: string
}

export function fetchProducts(query: ProductQuery) {
  const params: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value
    }
  }
  return api.get<ProductListResponse>('/products', { params }).then((r) => r.data)
}

export function fetchProductBySlug(slug: string) {
  return api.get<ProductDetail>(`/products/${slug}`).then((r) => r.data)
}

export function fetchCategories() {
  return api.get<Category[]>('/categories').then((r) => r.data)
}

export function fetchSizes() {
  return api.get<Size[]>('/sizes').then((r) => r.data)
}

export function fetchColors() {
  return api.get<Color[]>('/colors').then((r) => r.data)
}
