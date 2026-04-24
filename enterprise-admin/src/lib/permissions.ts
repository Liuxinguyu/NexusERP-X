const SUPER_PERM = '*:*:*'

/** 从 /system/user/info 等任意结构里收集权限字符串（兼容若依常见字段名） */
export function extractPermissionStrings(raw: unknown): string[] {
  if (raw == null || typeof raw !== 'object') return []
  const o = raw as Record<string, unknown>
  const candidates: unknown[] = [
    o.permissions,
    o.perms,
    o.authorities,
    (o.user as Record<string, unknown> | undefined)?.permissions,
  ]
  const out = new Set<string>()
  for (const c of candidates) {
    if (Array.isArray(c)) {
      for (const x of c) {
        if (typeof x === 'string' && x) out.add(x)
      }
    }
  }
  return [...out]
}

/** 从 /system/user/info 返回中提取角色标识（兼容 roles / roleList / roleKeys） */
export function extractRoleStrings(raw: unknown): string[] {
  if (raw == null || typeof raw !== 'object') return []
  const o = raw as Record<string, unknown>
  const candidates: unknown[] = [
    o.roles,
    o.roleList,
    o.roleKeys,
    (o.user as Record<string, unknown> | undefined)?.roles,
  ]
  const out = new Set<string>()
  for (const c of candidates) {
    if (Array.isArray(c)) {
      for (const x of c) {
        if (typeof x === 'string' && x) out.add(x)
      }
    }
  }
  return [...out]
}

/** 从 getInfo 返回中提取 user 对象 */
export function extractUserInfo(raw: unknown): Record<string, unknown> {
  if (raw == null || typeof raw !== 'object') return {}
  const o = raw as Record<string, unknown>
  if (o.profile && typeof o.profile === 'object') {
    return o.profile as Record<string, unknown>
  }
  if (o.user && typeof o.user === 'object') {
    return o.user as Record<string, unknown>
  }
  return o
}

export function buildPermissionSet(perms: string[]): Set<string> {
  return new Set(perms.map((p) => p.trim()).filter(Boolean))
}

/**
 * 与后端 PermissionService#hasPermi 语义对齐：超级权限或包含目标标识则通过。
 */
export function hasPermi(set: Set<string>, permission: string): boolean {
  if (set.has(SUPER_PERM)) return true
  if (!permission) return false
  return set.has(permission.trim())
}

export function hasAnyPerm(set: Set<string>, permissions: string[]): boolean {
  if (permissions.length === 0) return false
  return permissions.some((p) => hasPermi(set, p))
}

export function hasAllPerm(set: Set<string>, permissions: string[]): boolean {
  if (permissions.length === 0) return false
  return permissions.every((p) => hasPermi(set, p))
}

/** 角色判断：是否包含指定角色标识（如 ROLE_ADMIN） */
export function hasRole(roles: string[], role: string): boolean {
  if (!role) return false
  return roles.includes(role.trim())
}

export function hasAnyRole(roles: string[], targets: string[]): boolean {
  if (targets.length === 0) return false
  return targets.some((r) => hasRole(roles, r))
}

