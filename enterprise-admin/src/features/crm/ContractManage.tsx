import { useCallback, useEffect, useState } from 'react'
import { contractApi } from '../../api/crm-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { CRM_PERMS } from '../../lib/business-perms'
import { formatDateTime } from '../../lib/format'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import Modal from '../../components/Modal'
import type { ContractRow, ContractItem } from '../../types/crm-crud'

export default function ContractManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()
  const [list, setList] = useState<ContractRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [loading, setLoading] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [contractName, setContractName] = useState('')
  const [amount, setAmount] = useState('')
  const [remark, setRemark] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [detailItems, setDetailItems] = useState<ContractItem[]>([])
  const [detailOpen, setDetailOpen] = useState(false)
  const guard = useStaleGuard()

  const loadData = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const res = await contractApi.page({ current, size })
      if (!guard.isCurrent(id)) return
      setList(pickPageRecords(res)); setTotal(res.total ?? 0)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载合同列表失败')
      setList([])
      setTotal(0)
    }
    finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, guard, toast])

  useEffect(() => { void loadData() }, [loadData])

  const openCreate = () => { setEditId(null); setContractName(''); setAmount(''); setRemark(''); setFormOpen(true) }
  const openEdit = (row: ContractRow) => {
    setEditId(row.id!); setContractName(row.contractName ?? ''); setAmount(String(row.amount ?? '')); setRemark(row.remark ?? '')
    setFormOpen(true)
  }

  const handleSubmit = async () => {
    if (!contractName.trim()) return
    const parsedAmount = amount ? Number(amount) : undefined
    if (parsedAmount !== undefined && Number.isNaN(parsedAmount)) {
      toast.error('金额格式不正确')
      return
    }
    if (submitting) return
    setSubmitting(true)
    try {
      const body: Partial<ContractRow> = { contractName, amount: parsedAmount, remark }
      if (editId !== null) await contractApi.update(editId, body)
      else await contractApi.create(body)
      setFormOpen(false); toast.success(editId !== null ? '合同更新成功' : '合同创建成功'); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
    finally { setSubmitting(false) }
  }

  const handleDelete = async (id: number) => {
    const ok = await confirm({ title: '删除合同', message: '确认删除此合同？此操作不可撤销。', danger: true })
    if (!ok) return
    try { await contractApi.remove(id); toast.success('合同已删除'); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '删除失败') }
  }

  const openDetail = async (id: number) => {
    try { setDetailItems(await contractApi.getItems(id)); setDetailOpen(true) }
    catch (e) {
      toast.error(e instanceof Error ? e.message : '加载合同明细失败')
      setDetailItems([])
      setDetailOpen(true)
    }
  }

  if (!can(CRM_PERMS.contract.list)) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-end">
        <PermGate perms={[CRM_PERMS.contract.add]}>
          <button onClick={openCreate} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建合同</button>
        </PermGate>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">合同编号</th>
              <th className="px-8 py-5">合同名称</th>
              <th className="px-8 py-5">客户</th>
              <th className="px-8 py-5 text-right">总额</th>
              <th className="px-8 py-5">签约日期</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && !list.length ? (
              <tr><td colSpan={6} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={6} className="px-8 py-12 text-center text-slate-300 font-black">暂无合同</td></tr>
            ) : list.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{row.contractNo ?? row.id}</td>
                <td className="px-8 py-6 text-slate-700">{row.contractName ?? '-'}</td>
                <td className="px-8 py-6 text-slate-500">{row.customerName ?? '-'}</td>
                <td className="px-8 py-6 text-right text-indigo-600 font-black">¥{(row.amount ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-slate-400 text-xs">{formatDateTime(row.signDate)}</td>
                <td className="px-8 py-6 text-right">
                  <div className="flex gap-3 justify-end">
                    <button onClick={() => void openDetail(row.id!)} className="text-slate-600 text-xs font-black hover:underline">明细</button>
                    <PermGate perms={[CRM_PERMS.contract.edit]}>
                      <button onClick={() => openEdit(row)} className="text-indigo-600 text-xs font-black hover:underline">编辑</button>
                    </PermGate>
                    <PermGate perms={[CRM_PERMS.contract.remove]}>
                      <button onClick={() => void handleDelete(row.id!)} className="text-rose-500 text-xs font-black hover:underline">删除</button>
                    </PermGate>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-8 py-4 bg-slate-50/50 border-t border-slate-50 flex justify-between items-center text-xs font-bold text-slate-500">
          <div>共 <span className="text-slate-900">{total}</span> 条</div>
          <div className="flex gap-2">
            <button onClick={() => setCurrent(p => Math.max(1, p - 1))} disabled={current <= 1} className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50">前页</button>
            <span className="px-3 py-1 text-slate-900 bg-white rounded-lg ring-1 ring-indigo-200 font-black">{current}</span>
            <button onClick={() => setCurrent(p => p + 1)} disabled={current * size >= total} className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50">后页</button>
          </div>
        </div>
      </div>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editId !== null ? '编辑合同' : '新建合同'}>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">合同名称</label>
              <input value={contractName} onChange={e => setContractName(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">总额</label>
              <input type="number" value={amount} onChange={e => setAmount(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">备注</label>
              <input value={remark} onChange={e => setRemark(e.target.value)}
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

      <Modal open={detailOpen} onClose={() => setDetailOpen(false)} title="合同明细" maxWidth="lg">
            {detailItems.length === 0 ? (
              <div className="text-center text-slate-300 font-black py-8">暂无明细</div>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-[10px] font-black text-slate-400 uppercase">
                  <tr><th className="py-2">产品</th><th className="py-2 text-right">数量</th><th className="py-2 text-right">单价</th><th className="py-2 text-right">小计</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {detailItems.map(it => (
                    <tr key={it.id}>
                      <td className="py-3 font-bold text-slate-900">{it.productName ?? '-'}</td>
                      <td className="py-3 text-right text-slate-500">{it.quantity}</td>
                      <td className="py-3 text-right text-slate-500">¥{(it.unitPrice ?? 0).toLocaleString()}</td>
                      <td className="py-3 text-right font-black text-indigo-600">¥{(it.subtotal ?? 0).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <button onClick={() => setDetailOpen(false)} className="w-full py-3 bg-slate-100 rounded-2xl font-black text-xs text-slate-400">关闭</button>
      </Modal>
    </div>
  )
}
