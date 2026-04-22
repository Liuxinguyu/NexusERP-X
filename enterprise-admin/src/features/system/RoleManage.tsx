import { useCallback, useEffect, useState } from 'react'
import { menuApi, orgApi, roleApi, shopApi, unwrapPage } from '../../api/system-crud'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { useToast } from '../../components/Toast'
import { PermGate } from '../../context/PermissionsContext'
import { DATA_SCOPE_OPTIONS, dataScopeLabel } from '../../lib/data-scope'
import { flattenOrgTree } from '../../lib/org-flat'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import type { RoleRow } from '../../types/system-crud'

function parseIdArray(raw: unknown): number[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map((x) => Number(x))
    .filter((n) => Number.isFinite(n))
}

type MenuTreeLite = {
  id: number
  label: string
  children: MenuTreeLite[]
}

function buildMenuTree(raw: unknown): MenuTreeLite[] {
  if (!Array.isArray(raw)) return []
  const walk = (nodes: unknown[]): MenuTreeLite[] => {
    const out: MenuTreeLite[] = []
    for (const n of nodes) {
      if (!n || typeof n !== 'object') continue
      const o = n as Record<string, unknown>
      const id = Number(o.id)
      if (!Number.isFinite(id)) continue
      const label = String(o.menuName ?? o.path ?? id)
      const children = Array.isArray(o.children) ? walk(o.children as unknown[]) : []
      out.push({ id, label, children })
    }
    return out
  }
  return walk(raw as unknown[])
}

function MenuTreeView({
  nodes,
  expandedIds,
  checked,
  onToggleExpand,
  onToggleCheck,
  depth,
}: {
  nodes: MenuTreeLite[]
  expandedIds: Set<number>
  checked: Set<number>
  onToggleExpand: (id: number) => void
  onToggleCheck: (id: number) => void
  depth: number
}) {
  return (
    <div className="space-y-1">
      {nodes.map((n) => {
        const hasChildren = n.children.length > 0
        const expanded = expandedIds.has(n.id)
        return (
          <div key={n.id}>
            <div
              className="flex items-center gap-2 text-xs font-bold text-slate-700 px-2 py-1 hover:bg-slate-50 rounded"
              style={{ paddingLeft: `${depth * 1.25}rem` }}
            >
              {hasChildren ? (
                <button
                  type="button"
                  onClick={() => onToggleExpand(n.id)}
                  className="w-5 h-5 flex items-center justify-center text-slate-400 hover:text-indigo-600 transition"
                  title={expanded ? '折叠' : '展开'}
                >
                  {expanded ? '▾' : '▸'}
                </button>
              ) : (
                <span className="w-5" />
              )}
              <input
                type="checkbox"
                checked={checked.has(n.id)}
                onChange={() => onToggleCheck(n.id)}
              />
              <span className="truncate" title={n.label}>
                {n.label}
              </span>
            </div>
            {hasChildren && expanded ? (
              <MenuTreeView
                nodes={n.children}
                expandedIds={expandedIds}
                checked={checked}
                onToggleExpand={onToggleExpand}
                onToggleCheck={onToggleCheck}
                depth={depth + 1}
              />
            ) : null}
          </div>
        )
      })}
    </div>
  )
}

type FormErrors = Record<string, string>

function validateRoleForm(
  form: { roleName: string; roleCode: string; dataScope: number },
  selectedOrgIds: Set<number>,
  selectedShopIds: Set<number>,
  isNew: boolean,
): FormErrors {
  const errors: FormErrors = {}
  if (!form.roleName.trim()) errors.roleName = '角色名称不能为空'
  if (isNew && !form.roleCode.trim()) errors.roleCode = '角色编码不能为空'
  if (form.dataScope === 2 && selectedOrgIds.size === 0) {
    errors.orgIds = '⚠️ 自定数据权限必须选择至少一个部门，否则后端将返回无权限'
  }
  if (form.dataScope === 6 && selectedShopIds.size === 0) {
    errors.shopIds = '⚠️ 本店数据权限必须选择至少一个店铺，否则后端将返回无权限'
  }
  return errors
}

