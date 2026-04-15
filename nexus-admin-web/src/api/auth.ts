import { get, post, put } from './request'

export interface LoginReq {
  username: string
  password: string
  tenantId: number
  captcha: string
  captchaKey: string
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

export interface CaptchaImage {
  uuid: string
  img: string
}

export interface ShopItem {
  shopId: number
  shopName: string
  shopType: number
  orgId: number
}

export interface UserMenuNode {
  id: number
  parentId: number
  menuType: string
  menuName: string
  path: string
  fullPath: string
  component: string | null
  icon: string
  perms: string
  sort: number
  children: UserMenuNode[]
}

export interface CurrentUserInfo {
  userId: number
  username: string
  realName: string
  tenantId: number
  orgId: number
  orgName: string
  shopId: number
  shopName: string
  roles: string[]
  menus: UserMenuNode[]
  permissions: string[]
}

export const authApi = {
  getCaptchaImage: () => get<CaptchaImage>('/system/captcha/image'),
  login: (data: LoginReq) => post<LoginResp>('/auth/login', data),
  getCurrentUserInfo: () => get<CurrentUserInfo>('/system/user/info'),
  logout: () => post<void>('/auth/logout'),
  getShops: () => get<ShopItem[]>('/auth/shops'),
  switchShop: (shopId: number) => put<LoginResp>('/auth/switch-shop', { shopId }),
}
