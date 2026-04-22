/** 与后端角色 data_scope / DataScope 约定一致 */
export const DATA_SCOPE_OPTIONS: Array<{ value: number; label: string }> = [
  { value: 1, label: '1 · 全部数据权限' },
  { value: 2, label: '2 · 自定数据权限（需选部门）' },
  { value: 3, label: '3 · 本部门数据权限' },
  { value: 4, label: '4 · 本部门及以下数据权限' },
  { value: 5, label: '5 · 仅本人数据权限' },
  { value: 6, label: '6 · 本店数据权限（需选店铺）' },
]

export function dataScopeLabel(v: number | undefined): string {
  return DATA_SCOPE_OPTIONS.find((o) => o.value === v)?.label ?? String(v ?? '—')
}
