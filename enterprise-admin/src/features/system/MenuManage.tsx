import { useCallback, useEffect, useState } from 'react'
import { menuApi } from '../../api/system-crud'
import type { MenuTreeNode, MenuForm } from '../../types/system-crud'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { useStaleGuard } from '../../hooks/useStaleGuard'

interface FlatMenuRow extends MenuTreeNode {
  depth: number
  hasChildren: boolean
}

function flattenTree(
  nodes: MenuTreeNode[],
  depth: number,
  expandedIds: Set<number>,
): FlatMenuRow[] {
  const out: FlatMenuRow[] = []
  for (const n of nodes) {
    const kids = n.children ?? []
    out.push({ ...n, depth, hasChildren: kids.length > 0 })
    if (kids.length > 0 && expandedIds.has(n.id)) {
      out.push(...flattenTree(kids, depth + 1, expandedIds))
    }
  }
  return out
}

/** Build a flat option list for parentId dropdown with indentation */
function flattenForSelect(
  nodes: MenuTreeNode[],
  depth = 0,
): { id: number; label: string }[] {
  const out: { id: number; label: string }[] = []
  for (const n of nodes) {
    const pad = '\u3000'.repeat(depth)
    out.push({ id: n.id, label: pad + n.menuName })
    if (n.children?.length) {
      out.push(...flattenForSelect(n.children, depth + 1))
    }
  }
  return out
}

function collectDescendantIds(nodes: MenuTreeNode[], targetId: number): Set<number> {
  const result = new Set<number>()
  const walk = (arr: MenuTreeNode[], inTargetBranch: boolean) => {
    for (const n of arr) {
      const hit = inTargetBranch || n.id === targetId
      if (hit) result.add(n.id)
      walk(n.children ?? [], hit)
    }
  }
  walk(nodes, false)
  return result
}

const MENU_TYPE_BADGE: Record<string, { label: string; cls: string }> = {
  M: { label: '目录', cls: 'bg-blue-50 text-blue-600' },
  C: { label: '菜单', cls: 'bg-emerald-50 text-emerald-600' },
  F: { label: '按钮', cls: 'bg-amber-50 text-amber-600' },
}

const emptyForm: MenuForm = {
  parentId: 0,
  menuName: '',
  menuType: 'M',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1,
}

