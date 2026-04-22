import { useCallback, useEffect, useState } from 'react'
import { monthlySlipApi } from '../../api/wage-crud'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import type { MonthlySlipRow } from '../../types/wage-crud'

export default function SlipManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const [list, setList] = useState<MonthlySlipRow[]>([])
  const [loading, setLoading] = useState(false)
  const [month, setMonth] = useState(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  })
  const [generating, setGenerating] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [adjustModal, setAdjustModal] = useState<MonthlySlipRow | null>(null)
  const [baseSalary, setBaseSalary] = useState('')
  const [subsidyTotal, setSubsidyTotal] = useState('')
  const [deductionTotal, setDeductionTotal] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    try { setList(await monthlySlipApi.list({ belongMonth: month })) }
    catch (e) {
      toast.error(e instanceof Error ? e.message : '加载工资条失败')
      setList([])
    }
    finally { setLoading(false) }
  }, [month])

  useEffect(() => { void loadData() }, [loadData])

  const handleGenerate = async () => {
    setGenerating(true)
    try { await monthlySlipApi.generate({ belongMonth: month }); toast.success('工资条生成成功'); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '生成失败') }
    finally { setGenerating(false) }
  }

  const handleConfirmPay = async () => {
    const ids = list.filter(r => r.status === 0).map(r => r.id!).filter(Boolean)
    if (!ids.length) { toast.error('没有待发放的工资条'); return }
    const ok = await confirm({ title: '确认发放', message: `确认发放 ${month} 共 ${ids.length} 条工资？` })
    if (!ok) return
    setConfirming(true)
    try { await monthlySlipApi.confirmPay({ slipIds: ids }); toast.success('工资发放成功'); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '发放失败') }
    finally { setConfirming(false) }
  }

  const openAdjust = (row: MonthlySlipRow) => {
    setAdjustModal(row)
    setBaseSalary(String(row.baseSalary ?? 0))
    setSubsidyTotal(String(row.subsidyTotal ?? 0))
    setDeductionTotal(String(row.deductionTotal ?? 0))
  }

  const handleAdjust = async () => {
    if (adjustModal?.id == null) return
    const ok = await confirm({ title: '薪资调整', message: '确认提交此次薪资调整？' })
    if (!ok) return
    try {
      await monthlySlipApi.adjust(adjustModal.id, {
        baseSalary: Number(baseSalary) || 0,
        subsidyTotal: Number(subsidyTotal) || 0,
        deductionTotal: Number(deductionTotal) || 0,
      })
      setAdjustModal(null)
      toast.success('薪资调整成功')
      void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '调整失败') }
  }

  const statusLabel = (s?: number) => {
    if (s === 1) return <span className="text-emerald-600">已发放</span>
    return <span className="text-amber-500">待发放</span>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-between items-end">
        <div className="flex gap-4 items-end">
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">计薪月份</label>
            <input type="month" value={month} onChange={e => setMonth(e.target.value)}
              className="px-4 py-2 rounded-xl bg-white ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          </div>
        </div>
        <div className="flex gap-3">
          <button onClick={handleGenerate} disabled={generating}
            className="px-6 py-2 bg-white rounded-2xl ring-1 ring-slate-200 text-xs font-black text-slate-600 hover:bg-slate-50 transition-all disabled:opacity-60">
            {generating ? '生成中...' : '批量生成'}
          </button>
          <button onClick={handleConfirmPay} disabled={confirming}
            className="px-6 py-2 bg-indigo-600 text-white rounded-2xl text-xs font-black shadow-lg shadow-indigo-200 hover:bg-indigo-500 transition-all disabled:opacity-60">
            {confirming ? '发放中...' : '确认发放'}
          </button>
        </div>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">员工ID</th>
              <th className="px-8 py-5 text-right">基本工资</th>
              <th className="px-8 py-5 text-right">补贴合计</th>
              <th className="px-8 py-5 text-right">扣款合计</th>
              <th className="px-8 py-5 text-right">实发</th>
              <th className="px-8 py-5 text-center">状态</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr><td colSpan={7} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={7} className="px-8 py-12 text-center text-slate-300 font-black">暂无工资条</td></tr>
            ) : list.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{row.employeeId ?? '-'}</td>
                <td className="px-8 py-6 text-right text-slate-700">¥{(row.baseSalary ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-right text-emerald-600">+ ¥{(row.subsidyTotal ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-right text-rose-500">- ¥{(row.deductionTotal ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-right text-lg font-black text-slate-900">¥{(row.netPay ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6 text-center text-xs font-black">{statusLabel(row.status)}</td>
                <td className="px-8 py-6 text-right">
                  <button onClick={() => openAdjust(row)} className="text-indigo-600 text-xs font-black hover:underline">调整</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {adjustModal != null && (
        <div className="fixed inset-0 z-50 bg-black/30 backdrop-blur-sm flex items-center justify-center" onClick={() => setAdjustModal(null)}>
          <div className="bg-white rounded-[2rem] shadow-2xl w-full max-w-md p-8 space-y-6" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-black text-slate-900">薪资调整</h3>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">基本工资</label>
              <input type="number" value={baseSalary} onChange={e => setBaseSalary(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">补贴合计</label>
              <input type="number" value={subsidyTotal} onChange={e => setSubsidyTotal(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">扣款合计</label>
              <input type="number" value={deductionTotal} onChange={e => setDeductionTotal(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="flex gap-4">
              <button onClick={() => setAdjustModal(null)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
              <button onClick={handleAdjust} className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs hover:bg-indigo-500 transition-all">确认调整</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
