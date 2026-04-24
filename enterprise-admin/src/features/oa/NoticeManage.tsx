import { useCallback, useEffect, useState } from 'react'
import { noticeApi } from '../../api/notice'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { OA_PERMS } from '../../lib/business-perms'
import { formatDateTime } from '../../lib/format'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import Modal from '../../components/Modal'
import type { NoticeRow } from '../../types/oa-crud'

export default function NoticeManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()
  const [list, setList] = useState<NoticeRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [loading, setLoading] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const guard = useStaleGuard()

  const loadData = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const res = await noticeApi.page({ current, size })
      if (!guard.isCurrent(id)) return
      setList(pickPageRecords(res)); setTotal(res.total ?? 0)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载公告列表失败')
      setList([])
      setTotal(0)
    }
    finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, guard, toast])

  useEffect(() => { void loadData() }, [loadData])

  const openCreate = () => { setEditId(null); setTitle(''); setContent(''); setFormOpen(true) }
  const openEdit = (row: NoticeRow) => { setEditId(row.id!); setTitle(row.title ?? ''); setContent(row.content ?? ''); setFormOpen(true) }

  const handleSubmit = async () => {
    if (!title.trim()) return
    if (submitting) return
    setSubmitting(true)
    try {
      if (editId !== null) await noticeApi.update(editId, { title, content })
      else await noticeApi.create({ title, content })
      toast.success('保存成功')
      setFormOpen(false); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
    finally { setSubmitting(false) }
  }

  const handlePublish = async (id: number) => {
    const ok = await confirm({ title: '发布公告', message: '确认发布此公告？发布后所有用户可见。' })
    if (!ok) return
    try { await noticeApi.publish(id); toast.success('发布成功'); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '发布失败') }
  }

  const statusLabel = (s: number | undefined) => {
    if (s === 1) return <span className="px-2 py-1 rounded-lg text-[10px] font-black bg-emerald-50 text-emerald-600">已发布</span>
    return <span className="px-2 py-1 rounded-lg text-[10px] font-black bg-slate-100 text-slate-500">草稿</span>
  }

  if (!can(OA_PERMS.notice.list)) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-end">
        <PermGate perms={[OA_PERMS.notice.add]}>
          <button onClick={openCreate} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建公告</button>
        </PermGate>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">标题</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5">创建时间</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && !list.length ? (
              <tr><td colSpan={4} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={4} className="px-8 py-12 text-center text-slate-300 font-black">暂无公告</td></tr>
            ) : list.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{row.title ?? '-'}</td>
                <td className="px-8 py-6">{statusLabel(row.status)}</td>
                <td className="px-8 py-6 text-slate-400 text-xs">{formatDateTime(row.createTime)}</td>
                <td className="px-8 py-6 text-right">
                  <div className="flex gap-3 justify-end">
                    <PermGate perms={[OA_PERMS.notice.edit]}>
                      <button onClick={() => openEdit(row)} className="text-indigo-600 text-xs font-black hover:underline">编辑</button>
                    </PermGate>
                    {row.status !== 1 && (
                      <PermGate perms={[OA_PERMS.notice.publish]}>
                        <button onClick={() => void handlePublish(row.id!)} className="text-emerald-600 text-xs font-black hover:underline">发布</button>
                      </PermGate>
                    )}
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

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editId ? '编辑公告' : '新建公告'} maxWidth="lg">
        <div className="space-y-6">
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">标题</label>
            <input value={title} onChange={e => setTitle(e.target.value)} className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          </div>
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">内容</label>
            <textarea value={content} onChange={e => setContent(e.target.value)} rows={5}
              className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 resize-none" />
          </div>
          <div className="flex gap-4">
            <button onClick={() => setFormOpen(false)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
            <button onClick={() => void handleSubmit()} disabled={submitting}
              className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs hover:bg-indigo-500 transition-all disabled:opacity-60">
              {submitting ? '提交中...' : '保存'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
