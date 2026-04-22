export interface BizResponse<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

const SUCCESS_CODES = new Set([0, 200])

export function extractBizData<T>(payload: BizResponse<T>): T {
  if (!SUCCESS_CODES.has(payload.code)) {
    throw new Error(payload.msg || payload.message || '请求失败')
  }
  return payload.data
}

export function pickPageRecords<T>(payload: {
  records?: T[]
  list?: T[]
} | undefined): T[] {
  if (!payload) return []
  return payload.records ?? payload.list ?? []
}
