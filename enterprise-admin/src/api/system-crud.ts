import { httpDelete, httpGet, httpPost, httpPut } from '../lib/request'
import { pickPageRecords } from '../lib/http-helpers'
import type { PageResult } from '../types/api'
import type {
  DashboardSummary,
  DictItemRow,
  DictTypeRow,
  LoginLogRow,
  RoleRow,
  ShopItem,
  ShopQuery,
  ShopCreateRequest,
  ShopUpdateRequest,
  ShopStatusRequest,
  ShopRoleItem,
  UserRow,
  OrgNode,
  OrgCreateRequest,
  OrgUpdateRequest,
  MenuTreeNode,
  MenuForm,
  ConfigRow,
  PostRow,
  OperLogRow,
  OnlineUserRow,
} from '../types/system-crud'

export async function unwrapPage<T>(p: Promise<PageResult<T>>): Promise<{
  rows: T[]
  total: number
}> {
  const res = await p
  return { rows: pickPageRecords(res), total: res.total ?? 0 }
}

/** 字典类型 — 标准 REST + 分页 */
export const dictTypeApi = {
  page: (params: { current: number; size: number }) =>
    httpGet<PageResult<DictTypeRow>>('/system/dict-type/page', { params }),
  /** 兼容旧列表接口 */
  list: () => httpGet<DictTypeRow[]>('/system/dict-type/list'),
  get: (id: number) => httpGet<DictTypeRow>(`/system/dict-type/detail/${id}`),
  create: (body: Partial<DictTypeRow>) =>
    httpPost<unknown>('/system/dict-type', body),
  update: (id: number, body: Partial<DictTypeRow>) =>
    httpPut<unknown>(`/system/dict-type/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/dict-type/${id}`),
}

/** 字典项 */
export const dictItemApi = {
  page: (params: {
    current: number
    size: number
    dictType?: string
  }) =>
    httpGet<PageResult<DictItemRow>>('/system/dict-item/page', { params }),
  listByType: (dictType: string) =>
    httpGet<DictItemRow[]>('/system/dict-item/list-by-type', {
      params: { dictType },
    }),
  get: (id: number) => httpGet<DictItemRow>(`/system/dict-item/detail/${id}`),
  create: (body: Partial<DictItemRow>) =>
    httpPost<unknown>('/system/dict-item', body),
  update: (id: number, body: Partial<DictItemRow>) =>
    httpPut<unknown>(`/system/dict-item/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/dict-item/${id}`),
}

export const roleApi = {
  page: (params: {
    current: number
    size: number
    roleName?: string
    roleCode?: string
    status?: number
  }) =>
    httpGet<PageResult<RoleRow>>('/system/roles/page', { params }),
  options: () => httpGet<RoleRow[]>('/system/roles/options'),
  get: (id: number) => httpGet<RoleRow>(`/system/roles/detail/${id}`),
  create: (body: Partial<RoleRow>) => httpPost<unknown>('/system/roles', body),
  update: (id: number, body: Partial<RoleRow>) =>
    httpPut<unknown>(`/system/roles/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/roles/${id}`),
  getMenuIds: (id: number) =>
    httpGet<number[]>(`/system/roles/${id}/menu-ids`),
  /** 后端 @RequestBody：{ "menuIds": [1, 2, 3] } */
  setMenus: (id: number, menuIds: number[]) =>
    httpPut<unknown>(`/system/roles/${id}/menus`, { menuIds }),
}

export const shopApi = {
  getShopPage: (params: ShopQuery) =>
    httpGet<PageResult<ShopItem>>('/system/shops/page', { params }),
  getShopOptions: () => httpGet<ShopItem[]>('/system/shops/options'),
  getShopDetail: (id: number) => httpGet<ShopItem>(`/system/shops/detail/${id}`),
  createShop: (body: ShopCreateRequest) => httpPost<unknown>('/system/shops', body),
  updateShop: (id: number, body: ShopUpdateRequest) =>
    httpPut<unknown>(`/system/shops/${id}`, body),
  deleteShop: (id: number) => httpDelete<unknown>(`/system/shops/${id}`),
  updateShopStatus: (id: number, body: ShopStatusRequest) =>
    httpPut<unknown>(`/system/shops/${id}/status`, body),
}

