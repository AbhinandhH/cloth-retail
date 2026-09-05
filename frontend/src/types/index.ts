// Types matching the backend API contract exactly.

export type Role = "CUSTOMER" | "ADMIN" | "SUPER_ADMIN";

export interface User {
  id: number | string;
  fullName: string;
  email: string;
  roles: Role[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: User;
}

export interface RefreshResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
}

export interface ProductListItem {
  id: number | string;
  slug: string;
  name: string;
  brand: string;
  primaryImageUrl: string;
  minPrice: number;
  maxPrice: number;
  discountPercent: number;
  categoryName: string;
  inStock: boolean;
}

export interface ProductListResponse {
  content: ProductListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductVariant {
  id: number | string;
  sku: string;
  sizeName: string;
  colorName: string;
  colorHex: string;
  sellingPrice: number;
  discountPercent: number;
  stockQuantity: number;
  images: string[];
}

export interface ProductDetail {
  id: number | string;
  slug: string;
  name: string;
  description: string;
  categoryName: string;
  subCategoryName: string;
  brand: string;
  material: string;
  variants: ProductVariant[];
}

export interface Category {
  id: number | string;
  name: string;
  slug: string;
}

export interface Size {
  id: number | string;
  name: string;
}

export interface Color {
  id: number | string;
  name: string;
  hexCode: string;
}

export interface Theme {
  id: number | string;
  name: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string | null;
  backgroundColor: string;
  textColor: string;
  displayOrder?: number;
}

/** The color set embedded in SiteConfiguration — same shape as Theme minus id/displayOrder. */
export interface ThemeColors {
  name: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string | null;
  backgroundColor: string;
  textColor: string;
}

export interface SiteConfiguration {
  theme: ThemeColors | null;
  businessName: string | null;
  tagline: string | null;
  logoUrl: string | null;
  faviconUrl: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  instagramUrl: string | null;
  whatsappNumber: string | null;
  facebookUrl: string | null;
  footerText: string | null;
  loginBackgroundImageUrl: string | null;
  loginPromoImageUrl: string | null;
  loginPromoText: string | null;
  registrationImageUrl: string | null;
}

/**
 * GET /admin/configuration response shape. The actual backend field is
 * `activeTheme` (a nested Theme object with `id`) — also accepting
 * `activeThemeId`/`theme` here since those were the originally-contracted
 * possibilities; see getActiveThemeId() in api/configuration.ts, which reads
 * whichever of the three is present.
 */
export interface AdminSiteConfiguration extends Omit<
  SiteConfiguration,
  "theme"
> {
  activeThemeId?: number | string | null;
  activeTheme?: (ThemeColors & { id?: number | string }) | null;
  theme?: (ThemeColors & { id?: number | string }) | null;
}

// --- Admin product management -------------------------------------------
// NOTE: ProductStatus is declared once, below, alongside PageResponse (both
// are shared with the inventory/dashboard screens built in parallel).

export interface Brand {
  id: number | string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface Material {
  id: number | string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface SubCategory {
  id: number | string;
  name: string;
  slug: string;
  categoryId: number | string;
}

export interface AdminProductListItem {
  id: number | string;
  name: string;
  slug: string;
  baseSku: string | null;
  categoryName: string;
  brandName: string | null;
  status: ProductStatus;
  variantCount: number;
  totalStock: number;
  updatedAt: string;
  /**
   * Not part of the contracted list DTO (see AdminProductList.tsx notes) — the
   * backend list response has no image field today. Kept optional so the
   * thumbnail column upgrades automatically if the backend adds one later;
   * until then the list falls back to a placeholder.
   */
  primaryImageUrl?: string | null;
}

/** Alias kept for readability at call sites — same generic PageResponse used by inventory. */
export type AdminProductListResponse = PageResponse<AdminProductListItem>;

export interface AdminProductImage {
  id?: number | string;
  url: string;
  displayOrder: number;
  primary: boolean;
}

export interface AdminProductVariant {
  id?: number | string;
  sku: string;
  sizeId: number | string;
  sizeName: string;
  colorId: number | string;
  colorName: string;
  sellingPrice: number;
  costPrice: number | null;
  discountPercent: number;
  stockQuantity: number;
  reservedQuantity: number;
  damagedQuantity: number;
  availableQuantity: number;
  lowStockThreshold: number | null;
  active: boolean;
  images: AdminProductImage[];
}

export interface AdminProductDetail {
  id: number | string;
  categoryId: number | string;
  categoryName: string;
  subCategoryId: number | string | null;
  subCategoryName: string | null;
  brandId: number | string | null;
  brandName: string | null;
  materialId: number | string;
  materialName: string;
  name: string;
  slug: string;
  description: string;
  status: ProductStatus;
  baseSku: string | null;
  baseSellingPrice: number | null;
  baseCostPrice: number | null;
  variants: AdminProductVariant[];
}

export interface ProductVariantImageRequest {
  url: string;
  displayOrder: number;
  primary: boolean;
}

export interface ProductVariantRequest {
  id?: number | string | null;
  sku: string;
  sizeId: number | string;
  colorId: number | string;
  sellingPrice: number;
  costPrice: number | null;
  discountPercent: number;
  stockQuantity: number;
  lowStockThreshold: number | null;
  active: boolean;
  images: ProductVariantImageRequest[];
}

export interface ProductAdminRequest {
  categoryId: number | string;
  subCategoryId: number | string | null;
  brandId: number | string | null;
  materialId: number | string;
  name: string;
  slug: string;
  description: string;
  status: ProductStatus;
  baseSku: string | null;
  baseSellingPrice: number | null;
  baseCostPrice: number | null;
  variants: ProductVariantRequest[];
}

// --- Admin inventory ---

/** Generic paginated response shape used by the admin inventory endpoints. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type StockStatus = "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK";
export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "ARCHIVED";
export type InventoryTransactionType =
  | "PURCHASE_IN"
  | "SALE_OUT"
  | "RETURN_IN"
  | "ADJUSTMENT"
  | "CANCEL_REVERSAL"
  | "DAMAGE";

/** One row of GET /admin/inventory/variants, and the shape embedded in the dashboard's low/out-of-stock lists. */
export interface InventoryVariantRow {
  variantId: number | string;
  productId: number | string;
  productName: string;
  productSlug: string;
  sku: string;
  categoryName: string;
  colorName: string;
  colorHex: string;
  sizeName: string;
  sellingPrice: number;
  costPrice: number | null;
  stockQuantity: number;
  reservedQuantity: number;
  damagedQuantity: number;
  availableQuantity: number;
  lowStockThreshold: number | null;
  stockStatus: StockStatus;
  active: boolean;
  productStatus: ProductStatus;
  updatedAt: string;
}

export interface InventoryRecentProduct {
  id: number | string;
  name: string;
  slug: string;
  status: ProductStatus;
  createdAt: string;
}

export interface InventoryTransactionRow {
  id: number | string;
  variantId: number | string;
  sku: string;
  productName: string;
  type: InventoryTransactionType;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;
  reason: string | null;
  referenceType: string | null;
  referenceId: number | string | null;
  performedByName: string | null;
  createdAt: string;
}

export interface InventoryDamageRow {
  id: number | string;
  variantId: number | string;
  sku: string;
  productName: string;
  quantity: number;
  /** Resolved DamageReason name from the backend (no longer a fixed enum union). */
  reason: string;
  notes: string | null;
  reportedByName: string | null;
  createdAt: string;
}

export interface InventoryDashboard {
  totalProducts: number;
  totalVariants: number;
  totalAvailableStock: number;
  lowStockCount: number;
  outOfStockCount: number;
  totalDamagedStock: number;
  lowStockItems: InventoryVariantRow[];
  outOfStockItems: InventoryVariantRow[];
  recentProducts: InventoryRecentProduct[];
  recentTransactions: InventoryTransactionRow[];
  recentDamages: InventoryDamageRow[];
}

export interface StockAdjustResult {
  variantId: number | string;
  previousQuantity: number;
  newQuantity: number;
  availableQuantity: number;
}

export interface MarkDamagedResult {
  variantId: number | string;
  damagedQuantity: number;
  availableQuantity: number;
}

// --- Master Data Management ------------------------------------------------
// Shared audit fields on every /api/admin/{master} row (see API CONTRACT).

export interface MasterAuditFields {
  createdByName: string | null;
  updatedByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminCategory extends MasterAuditFields {
  id: number | string;
  name: string;
  slug: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminColor extends MasterAuditFields {
  id: number | string;
  name: string;
  hexCode: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminSize extends MasterAuditFields {
  id: number | string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminBrand extends MasterAuditFields {
  id: number | string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminMaterial extends MasterAuditFields {
  id: number | string;
  name: string;
  displayOrder: number;
  active: boolean;
}

/** Matches VendorAdminRequest/Response exactly (backend/masterdata/dto) — no displayOrder or address/GST fields exist. */
export interface AdminVendor extends MasterAuditFields {
  id: number | string;
  name: string;
  contactName: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  active: boolean;
}

/** Existing public-facing SubCategory (id/name/slug/categoryId) extended with the admin contract's added fields. */
export interface AdminSubCategory extends MasterAuditFields {
  id: number | string;
  name: string;
  slug: string;
  categoryId: number | string;
  categoryName: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminDamageReason extends MasterAuditFields {
  id: number | string;
  name: string;
  code: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
}

/** GET /api/damage-reasons (public, active-only). */
export interface DamageReasonOption {
  id: number | string;
  name: string;
}

export interface SizeGroupSizeRef {
  id: number | string;
  name: string;
  displayOrder: number;
}

export interface AdminSizeGroup extends MasterAuditFields {
  id: number | string;
  name: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
  categoryIds: (number | string)[];
  categoryNames: string[];
  sizes: SizeGroupSizeRef[];
}

export interface SizeGroupRequest {
  name: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
  categoryIds: (number | string)[];
  /** Ordered — display order of the group's sizes follows this array's order. */
  sizeIds: (number | string)[];
}

// --- Customer orders ---------------------------------------------------
// GET /api/orders (list) and GET /api/orders/{id} (detail). Shared with the
// Cart/Checkout/Payment flow (PaymentPage navigates to the detail page after
// a successful simulated payment) — see src/api/orders.ts.

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PAYMENT_PROCESSING"
  | "PAYMENT_FAILED"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED";

export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | null;

/** One row of GET /api/orders. */
export interface OrderListItem {
  id: number | string;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
}

export interface OrderItem {
  id: number | string;
  productName: string;
  sku: string;
  colorName: string;
  sizeName: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  lineTotal: number;
}

export interface OrderShippingAddress {
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

/** GET /api/orders/{id} response. */
export interface OrderDetail {
  id: number | string;
  orderNumber: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  items: OrderItem[];
  subtotal: number;
  discountTotal: number;
  shippingCharge: number;
  totalAmount: number;
  shippingAddress: OrderShippingAddress;
  contactName: string;
  contactPhone: string;
  reservationExpiresAt: string | null;
  createdAt: string;
}

/** POST /api/orders request body. */
export interface CreateOrderRequest {
  idempotencyKey: string;
  shippingAddressId: number | string;
  contactPhone?: string | null;
}

// --- Admin orders ---------------------------------------------------------
// /api/admin/orders — the admin Order Dashboard + List screen. Distinct from
// the customer-facing OrderStatus/OrderListItem/OrderDetail above (which only
// cover the 6 statuses a customer can see); the admin contract exposes the
// full 11-value order status lifecycle plus richer list/summary fields.

export type AdminOrderStatus =
  | "PENDING_PAYMENT"
  | "PAYMENT_PROCESSING"
  | "PAYMENT_FAILED"
  | "CONFIRMED"
  | "PROCESSING"
  | "PACKED"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "RETURNED"
  | "REFUNDED";

/** Row shape shared by GET /admin/orders (list) and dashboard.recentOrders. */
export interface AdminOrderSummary {
  id: number | string;
  orderNumber: string;
  customerName: string;
  customerContact: string;
  status: AdminOrderStatus;
  paymentStatus: PaymentStatus;
  itemCount: number;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

/** GET /admin/orders/dashboard response. */
export interface AdminOrderDashboardData {
  totalOrders: number;
  pendingCount: number;
  processingCount: number;
  packedCount: number;
  shippedCount: number;
  deliveredCount: number;
  cancelledCount: number;
  paymentFailedCount: number;
  recentOrders: AdminOrderSummary[];
}

// --- Admin order detail --------------------------------------------------
// GET /admin/orders/{id} and its mutating actions (status/cancel/notes/
// shipment/refund) — the admin Order Detail screen. See AdminOrderDetail.tsx
// and api/adminOrders.ts.

/** One line item on GET /admin/orders/{id} — distinct from the customer-facing OrderItem (adds imageUrl). */
export interface AdminOrderLineItem {
  id: number | string;
  productName: string;
  sku: string;
  colorName: string;
  sizeName: string;
  imageUrl: string | null;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  lineTotal: number;
}

export interface AdminOrderCustomer {
  name: string;
  phone: string;
  email: string;
}

/** Independent of AdminOrderStatus/PaymentStatus — the admin payment sub-object's own status. */
export type AdminOrderPaymentStatus = "PENDING" | "SUCCESS" | "FAILED";

export interface AdminOrderPayment {
  id: number | string;
  status: AdminOrderPaymentStatus;
  method: string;
  amount: number;
  gatewayReference: string | null;
  failureReason: string | null;
  createdAt: string;
}

/** Refund status is not a fixed contracted enum — rendered as free text (e.g. "COMPLETED"). */
export interface AdminOrderRefund {
  id: number | string;
  amount: number;
  status: string;
  reference: string | null;
  reason?: string | null;
  createdAt: string;
}

export interface AdminOrderShipment {
  id?: number | string;
  provider: string | null;
  trackingNumber: string | null;
  shipmentDate: string | null;
  deliveryDate: string | null;
  notes: string | null;
}

/** PUT /admin/orders/{id}/shipment request body — all fields nullable. */
export type AdminOrderShipmentRequest = Omit<AdminOrderShipment, "id">;

export interface AdminOrderStatusHistoryEntry {
  previousStatus: AdminOrderStatus | null;
  newStatus: AdminOrderStatus;
  /** null for system-driven transitions — render as "System". */
  changedByName: string | null;
  reason: string | null;
  createdAt: string;
}

export interface AdminOrderNote {
  id: number | string;
  note: string;
  adminName: string;
  createdAt: string;
}

/** GET /admin/orders/{id} response, and the shape returned (refreshed) by every mutating status/cancel action. */
export interface AdminOrderDetail {
  id: number | string;
  orderNumber: string;
  status: AdminOrderStatus;
  /** Drives which status-transition buttons render — never hardcode the transition graph client-side. */
  availableNextStatuses: AdminOrderStatus[];
  items: AdminOrderLineItem[];
  subtotal: number;
  discountTotal: number;
  shippingCharge: number;
  totalAmount: number;
  customer: AdminOrderCustomer;
  shippingAddress: OrderShippingAddress;
  payment: AdminOrderPayment | null;
  refund: AdminOrderRefund | null;
  shipment: AdminOrderShipment | null;
  statusHistory: AdminOrderStatusHistoryEntry[];
  notes: AdminOrderNote[];
  createdAt: string;
  updatedAt: string;
}

// --- Customer addresses --------------------------------------------------
// /api/customer/addresses — used by CheckoutPage to pick/create a shipping
// address ahead of order placement.

export interface Address {
  id: number | string;
  label: string | null;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
}

export type AddressRequest = Omit<Address, "id">;

// --- Customer cart ---------------------------------------------------------
// /api/cart — every mutating endpoint returns the full current cart; the
// frontend never computes totals itself, it always uses what the backend
// returns.

export interface CartItem {
  id: number | string;
  productVariantId: number | string;
  productName: string;
  productSlug: string;
  sku: string;
  colorName: string;
  colorHex: string;
  sizeName: string;
  imageUrl: string | null;
  unitPrice: number;
  discountPercent: number;
  lineTotal: number;
  quantity: number;
  availableQuantity: number;
  active: boolean;
}

export interface Cart {
  id: number | string;
  items: CartItem[];
  subtotal: number;
  discountTotal: number;
  total: number;
  itemCount: number;
}

// --- Customer payments -------------------------------------------------
// /api/payments — dev/simulation gateway only (see PaymentPage.tsx).

export interface PaymentInitiateResponse {
  paymentId: number | string;
  orderId: number | string;
  amount: number;
  gatewayReference: string;
}

export type PaymentOutcome = "SUCCESS" | "FAILURE";

export interface PaymentSimulateResponse {
  orderId: number | string;
  orderStatus: OrderStatus;
  paymentStatus: PaymentStatus;
}
