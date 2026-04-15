import { get, post, put, del } from './request'

function withPageParams(current: number, size: number, extra?: Record<string, unknown>) {
  return { current, size, page: current, pageNum: current, pageSize: size, ...(extra || {}) }
}

// Product
export interface ErpProductInfo {
  id: number
  productCode: string
  productName: string
  categoryId: number
  specModel: string
  unit: string
  price: number
  stockQty: number
  status: number
}

export interface ErpProductCategory {
  id: number
  name: string
  parentId: number
  sortOrder?: number
  status: number
}

export interface ErpCustomer {
  id: number
  name: string
  contactName: string
  contactPhone: string
  level: string
  creditLimit: number
}

export interface ErpSupplier {
  id: number
  supplierCode: string
  supplierName: string
  contactName: string
  phone: string
  status: number
}

export interface ErpWarehouse {
  id: number
  warehouseCode: string
  warehouseName: string
  managerName: string
  contactInfo: string
  address: string
  status: number
}

export interface ErpStock {
  id: number
  productId: number
  productName: string
  warehouseId: number
  warehouseName: string
  qty: number
}

export interface ErpSaleOrder {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  warehouseId: number
  totalAmount: number
  status: number
  createTime?: string
}

export interface ErpPurchaseOrder {
  id: number
  orderNo: string
  supplierId: number
  warehouseId: number
  totalAmount: number
  status: number
  createTime?: string
}

export interface FinReceivable {
  id: number
  receivableNo: string
  customerId: number
  customerName: string
  totalAmount: number
  receivedAmount: number
  pendingAmount: number
  status: number
}

export interface FinPayable {
  id: number
  payableNo: string
  supplierId: number
  supplierName: string
  totalAmount: number
  paidAmount: number
  pendingAmount: number
  status: number
}

export interface PageResult<T> {
  records?: T[]
  list?: T[]
  total: number
  current: number
  size: number
}

