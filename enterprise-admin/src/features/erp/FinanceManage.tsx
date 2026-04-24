import { useCallback, useEffect, useState } from 'react'
import { receivableApi, payableApi } from '../../api/erp-finance'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { ERP_PERMS } from '../../lib/business-perms'
import { formatDateTime } from '../../lib/format'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import Modal from '../../components/Modal'
import type { ReceivableRow, PayableRow, FinanceSummary } from '../../types/erp-crud'

type Tab = '应收' | '应付'

export default function FinanceManage() {
  const toast = useToast()
  const { can } = usePermissions()
  const [tab, setTab] = useState<Tab>('应收')
  const [recList, setRecList] = useState<ReceivableRow[]>([])
  const [payList, setPayList] = useState<PayableRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [loading, setLoading] = useState(false)
  const [summary, setSummary] = useState<FinanceSummary>({})
  const [paymentModal, setPaymentModal] = useState<{ id: number; type: Tab } | null>(null)
  const [payAmount, setPayAmount] = useState('')
  const [payRemark, setPayRemark] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const guard = useStaleGuard()

  const loadData = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    try {
      if (tab === '应收') {
        const [res, sum] = await Promise.all([
          receivableApi.page({ current, size }),
          receivableApi.summary(),
        ])
        if (!guard.isCurrent(id)) return
        setRecList(pickPageRecords(res)); setTotal(res.total ?? 0); setSummary(sum)
      } else {
        const [res, sum] = await Promise.all([
          payableApi.page({ current, size }),
          payableApi.summary(),
        ])
        if (!guard.isCurrent(id)) return
        setPayList(pickPageRecords(res)); setTotal(res.total ?? 0); setSummary(sum)
      }
    } catch (e) {
      if (!guard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载财务数据失败')
      tab === '应收' ? setRecList([]) : setPayList([])
      setTotal(0)
    }
    finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, tab, guard, toast])

  useEffect(() => { void loadData() }, [loadData])

  const handleTabChange = (t: Tab) => { setTab(t); setCurrent(1) }

  const handleRecordPayment = async () => {
    if (!paymentModal || !payAmount) return
    const parsedAmount = Number(payAmount)
    if (Number.isNaN(parsedAmount) || parsedAmount <= 0) { toast.error('金额必须大于零'); return }
    if (submitting) return
    setSubmitting(true)
    try {
      const api = paymentModal.type === '应收' ? receivableApi : payableApi
      await api.recordPayment(paymentModal.id, { amount: parsedAmount, remark: payRemark })
      setPaymentModal(null); setPayAmount(''); setPayRemark(''); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
    finally { setSubmitting(false) }
  }

  const summaryCards = [
    { label: '总额', value: summary.totalAmount, color: 'text-slate-900' },
    { label: '已结', value: summary.settledAmount, color: 'text-emerald-600' },
    { label: '待结', value: summary.pendingAmount, color: 'text-amber-600' },
    { label: '逾期', value: summary.overdueAmount, color: 'text-rose-600' },
  ]

  const list = tab === '应收' ? recList : payList

  const canViewFinance = can(ERP_PERMS.receivable.list) || can(ERP_PERMS.payable.list)
  if (!canViewFinance) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="grid grid-cols-4 gap-4">
        {summaryCards.map(c => (
          <div key={c.label} className="bg-white p-6 rounded-[2rem] shadow-sm ring-1 ring-slate-100">
            <div className="text-[10px] font-black text-slate-400 uppercase mb-2">{c.label}</div>
            <div className={`text-2xl font-black ${c.color}`}>¥{(c.value ?? 0).toLocaleString()}</div>
          </div>
        ))}
      </div>

      <div className="flex gap-3">
        {(['应收', '应付'] as Tab[]).map(t => (
          <button key={t} onClick={() => handleTabChange(t)}
            className={`px-6 py-2 rounded-2xl text-xs font-black transition-all ${tab === t ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200' : 'bg-white shadow-sm ring-1 ring-slate-100 text-slate-400'}`}
          >{t}管理</button>
        ))}
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">{tab === '应收' ? '客户' : '供应商'}</th>
              <th className="px-8 py-5">总额</th>
              <th className="px-8 py-5">{tab === '应收' ? '已收' : '已付'}</th>
              <th className="px-8 py-5">到期日</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && !list.length ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">暂无数据</td></tr>
            ) : list.map((row: ReceivableRow & PayableRow) => (
              <tr key={row.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{row.customerName ?? row.supplierName ?? '-'}</td>
                <td className="px-8 py-6 text-indigo-600 font-black">¥{Number(row.totalAmount ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-emerald-600">¥{Number((tab === '应收' ? row.receivedAmount : row.paidAmount) ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-slate-500">{formatDateTime(row.dueDate)}</td>
                <td className="px-8 py-6 text-right">
                  {tab === '应收' ? (
                    <PermGate perms={[ERP_PERMS.receivable.record]}>
                      <button onClick={() => void setPaymentModal({ id: row.id!, type: tab })}
                        className="text-indigo-600 text-xs font-black hover:underline">收款登记</button>
                    </PermGate>
                  ) : (
                    <PermGate perms={[ERP_PERMS.payable.record]}>
                      <button onClick={() => void setPaymentModal({ id: row.id!, type: tab })}
                        className="text-indigo-600 text-xs font-black hover:underline">付款登记</button>
                    </PermGate>
                  )}
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

      <Modal open={!!paymentModal} onClose={() => setPaymentModal(null)} title={paymentModal?.type === '应收' ? '收款登记' : '付款登记'}>
        <div className="p-8 space-y-6">
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">金额</label>
            <input type="number" value={payAmount} onChange={e => setPayAmount(e.target.value)} min={0}
              className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          </div>
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">备注</label>
            <input value={payRemark} onChange={e => setPayRemark(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          </div>
          <div className="flex gap-4">
            <button onClick={() => setPaymentModal(null)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
            <button onClick={() => void handleRecordPayment()} disabled={submitting} className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all disabled:opacity-60">确认</button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
