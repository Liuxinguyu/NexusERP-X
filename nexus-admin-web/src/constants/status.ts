export const SALE_ORDER_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
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

