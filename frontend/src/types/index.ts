// Types matching the backend API contract exactly.

export type Role = 'CUSTOMER' | 'ADMIN' | 'SUPER_ADMIN'

export interface User {
  id: number | string
  fullName: string
  email: string
  roles: Role[]
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

export interface RefreshResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: FieldError[]
}

export interface ProductListItem {
  id: number | string
  slug: string
  name: string
  brand: string
  primaryImageUrl: string
  minPrice: number
  maxPrice: number
  discountPercent: number
  categoryName: string
  inStock: boolean
}

export interface ProductListResponse {
  content: ProductListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProductVariant {
  id: number | string
  sku: string
  sizeName: string
  colorName: string
  colorHex: string
  sellingPrice: number
  discountPercent: number
  stockQuantity: number
  images: string[]
}

export interface ProductDetail {
  id: number | string
  slug: string
  name: string
  description: string
  categoryName: string
  subCategoryName: string
  brand: string
  material: string
  variants: ProductVariant[]
}

export interface Category {
  id: number | string
  name: string
  slug: string
}

export interface Size {
  id: number | string
  name: string
}

export interface Color {
  id: number | string
  name: string
  hexCode: string
}
