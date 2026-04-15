import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

const AUTH_WHITELIST_PATHS = ['/auth/login', '/system/captcha/image', '/system/captcha/validate']

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const raw = localStorage.getItem('nexus_token')
    const token = raw && raw !== 'null' && raw !== 'undefined' ? raw : null
    const reqUrl = String(config.url || '')
    const shouldSkipAuth = AUTH_WHITELIST_PATHS.some((path) => reqUrl.includes(path))
    if (token && config.headers && !shouldSkipAuth) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code === undefined || res.code === 0) {
      return res.data !== undefined ? res.data : response.data
    }
    if (res.code === 401) {
      localStorage.removeItem('nexus_token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error(res.message || '未认证'))
    }
    if (res.code === 403) {
      ElMessage.error('无访问权限')
      return Promise.reject(new Error(res.message || '无权限'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // 不要在登录页对 401/403 弹错误提示，避免干扰用户输入
    const isLoginPage = router.currentRoute.value.path === '/login'
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      localStorage.removeItem('nexus_token')
      if (!isLoginPage) {
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      }
    } else if (error.response?.status !== 401) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request

export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, { params, ...config }) as Promise<T>
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.post(url, data, config) as Promise<T>
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.put(url, data, config) as Promise<T>
}

export function del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.delete(url, { params, ...config }) as Promise<T>
}
