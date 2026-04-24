import { useCallback, useEffect, useState } from 'react'
import { opportunityApi } from '../../api/crm-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { CRM_PERMS } from '../../lib/business-perms'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import Modal from '../../components/Modal'
import type { OpportunityRow } from '../../types/crm-crud'

const STAGES = ['线索', '需求确认', '方案报价', '商务谈判', '赢单', '输单'] as const

export default function OpportunityManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()
  const [list, setList] = useState<OpportunityRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [loading, setLoading] = useState(false)
  const [stageFilter, setStageFilter] = useState<string | undefined>()
  const [formOpen, setFormOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [amount, setAmount] = useState('')
  const [remark, setRemark] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const guard = useStaleGuard()

  const loadData = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const res = await opportunityApi.page({ current, size, stage: stageFilter })
      if (!guard.isCurrent(id)) return
      setList(pickPageRecords(res)); setTotal(res.total ?? 0)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载商机列表失败')
      setList([])
      setTotal(0)
    }
    finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, stageFilter, guard, toast])

  useEffect(() => { void loadData() }, [loadData])

  const openCreate = () => { setEditId(null); setName(''); setAmount(''); setRemark(''); setFormOpen(true) }
  const openEdit = (row: OpportunityRow) => {
    setEditId(row.id!); setName(row.opportunityName ?? ''); setAmount(String(row.amount ?? '')); setRemark(row.remark ?? '')
    setFormOpen(true)
  }

  const handleSubmit = async () => {
    if (!name.trim()) return
    const parsedAmount = amount ? Number(amount) : undefined
    if (parsedAmount !== undefined && Number.isNaN(parsedAmount)) {
      toast.error('金额格式不正确')
      return
    }
    if (submitting) return
    setSubmitting(true)
    try {
      const body: Partial<OpportunityRow> = { opportunityName: name, amount: parsedAmount, remark }
      if (editId !== null) await opportunityApi.update(editId, body)
      else await opportunityApi.create(body)
      setFormOpen(false); toast.success(editId !== null ? '商机更新成功' : '商机创建成功'); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
    finally { setSubmitting(false) }
  }

  const handleAdvanceStage = async (id: number, stage: string) => {
    const ok = await confirm({ title: '推进阶段', message: `确认将商机推进到「${stage}」阶段？` })
    if (!ok) return
    try { await opportunityApi.advanceStage(id, { stage }); toast.success(`已推进到「${stage}」`); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '推进失败') }
  }

  const handleDelete = async (id: number) => {
    const ok = await confirm({ title: '删除商机', message: '确认删除此商机？此操作不可撤销。', danger: true })
    if (!ok) return
    try { await opportunityApi.remove(id); toast.success('商机已删除'); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '删除失败') }
  }

  if (!can(CRM_PERMS.opportunity.list)) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-between items-center">
        <div className="flex gap-3">
          <button onClick={() => { setStageFilter(undefined); setCurrent(1) }}
            className={`px-5 py-2 rounded-2xl text-xs font-black transition-all ${!stageFilter ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200' : 'bg-white shadow-sm ring-1 ring-slate-100 text-slate-400'}`}>全部</button>
          {STAGES.map(s => (
            <button key={s} onClick={() => { setStageFilter(s); setCurrent(1) }}
              className={`px-5 py-2 rounded-2xl text-xs font-black transition-all ${stageFilter === s ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200' : 'bg-white shadow-sm ring-1 ring-slate-100 text-slate-400 hover:text-indigo-600'}`}>{s}</button>
          ))}
        </div>
        <PermGate perms={[CRM_PERMS.opportunity.add]}>
          <button onClick={openCreate} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建商机</button>
        </PermGate>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">商机名称</th>
              <th className="px-8 py-5">客户</th>
              <th className="px-8 py-5">阶段</th>
              <th className="px-8 py-5 text-right">金额</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && !list.length ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">暂无商机</td></tr>
            ) : list.map(row => {
              const idx = STAGES.indexOf(row.stage as typeof STAGES[number])
              const nextStage = idx >= 0 && idx < STAGES.length - 1 ? STAGES[idx + 1] : null
              return (
                <tr key={row.id} className="hover:bg-slate-50 transition-all">
                  <td className="px-8 py-6 font-black text-slate-900">{row.opportunityName ?? '-'}</td>
                  <td className="px-8 py-6 text-slate-500">{row.customerName ?? '-'}</td>
                  <td className="px-8 py-6">
                    <span className={`px-2 py-1 rounded-lg text-[10px] font-black ${
                      row.stage === '赢单' ? 'bg-emerald-50 text-emerald-600' :
                      row.stage === '输单' ? 'bg-rose-50 text-rose-600' :
                      'bg-amber-50 text-amber-600'
                    }`}>{row.stage ?? '-'}</span>
                  </td>
                  <td className="px-8 py-6 text-right text-indigo-600 font-black">¥{(row.amount ?? 0).toLocaleString()}</td>
                  <td className="px-8 py-6 text-right">
                    <div className="flex gap-3 justify-end">
                      <PermGate perms={[CRM_PERMS.opportunity.edit]}>
                        <button onClick={() => openEdit(row)} className="text-indigo-600 text-xs font-black hover:underline">编辑</button>
                      </PermGate>
                      {nextStage && (
                        <PermGate perms={[CRM_PERMS.opportunity.advance]}>
                          <button onClick={() => void handleAdvanceStage(row.id!, nextStage)} className="text-emerald-600 text-xs font-black hover:underline">推进→{nextStage}</button>
                        </PermGate>
                      )}
                      <PermGate perms={[CRM_PERMS.opportunity.remove]}>
                        <button onClick={() => void handleDelete(row.id!)} className="text-rose-500 text-xs font-black hover:underline">删除</button>
                      </PermGate>
                    </div>
                  </td>
                </tr>
              )
            })}
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

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editId !== null ? '编辑商机' : '新建商机'}>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">商机名称</label>
              <input value={name} onChange={e => setName(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-black text-slate-400 uppercase">金额</label>
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
    </div>
  )
}
