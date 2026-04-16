import { get, post, put, del, type Result } from './request'

function withPageParams(current: number, size: number, extra?: object) {
  return { current, size, page: current, pageNum: current, pageSize: size, ...((extra || {}) as object) }
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
  status: 0 | 1
}

export interface ErpProductCategory {
  id: number
  name: string
  parentId: number
  sortOrder?: number
  status: 0 | 1
  children?: ErpProductCategory[]
}

export interface ErpCustomer {
  id: number
  name: string
  contactName: string
  contactPhone: string
  address?: string
  level: string
  creditLimit: number
  status: 0 | 1
}

export interface ErpSupplier {
  id: number
  supplierCode: string
  supplierName: string
  contactName: string
  phone: string
  address?: string
  status: 0 | 1
}

export interface ErpWarehouse {
  id: number
  warehouseCode: string
  warehouseName: string
  managerName?: string
  contactInfo?: string
  address?: string
  status: number
}

export interface ErpStock {
  id?: number
  productId: number
  productName: string
  warehouseId: number
  warehouseName: string
  qty?: number
  quantity?: number
  minStock?: number
  maxStock?: number
}

export interface ErpSaleOrder {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  warehouseId: number
  warehouseName?: string
  totalAmount: number
  status: -1 | 0 | 1
  createTime?: string
}

export interface SaleOrderPageQuery extends PageQuery {
  orderNo?: string
  status?: -1 | 0 | 1
}

export interface SaleOrderItem {
  id: number
  saleOrderId: number
  productId: number
  productName?: string
  quantity: number
  unitPrice: number
  amount?: number
}

export interface ErpPurchaseOrder {
  id: number
  orderNo: string
  supplierId: number
  supplierName?: string
  warehouseId: number
  warehouseName?: string
  totalAmount: number
  status: number
  remark?: string
  createTime?: string
}

