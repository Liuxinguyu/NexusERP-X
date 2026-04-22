import { httpGet, httpPost, httpDelete } from '../lib/request'
import type { OaFileFolder, OaFileRow } from '../types/oa-crud'

export const fileApi = {
  getFolders: () => httpGet<OaFileFolder[]>('/oa/files/folders'),
  createFolder: (body: { folderName: string; parentId?: number }) =>
    httpPost<unknown>('/oa/files/folders', body),
  deleteFolder: (id: number) => httpDelete<unknown>(`/oa/files/folders/${id}`),
  listFiles: (params?: { folderId?: number }) =>
    httpGet<OaFileRow[]>('/oa/files', { params }),
  upload: (folderId: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    form.append('folderId', String(folderId))
    return httpPost<unknown>('/oa/files/upload', form)
  },
  download: (id: number) =>
    httpGet<Blob>(`/oa/files/${id}/download`, { responseType: 'blob' as never }),
  remove: (id: number) => httpDelete<unknown>(`/oa/files/${id}`),
}
