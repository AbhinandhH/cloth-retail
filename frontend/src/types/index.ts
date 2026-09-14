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

export type OtpChannel = "EMAIL" | "MOBILE";

/**
 * Shared response shape for /auth/register and /auth/otp/verify. When completed is false, no
 * account exists yet — registrationId identifies the pending signup (see the backend's
 * PendingRegistration), and email/mobileVerificationRequired say which channels are still
 * outstanding (recomputed on every call, so a partially-verified signup only shows the channel
 * that's still pending). Once completed is true, auth carries the real tokens — that's the point
 * a real account is actually created and the customer is logged in.
 */
export interface VerificationStatusResponse {
  completed: boolean;
  registrationId: number | string | null;
  emailVerificationRequired: boolean;
  mobileVerificationRequired: boolean;
  auth: AuthResponse | null;
}

/**
 * Admin-controllable on/off switches for the OTP notification channels, plus the message
 * content — see the Notifications admin module. messageTemplate is shared by both channels and
 * supports {code}/{ttlMinutes} placeholders; emailSubject is email-only.
 */
export interface NotificationSettings {
  emailVerificationEnabled: boolean;
  mobileVerificationEnabled: boolean;
  emailSubject: string;
  messageTemplate: string;
}

/** The real password is never returned by the backend — passwordConfigured just says whether one is set. */
export interface SmtpSettings {
  host: string | null;
  port: number;
  username: string | null;
  passwordConfigured: boolean;
  fromAddress: string | null;
  useStarttls: boolean;
  /** "SMTP" (raw SMTP, the original approach) or "RESEND" (HTTP API — recommended when hosted somewhere that blocks outbound SMTP, e.g. most PaaS platforms). */
  provider: string;
  apiKeyConfigured: boolean;
}

/** password/apiKey omitted/blank means "keep the currently stored value" — see SmtpSettings.passwordConfigured/apiKeyConfigured. */
export interface SmtpSettingsUpdate {
  host: string | null;
  port: number;
  username: string | null;
  password?: string;
  fromAddress: string | null;
  useStarttls: boolean;
  provider: string;
  apiKey?: string;
}

export interface SmtpTestResult {
  success: boolean;
  message: string;
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
  /** False reproduces the pre-redesign look: no page-wide ambient wash, just BrandHero's own local hero glow. */
  richAmbient: boolean;
  /** "SIGNATURE" (ornate/serif/shimmer, the default) or "STUDIO" (calm/geometric/flat) — see index.css's [data-motif] rules. */
  motif: string;
}

