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

export interface Theme {
  id: number | string
  name: string
  primaryColor: string
  secondaryColor: string
  accentColor: string | null
  backgroundColor: string
  textColor: string
  displayOrder?: number
}

/** The color set embedded in SiteConfiguration — same shape as Theme minus id/displayOrder. */
export interface ThemeColors {
  name: string
  primaryColor: string
  secondaryColor: string
  accentColor: string | null
  backgroundColor: string
  textColor: string
}

export interface SiteConfiguration {
  theme: ThemeColors | null
  businessName: string | null
  tagline: string | null
  logoUrl: string | null
  faviconUrl: string | null
  contactEmail: string | null
  contactPhone: string | null
  instagramUrl: string | null
  whatsappNumber: string | null
  facebookUrl: string | null
  footerText: string | null
  loginBackgroundImageUrl: string | null
  loginPromoImageUrl: string | null
  loginPromoText: string | null
  registrationImageUrl: string | null
}

/**
 * GET /admin/configuration response shape. The actual backend field is
 * `activeTheme` (a nested Theme object with `id`) — also accepting
 * `activeThemeId`/`theme` here since those were the originally-contracted
 * possibilities; see getActiveThemeId() in api/configuration.ts, which reads
 * whichever of the three is present.
 */
export interface AdminSiteConfiguration extends Omit<SiteConfiguration, 'theme'> {
  activeThemeId?: number | string | null
  activeTheme?: (ThemeColors & { id?: number | string }) | null
  theme?: (ThemeColors & { id?: number | string }) | null
}

// --- Admin product management -------------------------------------------
// NOTE: ProductStatus is declared once, below, alongside PageResponse (both
// are shared with the inventory/dashboard screens built in parallel).

export interface Brand {
  id: number | string
  name: string
  displayOrder: number
  active: boolean
}

export interface Material {
  id: number | string
  name: string
  displayOrder: number
  active: boolean
}

export interface SubCategory {
  id: number | string
  name: string
  slug: string
  categoryId: number | string
}

export interface AdminProductListItem {
  id: number | string
  name: string
  slug: string
  baseSku: string | null
  categoryName: string
  brandName: string | null
  status: ProductStatus
  variantCount: number
  totalStock: number
  updatedAt: string
  /**
   * Not part of the contracted list DTO (see AdminProductList.tsx notes) — the
   * backend list response has no image field today. Kept optional so the
   * thumbnail column upgrades automatically if the backend adds one later;
   * until then the list falls back to a placeholder.
   */
  primaryImageUrl?: string | null
}

/** Alias kept for readability at call sites — same generic PageResponse used by inventory. */
export type AdminProductListResponse = PageResponse<AdminProductListItem>

export interface AdminProductImage {
  id?: number | string
  url: string
  displayOrder: number
  primary: boolean
}

export interface AdminProductVariant {
  id?: number | string
  sku: string
  sizeId: number | string
  sizeName: string
  colorId: number | string
  colorName: string
  sellingPrice: number
  costPrice: number | null
  discountPercent: number
  stockQuantity: number
  reservedQuantity: number
  damagedQuantity: number
  availableQuantity: number
  lowStockThreshold: number | null
  active: boolean
  images: AdminProductImage[]
}

export interface AdminProductDetail {
  id: number | string
  categoryId: number | string
  categoryName: string
  subCategoryId: number | string | null
  subCategoryName: string | null
  brandId: number | string | null
  brandName: string | null
  materialId: number | string
  materialName: string
  name: string
  slug: string
  description: string
  status: ProductStatus
  baseSku: string | null
  baseSellingPrice: number | null
  baseCostPrice: number | null
  variants: AdminProductVariant[]
}

export interface ProductVariantImageRequest {
  url: string
  displayOrder: number
  primary: boolean
}

export interface ProductVariantRequest {
  id?: number | string | null
  sku: string
  sizeId: number | string
  colorId: number | string
  sellingPrice: number
  costPrice: number | null
  discountPercent: number
  stockQuantity: number
  lowStockThreshold: number | null
  active: boolean
  images: ProductVariantImageRequest[]
}

export interface ProductAdminRequest {
  categoryId: number | string
  subCategoryId: number | string | null
  brandId: number | string | null
  materialId: number | string
  name: string
  slug: string
  description: string
  status: ProductStatus
  baseSku: string | null
  baseSellingPrice: number | null
  baseCostPrice: number | null
  variants: ProductVariantRequest[]
}

// --- Admin inventory ---

/** Generic paginated response shape used by the admin inventory endpoints. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'
export type InventoryTransactionType =
  | 'PURCHASE_IN'
  | 'SALE_OUT'
  | 'RETURN_IN'
  | 'ADJUSTMENT'
  | 'CANCEL_REVERSAL'
  | 'DAMAGE'
export type DamageReason = 'DEFECTIVE' | 'TRANSIT_DAMAGE' | 'WAREHOUSE_DAMAGE' | 'RETURN_DAMAGE' | 'OTHER'

/** One row of GET /admin/inventory/variants, and the shape embedded in the dashboard's low/out-of-stock lists. */
export interface InventoryVariantRow {
  variantId: number | string
  productId: number | string
  productName: string
  productSlug: string
  sku: string
  categoryName: string
  colorName: string
  colorHex: string
  sizeName: string
  sellingPrice: number
  costPrice: number | null
  stockQuantity: number
  reservedQuantity: number
  damagedQuantity: number
  availableQuantity: number
  lowStockThreshold: number | null
  stockStatus: StockStatus
  active: boolean
  productStatus: ProductStatus
  updatedAt: string
}

export interface InventoryRecentProduct {
  id: number | string
  name: string
  slug: string
  status: ProductStatus
  createdAt: string
}

export interface InventoryTransactionRow {
  id: number | string
  variantId: number | string
  sku: string
  productName: string
  type: InventoryTransactionType
  quantity: number
  previousQuantity: number
  newQuantity: number
  reason: string | null
  referenceType: string | null
  referenceId: number | string | null
  performedByName: string | null
  createdAt: string
}

export interface InventoryDamageRow {
  id: number | string
  variantId: number | string
  sku: string
  productName: string
  quantity: number
  reason: DamageReason
  notes: string | null
  reportedByName: string | null
  createdAt: string
}

export interface InventoryDashboard {
  totalProducts: number
  totalVariants: number
  totalAvailableStock: number
  lowStockCount: number
  outOfStockCount: number
  totalDamagedStock: number
  lowStockItems: InventoryVariantRow[]
  outOfStockItems: InventoryVariantRow[]
  recentProducts: InventoryRecentProduct[]
  recentTransactions: InventoryTransactionRow[]
  recentDamages: InventoryDamageRow[]
}

export interface StockAdjustResult {
  variantId: number | string
  previousQuantity: number
  newQuantity: number
  availableQuantity: number
}

export interface MarkDamagedResult {
  variantId: number | string
  damagedQuantity: number
  availableQuantity: number
}
