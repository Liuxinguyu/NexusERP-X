import { httpGet, httpPost, httpPut, httpDelete } from '../lib/request'
import type { PageResult } from '../types/api'
import type {
  SaleOrderRow, SaleOrderItem, SaleOrderCreateRequest,
  PurchaseOrderRow, PurchaseOrderItem, PurchaseOrderCreateRequest,
  ProductRow, CustomerRow, SupplierRow, WarehouseRow, StockRow,
  SalesMonthly, RankItem, StockAlarm,
} from '../types/erp-crud'

export const saleOrderApi = {
  page: (params: { current: number; size: number; status?: string; orderNo?: string }) =>
    httpGet<PageResult<SaleOrderRow>>('/erp/sale-orders/page', { params }),
  getItems: (id: number) =>
    httpGet<SaleOrderItem[]>(`/erp/sale-orders/${id}/items`),
  create: (body: SaleOrderCreateRequest) =>
    httpPost<unknown>('/erp/sale-orders', body),
  submit: (body: SaleOrderCreateRequest) =>
    httpPost<unknown>('/erp/sale-orders/submit', body),
  submitById: (id: number) =>
    httpPut<unknown>(`/erp/sale-orders/${id}/submit`),
  approve: (id: number) =>
    httpPut<unknown>(`/erp/sale-orders/${id}/approve`),
  outbound: (id: number) =>
    httpPut<unknown>(`/erp/sale-orders/${id}/outbound`),
  reject: (id: number) =>
    httpPut<unknown>(`/erp/sale-orders/${id}/reject`),
  remove: (id: number) =>
    httpDelete<unknown>(`/erp/sale-orders/${id}`),
}

export const purchaseOrderApi = {
  page: (params: { current: number; size: number; status?: string }) =>
    httpGet<PageResult<PurchaseOrderRow>>('/erp/purchase-orders/page', { params }),
  getItems: (id: number) =>
    httpGet<PurchaseOrderItem[]>(`/erp/purchase-orders/${id}/items`),
  create: (body: PurchaseOrderCreateRequest) =>
    httpPost<unknown>('/erp/purchase-orders', body),
  quickInbound: (body: PurchaseOrderCreateRequest) =>
    httpPost<unknown>('/erp/purchase-orders/quick-inbound', body),
  confirmInbound: (id: number) =>
    httpPut<unknown>(`/erp/purchase-orders/${id}/confirm-inbound`),
  submit: (id: number) =>
    httpPut<unknown>(`/erp/purchase-orders/${id}/submit`),
  approve: (id: number) =>
    httpPut<unknown>(`/erp/purchase-orders/${id}/approve`),
  reject: (id: number) =>
    httpPut<unknown>(`/erp/purchase-orders/${id}/reject`),
  remove: (id: number) =>
    httpDelete<unknown>(`/erp/purchase-orders/${id}`),
}

export const productApi = {
  page: (params: { current: number; size: number; productName?: string }) =>
    httpGet<PageResult<ProductRow>>('/erp/products/page', { params }),
  create: (body: Partial<ProductRow>) =>
    httpPost<unknown>('/erp/products', body),
  update: (id: number, body: Partial<ProductRow>) =>
    httpPut<unknown>(`/erp/products/${id}`, body),
  updateStatus: (id: number, status: number) =>
    httpPut<unknown>(`/erp/products/${id}/status`, undefined, { params: { status } }),
}

export const customerApi = {
  page: (params: { current: number; size: number; customerName?: string }) =>
    httpGet<PageResult<CustomerRow>>('/erp/customers/page', { params }),
  create: (body: Partial<CustomerRow>) =>
    httpPost<unknown>('/erp/customers', body),
  update: (id: number, body: Partial<CustomerRow>) =>
    httpPut<unknown>(`/erp/customers/${id}`, body),
}

export const supplierApi = {
  page: (params: { current: number; size: number }) =>
    httpGet<PageResult<SupplierRow>>('/erp/suppliers/page', { params }),
  create: (body: Partial<SupplierRow>) =>
    httpPost<unknown>('/erp/suppliers', body),
  update: (id: number, body: Partial<SupplierRow>) =>
    httpPut<unknown>(`/erp/suppliers/${id}`, body),
  remove: (id: number) =>
    httpDelete<unknown>(`/erp/suppliers/${id}`),
}

export const warehouseApi = {
  page: (params: { current: number; size: number }) =>
    httpGet<PageResult<WarehouseRow>>('/erp/warehouses/page', { params }),
  create: (body: Partial<WarehouseRow>) =>
    httpPost<unknown>('/erp/warehouses', body),
  update: (id: number, body: Partial<WarehouseRow>) =>
    httpPut<unknown>(`/erp/warehouses/${id}`, body),
  remove: (id: number) =>
    httpDelete<unknown>(`/erp/warehouses/${id}`),
}

export const stockApi = {
  page: (params: { current: number; size: number; productName?: string; warehouseId?: number }) =>
    httpGet<PageResult<StockRow>>('/erp/stocks/page', { params }),
}

export const reportApi = {
  salesMonthly: (params: { year: number; month: number }) =>
    httpGet<SalesMonthly>('/erp/reports/sales/monthly', { params }),
  salesTrend: (params: { year: number }) =>
    httpGet<SalesMonthly[]>('/erp/reports/sales/trend', { params }),
  productRank: (params: { year: number; month: number; limit?: number }) =>
    httpGet<RankItem[]>('/erp/reports/sales/product-rank', { params }),
  customerRank: (params: { year: number; month: number; limit?: number }) =>
    httpGet<RankItem[]>('/erp/reports/sales/customer-rank', { params }),
  stockAlarm: () =>
    httpGet<StockAlarm[]>('/erp/reports/stock/alarm'),
  stockSummary: () =>
    httpGet<Record<string, unknown>>('/erp/reports/stock/summary'),
}
