import axios from 'axios'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { extractBizData } from './http-helpers'
import { clearAuthSession, getAccessToken, getCurrentShopId, getTenantId } from './storage'

const API_BASE_URL = '/api/v1'
let isHandling401Logout = false

export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getAccessToken()
  const tenantId = getTenantId()
  const shopId = getCurrentShopId()

  if (token && tenantId == null) {
    window.dispatchEvent(new Event('nexus-auth-expired'))
    return Promise.reject(new Error('租户信息缺失，请重新登录'))
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (tenantId != null) {
    config.headers['X-Tenant-Id'] = String(tenantId)
  }
  if (shopId != null) {
    config.headers['X-Shop-Id'] = String(shopId)
  }
  return config
})

async function notifyServerLogoutBeforeClearSession(): Promise<void> {
  const token = getAccessToken()
  const tenantId = getTenantId()
  if (!token) return
  if (tenantId == null) return
  try {
    const shopId = getCurrentShopId()
    await fetch(`${API_BASE_URL}/auth/logout`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Tenant-Id': String(tenantId),
        ...(shopId != null
          ? { 'X-Shop-Id': String(shopId) }
          : {}),
        'Content-Type': 'application/json',
      },
    })
  } catch {
    /* 与 axios 退避一致：失败仍清本地会话 */
  }
}

request.interceptors.response.use(
  (response) => extractBizData(response.data),
  async (error: AxiosError<{ msg?: string; message?: string }>) => {
    const status = error.response?.status
    if (status === 401) {
      if (isHandling401Logout) {
        return Promise.reject(new Error('登录状态已失效，请重新登录'))
      }
      isHandling401Logout = true
      try {
        await notifyServerLogoutBeforeClearSession()
      } catch {
        /* ignore */
      } finally {
        clearAuthSession()
        window.dispatchEvent(new Event('nexus-auth-expired'))
        isHandling401Logout = false
      }
      return Promise.reject(new Error('登录状态已失效，请重新登录'))
    }
    if (status === 403) {
      return Promise.reject(new Error('权限不足，请联系管理员'))
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
