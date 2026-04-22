import { lazy, type ComponentType } from 'react'

type LazyComponent = React.LazyExoticComponent<ComponentType<unknown>>

const COMPONENT_MAP: Record<string, LazyComponent> = {
  'system/user': lazy(() => import('../features/system/UserManage')),
  'system/role': lazy(() => import('../features/system/RoleManage')),
  'system/menu': lazy(() => import('../features/system/MenuManage')),
  'system/org': lazy(() => import('../features/system/OrgManage')),
  'system/post': lazy(() => import('../features/system/PostManage')),
  'system/config': lazy(() => import('../features/system/ConfigManage')),
  'system/dict': lazy(() => import('../features/system/DictManage')),
  'system/shop': lazy(() => import('../features/system/ShopManage')),
  'monitor/loginlog': lazy(() => import('../features/system/LoginLogManage')),
  'monitor/operlog': lazy(() => import('../features/system/OperLogManage')),
  'monitor/online': lazy(() => import('../features/system/OnlineUserManage')),
  'erp/sale-order': lazy(() => import('../features/erp/SaleOrderManage')),
  'erp/purchase-order': lazy(() => import('../features/erp/PurchaseOrderManage')),
  'erp/stock': lazy(() => import('../features/erp/StockQuery')),
  'erp/finance': lazy(() => import('../features/erp/FinanceManage')),
  'erp/report': lazy(() => import('../features/erp/ReportView')),
  'oa/attendance': lazy(() => import('../features/oa/AttendanceManage')),
  'oa/approval': lazy(() => import('../features/oa/ApprovalCenter')),
  'oa/task': lazy(() => import('../features/oa/TaskManage')),
  'oa/cloud-disk': lazy(() => import('../features/oa/CloudDisk')),
  'oa/notice': lazy(() => import('../features/oa/NoticeManage')),
  'wage/slip': lazy(() => import('../features/wage/SlipManage')),
  'wage/config': lazy(() => import('../features/wage/ItemConfigManage')),
  'crm/opportunity': lazy(() => import('../features/crm/OpportunityManage')),
  'crm/contract': lazy(() => import('../features/crm/ContractManage')),
}

/**
 * 将后端 component 字段标准化为注册表 key。
 * 兼容旧格式 'views/system/user/index' → 'system/user'
 */
function normalizeComponent(component: string): string {
  let c = component
  if (c.startsWith('views/')) c = c.slice(6)
  if (c.endsWith('/index')) c = c.slice(0, -6)
  return c
}

export function resolveComponent(component: string | undefined): LazyComponent | null {
  if (!component) return null
  return COMPONENT_MAP[component] ?? COMPONENT_MAP[normalizeComponent(component)] ?? null
}
