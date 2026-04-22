import { useCallback, useEffect, useState } from 'react'
import { postApi, unwrapPage } from '../../api/system-crud'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { useToast } from '../../components/Toast'
import { PermGate } from '../../context/PermissionsContext'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import type { PostRow } from '../../types/system-crud'

export default function PostManage() {
  const toast = useToast()
  const confirm = useConfirm()

  const [list, setList] = useState<PostRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [size] = useState(10)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // --- search ---
  const [postCodeQ, setPostCodeQ] = useState('')
  const [postNameQ, setPostNameQ] = useState('')
  const [searchNonce, setSearchNonce] = useState(0)

  // --- modal ---
  const [modal, setModal] = useState<{ open: boolean; row: PostRow | null }>({
    open: false,
    row: null,
  })
  const [form, setForm] = useState({
    postCode: '',
    postName: '',
    sort: 0,
    status: 1,
  })
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { rows, total: t } = await unwrapPage(
        postApi.page({
          current,
          size,
          postCode: postCodeQ.trim() || undefined,
          postName: postNameQ.trim() || undefined,
        }),
      )
      setList(rows)
      setTotal(t)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载岗位失败')
    } finally {
      setLoading(false)
    }
  }, [current, size, postCodeQ, postNameQ, searchNonce])

  useEffect(() => {
    void load()
  }, [load])

  const openEdit = async (row: PostRow | null) => {
    setFormErrors({})
    if (row?.id != null) {
      try {
        const detail = await postApi.get(row.id)
        setForm({
          postCode: String(detail.postCode ?? ''),
          postName: String(detail.postName ?? ''),
          sort: Number(detail.sort ?? 0),
          status: Number(detail.status ?? 1),
        })
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '获取岗位详情失败')
        return
      }
      setModal({ open: true, row })
    } else {
      setForm({ postCode: '', postName: '', sort: 0, status: 1 })
      setModal({ open: true, row: null })
    }
  }

  const save = async () => {
    const errors: Record<string, string> = {}
    if (!form.postCode.trim()) errors.postCode = '岗位编码不能为空'
    if (!form.postName.trim()) errors.postName = '岗位名称不能为空'
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) return

    const body: Partial<PostRow> = {
      postCode: form.postCode,
      postName: form.postName,
      sort: form.sort,
      status: form.status,
    }
    try {
      if (modal.row?.id != null) {
        await postApi.update(modal.row.id, body)
      } else {
        await postApi.create(body)
      }
      setModal({ open: false, row: null })
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存失败')
    }
  }

  const remove = async (row: PostRow) => {
    if (row.id == null) return
    const name = row.postName ?? row.postCode ?? String(row.id)
    const ok = await confirm({
      title: '删除岗位',
      message: `确定删除岗位「${name}」？此操作不可撤销。`,
      danger: true,
    })
    if (!ok) return
    try {
      await postApi.remove(row.id)
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const handleSearch = () => {
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  const handleReset = () => {
    setPostCodeQ('')
    setPostNameQ('')
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">岗位管理</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">系统全局视图</span>
          </div>
        </div>
        <PermGate perms={[SYSTEM_PERMS.post.add]}>
          <button
            type="button"
            onClick={() => void openEdit(null)}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
          >
            + 新增岗位
          </button>
        </PermGate>
      </div>

      {/* Search bar */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">岗位编码</label>
          <input
            placeholder="搜索编码"
            value={postCodeQ}
            onChange={(e) => setPostCodeQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">岗位名称</label>
          <input
            placeholder="搜索名称"
            value={postNameQ}
            onChange={(e) => setPostNameQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <PermGate perms={[SYSTEM_PERMS.post.query]}>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleSearch}
              className="px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold"
            >
              查询
            </button>
            <button
              type="button"
              onClick={handleReset}
              className="px-4 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-600"
            >
              重置
            </button>
          </div>
        </PermGate>
      </div>

      {error ? (
        <div className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">
          {error}
        </div>
      ) : null}

      {/* Table */}
      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
            <tr>
              <th className="px-8 py-5">岗位编码</th>
              <th className="px-8 py-5">岗位名称</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5">排序</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-8 py-8 text-center text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : list.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-8 py-8 text-center text-slate-300">
                  暂无数据
                </td>
              </tr>
            ) : (
              list.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80">
                  <td className="px-8 py-5 text-slate-500 font-mono text-xs">{r.postCode}</td>
                  <td className="px-8 py-5">{r.postName}</td>
                  <td className="px-8 py-5">
                    <span
                      className={`px-2 py-1 rounded-lg text-xs ${
                        Number(r.status) === 1
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {Number(r.status) === 1 ? '正常' : '停用'}
                    </span>
                  </td>
                  <td className="px-8 py-5 text-slate-400 text-xs">{r.sort ?? '—'}</td>
                  <td className="px-8 py-5 text-right space-x-2">
                    <PermGate perms={[SYSTEM_PERMS.post.edit]}>
                      <button
                        type="button"
                        onClick={() => void openEdit(r)}
                        className="text-amber-600 hover:text-amber-700 text-xs font-bold"
                      >
                        编辑
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.post.remove]}>
                      <button
                        type="button"
                        onClick={() => void remove(r)}
                        className="text-rose-600 hover:text-rose-700 text-xs font-bold"
                      >
                        删除
                      </button>
                    </PermGate>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <div className="p-4 flex justify-between items-center border-t border-slate-50 text-xs text-slate-500">
          <span>共 {total} 条</span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={current <= 1}
              onClick={() => setCurrent((c) => Math.max(1, c - 1))}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={current * size >= total}
              onClick={() => setCurrent((c) => c + 1)}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      {/* Edit / Create Modal */}
      <Modal
        open={modal.open}
        onClose={() => setModal({ open: false, row: null })}
        title={modal.row ? '编辑岗位' : '新增岗位'}
      >
        <div className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">
              岗位编码 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.postCode}
              onChange={(e) => setForm((f) => ({ ...f, postCode: e.target.value }))}
              className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 text-sm font-bold focus:ring-indigo-500 outline-none transition ${
                formErrors.postCode ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.postCode && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.postCode}</p>
            )}
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">
              岗位名称 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.postName}
              onChange={(e) => setForm((f) => ({ ...f, postName: e.target.value }))}
              className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 text-sm font-bold focus:ring-indigo-500 outline-none transition ${
                formErrors.postName ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.postName && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.postName}</p>
            )}
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">排序</label>
            <input
              type="number"
              value={form.sort}
              onChange={(e) => setForm((f) => ({ ...f, sort: Number(e.target.value) }))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">状态</label>
            <select
              value={form.status}
              onChange={(e) => setForm((f) => ({ ...f, status: Number(e.target.value) }))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            >
              <option value={1}>正常</option>
              <option value={0}>停用</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setModal({ open: false, row: null })}
              className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
            >
              取消
            </button>
            <button
              type="button"
              onClick={() => void save()}
              className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
              保存
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
