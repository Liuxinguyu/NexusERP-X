import { get, post, put, del } from './request'

export interface WageItemConfig {
  id: number
  itemName: string
  calcType: number
  defaultAmount: number
  itemKind: number
  isDeduction: number
  sortOrder: number
  status: number
}

export interface WageMonthlySlip {
  id: number
  belongMonth: string
  employeeId: number
  baseSalary: number
  subsidyTotal: number
  deductionTotal: number
  netPay: number
  status: number
  createTime?: string
}

export const wageApi = {
  // Wage Item Config
  getItemConfigs: () => get<WageItemConfig[]>('/wage/item-configs'),
  getItemConfig: (id: number) => get<WageItemConfig>(`/wage/item-configs/${id}`),
  createItemConfig: (data: any) => post<number>('/wage/item-configs', data),
  updateItemConfig: (id: number, data: any) => put(`/wage/item-configs/${id}`, data),
  deleteItemConfig: (id: number) => del(`/wage/item-configs/${id}`),

  // Monthly Slip
  getMonthlySlips: (belongMonth?: string) =>
    get<WageMonthlySlip[]>('/wage/monthly-slips', { belongMonth }),
  getMonthlySlip: (id: number) => get<WageMonthlySlip>(`/wage/monthly-slips/${id}`),
  generateMonthly: (belongMonth: string, employeeIds?: number[]) =>
    post<number>('/wage/monthly-slips/generate', { belongMonth, employeeIds }),
  adjustSlip: (id: number, data: any) => put(`/wage/monthly-slips/${id}/adjust`, data),
  confirmPay: (slipIds: number[]) => post<number>('/wage/monthly-slips/confirm-pay', { slipIds }),
  deleteMonthlySlip: (id: number) => del(`/wage/monthly-slips/${id}`),
  getItemConfigPage: (current: number, size: number, params?: any) =>
    get<any>(`/wage/item-configs/page`, { current, size, ...params }),
  getMonthlySlipPage: (current: number, size: number, params?: any) =>
    get<any>(`/wage/monthly-slips/page`, { current, size, ...params }),
}
