import { httpGet, httpPost, httpPut, httpDelete } from '../lib/request'
import type { PageResult } from '../types/api'
import type { ReceivableRow, PayableRow, FinanceSummary, PaymentRecord } from '../types/erp-crud'

export const receivableApi = {
  page: (params: { current: number; size: number; customerName?: string; status?: string }) =>
    httpGet<PageResult<ReceivableRow>>('/erp/receivables/page', { params }),
  get: (id: number) => httpGet<ReceivableRow>(`/erp/receivables/${id}`),
  create: (body: Partial<ReceivableRow>) =>
    httpPost<unknown>('/erp/receivables', body),
  update: (id: number, body: Partial<ReceivableRow>) =>
    httpPut<unknown>(`/erp/receivables/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/erp/receivables/${id}`),
  recordPayment: (id: number, body: { amount: number; remark?: string }) =>
    httpPost<unknown>(`/erp/receivables/${id}/record`, body),
  getRecords: (id: number) =>
    httpGet<PaymentRecord[]>(`/erp/receivables/${id}/records`),
  summary: () => httpGet<FinanceSummary>('/erp/receivables/summary'),
}

export const payableApi = {
  page: (params: { current: number; size: number; supplierName?: string; status?: string }) =>
    httpGet<PageResult<PayableRow>>('/erp/payables/page', { params }),
  get: (id: number) => httpGet<PayableRow>(`/erp/payables/${id}`),
  create: (body: Partial<PayableRow>) =>
    httpPost<unknown>('/erp/payables', body),
  update: (id: number, body: Partial<PayableRow>) =>
    httpPut<unknown>(`/erp/payables/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/erp/payables/${id}`),
  recordPayment: (id: number, body: { amount: number; remark?: string }) =>
    httpPost<unknown>(`/erp/payables/${id}/record`, body),
  getRecords: (id: number) =>
    httpGet<PaymentRecord[]>(`/erp/payables/${id}/records`),
  summary: () => httpGet<FinanceSummary>('/erp/payables/summary'),
}
