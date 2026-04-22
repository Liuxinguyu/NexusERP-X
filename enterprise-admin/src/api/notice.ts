import { httpDelete, httpGet, httpPost, httpPut } from '../lib/request'
import type { PageResult } from '../types/api'
import type { NoticeRow } from '../types/oa-crud'

export const noticeApi = {
  page: (params: { pageNum: number; pageSize: number }) =>
    httpGet<PageResult<NoticeRow>>('/system/notice/page', { params }),
  create: (body: Partial<NoticeRow>) =>
    httpPost<unknown>('/system/notice', body),
  update: (id: number, body: Partial<NoticeRow>) =>
    httpPut<unknown>(`/system/notice/${id}`, body),
  publish: (id: number) =>
    httpPut<unknown>(`/system/notice/${id}/publish`),
  delete: (id: number) =>
    httpDelete<unknown>(`/system/notice/${id}`),
}
