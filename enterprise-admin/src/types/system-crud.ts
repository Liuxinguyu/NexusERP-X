export interface DashboardSummary {
  revenueToday?: number
  orderToday?: number
  pendingApproval?: number
  activeCustomer?: number
  [key: string]: unknown
}

export interface DictTypeRow {
  id?: number
  dictName?: string
  dictType?: string
  status?: number
  remark?: string
  [key: string]: unknown
}

export interface DictItemRow {
  id?: number
  dictType?: string
  dictLabel?: string
  label?: string
  dictValue?: string
  itemValue?: string
  sort?: number
  status?: number
  remark?: string
  [key: string]: unknown
}

export interface RoleRow {
  id?: number
  roleName?: string
  roleCode?: string
  /** 1全部 2自定 3本部门 4本部门及以下 5仅本人 6本店 */
  dataScope?: number
  status?: number
  remark?: string
  /** data_scope=2 时自定义可见部门 */
  orgIds?: number[]
  /** data_scope=6 等场景下绑定的店铺 */
  shopIds?: number[]
  createTime?: string
  [key: string]: unknown
}

export interface ShopRow {
  id?: number
  orgId?: number
  shopName?: string
  shopType?: number
  status?: number
  remark?: string
  [key: string]: unknown
}

export interface UserRow {
  id?: number
  username?: string
  realName?: string
  phone?: string
  status?: number
  mainShopId?: number
  mainOrgId?: number
  password?: string
  /** 后端 selectUserList 连表返回 */
  deptName?: string
  /** 后端 selectUserList 连表返回 */
  shopName?: string
  createTime?: string
  [key: string]: unknown
}

export interface ShopRoleItem {
  shopId: number
  roleId: number
}

// --- 店铺管理专用类型 ---

export interface ShopItem {
  id?: number
  shopName?: string
  shopType?: number
  status?: number
  orgId?: number
  remark?: string
  [key: string]: unknown
}

export interface ShopQuery {
  current: number
  size: number
  shopName?: string
  orgId?: number
}

export interface ShopCreateRequest {
  shopName: string
  shopType?: number
  orgId: number
  remark?: string
}

export interface ShopUpdateRequest {
  shopName: string
  shopType?: number
  orgId: number
  remark?: string
}

export interface ShopStatusRequest {
  status: number
}

// --- 组织结构专用类型 ---

export interface OrgNode {
  id: number
  parentId: number
  orgName: string
  orgCode: string
  userCount: number
  orgType?: number
  sort?: number
  status?: number
  children?: OrgNode[]
}

export interface OrgCreateRequest {
  parentId: number
  orgCode: string
  orgName: string
  orgType?: number
  sort?: number
  status?: number
}

export interface OrgUpdateRequest extends OrgCreateRequest {
  id: number
}

// --- 菜单管理类型 ---

export interface MenuTreeNode {
  id: number
  parentId: number
  /** M=目录 C=菜单 F=按钮 */
  menuType: string
  menuName: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  children?: MenuTreeNode[]
}

export interface MenuForm {
  parentId: number
  menuName: string
  menuType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
}

// --- 参数配置类型 ---

export interface ConfigRow {
  id?: number
  configName?: string
  configKey?: string
  configValue?: string
  /** Y=内置 N=非内置 */
  configType?: string
  remark?: string
  createTime?: string
}

// --- 岗位管理类型 ---

export interface PostRow {
  id?: number
  postCode?: string
  postName?: string
  status?: number
  sort?: number
  remark?: string
  createTime?: string
}

// --- 操作日志类型 ---

export interface OperLogRow {
  id?: number
  module?: string
  operType?: string
  username?: string
  operUrl?: string
  operIp?: string
  requestParam?: string
  responseData?: string
  status?: number
  costTime?: number
  errorMsg?: string
  createTime?: string
}

// --- 登录日志类型 ---

export interface LoginLogRow {
  id?: number
  username?: string
  status?: number
  ip?: string
  userAgent?: string
  msg?: string
  createTime?: string
  [key: string]: unknown
}

// --- 在线用户类型 ---

export interface OnlineUserRow {
  userId?: number
  username?: string
  ip?: string
  loginTime?: number
  userAgent?: string
}
