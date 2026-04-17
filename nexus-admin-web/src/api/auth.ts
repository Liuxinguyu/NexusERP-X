import request, { get, post, put } from './request'

export interface LoginReq {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}

export interface ShopItem {
  shopId: number
  shopName: string
  shopType?: number
  orgId?: number
}

export interface PreAuthLoginResponse {
  preAuthToken: string
  tenantId: number
  recommendedShopId?: number
  shops: ShopItem[]
  expiresInSeconds: number
  requiresShopSelection: boolean
}

export interface ConfirmShopReq {
  preAuthToken: string
  shopId: number
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  tenantId: number
  currentShopId: number
  currentOrgId?: number
  dataScope?: number
  accessibleShopIds?: number[]
  accessibleOrgIds?: number[]
}

export interface CaptchaImage {
  uuid: string
  img: string
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

export interface UserProfile {
  userId: number
  username: string
  realName: string
  avatarUrl?: string
  tenantId: number
  currentShopId?: number
  currentOrgId?: number
  dataScope?: number
  accessibleShopIds?: number[]
  accessibleOrgIds?: number[]
}

export interface CurrentUserInfo {
  profile: UserProfile
  menus: UserMenuNode[]
  latestNoticeTitle?: string
}

export const login = (data: LoginReq) => request.post<PreAuthLoginResponse>('/auth/login', data)
export const confirmShop = (data: ConfirmShopReq) => post<LoginResponse>('/auth/confirm-shop', data)
export const getUserShops = () => request.get<ShopItem[]>('/auth/shops')
export const switchShop = (shopId: number) => put<LoginResponse>('/auth/switch-shop', { shopId })

export const authApi = {
  getCaptchaImage: () => get<CaptchaImage>('/system/captcha/image'),
  // 内部直接用 request.get，不走通用 Result 包装（后端 /system/user/info 返回 Result<UserInfoResponse>）
  loginPreAuth: (data: LoginReq) => request.post<PreAuthLoginResponse>('/auth/login', data),
  getCurrentUserInfo: () => request.get<CurrentUserInfo>('/system/user/info'),
  logout: () => post<void>('/auth/logout'),
}