export default function MenuManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()

  const [tree, setTree] = useState<MenuTreeNode[]>([])
  const [loading, setLoading] = useState(false)
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set())
  const [menuNameQ, setMenuNameQ] = useState('')

  const [modal, setModal] = useState<{ open: boolean; isEdit: boolean; id?: number }>({
    open: false,
    isEdit: false,
  })
  const [form, setForm] = useState<MenuForm>({ ...emptyForm })
  const [submitting, setSubmitting] = useState(false)
  const guard = useStaleGuard()

  // ================= Load =================

  const loadTree = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const data = await menuApi.tree()
      if (!guard.isCurrent(id)) return
      setTree(Array.isArray(data) ? data : [])
    } catch (e) {
      if (!guard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载菜单树失败')
    } finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [guard, toast])

  useEffect(() => {
    void loadTree()
  }, [loadTree])

  // ================= Expand / Collapse =================

  const toggleExpand = (id: number) => {
    setExpandedIds((prev) => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }

  const expandAll = () => {
    const ids = new Set<number>()
    const walk = (nodes: MenuTreeNode[]) => {
      for (const n of nodes) {
        if (n.children?.length) {
          ids.add(n.id)
          walk(n.children)
        }
      }
    }
    walk(tree)
    setExpandedIds(ids)
  }

  const collapseAll = () => setExpandedIds(new Set())

  // ================= CRUD =================

  const openAdd = (parentId = 0) => {
    setForm({ ...emptyForm, parentId })
    setModal({ open: true, isEdit: false })
  }

  const openEdit = async (node: MenuTreeNode) => {
    try {
      const detail = await menuApi.get(node.id)
      setForm({
        parentId: detail.parentId ?? 0,
        menuName: detail.menuName ?? '',
        menuType: detail.menuType ?? 'M',
        path: detail.path ?? '',
        component: detail.component ?? '',
        perms: detail.perms ?? '',
        icon: detail.icon ?? '',
        sort: Number(detail.sort ?? 0),
        visible: Number(detail.visible ?? 1),
        status: Number(detail.status ?? 1),
      })
      setModal({ open: true, isEdit: true, id: node.id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '获取菜单详情失败')
    }
  }

  const submitForm = async () => {
    if (!form.menuName.trim()) { toast.error('菜单名称不能为空'); return }
    if (submitting) return
    setSubmitting(true)
    try {
      if (modal.isEdit && modal.id != null) {
        await menuApi.update(modal.id, form)
      } else {
        await menuApi.create(form)
      }
      setModal({ open: false, isEdit: false })
      await loadTree()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存菜单失败')
    } finally {
      setSubmitting(false)
    }
  }

  const removeMenu = async (node: MenuTreeNode) => {
    const ok = await confirm({ title: '删除菜单', message: `确认删除菜单「${node.menuName}」吗？`, danger: true })
    if (!ok) return
    try {
      await menuApi.remove(node.id)
      await loadTree()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  // ================= Derived =================

  const flatRows = flattenTree(tree, 0, expandedIds)
  const keyword = menuNameQ.trim().toLowerCase()
  const filteredRows = (() => {
    if (!keyword) return flatRows
    const matchExpanded = new Set<number>()
    const walk = (nodes: MenuTreeNode, ancestors: number[]): void => {
      const hit = String(nodes.menuName ?? '').toLowerCase().includes(keyword)
      if (hit) {
        ancestors.forEach((id) => matchExpanded.add(id))
      }
      ;(nodes.children ?? []).forEach((child) => walk(child, [...ancestors, nodes.id]))
    }
    tree.forEach((n) => walk(n, []))
    const merged = new Set([...expandedIds, ...matchExpanded])
    return flattenTree(tree, 0, merged).filter((r) => {
      if (String(r.menuName ?? '').toLowerCase().includes(keyword)) return true
      return matchExpanded.has(r.id)
    })
  })()
  const forbiddenParentIds =
    modal.isEdit && modal.id != null ? collectDescendantIds(tree, modal.id) : new Set<number>()
  const parentOptions = flattenForSelect(tree).filter((o) => !forbiddenParentIds.has(o.id))

  if (!can(SYSTEM_PERMS.menu.query)) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：菜单管理
      </div>
    )
  }

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">菜单管理</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">系统全局视图</span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={expandAll}
            className="px-3 py-2 bg-slate-100 text-slate-600 rounded-xl text-xs font-bold hover:bg-slate-200 transition"
          >
            全部展开
          </button>
          <button
            type="button"
            onClick={collapseAll}
            className="px-3 py-2 bg-slate-100 text-slate-600 rounded-xl text-xs font-bold hover:bg-slate-200 transition"
          >
            全部折叠
          </button>
          <PermGate perms={[SYSTEM_PERMS.menu.add]}>
            <button
              type="button"
              onClick={() => openAdd(0)}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
            >
              + 新增菜单
            </button>
          </PermGate>
        </div>
      </div>

      {/* Search */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">菜单名称</label>
          <input
            placeholder="搜索菜单名称"
            value={menuNameQ}
            onChange={(e) => setMenuNameQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-56 font-bold focus:ring-indigo-500"
          />
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setMenuNameQ('')}
            className="px-5 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-200 transition"
          >
            清空
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
            <tr>
              <th className="px-8 py-5">菜单名称</th>
              <th className="px-8 py-5">图标</th>
              <th className="px-8 py-5">类型</th>
              <th className="px-8 py-5">路由路径</th>
              <th className="px-8 py-5">权限标识</th>
              <th className="px-8 py-5">排序</th>
              <th className="px-8 py-5">可见</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr><td colSpan={9} className="px-8 py-10 text-center text-slate-400 italic font-bold">数据读取中...</td></tr>
            ) : filteredRows.length === 0 ? (
              <tr><td colSpan={9} className="px-8 py-10 text-center text-slate-400 italic">暂无菜单数据</td></tr>
            ) : (
              filteredRows.map((row) => {
                const badge = MENU_TYPE_BADGE[row.menuType] ?? { label: row.menuType, cls: 'bg-slate-100 text-slate-500' }
                return (
                  <tr key={row.id} className="hover:bg-slate-50/50 transition-colors group">
                    <td className="px-8 py-5 text-slate-900 whitespace-nowrap">
                      <div className="flex items-center" style={{ paddingLeft: `${row.depth * 1.5}rem` }}>
                        {row.hasChildren ? (
                          <button
                            type="button"
                            onClick={() => toggleExpand(row.id)}
                            className="w-5 h-5 flex items-center justify-center text-slate-400 hover:text-indigo-600 mr-1 transition"
                          >
                            {expandedIds.has(row.id) ? '▾' : '▸'}
                          </button>
                        ) : (
                          <span className="w-5 mr-1" />
                        )}
                        {row.menuName}
                      </div>
                    </td>
                    <td className="px-8 py-5 text-slate-400 text-xs">{row.icon || '—'}</td>
                    <td className="px-8 py-5">
                      <span className={`px-2 py-1 rounded-lg text-xs font-bold ${badge.cls}`}>
                        {badge.label}
                      </span>
                    </td>
                    <td className="px-8 py-5 text-slate-500 font-mono text-xs">{row.path || '—'}</td>
                    <td className="px-8 py-5 text-indigo-600 font-mono text-xs">{row.perms || '—'}</td>
                    <td className="px-8 py-5 text-slate-500 font-normal">{row.sort ?? 0}</td>
                    <td className="px-8 py-5">
                      <span className={`px-2 py-1 rounded-lg text-xs ${Number(row.visible) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>
                        {Number(row.visible) === 1 ? '显示' : '隐藏'}
                      </span>
                    </td>
                    <td className="px-8 py-5">
                      <span className={`px-2 py-1 rounded-lg text-xs ${Number(row.status) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>
                        {Number(row.status) === 1 ? '正常' : '停用'}
                      </span>
                    </td>
                    <td className="px-8 py-5 text-right space-x-3 opacity-0 group-hover:opacity-100 transition-opacity">
                      <PermGate perms={[SYSTEM_PERMS.menu.edit]}>
                        <button onClick={() => void openEdit(row)} className="text-amber-600 hover:text-amber-700 text-xs font-bold">编辑</button>
                      </PermGate>
                      <PermGate perms={[SYSTEM_PERMS.menu.add]}>
                        <button onClick={() => openAdd(row.id)} className="text-indigo-600 hover:text-indigo-700 text-xs font-bold">新增子级</button>
                      </PermGate>
                      <PermGate perms={[SYSTEM_PERMS.menu.remove]}>
                        <button onClick={() => void removeMenu(row)} className="text-rose-600 hover:text-rose-700 text-xs font-bold">删除</button>
                      </PermGate>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {/* ================= Modal ================= */}
      <Modal open={modal.open} onClose={() => setModal({ open: false, isEdit: false })} title={modal.isEdit ? '编辑菜单' : '新增菜单'} maxWidth="md">
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">上级菜单</label>
            <select
              value={form.parentId}
              onChange={(e) => setForm({ ...form, parentId: Number(e.target.value) })}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            >
              <option value={0}>顶级菜单（无上级）</option>
              {parentOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.label}</option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">菜单名称 <span className="text-rose-500">*</span></label>
              <input
                autoFocus
                value={form.menuName}
                onChange={(e) => setForm({ ...form, menuName: e.target.value })}
                placeholder="例如：系统管理"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">菜单类型</label>
              <select
                value={form.menuType}
                onChange={(e) => setForm({ ...form, menuType: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              >
                <option value="M">M - 目录</option>
                <option value="C">C - 菜单</option>
                <option value="F">F - 按钮</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">路由路径</label>
              <input
                value={form.path ?? ''}
                onChange={(e) => setForm({ ...form, path: e.target.value })}
                placeholder="例如：/system/user"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">图标</label>
              <input
                value={form.icon ?? ''}
                onChange={(e) => setForm({ ...form, icon: e.target.value })}
                placeholder="例如：Setting"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
          </div>
          {form.menuType === 'C' && (
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">组件路径</label>
              <input
                value={form.component ?? ''}
                onChange={(e) => setForm({ ...form, component: e.target.value })}
                placeholder="例如：system/user/index"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
          )}
          {(form.menuType === 'C' || form.menuType === 'F') && (
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">权限标识</label>
              <input
                value={form.perms ?? ''}
                onChange={(e) => setForm({ ...form, perms: e.target.value })}
                placeholder="例如：system:user:query"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
          )}
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">排序号</label>
              <input
                type="number"
                value={form.sort ?? 0}
                onChange={(e) => setForm({ ...form, sort: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">可见</label>
              <select
                value={form.visible ?? 1}
                onChange={(e) => setForm({ ...form, visible: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              >
                <option value={1}>显示</option>
                <option value={0}>隐藏</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">状态</label>
              <select
                value={form.status ?? 1}
                onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              >
                <option value={1}>正常</option>
                <option value={0}>停用</option>
              </select>
            </div>
          </div>
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button onClick={() => setModal({ open: false, isEdit: false })} className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-700 text-sm font-black hover:bg-slate-200 transition">取消</button>
          <button disabled={submitting} onClick={() => void submitForm()} className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-black shadow-md hover:bg-indigo-700 transition">{submitting ? '保存中...' : '确定提交'}</button>
        </div>
      </Modal>
    </div>
  )
}
