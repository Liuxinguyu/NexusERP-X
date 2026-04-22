import { useCallback, useEffect, useState } from 'react'
import { taskApi } from '../../api/oa-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import type { TaskRow, TaskComment } from '../../types/oa-crud'

export default function TaskManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const [all, setAll] = useState<TaskRow[]>([])
  const [loading, setLoading] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [desc, setDesc] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [detailTask, setDetailTask] = useState<TaskRow | null>(null)
  const [comments, setComments] = useState<TaskComment[]>([])
  const [newComment, setNewComment] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await taskApi.page({ current: 1, size: 200 })
      setAll(pickPageRecords(res))
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载任务列表失败')
      setAll([])
    }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { void loadData() }, [loadData])

  const cols = [
    { label: '待办', status: '0', color: 'border-amber-400' },
    { label: '进行中', status: '1', color: 'border-blue-400' },
    { label: '已完成', status: '2', color: 'border-emerald-400' },
  ]

  const handleCreate = async () => {
    if (!title.trim()) return
    setSubmitting(true)
    try {
      await taskApi.create({ title, description: desc })
      setFormOpen(false); setTitle(''); setDesc(''); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '创建失败') }
    finally { setSubmitting(false) }
  }

  const handleAction = async (id: number, action: 'accept' | 'complete' | 'cancel') => {
    const ok = await confirm({
      title: action === 'cancel' ? '取消任务' : action === 'complete' ? '完成任务' : '接受任务',
      message: '确认执行此操作？',
      danger: action === 'cancel',
    })
    if (!ok) return
    try {
      if (action === 'accept') await taskApi.accept(id)
      else if (action === 'complete') await taskApi.complete(id)
      else await taskApi.cancel(id)
      void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
  }

  const openDetail = async (task: TaskRow) => {
    setDetailTask(task)
    try { setComments(await taskApi.getComments(task.id!)) } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载评论失败')
      setComments([])
    }
  }

  const handleAddComment = async () => {
    if (!detailTask || !newComment.trim()) return
    try {
      await taskApi.addComment(detailTask.id!, { content: newComment })
      setNewComment('')
      setComments(await taskApi.getComments(detailTask.id!))
    } catch (e) { toast.error(e instanceof Error ? e.message : '评论失败') }
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-between items-center">
        <h3 className="font-black text-slate-900 uppercase tracking-wider">任务看板</h3>
        <button onClick={() => setFormOpen(true)} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建任务</button>
      </div>
      {loading && <div className="text-center text-slate-300 font-black py-8">加载中...</div>}
      <div className="grid grid-cols-3 gap-6">
        {cols.map(col => (
          <div key={col.status} className={`bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden border-t-4 ${col.color}`}>
            <div className="p-6 border-b border-slate-50">
              <div className="flex justify-between items-center">
                <span className="text-xs font-black text-slate-900 uppercase">{col.label}</span>
                <span className="text-[10px] font-black text-slate-300">{all.filter(t => String(t.status) === col.status).length}</span>
              </div>
            </div>
            <div className="p-4 space-y-3 max-h-[60vh] overflow-y-auto">
              {all.filter(t => String(t.status) === col.status).map(task => (
                <div key={task.id} onClick={() => openDetail(task)}
                  className="p-4 bg-slate-50 rounded-2xl cursor-pointer hover:bg-white hover:ring-1 ring-slate-200 hover:shadow-sm transition-all">
                  <div className="text-xs font-black text-slate-900 mb-2">{task.title}</div>
                  {task.assigneeName && <div className="text-[10px] text-slate-400 mb-2">{task.assigneeName}</div>}
                  {task.deadline && <div className="text-[10px] text-slate-300">{task.deadline}</div>}
                  <div className="flex gap-2 mt-3">
                    {col.status === '0' && <button onClick={e => { e.stopPropagation(); handleAction(task.id!, 'accept') }} className="text-[10px] font-black text-indigo-600 hover:underline">接受</button>}
                    {col.status === '1' && <button onClick={e => { e.stopPropagation(); handleAction(task.id!, 'complete') }} className="text-[10px] font-black text-emerald-600 hover:underline">完成</button>}
                    {(col.status === '0' || col.status === '1') && <button onClick={e => { e.stopPropagation(); handleAction(task.id!, 'cancel') }} className="text-[10px] font-black text-rose-400 hover:underline">取消</button>}
                  </div>
                </div>
              ))}
              {all.filter(t => String(t.status) === col.status).length === 0 && (
                <div className="text-center text-slate-200 text-xs font-bold py-8">空</div>
              )}
            </div>
          </div>
        ))}
      </div>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title="新建任务" maxWidth="md">
        <div className="space-y-6">
          <input value={title} onChange={e => setTitle(e.target.value)} placeholder="任务标题"
            className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          <textarea value={desc} onChange={e => setDesc(e.target.value)} placeholder="任务描述" rows={3}
            className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 resize-none" />
          <div className="flex gap-4">
            <button onClick={() => setFormOpen(false)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
            <button onClick={handleCreate} disabled={submitting}
              className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs hover:bg-indigo-500 transition-all disabled:opacity-60">
              {submitting ? '创建中...' : '创建'}
            </button>
          </div>
        </div>
      </Modal>

      <Modal open={!!detailTask} onClose={() => setDetailTask(null)} title={detailTask?.title ?? ''} maxWidth="lg">
        <div className="space-y-6">
          {detailTask?.description && <p className="text-sm text-slate-500">{detailTask.description}</p>}
          <div className="border-t border-slate-100 pt-4">
            <h4 className="text-[10px] font-black text-slate-400 uppercase mb-3">评论</h4>
            {comments.length === 0 ? (
              <div className="text-xs text-slate-300 font-bold">暂无评论</div>
            ) : comments.map(c => (
              <div key={c.id} className="p-3 bg-slate-50 rounded-xl mb-2">
                <div className="text-[10px] font-black text-slate-400">{c.userName} · {c.createTime}</div>
                <div className="text-xs text-slate-700 mt-1">{c.content}</div>
              </div>
            ))}
            <div className="flex gap-2 mt-3">
              <input value={newComment} onChange={e => setNewComment(e.target.value)} placeholder="写评论..."
                className="flex-1 px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-xs font-bold focus:ring-indigo-500" />
              <button onClick={handleAddComment} className="px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-black hover:bg-indigo-500 transition-all">发送</button>
            </div>
          </div>
          <button onClick={() => setDetailTask(null)} className="w-full py-3 bg-slate-100 rounded-2xl font-black text-xs text-slate-400">关闭</button>
        </div>
      </Modal>
    </div>
  )
}
