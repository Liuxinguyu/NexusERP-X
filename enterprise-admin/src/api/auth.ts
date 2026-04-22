import { httpGet, httpPost, httpPut } from '../lib/request'

export interface PreAuthPayload {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
  tenantId?: number
}

export interface PreAuthResult {
  preAuthToken: string
  tenantId: number
  shops: Array<{ shopId: number; shopName: string }>
  expiresInSeconds: number
}

export interface ConfirmShopPayload {
  preAuthToken: string
  shopId: number
}

export interface ConfirmShopResult {
  accessToken: string
  tenantId: number
  currentShopId: number
  currentOrgId?: number
  dataScope?: number
}

export const authApi = {
  getCaptchaImage: () =>
    httpGet<{ uuid: string; img: string }>('/system/captcha/image'),

  validateCaptcha: (uuid: string, code: string) =>
    httpPost<boolean>(
      '/system/captcha/validate',
      new URLSearchParams({ uuid, code }),
      {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      },
    ),

  loginPreAuth: (payload: PreAuthPayload) =>
    httpPost<PreAuthResult>('/auth/login', payload),

  confirmShop: (payload: ConfirmShopPayload) =>
    httpPost<ConfirmShopResult>('/auth/confirm-shop', payload),

  switchShop: (shopId: number) =>
    httpPut<ConfirmShopResult>('/auth/switch-shop', { shopId }),

  /** 后端常返回 user、roles、permissions/perms 等，由前端灵活解析 */
  getCurrentUserInfo: () => httpGet<Record<string, unknown>>('/system/user/info'),

  logout: () => httpPost('/auth/logout'),
}
