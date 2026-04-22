import axios from 'axios'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { extractBizData } from './http-helpers'
import { clearAuthSession, getAccessToken, getCurrentShopId, getTenantId } from './storage'

export const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getAccessToken()
  const tenantId = getTenantId()
  const shopId = getCurrentShopId()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Tenant-Id'] = String(tenantId ?? 1)
  if (shopId) {
    config.headers['X-Shop-Id'] = String(shopId)
  }
  return config
})

request.interceptors.response.use(
  (response) => extractBizData(response.data),
  (error: AxiosError<{ msg?: string; message?: string }>) => {
    if (error.response?.status === 401) {
      clearAuthSession()
      window.dispatchEvent(new Event('nexus-auth-expired'))
    }
    const body = error.response?.data
    const message =
      body?.msg ?? body?.message ?? error.message ?? '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export function httpGet<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, config) as Promise<T>
}

export function httpPost<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<T> {
  return request.post(url, data, config) as Promise<T>
}

export function httpPut<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<T> {
  return request.put(url, data, config) as Promise<T>
}

export function httpDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.delete(url, config) as Promise<T>
}
