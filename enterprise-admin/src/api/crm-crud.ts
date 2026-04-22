import { httpGet, httpPost, httpPut, httpDelete } from '../lib/request'
import type { PageResult } from '../types/api'
import type { OpportunityRow, ContractRow, ContractItem } from '../types/crm-crud'

export const opportunityApi = {
  page: (params: { current: number; size: number; stage?: string }) =>
    httpGet<PageResult<OpportunityRow>>('/erp/opportunities/page', { params }),
  get: (id: number) => httpGet<OpportunityRow>(`/erp/opportunities/${id}`),
  create: (body: Partial<OpportunityRow>) => httpPost<unknown>('/erp/opportunities', body),
  update: (id: number, body: Partial<OpportunityRow>) => httpPut<unknown>(`/erp/opportunities/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/erp/opportunities/${id}`),
  advanceStage: (id: number, body: { stage: string }) =>
    httpPut<unknown>(`/erp/opportunities/${id}/stage`, body),
}

export const contractApi = {
  page: (params: { current: number; size: number }) =>
    httpGet<PageResult<ContractRow>>('/erp/contracts/page', { params }),
  get: (id: number) => httpGet<ContractRow>(`/erp/contracts/${id}`),
  getItems: (id: number) => httpGet<ContractItem[]>(`/erp/contracts/${id}/items`),
  create: (body: Partial<ContractRow>) => httpPost<unknown>('/erp/contracts', body),
  update: (id: number, body: Partial<ContractRow>) => httpPut<unknown>(`/erp/contracts/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/erp/contracts/${id}`),
}
