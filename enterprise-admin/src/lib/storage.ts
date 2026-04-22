const ACCESS_TOKEN_KEY = 'nexus_token'
const CURRENT_SHOP_ID_KEY = 'nexus_current_shop_id'
const TENANT_ID_KEY = 'nexus_tenant_id'
const CURRENT_ORG_ID_KEY = 'nexus_current_org_id'
const DATA_SCOPE_KEY = 'nexus_data_scope'
const PRE_AUTH_TOKEN_KEY = 'nexus_pre_auth_token'
const PENDING_SHOPS_KEY = 'nexus_pending_shops'

export interface AuthSession {
  accessToken: string
  currentShopId: number
  tenantId?: number
  currentOrgId?: number
  dataScope?: number
}

export function setAuthSession(session: AuthSession): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  sessionStorage.setItem(CURRENT_SHOP_ID_KEY, String(session.currentShopId))
  if (typeof session.tenantId === 'number') {
    sessionStorage.setItem(TENANT_ID_KEY, String(session.tenantId))
  }
  if (typeof session.currentOrgId === 'number') {
    sessionStorage.setItem(CURRENT_ORG_ID_KEY, String(session.currentOrgId))
  }
  if (typeof session.dataScope === 'number') {
    sessionStorage.setItem(DATA_SCOPE_KEY, String(session.dataScope))
  }
}

export function clearAuthSession(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(CURRENT_SHOP_ID_KEY)
  sessionStorage.removeItem(TENANT_ID_KEY)
  sessionStorage.removeItem(CURRENT_ORG_ID_KEY)
  sessionStorage.removeItem(DATA_SCOPE_KEY)
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getCurrentShopId(): number | null {
  const raw = sessionStorage.getItem(CURRENT_SHOP_ID_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? null : parsed
}

export function getTenantId(): number | null {
  const raw = sessionStorage.getItem(TENANT_ID_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? null : parsed
}

export function getCurrentOrgId(): number | null {
  const raw = sessionStorage.getItem(CURRENT_ORG_ID_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? null : parsed
}

export function getDataScope(): number | null {
  const raw = sessionStorage.getItem(DATA_SCOPE_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? null : parsed
}

export function setPendingLoginContext(
  preAuthToken: string,
  shops: Array<{ shopId: number; shopName: string }>,
): void {
  sessionStorage.setItem(PRE_AUTH_TOKEN_KEY, preAuthToken)
  sessionStorage.setItem(PENDING_SHOPS_KEY, JSON.stringify(shops))
}

export function getPendingLoginContext(): {
  preAuthToken: string | null
  shops: Array<{ shopId: number; shopName: string }>
} {
  const preAuthToken = sessionStorage.getItem(PRE_AUTH_TOKEN_KEY)
  const rawShops = sessionStorage.getItem(PENDING_SHOPS_KEY)
  if (!rawShops) return { preAuthToken, shops: [] }
  try {
    const shops = JSON.parse(rawShops) as Array<{ shopId: number; shopName: string }>
    return { preAuthToken, shops }
  } catch {
    return { preAuthToken, shops: [] }
  }
}

export function clearPendingLoginContext(): void {
  sessionStorage.removeItem(PRE_AUTH_TOKEN_KEY)
  sessionStorage.removeItem(PENDING_SHOPS_KEY)
}
