import axios, { AxiosHeaders, type AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

export interface Result<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const TOKEN_KEY = 'nexus_token'
const CURRENT_SHOP_KEY = 'nexus_current_shop_id'
const REFRESH_PATH = '/auth/refresh'
const REFRESH_AHEAD_MS = 5 * 60 * 1000
const PROACTIVE_REFRESH_COOLDOWN_MS = 30 * 1000
const AUTH_WHITELIST_PATHS = ['/auth/login', '/auth/confirm-shop', '/system/captcha/image', '/system/captcha/validate']

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

let isRefreshing = false
let pendingQueue: Array<(token: string | null) => void> = []
let lastProactiveRefreshAt = 0

function getStoredToken(): string | null {
  const raw = localStorage.getItem(TOKEN_KEY)
  return raw && raw !== 'null' && raw !== 'undefined' ? raw : null
}

function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(CURRENT_SHOP_KEY)
}

function isAuthWhitelisted(url?: string): boolean {
  const path = String(url || '')
  return AUTH_WHITELIST_PATHS.some((item) => path.includes(item))
}

function isRefreshRequest(url?: string): boolean {
  return String(url || '').includes(REFRESH_PATH)
}

function flushPendingQueue(token: string | null) {
  pendingQueue.forEach((resolve) => resolve(token))
  pendingQueue = []
}

function parseJwtExpMs(token: string): number | null {
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    const exp = Number(payload?.exp)
    if (!Number.isFinite(exp)) return null
    return exp * 1000
  } catch {
    return null
  }
}

function shouldRefreshProactively(token: string): boolean {
  const expMs = parseJwtExpMs(token)
  if (!expMs) return false
  const now = Date.now()
  const inCooldown = now - lastProactiveRefreshAt < PROACTIVE_REFRESH_COOLDOWN_MS
  if (inCooldown) return false
  return expMs - now <= REFRESH_AHEAD_MS
}

async function refreshAccessToken(): Promise<string> {
  const oldToken = getStoredToken()
  if (!oldToken) throw new Error('未找到登录态')

  const refreshResp = await axios.post<Result<{ accessToken: string }>>(
    `/api/v1${REFRESH_PATH}`,
    undefined,
    {
      headers: { Authorization: `Bearer ${oldToken}` },
      timeout: 15000,
    }
  )

  const result = refreshResp.data
  if ((result.code !== 200 && result.code !== 0) || !result.data?.accessToken) {
    throw new Error(result.msg || result.message || 'Token 刷新失败')
  }
  return result.data.accessToken
}

async function refreshAccessTokenWithLock(): Promise<string> {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push((token) => {
        if (token) resolve(token)
        else reject(new Error('登录已过期，请重新登录'))
      })
    })
  }

  isRefreshing = true
  try {
    const newToken = await refreshAccessToken()
    setStoredToken(newToken)
    lastProactiveRefreshAt = Date.now()
    flushPendingQueue(newToken)
    return newToken
  } catch (error) {
    flushPendingQueue(null)
    throw error
  } finally {
    isRefreshing = false
  }
}

function redirectToLogin() {
  clearStoredToken()
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

request.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const shouldAttachAuth = !isAuthWhitelisted(config.url) && !isRefreshRequest(config.url)
    let token = getStoredToken()

    if (token && shouldAttachAuth && shouldRefreshProactively(token)) {
      try {
        token = await refreshAccessTokenWithLock()
      } catch {
        redirectToLogin()
        throw new Error('登录已过期，请重新登录')
      }
    }

    if (token && config.headers && shouldAttachAuth) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse<Result<any>>) => {
    const result = response.data
    // 兼容后端不同约定：code=200 或 code=0 都视为成功
    if (result.code === 200 || result.code === 0) return result.data

    const msg = result.msg || result.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  async (error: AxiosError<Result<any>>) => {
    const originalConfig = error.config as RetryableRequestConfig | undefined
    const status = error.response?.status

    if (!originalConfig) {
      ElMessage.error(error.message || '网络错误')
      return Promise.reject(error)
    }

    // 仅处理“非刷新接口”的 401，并发下仅允许一个刷新请求
    if (status === 401 && !originalConfig._retry && !isRefreshRequest(originalConfig.url)) {
      originalConfig._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push((newToken) => {
            if (!newToken) {
              reject(new Error('登录已过期，请重新登录'))
              return
            }
            if (!originalConfig.headers) originalConfig.headers = AxiosHeaders.from({})
            originalConfig.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalConfig))
          })
        })
      }

      try {
        const newToken = await refreshAccessTokenWithLock()
        if (!originalConfig.headers) originalConfig.headers = AxiosHeaders.from({})
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        return request(originalConfig)
      } catch (refreshError: any) {
        redirectToLogin()
        ElMessage.error(refreshError?.message || '登录已过期，请重新登录')
        return Promise.reject(refreshError)
      }
    }

    const backendMsg = error.response?.data?.msg || error.response?.data?.message
    ElMessage.error(backendMsg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request

export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.get<any, T>(url, { params, ...config })
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.post<any, T>(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.put<any, T>(url, data, config)
}

export function del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.delete<any, T>(url, { params, ...config })
}
