import { useCallback, useEffect, useState } from 'react'
import { orgApi } from '../../api/system-crud'
import type { OrgNode, OrgCreateRequest } from '../../types/system-crud'
import { PermGate } from '../../context/PermissionsContext'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'

interface FlatOrgRow extends OrgNode {
  depth: number
  hasChildren: boolean
}

function flattenTree(
  nodes: OrgNode[],
  depth: number,
  expandedIds: Set<number>,
): FlatOrgRow[] {
  const out: FlatOrgRow[] = []
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
  nodes: OrgNode[],
  depth = 0,
): { id: number; label: string }[] {
  const out: { id: number; label: string }[] = []
  for (const n of nodes) {
    const pad = '\u3000'.repeat(depth)
    out.push({ id: n.id, label: pad + n.orgName })
    if (n.children?.length) {
      out.push(...flattenForSelect(n.children, depth + 1))
    }
  }
  return out
}

interface OrgFormState {
  parentId: number
  orgCode: string
  orgName: string
  orgType: number
  sort: number
  status: number
}

const emptyForm: OrgFormState = {
  parentId: 0,
  orgCode: '',
  orgName: '',
  orgType: 0,
  sort: 0,
  status: 1,
}

export default function OrgManage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [tree, setTree] = useState<OrgNode[]>([])
  const [loading, setLoading] = useState(false)
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set())
  const [orgNameQ, setOrgNameQ] = useState('')

  const [modal, setModal] = useState<{ open: boolean; isEdit: boolean; id?: number }>({
    open: false,
    isEdit: false,
  })
  const [form, setForm] = useState<OrgFormState>({ ...emptyForm })
  const [submitting, setSubmitting] = useState(false)

  // ================= Load =================

  const loadTree = useCallback(async () => {
    setLoading(true)
    try {
      const data = await orgApi.tree()
      setTree(Array.isArray(data) ? data : [])
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载组织树失败')
    } finally {
      setLoading(false)
    }
  }, [])

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
    const walk = (nodes: OrgNode[]) => {
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

  const openEdit = (node: OrgNode) => {
    setForm({
      parentId: node.parentId ?? 0,
      orgCode: node.orgCode ?? '',
      orgName: node.orgName ?? '',
      orgType: Number(node.orgType ?? 0),
      sort: Number(node.sort ?? 0),
      status: Number(node.status ?? 1),
    })
    setModal({ open: true, isEdit: true, id: node.id })
  }

  const submitForm = async () => {
    if (!form.orgName.trim()) { toast.error('组织名称不能为空'); return }
    if (!form.orgCode.trim()) { toast.error('组织编码不能为空'); return }
    setSubmitting(true)
    try {
      if (modal.isEdit && modal.id != null) {
        await orgApi.update({ id: modal.id, ...form })
      } else {
        const body: OrgCreateRequest = {
          parentId: form.parentId,
          orgCode: form.orgCode,
          orgName: form.orgName,
          orgType: form.orgType,
          sort: form.sort,
          status: form.status,
        }
        await orgApi.create(body)
      }
      setModal({ open: false, isEdit: false })
      await loadTree()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存组织失败')
    } finally {
      setSubmitting(false)
    }
  }

  const removeOrg = async (node: OrgNode) => {
    const ok = await confirm({ title: '删除组织', message: `确认删除组织「${node.orgName}」吗？`, danger: true })
    if (!ok) return
    try {
      await orgApi.remove(node.id)
      await loadTree()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  // ================= Derived =================

  const flatRows = flattenTree(tree, 0, expandedIds)
  const filteredRows = orgNameQ.trim()
    ? flatRows.filter((r) =>
        String(r.orgName ?? '')
          .toLowerCase()
          .includes(orgNameQ.trim().toLowerCase()),
      )
    : flatRows
  const parentOptions = flattenForSelect(tree)

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">组织管理</h4>
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
          <PermGate perms={[SYSTEM_PERMS.org.add]}>
            <button
              type="button"
              onClick={() => openAdd(0)}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
            >
              + 新增组织
            </button>
          </PermGate>
        </div>
      </div>

      {/* Search */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">组织名称</label>
          <input
            placeholder="搜索组织名称"
            value={orgNameQ}
            onChange={(e) => setOrgNameQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-56 font-bold focus:ring-indigo-500"
          />
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setOrgNameQ('')}
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
              <th className="px-8 py-5">组织名称</th>
              <th className="px-8 py-5">组织编码</th>
              <th className="px-8 py-5">组织类型</th>
              <th className="px-8 py-5">用户数</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr><td colSpan={6} className="px-8 py-10 text-center text-slate-400 italic font-bold">数据读取中...</td></tr>
            ) : filteredRows.length === 0 ? (
              <tr><td colSpan={6} className="px-8 py-10 text-center text-slate-400 italic">暂无组织数据</td></tr>
            ) : (
              filteredRows.map((row) => (
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
                      {row.orgName}
                    </div>
                  </td>
                  <td className="px-8 py-5 text-indigo-600 font-mono text-xs">{row.orgCode}</td>
                  <td className="px-8 py-5 text-slate-500 font-normal">{row.orgType ?? '—'}</td>
                  <td className="px-8 py-5 text-slate-500 font-normal">{row.userCount ?? 0}</td>
                  <td className="px-8 py-5">
                    <span className={`px-2 py-1 rounded-lg text-xs ${Number(row.status) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>
                      {Number(row.status) === 1 ? '正常' : '停用'}
                    </span>
                  </td>
                  <td className="px-8 py-5 text-right space-x-3 opacity-0 group-hover:opacity-100 transition-opacity">
                    <PermGate perms={[SYSTEM_PERMS.org.edit]}>
                      <button onClick={() => openEdit(row)} className="text-amber-600 hover:text-amber-700 text-xs font-bold">编辑</button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.org.add]}>
                      <button onClick={() => openAdd(row.id)} className="text-indigo-600 hover:text-indigo-700 text-xs font-bold">新增子级</button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.org.remove]}>
                      <button onClick={() => void removeOrg(row)} className="text-rose-600 hover:text-rose-700 text-xs font-bold">删除</button>
                    </PermGate>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ================= Modal ================= */}
      <Modal open={modal.open} onClose={() => setModal({ open: false, isEdit: false })} title={modal.isEdit ? '编辑组织' : '新增组织'} maxWidth="md">
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">上级组织</label>
            <select
              value={form.parentId}
              onChange={(e) => setForm({ ...form, parentId: Number(e.target.value) })}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            >
              <option value={0}>顶级组织（无上级）</option>
              {parentOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.label}</option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">组织名称 <span className="text-rose-500">*</span></label>
              <input
                autoFocus
                value={form.orgName}
                onChange={(e) => setForm({ ...form, orgName: e.target.value })}
                placeholder="例如：技术部"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">组织编码 <span className="text-rose-500">*</span></label>
              <input
                value={form.orgCode}
                onChange={(e) => setForm({ ...form, orgCode: e.target.value })}
                placeholder="例如：TECH"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">组织类型</label>
              <input
                type="number"
                value={form.orgType}
                onChange={(e) => setForm({ ...form, orgType: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">排序号</label>
              <input
                type="number"
                value={form.sort}
                onChange={(e) => setForm({ ...form, sort: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">状态</label>
              <select
                value={form.status}
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
