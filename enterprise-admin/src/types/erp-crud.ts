export interface SaleOrderRow {
  id?: number
  orderNo?: string
  customerId?: number
  customerName?: string
  totalAmount?: number
  paidAmount?: number
  status?: number | string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface SaleOrderItem {
  id?: number
  orderId?: number
  productId?: number
  productName?: string
  sku?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  [key: string]: unknown
}

export interface SaleOrderCreateRequest {
  customerId: number
  remark?: string
  items: Array<{ productId: number; quantity: number; unitPrice: number }>
}

export interface PurchaseOrderRow {
  id?: number
  orderNo?: string
  supplierId?: number
  supplierName?: string
  totalAmount?: number
  status?: number | string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface PurchaseOrderItem {
  id?: number
  orderId?: number
  productId?: number
  productName?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  [key: string]: unknown
}

export interface PurchaseOrderCreateRequest {
  supplierId: number
  remark?: string
  items: Array<{ productId: number; quantity: number; unitPrice: number }>
}

export interface ProductRow {
  id?: number
  productName?: string
  sku?: string
  categoryId?: number
  categoryName?: string
  unitPrice?: number
  stock?: number
  status?: number
  remark?: string
  [key: string]: unknown
}

export interface CustomerRow {
  id?: number
  customerName?: string
  contactPerson?: string
  phone?: string
  email?: string
  address?: string
  remark?: string
  [key: string]: unknown
}

export interface SupplierRow {
  id?: number
  supplierName?: string
  contactPerson?: string
  phone?: string
  bankName?: string
  bankAccount?: string
  remark?: string
  [key: string]: unknown
}

export interface WarehouseRow {
  id?: number
  warehouseName?: string
  address?: string
  managerId?: number
  managerName?: string
  remark?: string
  [key: string]: unknown
}

export interface StockRow {
  id?: number
  productId?: number
  productName?: string
  warehouseId?: number
  warehouseName?: string
  quantity?: number
  [key: string]: unknown
}

export interface ReceivableRow {
  id?: number
  customerId?: number
  customerName?: string
  totalAmount?: number
  receivedAmount?: number
  status?: number | string
  dueDate?: string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface PayableRow {
  id?: number
  supplierId?: number
  supplierName?: string
  totalAmount?: number
  paidAmount?: number
  status?: number | string
  dueDate?: string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface FinanceSummary {
  totalAmount?: number
  settledAmount?: number
  pendingAmount?: number
  overdueAmount?: number
  [key: string]: unknown
}

export interface PaymentRecord {
  id?: number
  amount?: number
  paymentDate?: string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface SalesMonthly {
  month?: string
  totalAmount?: number
  orderCount?: number
  [key: string]: unknown
}

export interface RankItem {
  name?: string
  amount?: number
  count?: number
  [key: string]: unknown
}

export interface StockAlarm {
  productId?: number
  productName?: string
  warehouseName?: string
  quantity?: number
  minStock?: number
  [key: string]: unknown
}