export const erpApi = {
  // Product Categories
  getCategoryTree: () => get<any[]>('/erp/product-categories/tree'),
  getCategoryList: () => get<ErpProductCategory[]>('/erp/product-categories/list'),
  createCategory: (data: any) => post<number>('/erp/product-categories', data),
  updateCategory: (id: number, data: any) => put(`/erp/product-categories/${id}`, data),
  deleteCategory: (id: number) => del(`/erp/product-categories/${id}`),

  // Products
  getProductPage: (params: any) =>
    get<PageResult<any>>('/erp/products/page', params),
  createProduct: (data: any) => post<number>('/erp/products', data),
  updateProduct: (id: number, data: any) => put(`/erp/products/${id}`, data),
  updateProductStatus: (id: number, status: number) =>
    put(`/erp/products/${id}/status`, { status }),

  // Product Info
  getProductInfoPage: (current: number, size: number, categoryId?: number, productName?: string) =>
    get<PageResult<ErpProductInfo>>('/erp/product-infos/page', withPageParams(current, size, { categoryId, productName })),
  createProductInfo: (data: any) => post<number>('/erp/product-infos', data),
  updateProductInfo: (id: number, data: any) => put(`/erp/product-infos/${id}`, data),
  deleteProductInfo: (id: number) => del(`/erp/product-infos/${id}`),

  // Customers
  getCustomerPage: (current: number, size: number, name?: string, contactPhone?: string) =>
    get<PageResult<ErpCustomer>>('/erp/customers/page', withPageParams(current, size, { name, contactPhone })),
  createCustomer: (data: any) => post<number>('/erp/customers', data),
  updateCustomer: (id: number, data: any) => put(`/erp/customers/${id}`, data),

  // Suppliers
  getSupplierPage: (current: number, size: number, supplierName?: string) =>
    get<PageResult<ErpSupplier>>('/erp/suppliers/page', withPageParams(current, size, { supplierName })),
  createSupplier: (data: any) => post<number>('/erp/suppliers', data),
  updateSupplier: (id: number, data: any) => put(`/erp/suppliers/${id}`, data),
  deleteSupplier: (id: number) => del(`/erp/suppliers/${id}`),

  // Warehouses
  getWarehousePage: (current: number, size: number, warehouseName?: string) =>
    get<PageResult<ErpWarehouse>>('/erp/warehouses/page', withPageParams(current, size, { warehouseName })),
  createWarehouse: (data: any) => post<number>('/erp/warehouses', data),
  updateWarehouse: (id: number, data: any) => put(`/erp/warehouses/${id}`, data),
  deleteWarehouse: (id: number) => del(`/erp/warehouses/${id}`),

  // Stock
  getStockPage: (current: number, size: number, productId?: number, warehouseId?: number) =>
    get<PageResult<any>>('/erp/stocks/page', withPageParams(current, size, { productId, warehouseId })),

  // Sale Orders
  getSaleOrderPage: (current: number, size: number, status?: number, orderNo?: string) =>
    get<PageResult<ErpSaleOrder>>('/erp/sale-order/page', withPageParams(current, size, { status, orderNo })),
  getSaleOrderItems: (id: number) => get<any[]>('/erp/sale-order/' + id + '/items'),
  createSaleOrder: (data: any) => post<number>('/erp/sale-order', data),
  submitSaleOrder: (data: any) => post<number>('/erp/sale-order/submit', data),
  submitDraftSaleOrder: (id: number) => put(`/erp/sale-order/${id}/submit`),
  approveSaleOrder: (id: number) => put(`/erp/sale-order/${id}/approve`),
  rejectSaleOrder: (id: number) => put(`/erp/sale-order/${id}/reject`),
  deleteSaleOrder: (id: number) => del(`/erp/sale-order/${id}`),

  // Purchase Orders
  getPurchaseOrderPage: (current: number, size: number, status?: number) =>
    get<PageResult<ErpPurchaseOrder>>('/erp/purchase-orders/page', withPageParams(current, size, { status })),
  getPurchaseOrderItems: (id: number) => get<any[]>('/erp/purchase-orders/' + id + '/items'),
  createPurchaseOrder: (data: any) => post<number>('/erp/purchase-orders', data),
  confirmInbound: (id: number) => put(`/erp/purchase-orders/${id}/confirm-inbound`),
  submitPurchaseOrder: (id: number) => put(`/erp/purchase-orders/${id}/submit`),
  approvePurchaseOrder: (id: number) => put(`/erp/purchase-orders/${id}/approve`),
  rejectPurchaseOrder: (id: number) => put(`/erp/purchase-orders/${id}/reject`),
  deletePurchaseOrder: (id: number) => del(`/erp/purchase-orders/${id}`),

  // Reports
  getSalesMonthly: (year: number, month: number) =>
    get<any>('/erp/reports/sales/monthly', { year, month }),
  getSalesTrend: (year = 2026) => get<any>('/erp/reports/sales/trend', { year }),
  getProductRank: (limit = 10, year?: number, month?: number) =>
    get<any[]>('/erp/reports/sales/product-rank', { limit, year, month }),
  getCustomerRank: (limit = 10, year?: number) =>
    get<any[]>('/erp/reports/sales/customer-rank', { limit, year }),
  getStockAlarm: () => get<any>('/erp/reports/stock/alarm'),
  getStockSummary: () => get<any>('/erp/reports/stock/summary'),

  // Fin Receivable
  getReceivablePage: (current: number, size: number, params?: any) =>
    get<any>('/erp/receivables/page', withPageParams(current, size, params)),
  getReceivable: (id: number) => get<any>(`/erp/receivables/${id}`),
  createReceivable: (data: any) => post<number>('/erp/receivables', data),
  updateReceivable: (id: number, data: any) => put(`/erp/receivables/${id}`, data),
  deleteReceivable: (id: number) => del(`/erp/receivables/${id}`),
  recordReceipt: (id: number, data: any) => post(`/erp/receivables/${id}/record`, data),
  getReceivableRecords: (id: number) => get<any[]>(`/erp/receivables/${id}/records`),
  getReceivableSummary: (customerId: number, month?: string) =>
    get<any>('/erp/receivables/summary', { customerId, month }),

  // Fin Payable
  getPayablePage: (current: number, size: number, params?: any) =>
    get<any>('/erp/payables/page', withPageParams(current, size, params)),
  getPayable: (id: number) => get<any>(`/erp/payables/${id}`),
  createPayable: (data: any) => post<number>('/erp/payables', data),
  updatePayable: (id: number, data: any) => put(`/erp/payables/${id}`, data),
  deletePayable: (id: number) => del(`/erp/payables/${id}`),
  recordPayment: (id: number, data: any) => post(`/erp/payables/${id}/record`, data),
  getPayableRecords: (id: number) => get<any[]>(`/erp/payables/${id}/records`),
  getPayableSummary: (supplierId: number, month?: string) =>
    get<any>('/erp/payables/summary', { supplierId, month }),
}
