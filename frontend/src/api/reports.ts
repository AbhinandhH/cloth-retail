import { api } from './client'
import type {
  CategorySalesRow,
  CustomerSalesRow,
  GstReportResponse,
  InventoryVariantRow,
  PageResponse,
  ProductSalesRow,
  SalesSummaryResponse,
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

export interface DateRangeQuery {
  dateFrom?: string
  dateTo?: string
}

export function fetchSalesSummary(query: DateRangeQuery) {
  return api.get<SalesSummaryResponse>('/admin/reports/sales-summary', { params: cleanParams(query) }).then((r) => r.data)
}

export interface ProductSalesQuery extends DateRangeQuery {
  productId?: string | number
  page?: number
  size?: number
}

export function fetchProductSales(query: ProductSalesQuery) {
  return api
    .get<PageResponse<ProductSalesRow>>('/admin/reports/product-sales', { params: cleanParams(query) })
    .then((r) => r.data)
}

export interface CategorySalesQuery extends DateRangeQuery {
  categoryId?: string | number
  page?: number
  size?: number
}

export function fetchCategorySales(query: CategorySalesQuery) {
  return api
    .get<PageResponse<CategorySalesRow>>('/admin/reports/category-sales', { params: cleanParams(query) })
    .then((r) => r.data)
}

export interface CustomerSalesQuery extends DateRangeQuery {
  customerId?: string | number
  page?: number
  size?: number
}

export function fetchCustomerSales(query: CustomerSalesQuery) {
  return api
    .get<PageResponse<CustomerSalesRow>>('/admin/reports/customer-sales', { params: cleanParams(query) })
    .then((r) => r.data)
}

export interface StockReportQuery {
  categoryId?: string | number
  productId?: string | number
  stockStatus?: string
  page?: number
  size?: number
}

export function fetchStockReport(query: StockReportQuery) {
  return api
    .get<PageResponse<InventoryVariantRow>>('/admin/reports/stock', { params: cleanParams(query) })
    .then((r) => r.data)
}

export interface GstReportQuery extends DateRangeQuery {
  page?: number
  size?: number
}

export function fetchGstReport(query: GstReportQuery) {
  return api.get<GstReportResponse>('/admin/reports/gst', { params: cleanParams(query) }).then((r) => r.data)
}

export type ReportType = 'sales-summary' | 'product-sales' | 'category-sales' | 'customer-sales' | 'stock' | 'gst'

/** Fetches the matching /export PDF endpoint as a blob for download - same blob mechanics as fetchAdminEvidenceBlob (adminReturns.ts), just saved to disk instead of viewed inline. */
export function exportReportPdf(type: ReportType, query: Record<string, string | number | undefined>) {
  return api
    .get(`/admin/reports/${type}/export`, { params: cleanParams(query), responseType: 'blob' })
    .then((r) => r.data as Blob)
}
