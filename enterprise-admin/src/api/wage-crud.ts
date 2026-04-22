import { httpGet, httpPost, httpPut, httpDelete } from '../lib/request'
import { pickPageRecords } from '../lib/http-helpers'
import type { PageResult } from '../types/api'
import type { MonthlySlipRow, ItemConfigRow } from '../types/wage-crud'

export const monthlySlipApi = {
  list: async (params: { belongMonth?: string }) => {
    const page = await httpGet<PageResult<MonthlySlipRow>>('/wage/monthly-slips', { params })
    return pickPageRecords(page)
  },
  get: (id: number) => httpGet<MonthlySlipRow>(`/wage/monthly-slips/${id}`),
  generate: (body: { belongMonth: string; employeeIds?: number[] }) =>
    httpPost<unknown>('/wage/monthly-slips/generate', body),
  adjust: (id: number, body: { baseSalary: number; subsidyTotal: number; deductionTotal: number }) =>
    httpPut<unknown>(`/wage/monthly-slips/${id}/adjust`, body),
  confirmPay: (body: { slipIds: number[] }) =>
    httpPost<unknown>('/wage/monthly-slips/confirm-pay', body),
}

export const itemConfigApi = {
  list: async () => {
    const page = await httpGet<PageResult<ItemConfigRow>>('/wage/item-configs')
    return pickPageRecords(page)
  },
  get: (id: number) => httpGet<ItemConfigRow>(`/wage/item-configs/${id}`),
  create: (body: Partial<ItemConfigRow>) => httpPost<unknown>('/wage/item-configs', body),
  update: (id: number, body: Partial<ItemConfigRow>) => httpPut<unknown>(`/wage/item-configs/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/wage/item-configs/${id}`),
}