export default function RoleManage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [list, setList] = useState<RoleRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [size] = useState(10)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // --- search ---
  const [roleNameQ, setRoleNameQ] = useState('')
  const [roleCodeQ, setRoleCodeQ] = useState('')
  const [statusQ, setStatusQ] = useState<number | undefined>(undefined)
  const [searchNonce, setSearchNonce] = useState(0)

  const [orgOptions, setOrgOptions] = useState<{ id: number; label: string }[]>(
    [],
  )
  const [shopOptions, setShopOptions] = useState<
    { id: number; label: string }[]
  >([])

  const [modal, setModal] = useState<{ open: boolean; row: RoleRow | null }>({
    open: false,
    row: null,
  })
  const [form, setForm] = useState({
    roleName: '',
    roleCode: '',
    dataScope: 4,
    remark: '',
  })
  const [formErrors, setFormErrors] = useState<FormErrors>({})
  const [selectedOrgIds, setSelectedOrgIds] = useState<Set<number>>(new Set())
  const [selectedShopIds, setSelectedShopIds] = useState<Set<number>>(new Set())

  const [menuModal, setMenuModal] = useState<{
    open: boolean
    roleId: number | null
  }>({ open: false, roleId: null })
  const [menuTree, setMenuTree] = useState<MenuTreeLite[]>([])
  const [menuExpandedIds, setMenuExpandedIds] = useState<Set<number>>(new Set())
  const [checkedMenus, setCheckedMenus] = useState<Set<number>>(new Set())
  const [menuParent, setMenuParent] = useState<Map<number, number | null>>(new Map())
  const [menuChildren, setMenuChildren] = useState<Map<number, number[]>>(new Map())

  const loadRefs = useCallback(async () => {
    try {
      const tree = await orgApi.tree()
      setOrgOptions(flattenOrgTree(Array.isArray(tree) ? tree : []))
    } catch {
      setOrgOptions([])
    }
    try {
      const s = await shopApi.getShopOptions()
      const rows = Array.isArray(s) ? s : []
      setShopOptions(
        rows.map((r) => ({
          id: Number(r.id),
          label: String(r.shopName ?? r.id),
        })),
      )
    } catch {
      try {
        const { rows } = await unwrapPage(shopApi.getShopPage({ current: 1, size: 500 }))
        setShopOptions(
          rows.map((r) => ({
            id: Number(r.id),
            label: String(r.shopName ?? r.id),
          })),
        )
      } catch {
        setShopOptions([])
      }
    }
  }, [])

  useEffect(() => {
    void loadRefs()
  }, [loadRefs])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { rows, total: t } = await unwrapPage(
        roleApi.page({
          current,
          size,
          roleName: roleNameQ.trim() || undefined,
          roleCode: roleCodeQ.trim() || undefined,
          status: statusQ,
        }),
      )
      setList(rows)
      setTotal(t)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载角色失败')
    } finally {
      setLoading(false)
    }
  }, [current, size, roleNameQ, roleCodeQ, statusQ, searchNonce])

  useEffect(() => {
    void load()
  }, [load])

  /** 编辑时拉取详情中的 orgIds / shopIds（兼容 deptIds） */
  useEffect(() => {
    if (!modal.open) return
    if (!modal.row?.id) {
      setSelectedOrgIds(new Set())
      setSelectedShopIds(new Set())
      return
    }
    void (async () => {
      try {
        const d = await roleApi.get(modal.row!.id!)
        const raw = d as RoleRow & { deptIds?: unknown }
        setSelectedOrgIds(new Set(parseIdArray(raw.orgIds ?? raw.deptIds)))
        setSelectedShopIds(new Set(parseIdArray(raw.shopIds)))
      } catch {
        setSelectedOrgIds(new Set())
        setSelectedShopIds(new Set())
      }
    })()
  }, [modal.open, modal.row?.id])

  const openEdit = (row: RoleRow | null) => {
    setFormErrors({})
    setModal({ open: true, row })
    if (row) {
      setForm({
        roleName: String(row.roleName ?? ''),
        roleCode: String(row.roleCode ?? ''),
        dataScope: Number(row.dataScope ?? 4),
        remark: String(row.remark ?? ''),
      })
    } else {
      setForm({
        roleName: '',
        roleCode: '',
        dataScope: 4,
        remark: '',
      })
    }
  }

  const save = async () => {
    const isNew = modal.row?.id == null
    const errors = validateRoleForm(form, selectedOrgIds, selectedShopIds, isNew)
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) return

    const body: Record<string, unknown> = {
      roleName: form.roleName,
      roleCode: form.roleCode,
      dataScope: form.dataScope,
      remark: form.remark,
    }
    if (form.dataScope === 2) {
      body.orgIds = [...selectedOrgIds]
      body.shopIds = []
    } else if (form.dataScope === 6) {
      body.shopIds = [...selectedShopIds]
      body.orgIds = []
    } else {
      body.orgIds = []
      body.shopIds = []
    }
    try {
      if (modal.row?.id != null) {
        await roleApi.update(modal.row.id, body)
      } else {
        await roleApi.create(body)
      }
      setModal({ open: false, row: null })
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存失败')
    }
  }

  const remove = async (row: RoleRow) => {
    if (row.id == null) return
    const name = row.roleName ?? row.roleCode ?? String(row.id)
    const ok = await confirm({ title: '删除角色', message: `确定删除角色「${name}」？此操作不可撤销。`, danger: true })
    if (!ok) return
    try {
      await roleApi.remove(row.id)
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const openMenuAssign = async (row: RoleRow) => {
    if (row.id == null) return
    try {
      const tree = await menuApi.tree()
      const built = buildMenuTree(tree)
      setMenuTree(built)
      setMenuExpandedIds(new Set())

      // build parent/children maps for cascade selection
      const parent = new Map<number, number | null>()
      const children = new Map<number, number[]>()
      const walk = (nodes: MenuTreeLite[], pid: number | null) => {
        for (const n of nodes) {
          parent.set(n.id, pid)
          children.set(n.id, n.children.map((c) => c.id))
          walk(n.children, n.id)
        }
      }
      walk(built, null)
      setMenuParent(parent)
      setMenuChildren(children)

      const ids = await roleApi.getMenuIds(row.id)
      const arr = Array.isArray(ids) ? ids : []
      setCheckedMenus(new Set(arr))
      setMenuModal({ open: true, roleId: row.id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载菜单失败')
    }
  }

  const toggleExpandMenu = (id: number) => {
    setMenuExpandedIds((prev) => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }

  const collectDescendants = (id: number): number[] => {
    const out: number[] = []
    const stack = [...(menuChildren.get(id) ?? [])]
    while (stack.length) {
      const x = stack.pop()!
      out.push(x)
      const kids = menuChildren.get(x) ?? []
      for (const k of kids) stack.push(k)
    }
    return out
  }

  const syncAncestors = (set: Set<number>, startId: number) => {
    let cur = menuParent.get(startId) ?? null
    while (cur != null) {
      const kids = menuChildren.get(cur) ?? []
      if (kids.length === 0) {
        cur = menuParent.get(cur) ?? null
        continue
      }
      const allChecked = kids.every((k) => set.has(k))
      if (allChecked) set.add(cur)
      else set.delete(cur)
      cur = menuParent.get(cur) ?? null
    }
  }

  const toggleMenu = (id: number) => {
    setCheckedMenus((prev) => {
      const n = new Set(prev)
      const willCheck = !n.has(id)
      const descendants = collectDescendants(id)
      if (willCheck) {
        n.add(id)
        descendants.forEach((d) => n.add(d))
      } else {
        n.delete(id)
        descendants.forEach((d) => n.delete(d))
      }
      syncAncestors(n, id)
      return n
    })
  }

  const saveMenus = async () => {
    if (menuModal.roleId == null) return
    try {
      await roleApi.setMenus(menuModal.roleId, [...checkedMenus])
      setMenuModal({ open: false, roleId: null })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存菜单权限失败')
    }
  }

  const toggleOrgId = (id: number) => {
    setSelectedOrgIds((prev) => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }

  const toggleShopId = (id: number) => {
    setSelectedShopIds((prev) => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }

  const handleSearch = () => {
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  const handleReset = () => {
    setRoleNameQ('')
    setRoleCodeQ('')
    setStatusQ(undefined)
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">角色管理</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">系统全局视图</span>
          </div>
          <p className="mt-2 text-[11px] font-medium text-slate-400 pl-4">
            * 数据权限由后端 DataScope 拦截；此处配置可分配的权限边界。
          </p>
        </div>
        <PermGate perms={[SYSTEM_PERMS.role.add]}>
          <button
            type="button"
            onClick={() => openEdit(null)}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
          >
            + 新增角色模板
          </button>
        </PermGate>
      </div>

      {/* Search bar */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">角色名称</label>
          <input
            placeholder="搜索名称"
            value={roleNameQ}
            onChange={(e) => setRoleNameQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">角色编码</label>
          <input
            placeholder="搜索编码"
            value={roleCodeQ}
            onChange={(e) => setRoleCodeQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">状态</label>
          <select
            value={statusQ ?? ''}
            onChange={(e) =>
              setStatusQ(e.target.value === '' ? undefined : Number(e.target.value))
            }
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-28"
          >
            <option value="">全部</option>
            <option value="1">正常</option>
            <option value="0">停用</option>
          </select>
        </div>
        <PermGate perms={[SYSTEM_PERMS.role.query]}>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleSearch}
              className="px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold"
            >
              查询
            </button>
            <button
              type="button"
              onClick={handleReset}
              className="px-4 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-600"
            >
              重置
            </button>
          </div>
        </PermGate>
      </div>

      {error ? (
        <div className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">
          {error}
        </div>
      ) : null}

      {/* Table */}
      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black">
            <tr>
              <th className="px-6 py-3">名称</th>
              <th className="px-6 py-3">编码</th>
              <th className="px-6 py-3">数据范围</th>
              <th className="px-6 py-3">状态</th>
              <th className="px-6 py-3">备注</th>
              <th className="px-6 py-3 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : list.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-300">
                  暂无数据
                </td>
              </tr>
            ) : (
              list.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80">
                  <td className="px-6 py-3">{r.roleName}</td>
                  <td className="px-6 py-3 text-slate-500 font-mono text-xs">
                    {r.roleCode}
                  </td>
                  <td
                    className="px-6 py-3 max-w-xs truncate text-xs text-slate-700"
                    title={dataScopeLabel(Number(r.dataScope))}
                  >
                    {dataScopeLabel(Number(r.dataScope))}
                  </td>
                  <td className="px-6 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-0.5 text-[10px] font-black ${
                        Number(r.status) === 1
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {Number(r.status) === 1 ? '正常' : '停用'}
                    </span>
                  </td>
                  <td className="px-6 py-3 text-slate-400 text-xs max-w-[120px] truncate">
                    {r.remark || '—'}
                  </td>
                  <td className="px-6 py-3 text-right space-x-2">
                    <PermGate perms={[SYSTEM_PERMS.role.edit]}>
                      <button
                        type="button"
                        onClick={() => void openMenuAssign(r)}
                        className="text-indigo-600 text-xs font-bold"
                      >
                        菜单权限
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.role.edit]}>
                      <button
                        type="button"
                        onClick={() => openEdit(r)}
                        className="text-slate-700 text-xs font-bold"
                      >
                        编辑
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.role.remove]}>
                      <button
                        type="button"
                        onClick={() => void remove(r)}
                        className="text-rose-600 text-xs font-bold"
                      >
                        删除
                      </button>
                    </PermGate>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <div className="p-4 flex justify-between items-center border-t border-slate-50 text-xs text-slate-500">
          <span>共 {total} 条</span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={current <= 1}
              onClick={() => setCurrent((c) => Math.max(1, c - 1))}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={current * size >= total}
              onClick={() => setCurrent((c) => c + 1)}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      {/* Edit Modal */}
      <Modal
        open={modal.open}
        onClose={() => setModal({ open: false, row: null })}
        title={modal.row ? '编辑角色' : '新增角色'}
        maxWidth="max-w-2xl"
      >
            <label className="text-[10px] font-black text-slate-500 block">
              角色名称 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.roleName}
              onChange={(e) =>
                setForm((f) => ({ ...f, roleName: e.target.value }))
              }
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm ${
                formErrors.roleName ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.roleName && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.roleName}</p>
            )}

            <label className="text-[10px] font-black text-slate-500 block">
              角色编码 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.roleCode}
              onChange={(e) =>
                setForm((f) => ({ ...f, roleCode: e.target.value }))
              }
              disabled={!!modal.row}
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm disabled:opacity-60 ${
                formErrors.roleCode ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.roleCode && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.roleCode}</p>
            )}

            <label className="text-[10px] font-black text-slate-500 block">
              数据范围（DataScope）
            </label>
            <select
              value={form.dataScope}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  dataScope: Number(e.target.value),
                }))
              }
              className="w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold"
            >
              {DATA_SCOPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>

            {form.dataScope === 2 ? (
              <div className={`rounded-xl border p-3 space-y-2 ${
                formErrors.orgIds
                  ? 'border-rose-200 bg-rose-50/50'
                  : 'border-amber-100 bg-amber-50/50'
              }`}>
                <div className="text-[10px] font-black uppercase text-amber-800">
                  自定数据权限 — 选择可见部门 <span className="text-rose-500">*</span>
                </div>
                {formErrors.orgIds && (
                  <p className="text-[10px] text-rose-600 font-bold bg-rose-50 rounded-lg px-2 py-1">
                    {formErrors.orgIds}
                  </p>
                )}
                <div className="max-h-40 overflow-y-auto space-y-1">
                  {orgOptions.length === 0 ? (
                    <div className="text-xs text-slate-500">暂无机构树</div>
                  ) : (
                    orgOptions.map((o) => (
                      <label
                        key={o.id}
                        className="flex items-center gap-2 text-xs font-bold text-slate-800"
                      >
                        <input
                          type="checkbox"
                          checked={selectedOrgIds.has(o.id)}
                          onChange={() => toggleOrgId(o.id)}
                        />
                        {o.label}
                      </label>
                    ))
                  )}
                </div>
              </div>
            ) : null}

            {form.dataScope === 6 ? (
              <div className={`rounded-xl border p-3 space-y-2 ${
                formErrors.shopIds
                  ? 'border-rose-200 bg-rose-50/50'
                  : 'border-emerald-100 bg-emerald-50/50'
              }`}>
                <div className="text-[10px] font-black uppercase text-emerald-800">
                  本店数据权限 — 选择可见店铺 <span className="text-rose-500">*</span>
                </div>
                {formErrors.shopIds && (
                  <p className="text-[10px] text-rose-600 font-bold bg-rose-50 rounded-lg px-2 py-1">
                    {formErrors.shopIds}
                  </p>
                )}
                <div className="max-h-40 overflow-y-auto space-y-1">
                  {shopOptions.length === 0 ? (
                    <div className="text-xs text-slate-500">暂无店铺</div>
                  ) : (
                    shopOptions.map((o) => (
                      <label
                        key={o.id}
                        className="flex items-center gap-2 text-xs font-bold text-slate-800"
                      >
                        <input
                          type="checkbox"
                          checked={selectedShopIds.has(o.id)}
                          onChange={() => toggleShopId(o.id)}
                        />
                        {o.label}
                      </label>
                    ))
                  )}
                </div>
              </div>
            ) : null}

            <label className="text-[10px] font-black text-slate-500 block">
              备注
            </label>
            <input
              value={form.remark}
              onChange={(e) =>
                setForm((f) => ({ ...f, remark: e.target.value }))
              }
              className="w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setModal({ open: false, row: null })}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => void save()}
                className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
              >
                保存
              </button>
            </div>
      </Modal>

      {/* Menu Permission Modal */}
      <Modal
        open={menuModal.open}
        onClose={() => setMenuModal({ open: false, roleId: null })}
        title="菜单权限（menuIds）"
        maxWidth="max-w-2xl"
      >
            <p className="text-[10px] text-slate-500 mb-2">
              勾选后保存，将提交 <code className="text-indigo-600">PUT .../menus</code>{' '}
              与后端权限标识一致。
            </p>
            <div className="flex-1 overflow-y-auto border border-slate-100 rounded-xl p-2 max-h-[50vh]">
              {menuTree.length === 0 ? (
                <div className="text-xs text-slate-500 p-3">暂无菜单树</div>
              ) : (
                <MenuTreeView
                  nodes={menuTree}
                  expandedIds={menuExpandedIds}
                  checked={checkedMenus}
                  onToggleExpand={toggleExpandMenu}
                  onToggleCheck={toggleMenu}
                  depth={0}
                />
              )}
            </div>
            <div className="flex justify-end gap-2 pt-3">
              <button
                type="button"
                onClick={() => setMenuModal({ open: false, roleId: null })}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => void saveMenus()}
                className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
              >
                保存权限
              </button>
            </div>
      </Modal>
    </div>
  )
}