export interface PurchaseOrderItem {
  id?: number
  purchaseOrderId?: number
  productId: number
  productName?: string
  quantity?: number
  qty?: number
  unitPrice?: number
  price?: number
  amount?: number
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

export interface PageQuery {
  current: number
  size: number
}

export interface CustomerPageQuery extends PageQuery {
  name?: string
  contactName?: string
  contactPhone?: string
}

export interface CustomerUpsertDTO {
  id?: number
  name: string
  contactName?: string
  contactPhone?: string
  address?: string
  level?: string
  creditLimit?: number
  status: 0 | 1
}

export interface SupplierPageQuery extends PageQuery {
  supplierName?: string
  contactName?: string
  phone?: string
}

export interface SupplierUpsertDTO {
  id?: number
  supplierCode: string
  supplierName: string
  contactName?: string
  phone?: string
  address?: string
  status: 0 | 1
}

export interface ProductCategoryUpsertDTO {
  id?: number
  name: string
  parentId: number
  sortOrder?: number
  status: 0 | 1
}

export interface ProductPageQuery extends PageQuery {
  categoryId?: number
  productName?: string
  productCode?: string
  status?: 0 | 1
}

export interface ProductUpsertDTO {
  id?: number
  productCode: string
  productName: string
  categoryId: number
  specModel?: string
  unit: string
  price?: number
  stockQty?: number
  minStock?: number
  description?: string
  status: 0 | 1
}

export interface WarehousePageQuery extends PageQuery {
  warehouseName?: string
}

export interface WarehouseUpsertDTO {
  id?: number
  warehouseCode: string
  warehouseName: string
  managerName?: string
  contactInfo?: string
  address?: string
  status: number
}

export interface StockPageQuery extends PageQuery {
  productId?: number
  warehouseId?: number
}

export interface PurchaseOrderPageQuery extends PageQuery {
  status?: number
  orderNo?: string
}

export interface PurchaseOrderUpsertItemDTO {
  id?: number
  productId: number
  quantity: number
  unitPrice: number
}

export interface PurchaseOrderUpsertDTO {
  id?: number
  supplierId: number
  warehouseId: number
  remark?: string
  status?: number
  totalAmount?: number
  items: PurchaseOrderUpsertItemDTO[]
}

export type PurchaseOrderCreateItemDTO = PurchaseOrderUpsertItemDTO

export type PurchaseOrderCreateDTO = PurchaseOrderUpsertDTO

export type ErpApiResult<T> = Result<T>

export const erpApi = {
  // Product Categories
  getProductCategoryTree: () => get<ErpProductCategory[]>('/erp/product-categories/tree'),
  getCategoryTree: () => get<ErpProductCategory[]>('/erp/product-categories/tree'),
  getCategoryList: () => get<ErpProductCategory[]>('/erp/product-categories/list'),
  addProductCategory: (data: ProductCategoryUpsertDTO) => post<number>('/erp/product-categories', data),
  updateProductCategory: (id: number, data: ProductCategoryUpsertDTO) => put(`/erp/product-categories/${id}`, data),
  createCategory: (data: ProductCategoryUpsertDTO) => post<number>('/erp/product-categories', data),
  updateCategory: (id: number, data: ProductCategoryUpsertDTO) => put(`/erp/product-categories/${id}`, data),
  deleteCategory: (id: number) => del(`/erp/product-categories/${id}`),

  // Products
  getProductPage: (params: ProductPageQuery) =>
    get<PageResult<ErpProductInfo>>('/erp/products/page', withPageParams(params.current, params.size, params)),
  addProduct: (data: ProductUpsertDTO) => post<number>('/erp/products', data),
  createProduct: (data: ProductUpsertDTO) => post<number>('/erp/products', data),
  updateProduct: (id: number, data: ProductUpsertDTO) => put(`/erp/products/${id}`, data),
  getProductDetail: (id: number) => get<ErpProductInfo>('/erp/products/' + id),
  updateProductStatus: (id: number, status: number) =>
    put(`/erp/products/${id}/status`, { status }),

  // Product Info
  getProductInfoPage: (current: number, size: number, categoryId?: number, productName?: string) =>
    get<PageResult<ErpProductInfo>>('/erp/product-infos/page', withPageParams(current, size, { categoryId, productName })),
  createProductInfo: (data: any) => post<number>('/erp/product-infos', data),
  updateProductInfo: (id: number, data: any) => put(`/erp/product-infos/${id}`, data),
  deleteProductInfo: (id: number) => del(`/erp/product-infos/${id}`),

  // Customers
  getCustomerPage: (params: CustomerPageQuery) =>
    get<PageResult<ErpCustomer>>('/erp/customers/page', withPageParams(params.current, params.size, params)),
  addCustomer: (data: CustomerUpsertDTO) => post<number>('/erp/customers', data),
  createCustomer: (data: CustomerUpsertDTO) => post<number>('/erp/customers', data),
  updateCustomer: (id: number, data: CustomerUpsertDTO) => put(`/erp/customers/${id}`, data),

  // Suppliers
  getSupplierPage: (params: SupplierPageQuery) =>
    get<PageResult<ErpSupplier>>('/erp/suppliers/page', withPageParams(params.current, params.size, params)),
  addSupplier: (data: SupplierUpsertDTO) => post<number>('/erp/suppliers', data),
  createSupplier: (data: SupplierUpsertDTO) => post<number>('/erp/suppliers', data),
  updateSupplier: (id: number, data: SupplierUpsertDTO) => put(`/erp/suppliers/${id}`, data),
  deleteSupplier: (id: number) => del(`/erp/suppliers/${id}`),

  // Warehouses
  getWarehousePage: (params: WarehousePageQuery) =>
    get<PageResult<ErpWarehouse>>('/erp/warehouses/page', withPageParams(params.current, params.size, params)),
  addWarehouse: (data: WarehouseUpsertDTO) => post<number>('/erp/warehouses', data),
  createWarehouse: (data: WarehouseUpsertDTO) => post<number>('/erp/warehouses', data),
  updateWarehouse: (id: number, data: WarehouseUpsertDTO) => put(`/erp/warehouses/${id}`, data),
  deleteWarehouse: (id: number) => del(`/erp/warehouses/${id}`),

  // Stock
  getStockPage: (params: StockPageQuery) =>
    get<PageResult<ErpStock>>('/erp/stocks/page', withPageParams(params.current, params.size, params)),

  // Sale Orders
  getSaleOrderPage: (params: SaleOrderPageQuery) =>
    get<PageResult<ErpSaleOrder>>('/erp/sale-orders/page', withPageParams(params.current, params.size, params)),
  getSaleOrderDetail: (id: number) => get<ErpSaleOrder>(`/erp/sale-orders/${id}`),
  getSaleOrderItems: (id: number) => get<SaleOrderItem[]>(`/erp/sale-orders/${id}/items`),
  addSaleOrder: (data: any) => post<number>('/erp/sale-orders', data),
  updateSaleOrder: (id: number, data: any) => put(`/erp/sale-orders/${id}`, data),
  createSaleOrder: (data: any) => post<number>('/erp/sale-order', data),
  submitSaleOrder: (data: any) => post<number>('/erp/sale-order/submit', data),
  submitDraftSaleOrder: (id: number) => put(`/erp/sale-orders/${id}/submit`),
  approveSaleOrder: (id: number) => put(`/erp/sale-order/${id}/approve`),
  rejectSaleOrder: (id: number) => put(`/erp/sale-order/${id}/reject`),
  deleteSaleOrder: (id: number) => del(`/erp/sale-order/${id}`),

  // Purchase Orders
  getPurchaseOrderPage: (params: PurchaseOrderPageQuery) =>
    get<PageResult<ErpPurchaseOrder>>('/erp/purchase-orders/page', withPageParams(params.current, params.size, params)),
  createPurchaseOrder: (data: PurchaseOrderCreateDTO) => post<number>('/erp/purchase-orders', data),
  updatePurchaseOrder: (id: number, data: PurchaseOrderUpsertDTO) => put(`/erp/purchase-orders/${id}`, data),
  getPurchaseOrderDetail: (id: number) => get<ErpPurchaseOrder>(`/erp/purchase-orders/${id}`),
  getPurchaseOrderItems: (id: number) => get<PurchaseOrderItem[]>(`/erp/purchase-orders/${id}/items`),
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
