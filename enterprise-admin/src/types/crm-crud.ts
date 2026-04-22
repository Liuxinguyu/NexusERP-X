export interface OpportunityRow {
  id?: number
  opportunityName?: string
  customerId?: number
  customerName?: string
  stage?: string
  amount?: number
  probability?: number
  expectCloseDate?: string
  ownerUserId?: number
  ownerUserName?: string
  status?: number
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface ContractRow {
  id?: number
  contractNo?: string
  contractName?: string
  customerId?: number
  customerName?: string
  opportunityId?: number
  opportunityName?: string
  signDate?: string
  startDate?: string
  endDate?: string
  amount?: number
  signedBy?: string
  status?: number | string
  remark?: string
  createTime?: string
  [key: string]: unknown
}

export interface ContractItem {
  id?: number
  productId?: number
  productName?: string
  quantity?: number
  unitPrice?: number
  subtotal?: number
  [key: string]: unknown
}
