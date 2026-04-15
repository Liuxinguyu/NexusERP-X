import axios, { AxiosError } from 'axios'
import type { AxiosResponse } from 'axios'
import type { Result } from '../types/common'
import { ApiError } from '../types/common'
import { getAuthSnapshot } from '../store/authStore'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

const redirectToLogin = () => {
  const { clearSession } = getAuthSnapshot()
  clearSession()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

http.interceptors.request.use((config) => {
  const auth = getAuthSnapshot()
  const h = config.headers
  if (auth.token) {
    h.set('Authorization', `Bearer ${auth.token}`)
  } else {
    h.delete('Authorization')
  }
  if (auth.tenantId != null) {
    h.set('X-Tenant-Id', String(auth.tenantId))
  } else {
    h.delete('X-Tenant-Id')
  }
  if (auth.currentShopId != null) {
    h.set('X-Shop-Id', String(auth.currentShopId))
  } else {
    h.delete('X-Shop-Id')
  }
  return config
})

http.interceptors.response.use(
  ((response: AxiosResponse<Result<unknown>>) => {
    const payload = response.data as Result<unknown>
    if (payload.code !== 0) {
      return Promise.reject(new ApiError(payload.message || '请求失败', payload.code))
    }
    return payload.data
  }) as never,
  (error: AxiosError<Result<unknown>>) => {
    const status = error.response?.status
    if (status === 401) {
      redirectToLogin()
      return Promise.reject(new ApiError('登录已失效，请重新登录', 401))
    }
    const payload = error.response?.data
    if (payload) {
      return Promise.reject(new ApiError(payload.message || '请求失败', payload.code))
    }
    return Promise.reject(new ApiError(error.message || '网络异常', -1))
  },
)
