import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { authApi } from '../api/auth'
import {
  buildPermissionSet,
  extractPermissionStrings,
  extractRoleStrings,
  extractUserInfo,
  hasAllPerm,
  hasAnyPerm,
  hasAnyRole as _hasAnyRole,
  hasPermi,
  hasRole as _hasRole,
} from '../lib/permissions'

export type MenuNode = {
  id: number
  parentId: number
  menuType: string
  menuName: string
  path?: string
  fullPath?: string
  component?: string
  icon?: string
  perms?: string
  sort?: number
  children?: MenuNode[]
}

type PermissionsContextValue = {
  /** 是否已请求过用户信息（用于首屏避免误隐藏） */
  ready: boolean
  permissions: Set<string>
  roles: string[]
  user: Record<string, unknown>
  menus: MenuNode[]
  refresh: () => Promise<void>
  /** 单个权限 */
  can: (perm: string) => boolean
  /** 任一满足 */
  canAny: (perms: string[]) => boolean
  /** 全部满足 */
  canAll: (perms: string[]) => boolean
  /** 角色判断 */
  hasRole: (role: string) => boolean
  hasAnyRole: (roles: string[]) => boolean
}

const PermissionsContext = createContext<PermissionsContextValue | null>(null)

export function PermissionsProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const [permissions, setPermissions] = useState<Set<string>>(
    () => new Set(),
  )
  const [roles, setRoles] = useState<string[]>([])
  const [user, setUser] = useState<Record<string, unknown>>({})
  const [menus, setMenus] = useState<MenuNode[]>([])

  const refresh = useCallback(async () => {
    try {
      const data = await authApi.getCurrentUserInfo()
      const permList = extractPermissionStrings(data)
      setPermissions(buildPermissionSet(permList))
      setRoles(extractRoleStrings(data))
      setUser(extractUserInfo(data))
      const rawMenus = (data as Record<string, unknown>).menus
      setMenus(Array.isArray(rawMenus) ? rawMenus as MenuNode[] : [])
    } catch (e) {
      console.error('[PermissionsContext] 加载用户信息失败', e)
      setPermissions(new Set())
      setRoles([])
      setUser({})
      setMenus([])
    } finally {
      setReady(true)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const value = useMemo((): PermissionsContextValue => {
    const effective = (fn: (set: Set<string>) => boolean) => {
      if (!ready) return false
      return fn(permissions)
    }
    return {
      ready,
      permissions,
      roles,
      user,
      menus,
      refresh,
      can: (perm) => effective((set) => hasPermi(set, perm)),
      canAny: (perms) => effective((set) => hasAnyPerm(set, perms)),
      canAll: (perms) => effective((set) => hasAllPerm(set, perms)),
      hasRole: (role) => {
        if (!ready) return false
        return _hasRole(roles, role)
      },
      hasAnyRole: (targets) => {
        if (!ready) return false
        return _hasAnyRole(roles, targets)
      },
    }
  }, [ready, permissions, roles, user, menus, refresh])

  return (
    <PermissionsContext.Provider value={value}>
      {children}
    </PermissionsContext.Provider>
  )
}

export function usePermissions(): PermissionsContextValue {
  const ctx = useContext(PermissionsContext)
  if (!ctx) {
    throw new Error('usePermissions 必须在 PermissionsProvider 内使用')
  }
  return ctx
}

/** 类若依 v-hasPermi：无任一权限时不渲染子节点 */
export function PermGate({
  children,
  perms,
  match = 'any',
}: {
  children: ReactNode
  perms: string[]
  match?: 'any' | 'all'
}) {
  const { canAny, canAll } = usePermissions()
  const ok = match === 'all' ? canAll(perms) : canAny(perms)
  if (!ok) return null
  return <>{children}</>
}

/** 类若依 v-hasRole：无任一角色时不渲染子节点 */
export function RoleGate({
  children,
  roles,
}: {
  children: ReactNode
  roles: string[]
}) {
  const { hasAnyRole } = usePermissions()
  if (!hasAnyRole(roles)) return null
  return <>{children}</>
}
