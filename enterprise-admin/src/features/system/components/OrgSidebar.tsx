import { useCallback, useEffect, useState } from 'react'
import { orgApi } from '../../../api/system-crud'
import { useConfirm } from '../../../components/ConfirmDialog'
import Modal from '../../../components/Modal'
import { useToast } from '../../../components/Toast'
import { PermGate } from '../../../context/PermissionsContext'
import { flattenOrgTree } from '../../../lib/org-flat'
import { SYSTEM_PERMS } from '../../../lib/system-perms'
import type { OrgNode, OrgCreateRequest } from '../../../types/system-crud'

const ORG_TYPE_MAP: Record<number, string> = {
  1: '全资总部',
  2: '区域分公司',
  3: '直属部门',
}

function getOrgTypeLabel(type?: number) {
  return ORG_TYPE_MAP[type ?? 3] ?? '未知类型'
}

const OrgTreeNode = ({
  node,
  selectedId,
  onSelect,
  onAddSub,
  onEdit,
  onRemove,
  level = 0,
}: {
  node: OrgNode
  selectedId?: number
  onSelect: (id: number) => void
  onAddSub: (id: number) => void
  onEdit: (node: OrgNode) => void
  onRemove: (node: OrgNode) => void
  level?: number
}) => {
  const isSelected = selectedId === node.id
  const [expanded, setExpanded] = useState(true)

  return (
    <div className="select-none">
      <div
        className={`group flex items-center justify-between p-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
          isSelected
            ? 'bg-indigo-50 text-indigo-600 shadow-sm ring-1 ring-indigo-200'
            : 'text-slate-600 hover:bg-slate-50'
        }`}
        style={{ marginLeft: level > 0 ? `${level * 1.2}rem` : 0 }}
        onClick={(e) => {
          e.stopPropagation()
          onSelect(node.id)
        }}
      >
        <div className="flex items-center gap-2 truncate">
          {/* 折叠箭头占位 */}
          <div
            className="w-4 h-4 flex items-center justify-center cursor-pointer text-slate-400 hover:text-slate-700"
            onClick={(e) => {
              e.stopPropagation()
              setExpanded(!expanded)
            }}
          >
            {node.children && node.children.length > 0 ? (
              expanded ? '▼' : '▶'
            ) : (
              <span className="opacity-0">●</span>
            )}
          </div>
          <span className="truncate">{node.orgName}</span>
          {node.orgType && (
            <span className="hidden opacity-50 px-1 bg-slate-200 text-[9px] rounded-md xl:inline-block">
              {getOrgTypeLabel(node.orgType)}
            </span>
          )}
        </div>
        
        {/* 操作区 (hover 显示) */}
        <div className="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1 bg-white/50 backdrop-blur-sm rounded-lg px-1">
          <PermGate perms={[SYSTEM_PERMS.org.add]}>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onAddSub(node.id)
              }}
              className="px-1.5 py-1 text-indigo-600 hover:bg-indigo-100 rounded-md transition"
              title="新增下级"
            >
              +
            </button>
          </PermGate>
          <PermGate perms={[SYSTEM_PERMS.org.edit]}>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onEdit(node)
              }}
              className="px-1.5 py-1 text-amber-600 hover:bg-amber-100 rounded-md transition"
              title="编辑机构"
            >
              ✎
            </button>
          </PermGate>
          <PermGate perms={[SYSTEM_PERMS.org.remove]}>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onRemove(node)
              }}
              className="px-1.5 py-1 text-rose-600 hover:bg-rose-100 rounded-md transition"
              title="删除机构"
            >
              ×
            </button>
          </PermGate>
        </div>
      </div>
      
      {/* 渲染子节点 */}
      {expanded && node.children && node.children.length > 0 && (
        <div className="mt-1 relative before:absolute before:left-3 before:top-0 before:bottom-2 before:w-[1px] border-l border-slate-100 ml-[0.35rem]">
          {node.children.map((child) => (
            <OrgTreeNode
              key={child.id}
              node={child}
              selectedId={selectedId}
              onSelect={onSelect}
              onAddSub={onAddSub}
              onEdit={onEdit}
              onRemove={onRemove}
              level={level + 1}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export default function OrgSidebar({
  selectedOrgId,
  onSelectOrg,
  onOrgOptionsLoaded,
}: {
  selectedOrgId?: number
  onSelectOrg: (id?: number) => void
  onOrgOptionsLoaded?: (options: { id: number; label: string }[]) => void
}) {
  const toast = useToast()
  const confirm = useConfirm()

  const [orgTree, setOrgTree] = useState<OrgNode[]>([])
  const [orgModal, setOrgModal] = useState<{ open: boolean; isEdit: boolean; id?: number }>({
    open: false,
    isEdit: false,
  })
  const [orgForm, setOrgForm] = useState<Omit<OrgCreateRequest, 'parentId'> & { parentId: number }>({
    parentId: 0,
    orgCode: '',
    orgName: '',
    orgType: 3,
    sort: 1,
    status: 1,
  })
  const [submitting, setSubmitting] = useState(false)

  const loadOrgTree = useCallback(async () => {
    try {
      const tree = await orgApi.tree()
      const rawTree = Array.isArray(tree) ? tree : []
      setOrgTree(rawTree)
      if (onOrgOptionsLoaded) {
        onOrgOptionsLoaded(flattenOrgTree(rawTree))
      }
    } catch (e) {
      setOrgTree([])
    }
  }, [onOrgOptionsLoaded])

  useEffect(() => {
    void loadOrgTree()
  }, [loadOrgTree])

  const openAddOrg = (parentId: number = 0) => {
    setOrgForm({
      parentId,
      orgCode: '',
      orgName: '',
      orgType: 3,
      sort: 1,
      status: 1,
    })
    setOrgModal({ open: true, isEdit: false })
  }

  const openEditOrg = (node: OrgNode) => {
    setOrgForm({
      parentId: node.parentId,
      orgCode: node.orgCode || '',
      orgName: node.orgName || '',
      orgType: node.orgType ?? 3,
      sort: node.sort ?? 1,
      status: node.status ?? 1,
    })
    setOrgModal({ open: true, isEdit: true, id: node.id })
  }

  const removeOrg = async (node: OrgNode) => {
    const ok = await confirm({
      title: '删除组织',
      message: `确定删除组织「${node.orgName}」吗？操作不可恢复。`,
      danger: true,
      confirmText: '删除',
    })
    if (!ok) return
    try {
      await orgApi.remove(node.id)
      if (selectedOrgId === node.id) {
        onSelectOrg(undefined)
      }
      toast.success(`已删除组织「${node.orgName}」`)
      await loadOrgTree()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除组织失败')
    }
  }

  const submitOrgForm = async () => {
    if (!orgForm.orgName.trim() || !orgForm.orgCode.trim()) {
      toast.error('组织名称和编码必填')
      return
    }
    setSubmitting(true)
    try {
      if (orgModal.isEdit && orgModal.id != null) {
        await orgApi.update({ id: orgModal.id, ...orgForm })
        toast.success('组织节点已更新')
      } else {
        await orgApi.create(orgForm)
        toast.success('组织节点已创建')
      }
      setOrgModal({ open: false, isEdit: false })
      await loadOrgTree()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div className="w-80 shrink-0 bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 flex flex-col gap-4 max-h-[85vh] overflow-y-auto">
        <div className="flex items-center justify-between">
          <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">组织机构管理</h4>
          <PermGate perms={[SYSTEM_PERMS.org.add]}>
            <button
              onClick={() => openAddOrg(0)}
              className="text-xs font-bold text-indigo-600 bg-indigo-50 px-3 py-1.5 rounded-lg hover:bg-indigo-100 transition"
            >
              + 新增根
            </button>
          </PermGate>
        </div>
        
        <div className="space-y-1 mt-2 border-t border-slate-50 pt-2">
          <div 
            onClick={() => onSelectOrg(undefined)}
            className={`p-3 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              selectedOrgId === undefined 
                ? 'bg-indigo-50 text-indigo-600 shadow-sm ring-1 ring-indigo-200' 
                : 'text-slate-600 hover:bg-slate-50'
            }`}
          >
             🏢 全部组织 / 跨区域视图
          </div>
          
          <div className="pt-2">
             {orgTree.length === 0 ? (
                <div className="text-center text-xs text-slate-400 py-6">暂无组织数据</div>
             ) : (
                orgTree.map(rootNode => (
                  <OrgTreeNode 
                    key={rootNode.id} 
                    node={rootNode} 
                    selectedId={selectedOrgId}
                    onSelect={onSelectOrg}
                    onAddSub={openAddOrg}
                    onEdit={openEditOrg}
                    onRemove={removeOrg}
                  />
                ))
             )}
          </div>
        </div>
      </div>

      {/* 组织架构新建/编辑弹窗 */}
      <Modal
        open={orgModal.open}
        onClose={() => setOrgModal({ open: false, isEdit: false })}
        title={orgModal.isEdit ? '编辑组织节点' : '新增组织节点'}
      >
        <div className="space-y-4 pt-2">
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">上级节点 ID</label>
            <div className="px-4 py-2.5 rounded-xl bg-slate-100 text-slate-500 text-sm font-bold opacity-70">
              {orgForm.parentId === 0 ? '全局根节点 (Parent ID: 0)' : `父级 ID: ${orgForm.parentId}`}
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">组织名称 <span className="text-rose-500">*</span></label>
            <input
              autoFocus
              value={orgForm.orgName}
              onChange={(e) => setOrgForm({ ...orgForm, orgName: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              placeholder="例如：西南大区分部"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">组织统一编码 (orgCode) <span className="text-rose-500">*</span></label>
            <input
              value={orgForm.orgCode}
              onChange={(e) => setOrgForm({ ...orgForm, orgCode: e.target.value })}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              placeholder="例如：SW-REGION"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">组织类型</label>
            <select
              value={orgForm.orgType}
              onChange={(e) => setOrgForm({ ...orgForm, orgType: Number(e.target.value) })}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            >
              {Object.entries(ORG_TYPE_MAP).map(([val, label]) => (
                <option key={val} value={Number(val)}>{label}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-4">
          <button
            type="button"
            onClick={() => setOrgModal({ open: false, isEdit: false })}
            disabled={submitting}
            className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-700 text-sm font-black hover:bg-slate-200 transition"
          >
            取消
          </button>
          <button
            type="button"
            onClick={() => void submitOrgForm()}
            disabled={submitting}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-black shadow-md hover:bg-indigo-700 transition"
          >
            {submitting ? '保存中...' : '提交'}
          </button>
        </div>
      </Modal>
    </>
  )
}
