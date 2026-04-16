/** 销售订单：与后端 ErpSaleOrder 一致（0 草稿，1 待审核/一步出库单，2 已审核，-1 已拒绝） */
export const SALE_ORDER_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: -1,
} as const

export const PURCHASE_ORDER_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
  INBOUNDED: 4,
} as const

export const APPROVAL_TASK_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const