/** The color set embedded in SiteConfiguration — same shape as Theme minus id/displayOrder. */
export interface ThemeColors {
  name: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string | null;
  backgroundColor: string;
  textColor: string;
  richAmbient: boolean;
  motif: string;
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

/** Discriminates an AdminProductImage/ProductVariantImageRequest row as a still image or a video clip. */
export type MediaType = "IMAGE" | "VIDEO";

export interface AdminProductImage {
  id?: number | string;
  url: string;
  displayOrder: number;
  primary: boolean;
  /** Absent on media saved before video support existed — treat as "IMAGE". */
  mediaType?: MediaType;
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
}

/** One color's shared image/video set, reused by every size variant of that color. */
export interface AdminColorImages {
  colorId: number | string;
  colorName: string;
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
  vendorId: number | string | null;
  vendorName: string | null;
  name: string;
  slug: string;
  description: string;
  status: ProductStatus;
  baseSku: string | null;
  baseSellingPrice: number | null;
  baseCostPrice: number | null;
  variants: AdminProductVariant[];
  colorImages: AdminColorImages[];
}

export interface ProductVariantImageRequest {
  url: string;
  displayOrder: number;
  primary: boolean;
  mediaType?: MediaType;
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
}

/** One color's shared image/video set within a ProductAdminRequest — see ProductVariantRequest, which no longer carries its own images. */
export interface ColorImagesRequest {
  colorId: number | string;
  images: ProductVariantImageRequest[];
}

export interface ProductAdminRequest {
  categoryId: number | string;
  subCategoryId: number | string | null;
  brandId: number | string | null;
  materialId: number | string;
  vendorId: number | string;
  name: string;
  slug: string;
  description: string;
  status: ProductStatus;
  baseSku: string | null;
  baseSellingPrice: number | null;
  baseCostPrice: number | null;
  variants: ProductVariantRequest[];
  colorImages: ColorImagesRequest[];
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

// Mirrors the backend's single OrderStatus enum exactly (11 values) — the
// customer endpoints (GET /api/orders, GET /api/orders/{id}) serialize the
// same unfiltered enum admin does, with no customer-specific subset. This
// used to be a stale 6-value type (including a since-removed COMPLETED) that
// left PROCESSING/PACKED/SHIPPED/DELIVERED/RETURNED/REFUNDED unhandled.
export type OrderStatus =
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
// /api/admin/orders — the admin Order Dashboard + List screen. Same
// underlying status enum as the customer-facing OrderStatus above (both
// mirror the backend's single OrderStatus) — aliased rather than
// re-declared so the two can't drift out of sync again.

export type AdminOrderStatus = OrderStatus;

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

// --- Admin customers ---------------------------------------------------
// GET /admin/customers (list), GET /admin/customers/{id} (detail),
// GET /admin/customers/{id}/orders (paginated order history — reuses
// AdminOrderSummary, identical row shape to the Orders module), and
// PATCH /admin/customers/{id}/status (enable/disable — the same flag
// AuthService checks at login, so this is a real block, not cosmetic).

export interface AdminCustomerRow {
  id: number | string;
  userId: number | string;
  fullName: string;
  email: string;
  mobileNumber: string | null;
  enabled: boolean;
  orderCount: number;
  totalSpent: number;
  createdAt: string;
}

export interface AdminCustomerDetail {
  id: number | string;
  userId: number | string;
  fullName: string;
  email: string;
  mobileNumber: string | null;
  enabled: boolean;
  dateOfBirth: string | null;
  gender: string | null;
  orderCount: number;
  totalSpent: number;
  createdAt: string;
}

// --- Admin landing dashboard -----------------------------------------------
// GET /admin/dashboard — the admin home's "Today's sales / stock / recent
// orders / sales overview / top-selling products" module. Deliberately reuses
// AdminOrderSummary for recentOrders (identical shape to the Orders module's
// own dashboard row) rather than a parallel type.

export interface SalesOverviewPoint {
  date: string;
  sales: number;
  orderCount: number;
}

export interface TopSellingProductRow {
  sku: string;
  productName: string;
  imageUrl: string | null;
  quantitySold: number;
  revenue: number;
}

export interface AdminDashboardData {
  todaysSales: number;
  todaysOrderCount: number;
  totalOrders: number;
  pendingOrders: number;
  productsInStock: number;
  lowStockCount: number;
  outOfStockCount: number;
  recentOrders: AdminOrderSummary[];
  salesOverview: SalesOverviewPoint[];
  topSellingProducts: TopSellingProductRow[];
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

// --- Wishlist ------------------------------------------------------------

export interface Wishlist {
  /** Product ids only - a ProductCard just needs to know whether it's in the set. */
  productIds: (number | string)[];
}

// --- Customer payments -------------------------------------------------
// /api/payments — dev/simulation gateway, and (when configured) a real Razorpay checkout. See PaymentPage.tsx.

/** GET /api/payments/config — tells the frontend which checkout UI to render. keyId is only present (and only needed) when provider is "razorpay". */
export interface PaymentConfig {
  provider: "mock" | "razorpay" | string;
  keyId: string | null;
}

export interface PaymentInitiateResponse {
  paymentId: number | string;
  orderId: number | string;
  amount: number;
  /** The mock gateway's fake reference under "mock"; Razorpay's real order id (its own Checkout needs this exact value as order_id) under "razorpay". */
  gatewayReference: string;
}

export type PaymentOutcome = "SUCCESS" | "FAILURE";

export interface PaymentSimulateResponse {
  orderId: number | string;
  orderStatus: OrderStatus;
  paymentStatus: PaymentStatus;
}
