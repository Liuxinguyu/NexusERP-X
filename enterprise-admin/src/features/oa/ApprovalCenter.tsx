import { useCallback, useEffect, useState } from 'react'
import { approvalApi } from '../../api/oa-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import type { ApprovalTaskRow } from '../../types/oa-crud'

type Tab = '待我审批' | '我发起的'

export default function ApprovalCenter() {
  const toast = useToast()
  const confirm = useConfirm()
  const [tab, setTab] = useState<Tab>('待我审批')
  const [list, setList] = useState<ApprovalTaskRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [loading, setLoading] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const api = tab === '待我审批' ? approvalApi.myApprove : approvalApi.myInitiated
      const res = await api({ current, size })
      setList(pickPageRecords(res)); setTotal(res.total ?? 0)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载审批数据失败')
      setList([])
      setTotal(0)
    }
    finally { setLoading(false) }
  }, [current, tab])

  useEffect(() => { void loadData() }, [loadData])

  const handleAction = async (id: number, action: 'approve' | 'reject') => {
    const ok = await confirm({
      title: action === 'approve' ? '审批通过' : '驳回申请',
      message: action === 'approve' ? '确认通过此审批？' : '确认驳回此审批？',
      danger: action === 'reject',
    })
    if (!ok) return
    try {
      if (action === 'approve') await approvalApi.approve(id)
      else await approvalApi.reject(id)
      toast.success(action === 'approve' ? '已通过' : '已驳回')
      void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex gap-3">
        {(['待我审批', '我发起的'] as Tab[]).map(t => (
          <button key={t} onClick={() => { setTab(t); setCurrent(1) }}
            className={`px-6 py-2 rounded-2xl text-xs font-black transition-all ${tab === t ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200' : 'bg-white shadow-sm ring-1 ring-slate-100 text-slate-400'}`}
          >{t}</button>
        ))}
      </div>
      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
            <tr>
              <th className="px-8 py-5">标题</th>
              <th className="px-8 py-5">发起人</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5">时间</th>
              {tab === '待我审批' && <th className="px-8 py-5 text-right">操作</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">暂无审批任务</td></tr>
            ) : list.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-colors">
                <td className="px-8 py-5 font-black text-slate-900">{row.title ?? '-'}</td>
                <td className="px-8 py-5 text-slate-500">{row.applicantUserName ?? '-'}</td>
                <td className="px-8 py-5">
                  <span className={`px-2 py-1 rounded-lg text-[10px] font-black ${
                    String(row.status) === '0' ? 'bg-amber-50 text-amber-600' :
                    String(row.status) === '1' ? 'bg-emerald-50 text-emerald-600' :
                    'bg-rose-50 text-rose-600'
                  }`}>{String(row.status) === '0' ? '待审批' : String(row.status) === '1' ? '已通过' : '已驳回'}</span>
                </td>
                <td className="px-8 py-5 text-slate-400 text-xs">{row.createTime ?? '-'}</td>
                {tab === '待我审批' && (
                  <td className="px-8 py-5 text-right">
                    {String(row.status) === '0' && (
                      <div className="flex gap-3 justify-end">
                        <button onClick={() => handleAction(row.id as number, 'approve')} className="text-emerald-600 text-xs font-black hover:underline">通过</button>
                        <button onClick={() => handleAction(row.id as number, 'reject')} className="text-rose-500 text-xs font-black hover:underline">驳回</button>
                      </div>
                    )}
                  </td>
                )}
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
    </div>
  )
}