export const userAdminApi = {
  page: (params: {
    current: number
    size: number
    username?: string
    nickname?: string
    orgId?: number
  }) =>
    httpGet<PageResult<UserRow>>('/system/users/page', { params }),
  get: (id: number) => httpGet<UserRow>(`/system/users/detail/${id}`),
  create: (body: Partial<UserRow>) => httpPost<unknown>('/system/users', body),
  update: (id: number, body: Partial<UserRow>) =>
    httpPut<unknown>(`/system/users/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/users/${id}`),
  getShopRoles: (id: number) =>
    httpGet<unknown>(`/system/users/${id}/shop-roles`),
  setShopRoles: (id: number, items: ShopRoleItem[]) =>
    httpPut<unknown>(`/system/users/${id}/shop-roles`, { items }),
  /** 与店铺启停一致：Query ?status= */
  setStatus: (id: number, status: number) =>
    httpPut<unknown>(`/system/users/${id}/status`, undefined, {
      params: { status },
    }),
  resetPwd: (id: number, newPassword: string) =>
    httpPut<unknown>(`/system/users/${id}/reset-pwd`, { newPassword }),
}

export const menuApi = {
  tree: () => httpGet<MenuTreeNode[]>('/system/menus/tree'),
  get: (id: number) => httpGet<MenuTreeNode>(`/system/menus/detail/${id}`),
  create: (body: MenuForm) => httpPost<unknown>('/system/menus', body),
  update: (id: number, body: MenuForm) =>
    httpPut<unknown>(`/system/menus/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/menus/${id}`),
}

export const configApi = {
  page: (params: { current: number; size: number; configName?: string; configKey?: string }) =>
    httpGet<PageResult<ConfigRow>>('/system/config/page', { params }),
  get: (id: number) => httpGet<ConfigRow>(`/system/config/detail/${id}`),
  create: (body: Partial<ConfigRow>) => httpPost<unknown>('/system/config', body),
  update: (id: number, body: Partial<ConfigRow>) =>
    httpPut<unknown>(`/system/config/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/config/${id}`),
}

export const postApi = {
  page: (params: { current: number; size: number; postCode?: string; postName?: string }) =>
    httpGet<PageResult<PostRow>>('/system/posts/page', { params }),
  options: () => httpGet<PostRow[]>('/system/posts/options'),
  get: (id: number) => httpGet<PostRow>(`/system/posts/detail/${id}`),
  create: (body: Partial<PostRow>) => httpPost<unknown>('/system/posts', body),
  update: (id: number, body: Partial<PostRow>) =>
    httpPut<unknown>(`/system/posts/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/system/posts/${id}`),
}

export const operLogApi = {
  page: (params: { current: number; size: number; module?: string; username?: string; status?: number }) =>
    httpGet<PageResult<OperLogRow>>('/system/oper-log/page', { params }),
  clean: () => httpDelete<unknown>('/system/oper-log/clean'),
}

export const loginLogApi = {
  page: (params: { pageNum: number; pageSize: number; username?: string; status?: number }) =>
    httpGet<PageResult<LoginLogRow>>('/system/login-log/page', { params }),
  clean: () => httpDelete<unknown>('/system/login-log/clean'),
}

export const onlineUserApi = {
  page: (params: { pageNum: number; pageSize: number; username?: string; ip?: string }) =>
    httpGet<PageResult<OnlineUserRow>>('/system/online-users', { params }),
  kick: (userId: number) => httpDelete<unknown>(`/system/online-users/${userId}`),
}

export const orgApi = {
  tree: () => httpGet<OrgNode[]>('/system/org/tree'),
  create: (body: OrgCreateRequest) => httpPost<unknown>('/system/org', body),
  update: (body: OrgUpdateRequest) => httpPut<unknown>('/system/org', body),
  remove: (id: number) => httpDelete<unknown>(`/system/org/${id}`),
}

export const systemApi = {
  getDashboard: () =>
    httpGet<DashboardSummary>('/system/workbench/dashboard'),
  getSalesChart: () =>
    httpGet<Array<{ month: string; value: number }>>('/system/workbench/sales-chart'),
}
