import { useCallback, useEffect, useState } from 'react'
import { dictItemApi, dictTypeApi } from '../../api/system-crud'
import type { DictItemRow, DictTypeRow } from '../../types/system-crud'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { useStaleGuard } from '../../hooks/useStaleGuard'

export default function DictManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()
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
  const [typeFormErrors, setTypeFormErrors] = useState<Record<string, string>>({})

  // === 右侧：字典项 Context ===
  const [items, setItems] = useState<DictItemRow[]>([])
  const [itemTotal, setItemTotal] = useState(0)
  const [itemCurrent, setItemCurrent] = useState(1)
  const [itemSize, setItemSize] = useState(10)
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
  const [itemFormErrors, setItemFormErrors] = useState<Record<string, string>>({})
  const typeGuard = useStaleGuard()
  const itemGuard = useStaleGuard()

  // ================= 字典类型 CRUD =================

  const loadTypes = useCallback(async () => {
    const id = typeGuard.nextId()
    setTypesLoading(true)
    try {
      const data = await dictTypeApi.list()
      if (!typeGuard.isCurrent(id)) return
      setTypes(Array.isArray(data) ? data : [])
    } catch (e) {
      if (!typeGuard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载字典类型失败')
    } finally {
      if (!typeGuard.isCurrent(id)) return
      setTypesLoading(false)
    }
  }, [typeGuard, toast])

  useEffect(() => {
    void loadTypes()
  }, [loadTypes])

  const loadItems = useCallback(async (typeDictType: string, current = itemCurrent, size = itemSize) => {
    const id = itemGuard.nextId()
    setItemsLoading(true)
    try {
      const data = await dictItemApi.page({ current, size, dictType: typeDictType })
      const records = (data.records ?? data.list ?? []) as DictItemRow[]
      if (!itemGuard.isCurrent(id)) return
      setItems(Array.isArray(records) ? records : [])
      setItemTotal(Number(data.total ?? 0))
    } catch (e) {
      if (!itemGuard.isCurrent(id)) return
      setItems([])
      setItemTotal(0)
      toast.error(e instanceof Error ? e.message : '加载字典项失败')
    } finally {
      if (!itemGuard.isCurrent(id)) return
      setItemsLoading(false)
    }
  }, [itemCurrent, itemSize, toast, itemGuard])

  useEffect(() => {
    if (selectedType) {
      void loadItems(selectedType, itemCurrent, itemSize)
    } else {
      setItems([])
      setItemTotal(0)
    }
  }, [selectedType, loadItems, itemCurrent, itemSize])

  const openAddType = () => {
    setTypeFormErrors({})
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
      setTypeFormErrors({})
      setTypeModal({ open: true, isEdit: true, id: node.id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '获取详情失败')
    }
  }

  const submitTypeForm = async () => {
    const errors: Record<string, string> = {}
    if (!typeForm.dictName.trim()) errors.dictName = '字典名称必填'
    if (!typeForm.dictType.trim()) errors.dictType = '字典类型必填'
    setTypeFormErrors(errors)
    if (Object.keys(errors).length > 0) return
    if (typeSubmitting) return
    setTypeSubmitting(true)
    try {
      if (typeModal.isEdit && typeModal.id != null) {
        await dictTypeApi.update(typeModal.id, typeForm)
      } else {
        await dictTypeApi.create(typeForm)
      }
      toast.success(typeModal.isEdit ? '字典类型已保存' : '字典类型已新增')
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
      toast.success('字典类型已删除')
      if (selectedType === node.dictType) setSelectedType(undefined)
      await loadTypes()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  // ================= 字典项 CRUD =================

  const openAddItem = () => {
    if (!selectedType) { toast.error('请先在左侧选择一个字典类型'); return }
    setItemFormErrors({})
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
      setItemFormErrors({})
      setItemModal({ open: true, isEdit: true, id: row.id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '获取字典项详情失败')
    }
  }

  const submitItemForm = async () => {
    const errors: Record<string, string> = {}
    if (!itemForm.dictType.trim()) errors.dictType = '字典类型必填'
    if (!itemForm.dictLabel.trim()) errors.dictLabel = '字典标签必填'
    if (!itemForm.dictValue.trim()) errors.dictValue = '字典键值必填'
    setItemFormErrors(errors)
    if (Object.keys(errors).length > 0) return
    if (itemSubmitting) return
    setItemSubmitting(true)
    try {
      if (itemModal.isEdit && itemModal.id != null) {
        await dictItemApi.update(itemModal.id, itemForm)
      } else {
        await dictItemApi.create(itemForm)
      }
      toast.success(itemModal.isEdit ? '字典项已保存' : '字典项已新增')
      setItemModal({ open: false, isEdit: false })
      if (selectedType) await loadItems(selectedType, itemCurrent, itemSize)
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
      toast.success('字典项已删除')
      if (selectedType) await loadItems(selectedType, itemCurrent, itemSize)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  if (!can(SYSTEM_PERMS.dict.query)) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：字典管理
      </div>
    )
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
                        onClick={(e) => { e.stopPropagation(); void openEditType(t) }}
                        className="px-1.5 py-1 text-amber-600 hover:bg-amber-100 rounded-md transition"
                        title="编辑类型"
                      >
                        ✎
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.dict.remove]}>
                      <button
                        onClick={(e) => { e.stopPropagation(); void removeType(t) }}
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
                <tr><td colSpan={6} className="px-8 py-10 text-center text-slate-400 italic">该字典类型下暂无键值配置</td></tr>
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
                        <button onClick={() => void openEditItem(item)} className="text-amber-600 hover:text-amber-700">编辑</button>
                      </PermGate>
                      <PermGate perms={[SYSTEM_PERMS.dict.remove]}>
                        <button onClick={() => void removeItem(item)} className="text-rose-600 hover:text-rose-700">删除</button>
                      </PermGate>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <div className="px-8 py-4 bg-slate-50/50 border-t border-slate-50 flex justify-between items-center text-xs font-bold text-slate-500">
          <span>共 {itemTotal} 条</span>
          <div className="flex items-center gap-2">
            <select
              value={itemSize}
              onChange={(e) => {
                setItemSize(Number(e.target.value))
                setItemCurrent(1)
              }}
              className="px-2 py-1 bg-white rounded-lg ring-1 ring-slate-200"
            >
              <option value={10}>10条/页</option>
              <option value={20}>20条/页</option>
              <option value={50}>50条/页</option>
            </select>
            <span>第 {itemCurrent} / {Math.max(1, Math.ceil(itemTotal / itemSize))} 页</span>
            <button
              type="button"
              disabled={itemCurrent <= 1}
              onClick={() => setItemCurrent((v) => Math.max(1, v - 1))}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={itemCurrent >= Math.max(1, Math.ceil(itemTotal / itemSize))}
              onClick={() => setItemCurrent((v) => v + 1)}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              下一页
            </button>
          </div>
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
              onChange={(e) => setTypeForm((prev) => ({ ...prev, dictName: e.target.value }))}
              placeholder="例如：用户性别"
              className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ${
                typeFormErrors.dictName ? 'ring-rose-500' : 'ring-slate-200'
              } text-sm font-bold focus:ring-indigo-500 outline-none transition`}
            />
            {typeFormErrors.dictName ? <p className="text-[10px] text-rose-600 font-bold">{typeFormErrors.dictName}</p> : null}
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">字典类型编码 <span className="text-rose-500">*</span></label>
            <input
              value={typeForm.dictType}
              onChange={(e) => setTypeForm((prev) => ({ ...prev, dictType: e.target.value }))}
              placeholder="例如：sys_user_sex"
              className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ${
                typeFormErrors.dictType ? 'ring-rose-500' : 'ring-slate-200'
              } text-sm font-bold focus:ring-indigo-500 outline-none transition`}
            />
            {typeFormErrors.dictType ? <p className="text-[10px] text-rose-600 font-bold">{typeFormErrors.dictType}</p> : null}
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-black text-slate-500 block">状态</label>
            <select
              value={typeForm.status}
              onChange={(e) => setTypeForm((prev) => ({ ...prev, status: Number(e.target.value) }))}
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
                onChange={(e) => setItemForm((prev) => ({ ...prev, dictLabel: e.target.value }))}
                placeholder="例如：男"
                className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ${
                  itemFormErrors.dictLabel ? 'ring-rose-500' : 'ring-slate-200'
                } text-sm font-bold focus:ring-indigo-500 outline-none transition`}
              />
              {itemFormErrors.dictLabel ? <p className="text-[10px] text-rose-600 font-bold">{itemFormErrors.dictLabel}</p> : null}
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">键值 (Value) <span className="text-rose-500">*</span></label>
              <input
                value={itemForm.dictValue}
                onChange={(e) => setItemForm((prev) => ({ ...prev, dictValue: e.target.value }))}
                placeholder="例如：1"
                className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ${
                  itemFormErrors.dictValue ? 'ring-rose-500' : 'ring-slate-200'
                } text-sm font-bold focus:ring-indigo-500 outline-none transition`}
              />
              {itemFormErrors.dictValue ? <p className="text-[10px] text-rose-600 font-bold">{itemFormErrors.dictValue}</p> : null}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">排序号</label>
              <input
                type="number"
                value={itemForm.sort}
                onChange={(e) => setItemForm((prev) => ({ ...prev, sort: Number(e.target.value) }))}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-500 block">状态</label>
              <select
                value={itemForm.status}
                onChange={(e) => setItemForm((prev) => ({ ...prev, status: Number(e.target.value) }))}
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
