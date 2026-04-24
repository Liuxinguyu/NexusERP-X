import { useCallback, useEffect, useState } from 'react'
import { itemConfigApi } from '../../api/wage-crud'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { WAGE_PERMS } from '../../lib/business-perms'
import Modal from '../../components/Modal'
import type { ItemConfigRow } from '../../types/wage-crud'

const CALC_TYPE_MAP: Record<number, string> = { 1: '固定', 2: '浮动' }
const ITEM_KIND_MAP: Record<number, string> = { 1: '收入', 2: '补贴', 3: '扣减' }

export default function ItemConfigManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()
  const [list, setList] = useState<ItemConfigRow[]>([])
  const [loading, setLoading] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [itemName, setItemName] = useState('')
  const [calcType, setCalcType] = useState<number>(1)
  const [defaultAmount, setDefaultAmount] = useState('')
  const [itemKind, setItemKind] = useState<number>(1)
  const [submitting, setSubmitting] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    try { setList(await itemConfigApi.list()) }
    catch (e) {
      toast.error(e instanceof Error ? e.message : '加载薪资项失败')
      setList([])
    }
    finally { setLoading(false) }
  }, [toast])

  useEffect(() => { void loadData() }, [loadData])

  const openCreate = () => { setEditId(null); setItemName(''); setCalcType(1); setDefaultAmount(''); setItemKind(1); setFormOpen(true) }
  const openEdit = (row: ItemConfigRow) => {
    setEditId(row.id!); setItemName(row.itemName ?? ''); setCalcType(row.calcType ?? 1); setDefaultAmount(String(row.defaultAmount ?? '')); setItemKind(row.itemKind ?? 1)
    setFormOpen(true)
  }

  const handleSubmit = async () => {
    if (!itemName.trim()) return
    if (submitting) return
    setSubmitting(true)
    try {
      const body: Partial<ItemConfigRow> = { itemName, calcType, defaultAmount: defaultAmount ? Number(defaultAmount) : undefined, itemKind }
      if (editId !== null) await itemConfigApi.update(editId, body)
      else await itemConfigApi.create(body)
      setFormOpen(false); toast.success(editId !== null ? '薪资项更新成功' : '薪资项创建成功'); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
    finally { setSubmitting(false) }
  }

  const handleDelete = async (id: number) => {
    const ok = await confirm({ title: '删除薪资项', message: '确认删除此薪资项？此操作不可撤销。', danger: true })
    if (!ok) return
    try { await itemConfigApi.remove(id); toast.success('薪资项已删除'); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '删除失败') }
  }

  if (!can(WAGE_PERMS.itemConfig.list)) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-end">
        <PermGate perms={[WAGE_PERMS.itemConfig.add]}>
          <button onClick={openCreate} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建薪资项</button>
        </PermGate>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">项目名称</th>
              <th className="px-8 py-5">计算方式</th>
              <th className="px-8 py-5">类型</th>
              <th className="px-8 py-5 text-right">默认金额</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">暂无配置</td></tr>
            ) : list.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{row.itemName ?? '-'}</td>
                <td className="px-8 py-6 text-slate-500">{CALC_TYPE_MAP[row.calcType ?? 0] ?? '-'}</td>
                <td className="px-8 py-6 text-slate-500">{ITEM_KIND_MAP[row.itemKind ?? 0] ?? '-'}</td>
                <td className="px-8 py-6 text-right text-indigo-600 font-black">{row.defaultAmount != null ? `¥${Number(row.defaultAmount).toLocaleString()}` : '-'}</td>
                <td className="px-8 py-6 text-right">
                  <div className="flex gap-3 justify-end">
                    <PermGate perms={[WAGE_PERMS.itemConfig.edit]}>
                      <button onClick={() => openEdit(row)} className="text-indigo-600 text-xs font-black hover:underline">编辑</button>
                    </PermGate>
                    <PermGate perms={[WAGE_PERMS.itemConfig.delete]}>
                      <button onClick={() => void handleDelete(row.id!)} className="text-rose-500 text-xs font-black hover:underline">删除</button>
                    </PermGate>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editId !== null ? '编辑薪资项' : '新建薪资项'}>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">项目名称</label>
              <input value={itemName} onChange={e => setItemName(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">计算方式</label>
              <select value={calcType} onChange={e => setCalcType(Number(e.target.value))}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500">
                {Object.entries(CALC_TYPE_MAP).map(([v, l]) => <option key={v} value={Number(v)}>{l}</option>)}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">薪资项类型</label>
              <select value={itemKind} onChange={e => setItemKind(Number(e.target.value))}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500">
                {Object.entries(ITEM_KIND_MAP).map(([v, l]) => <option key={v} value={Number(v)}>{l}</option>)}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">默认金额</label>
              <input type="number" value={defaultAmount} onChange={e => setDefaultAmount(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="flex gap-4">
              <button onClick={() => setFormOpen(false)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
              <button onClick={() => void handleSubmit()} disabled={submitting}
                className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs hover:bg-indigo-500 transition-all disabled:opacity-60">
                {submitting ? '提交中...' : '保存'}
              </button>
            </div>
      </Modal>
    </div>
  )
}
