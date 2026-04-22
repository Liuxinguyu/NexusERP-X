import { useCallback, useEffect, useState } from 'react'
import { dictItemApi, dictTypeApi } from '../../api/system-crud'
import type { DictItemRow, DictTypeRow } from '../../types/system-crud'
import { PermGate } from '../../context/PermissionsContext'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'

export default function DictManage() {
  const toast = useToast()
  const confirm = useConfirm()
  // === 左侧：字典类型 Context ===
  const [types, setTypes] = useState<DictTypeRow[]>([])
  const [typesLoading, setTypesLoading] = useState(false)
  const [selectedType, setSelectedType] = useState<string | undefined>(undefined)
  const [typeSearch, setTypeSearch] = useState('')

  const [typeModal, setTypeModal] = useState<{ open: boolean; isEdit: boolean; id?: number }>({
    open: false,
    isEdit: false,
  })
  const [typeForm, setTypeForm] = useState<{
    dictName: string
    dictType: string
    status: number
    remark?: string
  }>({
    dictName: '',
    dictType: '',
    status: 1,
    remark: '',
  })
  const [typeSubmitting, setTypeSubmitting] = useState(false)

  // === 右侧：字典项 Context ===
  const [items, setItems] = useState<DictItemRow[]>([])
  const [itemsLoading, setItemsLoading] = useState(false)

  const [itemModal, setItemModal] = useState<{ open: boolean; isEdit: boolean; id?: number }>({
    open: false,
    isEdit: false,
  })
  const [itemForm, setItemForm] = useState<{
    dictType: string
    dictLabel: string
    dictValue: string
    sort: number
    status: number
    remark?: string
  }>({
    dictType: '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: 1,
    remark: '',
  })
  const [itemSubmitting, setItemSubmitting] = useState(false)

  // ================= 字典类型 CRUD =================

  const loadTypes = useCallback(async () => {
    setTypesLoading(true)
    try {
      const data = await dictTypeApi.list()
      setTypes(Array.isArray(data) ? data : [])
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载字典类型失败')
    } finally {
      setTypesLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadTypes()
  }, [loadTypes])

  const loadItems = useCallback(async (typeDictType: string) => {
    setItemsLoading(true)
    try {
      const data = await dictItemApi.listByType(typeDictType)
      setItems(Array.isArray(data) ? data : [])
    } catch (e) {
      setItems([])
    } finally {
      setItemsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (selectedType) {
      void loadItems(selectedType)
    } else {
      setItems([])
    }
  }, [selectedType, loadItems])

  const openAddType = () => {
    setTypeForm({ dictName: '', dictType: '', status: 1, remark: '' })
    setTypeModal({ open: true, isEdit: false })
  }

  const openEditType = async (node: DictTypeRow) => {
    if (!node.id) return
    try {
      const detail = await dictTypeApi.get(node.id)
      setTypeForm({
        dictName: detail.dictName ?? '',
        dictType: detail.dictType ?? '',
        status: Number(detail.status ?? 1),
        remark: detail.remark ?? '',
      })
      setTypeModal({ open: true, isEdit: true, id: node.id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '获取详情失败')
    }
  }

  const submitTypeForm = async () => {
    if (!typeForm.dictName.trim() || !typeForm.dictType.trim()) { toast.error('字典名称和类型必填'); return }
    setTypeSubmitting(true)
    try {
      if (typeModal.isEdit && typeModal.id != null) {
        await dictTypeApi.update(typeModal.id, typeForm)
      } else {
        await dictTypeApi.create(typeForm)
      }
      setTypeModal({ open: false, isEdit: false })
      await loadTypes()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存字典类型失败')
    } finally {
      setTypeSubmitting(false)
    }
  }

  const removeType = async (node: DictTypeRow) => {
    if (!node.id) return
    const ok = await confirm({ title: '删除字典类型', message: `确认删除字典类型「${node.dictName}」吗？`, danger: true })
    if (!ok) return
    try {
      await dictTypeApi.remove(node.id)
      if (selectedType === node.dictType) setSelectedType(undefined)
      await loadTypes()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  // ================= 字典项 CRUD =================

  const openAddItem = () => {
    if (!selectedType) { toast.error('请先在左侧选择一个字典类型'); return }
    setItemForm({ dictType: selectedType, dictLabel: '', dictValue: '', sort: 0, status: 1, remark: '' })
    setItemModal({ open: true, isEdit: false })
  }

  const openEditItem = async (row: DictItemRow) => {
    if (!row.id) return
    try {
      const detail = await dictItemApi.get(row.id)
      setItemForm({
        dictType: detail.dictType ?? selectedType ?? '',
        dictLabel: detail.dictLabel ?? '',
        dictValue: detail.dictValue ?? '',
        sort: Number(detail.sort ?? 0),
        status: Number(detail.status ?? 1),
        remark: detail.remark ?? '',
      })
      setItemModal({ open: true, isEdit: true, id: row.id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '获取字典项详情失败')
    }
  }

  const submitItemForm = async () => {
    if (!itemForm.dictLabel.trim() || !itemForm.dictValue.trim() || !itemForm.dictType.trim()) {
      toast.error('字典类型、标签、键值均为必填'); return
    }
    setItemSubmitting(true)
    try {
      if (itemModal.isEdit && itemModal.id != null) {
        await dictItemApi.update(itemModal.id, itemForm)
      } else {
        await dictItemApi.create(itemForm)
      }
      setItemModal({ open: false, isEdit: false })
      if (selectedType) await loadItems(selectedType)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存字典项失败')
    } finally {
      setItemSubmitting(false)
    }
  }

  const removeItem = async (row: DictItemRow) => {
    if (!row.id) return
    const ok = await confirm({ title: '删除字典项', message: `确认删除字典项「${row.dictLabel}」吗？`, danger: true })
    if (!ok) return
    try {
      await dictItemApi.remove(row.id)
      if (selectedType) await loadItems(selectedType)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <div className="flex gap-6 animate-in fade-in duration-500">
      
      {/* ================= 左侧：字典类型区域 ================= */}
      <div className="w-80 shrink-0 bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 flex flex-col gap-4 max-h-[85vh] overflow-y-auto">
        <div className="flex items-center justify-between">
          <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">字典分类</h4>
          <PermGate perms={[SYSTEM_PERMS.dict.add]}>
            <button
              onClick={openAddType}
              className="text-xs font-bold text-indigo-600 bg-indigo-50 px-3 py-1.5 rounded-lg hover:bg-indigo-100 transition"
            >
              + 新增种类
            </button>
          </PermGate>
        </div>

        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">搜索</label>
          <input
            placeholder="按名称/编码过滤"
            value={typeSearch}
            onChange={(e) => setTypeSearch(e.target.value)}
            className="w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500"
          />
        </div>
        
        <div className="space-y-2 mt-2 border-t border-slate-50 pt-4">
          {typesLoading ? (
             <div className="text-center text-xs text-slate-400 py-6">加载字典类型中...</div>
          ) : types.length === 0 ? (
             <div className="text-center text-xs text-slate-400 py-6">暂无字典类型</div>
          ) : (
            types
              .filter((t) => {
                const q = typeSearch.trim().toLowerCase()
                if (!q) return true
                return (
                  String(t.dictName ?? '').toLowerCase().includes(q) ||
                  String(t.dictType ?? '').toLowerCase().includes(q)
                )
              })
              .map(t => {
              const isSelected = selectedType === t.dictType
              return (
                <div 
                  key={t.id}
                  onClick={() => setSelectedType(t.dictType)}
                  className={`group flex items-center justify-between p-3 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                    isSelected 
                      ? 'bg-indigo-50 text-indigo-600 shadow-sm ring-1 ring-indigo-200' 
                      : 'text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex flex-col gap-0.5 truncate">
                    <span className="truncate">{t.dictName}</span>
                    <span className="text-[9px] font-medium opacity-60 truncate">{t.dictType}</span>
                  </div>
                  
                  {/* 类型 Hover 操作悬浮条 */}
                  <div className="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1 bg-white/50 backdrop-blur-sm rounded-lg px-1 shrink-0">
                    <PermGate perms={[SYSTEM_PERMS.dict.edit]}>
                      <button
                        onClick={(e) => { e.stopPropagation(); openEditType(t); }}
                        className="px-1.5 py-1 text-amber-600 hover:bg-amber-100 rounded-md transition"
                        title="编辑类型"
                      >
                        ✎
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.dict.remove]}>
                      <button
                        onClick={(e) => { e.stopPropagation(); removeType(t); }}
                        className="px-1.5 py-1 text-rose-600 hover:bg-rose-100 rounded-md transition"
                        title="删除类型"
                      >
                        ×
                      </button>
                    </PermGate>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>

      {/* ================= 右侧：字典项区域 ================= */}
      <div className="flex-1 min-w-0 space-y-4">
        {/* Header 面板 */}
        <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
          <div>
            <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
            <div className="flex items-center gap-3">
              <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">
                {selectedType ? `[${selectedType}] 元素数据字典` : '请选择字典类型'}
              </h4>
              <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">标准元数据层</span>
            </div>
          </div>
          <PermGate perms={[SYSTEM_PERMS.dict.add]}>
            <button
              type="button"
              disabled={!selectedType}
              onClick={openAddItem}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
                selectedType 
                  ? 'bg-indigo-600 hover:bg-indigo-700 text-white' 
                  : 'bg-slate-100 text-slate-400 cursor-not-allowed'
              }`}
            >
              + 挂靠新增字典项
            </button>
          </PermGate>
        </div>

        {/* 字典项表格 */}
        <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
          <table className="w-full text-left text-sm font-bold">
            <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
              <tr>
                <th className="px-8 py-5">字典标签 (Label)</th>
                <th className="px-8 py-5">字典键值 (Value)</th>
                <th className="px-8 py-5">字典类型 (Type)</th>
                <th className="px-8 py-5">排序号</th>
                <th className="px-8 py-5">状态</th>
                <th className="px-8 py-5 text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {itemsLoading ? (
                <tr><td colSpan={6} className="px-8 py-10 text-center text-slate-400 italic font-bold">数据读取中...</td></tr>
              ) : !selectedType ? (
                <tr><td colSpan={6} className="px-8 py-20 text-center text-slate-300 font-black text-lg italic">👈 请先在左侧选择字典分类</td></tr>
              ) : items.length === 0 ? (
                <tr><td colSpan={6} className="px-8 py-10 text-center text-slate-400 italic">该字典类型下暂无键值配偶</td></tr>
              ) : (
                items.map(item => (
                  <tr key={item.id} className="hover:bg-slate-50/50 transition-colors group">
                    <td className="px-8 py-5 text-slate-900">{item.dictLabel}</td>
                    <td className="px-8 py-5 text-indigo-600">{item.dictValue}</td>
                    <td className="px-8 py-5 text-xs text-slate-400">{item.dictType}</td>
                    <td className="px-8 py-5 text-slate-500 font-normal">{item.sort}</td>
                    <td className="px-8 py-5">
                      <span className={`px-2 py-1 rounded-lg text-xs ${Number(item.status) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>
                        {Number(item.status) === 1 ? '正常' : '停用'}
                      </span>
                    </td>
                    <td className="px-8 py-5 text-right space-x-3 opacity-0 group-hover:opacity-100 transition-opacity">
                      <PermGate perms={[SYSTEM_PERMS.dict.edit]}>
                        <button onClick={() => openEditItem(item)} className="text-amber-600 hover:text-amber-700">编辑</button>
                      </PermGate>
                      <PermGate perms={[SYSTEM_PERMS.dict.remove]}>
                        <button onClick={() => removeItem(item)} className="text-rose-600 hover:text-rose-700">删除</button>
                      </PermGate>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ================= MODALS: Dict Type ================= */}
      <Modal open={typeModal.open} onClose={() => setTypeModal({ open: false, isEdit: false })} title={typeModal.isEdit ? '编辑字典类型' : '新增字典类型'} maxWidth="md">
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">字典名称 <span className="text-rose-500">*</span></label>
            <input
              autoFocus
              value={typeForm.dictName}
              onChange={(e) => setTypeForm({ ...typeForm, dictName: e.target.value })}
              placeholder="例如：用户性别"
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">字典类型编码 <span className="text-rose-500">*</span></label>
            <input
              value={typeForm.dictType}
              onChange={(e) => setTypeForm({ ...typeForm, dictType: e.target.value })}
              placeholder="例如：sys_user_sex"
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">状态</label>
            <select
              value={typeForm.status}
              onChange={(e) => setTypeForm({ ...typeForm, status: Number(e.target.value) })}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            >
              <option value={1}>正常</option>
              <option value={0}>停用</option>
            </select>
          </div>
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button onClick={() => setTypeModal({ open: false, isEdit: false })} className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-700 text-sm font-black hover:bg-slate-200 transition">取消</button>
          <button disabled={typeSubmitting} onClick={() => void submitTypeForm()} className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-black shadow-md hover:bg-indigo-700 transition">{typeSubmitting ? '保存中...' : '确定提交'}</button>
        </div>
      </Modal>

      {/* ================= MODALS: Dict Item ================= */}
      <Modal open={itemModal.open} onClose={() => setItemModal({ open: false, isEdit: false })} title={itemModal.isEdit ? '编辑字典项' : '新增字典项'} maxWidth="md">
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-400 block uppercase tracking-widest">关联分类绑定</label>
            <div className="px-4 py-2.5 rounded-xl bg-indigo-50 text-indigo-700 border border-indigo-100 text-sm font-bold cursor-not-allowed">
              🔒 {itemForm.dictType}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">数据标签 (Label) <span className="text-rose-500">*</span></label>
              <input
                autoFocus
                value={itemForm.dictLabel}
                onChange={(e) => setItemForm({ ...itemForm, dictLabel: e.target.value })}
                placeholder="例如：男"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">键值 (Value) <span className="text-rose-500">*</span></label>
              <input
                value={itemForm.dictValue}
                onChange={(e) => setItemForm({ ...itemForm, dictValue: e.target.value })}
                placeholder="例如：1"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">排序号</label>
              <input
                type="number"
                value={itemForm.sort}
                onChange={(e) => setItemForm({ ...itemForm, sort: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">状态</label>
              <select
                value={itemForm.status}
                onChange={(e) => setItemForm({ ...itemForm, status: Number(e.target.value) })}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              >
                <option value={1}>正常</option>
                <option value={0}>停用</option>
              </select>
            </div>
          </div>
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button onClick={() => setItemModal({ open: false, isEdit: false })} className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-700 text-sm font-black hover:bg-slate-200 transition">取消</button>
          <button disabled={itemSubmitting} onClick={() => void submitItemForm()} className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-black shadow-md hover:bg-indigo-700 transition">{itemSubmitting ? '保存中...' : '确定提交'}</button>
        </div>
      </Modal>

    </div>
  )
}
