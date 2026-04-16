import request, { get, post, type Result } from './request'

// 1. 登录参数 (绝对不要包含 tenantId)
export interface LoginReq {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}

// 2. 店铺树节点
export interface ShopTreeVO {
  id: number
  parentId: number
  shopName: string
  children?: ShopTreeVO[]
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

export interface CurrentUserInfo {
  userId: number
  username: string
  realName: string
  orgId: number
  orgName: string
  shopId: number
  shopName: string
  roles: string[]
  menus: UserMenuNode[]
  permissions: string[]
}

// 3. 接口导出
export const login = (data: LoginReq) => request.post<Result<string>>('/api/v1/auth/login', data)
export const getUserShops = () => request.get<Result<ShopTreeVO[]>>('/api/v1/auth/shops')
export const switchShop = (shopId: number) => request.post<Result<string>>('/api/v1/auth/switch-shop', { shopId })

// 兼容已有模块
export const authApi = {
  getCaptchaImage: () => get<CaptchaImage>('/system/captcha/image'),
  getCurrentUserInfo: () => get<CurrentUserInfo>('/system/user/info'),
  logout: () => post<void>('/auth/logout'),
}
