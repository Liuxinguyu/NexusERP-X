import request, { get, post, put, del } from './request'

function withPageParams(current: number, size: number, extra?: Record<string, unknown>) {
  return { current, size, page: current, pageNum: current, pageSize: size, ...(extra || {}) }
}

// User Info
export interface UserProfile {
  userId: number
  username: string
  realName: string
  avatarUrl: string | null
  tenantId: number
  currentShopId: number
  currentOrgId: number
  dataScope: number
  accessibleShopIds: number[]
  accessibleOrgIds: number[]
}

export interface MenuNode {
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
  children: MenuNode[]
}

export interface UserInfoResp {
  profile: UserProfile
  menus: MenuNode[]
  latestNoticeTitle: string
}

// Page result wrapper
export interface PageResult<T> {
  list?: T[]
  records?: T[]
  total: number
  page?: number
  size?: number
  current?: number
}

// Org
export interface OrgTreeNode {
  id: number
  parentId: number
  orgName: string
  orgCode: string
  userCount?: number
  children?: OrgTreeNode[]
}

// Shops
export interface ShopVO {
  id: number
  orgId: number
  shopName: string
  shopType: number
  status: number
}

// Users
export interface SysUser {
  id: number
  username: string
  realName: string
  status: number
  mainShopId: number
  mainOrgId: number
  delFlag: number
  createTime?: string
}

// Roles
export interface SysRole {
  id: number
  shopId: number
  roleCode: string
  roleName: string
  dataScope: number
  delFlag: number
}

// Notices
export interface SysNotice {
  id: number
  title: string
  content: string
  noticeType: string
  status: number
  expireTime?: string
  createTime?: string
}

// Login Log
export interface SysLoginLog {
  id: number
  username: string
  status: number
  ip: string
  userAgent: string
  msg: string
  createTime: string
}

// Online User
export interface OnlineUser {
  userId: number
  username: string
  realName: string
  shopId: number
  shopName: string
  ip: string
  loginTime: string
}

// Workbench
export interface DashboardSummary {
  todaySaleAmount: number
  monthlyPurchaseAmount: number
  customerCount: number
  supplierCount: number
  pendingApprovalCount: number
  stockAlarmCount: number
}

export interface DictItemVO {
  id?: number
  typeCode?: string
  itemLabel?: string
  itemValue?: string | number
  label?: string
  value?: string | number
  dictLabel?: string
  dictValue?: string | number
  name?: string
  code?: string | number
}

export const systemApi = {
  // Auth / User
  getUserInfo: () => get<UserInfoResp>('/system/user/info'),
  changeOrg: (userId: number, newOrgId: number) =>
    put('/system/user/change-org', { userId, newOrgId }),

  // Shops
  getShopPage: (current: number, size: number, shopName?: string) =>
    get<PageResult<ShopVO>>('/system/shops/page', withPageParams(current, size, { shopName })),
  getShopOptions: () => get<ShopVO[]>('/system/shops/options'),
  createShop: (data: any) => post<number>('/system/shops', data),
  updateShop: (id: number, data: any) => put(`/system/shops/${id}`, data),
  updateShopStatus: (id: number, status: number) =>
    put(`/system/shops/${id}/status`, { status }),

  // Users
  getUserPage: (current: number, size: number, username?: string) =>
    get<PageResult<SysUser>>('/system/users/page', withPageParams(current, size, { username })),
  createUser: (data: any) => post<number>('/system/users', data),
  updateUser: (id: number, data: any) => put(`/system/users/${id}`, data),
  getUserShopRoles: (id: number) => get<any[]>('/system/users/' + id + '/shop-roles'),
  saveUserShopRoles: (id: number, data: any) => put(`/system/users/${id}/shop-roles`, data),

  // Roles
  getRolePage: (current: number, size: number, roleName?: string, roleCode?: string) =>
    get<PageResult<SysRole>>('/system/roles/page', withPageParams(current, size, { roleName, roleCode })),
  getRoleOptions: () => get<any[]>('/system/roles/options'),
  createRole: (data: any) => post<number>('/system/roles', data),
  updateRole: (id: number, data: any) => put(`/system/roles/${id}`, data),
  getRoleMenuIds: (id: number) => get<number[]>('/system/roles/' + id + '/menu-ids'),
  assignRoleMenus: (id: number, menuIds: number[]) => put(`/system/roles/${id}/menus`, { menuIds }),

  // Orgs
  getOrgTree: () => get<OrgTreeNode[]>('/system/org/tree'),
  getOrgTreeLazy: (parentId = 0) => get<OrgTreeNode[]>('/system/org/tree-lazy', { parentId }),
  createOrg: (data: any) => post<number>('/system/org', data),
  updateOrg: (data: any) => put('/system/org', data),
  deleteOrg: (id: number) => del(`/system/org/${id}`),

  // Menus
  getMenuTree: () => get<MenuNode[]>('/system/menus/tree'),

  // Notices
  getNoticePage: (pageNum: number, pageSize: number) =>
    get<PageResult<SysNotice>>('/system/notice/page', withPageParams(pageNum, pageSize)),
  createNotice: (data: any) => post<number>('/system/notice', data),
  updateNotice: (id: number, data: any) => put(`/system/notice/${id}`, data),
  publishNotice: (id: number) => put(`/system/notice/${id}/publish`),

  // Login Log
  getLoginLogPage: (pageNum: number, pageSize: number) =>
    get<PageResult<SysLoginLog>>('/system/login-log/page', withPageParams(pageNum, pageSize)),

  // Online Users
  getOnlineUserPage: (pageNum: number, pageSize: number) =>
    get<{ records: OnlineUser[]; total: number }>('/system/online-users', withPageParams(pageNum, pageSize)),
  kickUser: (userId: number) => del(`/system/online-users/${userId}`),

  // Workbench
  getDashboard: () => get<DashboardSummary>('/system/workbench/dashboard'),
  getSalesChart: () => get<any>('/system/workbench/sales-chart'),
  getPurchaseChart: () => get<any>('/system/workbench/purchase-chart'),
  getTopProducts: (limit = 10) => get<any[]>('/system/workbench/top-products', { limit }),
  getStockAlarms: (limit = 20) => get<any[]>('/system/workbench/stock-alarms', { limit }),

  // Dict
  getDictTypes: () => get<any[]>('/system/dict-type/list'),
  getDictItems: (dictType: string) => get<any[]>('/system/dict-item/list-by-type', { dictType }),
  getDictItemsByTypeCode: (typeCode: string) =>
    get<DictItemVO[]>(`/system/dicts/items/${encodeURIComponent(typeCode)}`),

  // Message
  getUnreadCount: () => get<number>('/system/message/unread-count'),
}
