import { get, post, put } from './request'

export interface LoginReq {
  username: string
  password: string
  tenantId?: number
  captcha?: string
  captchaKey?: string
}

export interface LoginResp {
  accessToken: string
  tokenType: string
  tenantId: number
  currentShopId: number
  currentOrgId: number
  dataScope: number
  accessibleShopIds: number[]
  accessibleOrgIds: number[]
}

export interface ShopItem {
  shopId: number
  shopName: string
  shopType: number
  orgId: number
}

export const authApi = {
  login: (data: LoginReq) => post<LoginResp>('/auth/login', data),
  logout: () => post<void>('/auth/logout'),
  getShops: () => get<ShopItem[]>('/auth/shops'),
  switchShop: (shopId: number) => put<LoginResp>('/auth/switch-shop', { shopId }),
}
