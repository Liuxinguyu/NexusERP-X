import { useCallback, useEffect, useState } from 'react'
import { operLogApi, unwrapPage } from '../../api/system-crud'
import type { OperLogRow } from '../../types/system-crud'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { formatDateTime } from '../../lib/format'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import Modal from '../../components/Modal'
import { useStaleGuard } from '../../hooks/useStaleGuard'

interface Query {
  current: number
  size: number
  module: string
  username: string
  status?: number
}

export default function OperLogManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()

  const [list, setList] = useState<OperLogRow[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<OperLogRow | null>(null)

  const [query, setQuery] = useState<Query>({
    current: 1,
    size: 10,
    module: '',
    username: '',
    status: undefined,
  })
  const [draft, setDraft] = useState({ module: '', username: '', status: '' })
  const guard = useStaleGuard()

  const loadData = useCallback(async (q: Query) => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const { rows, total: t } = await unwrapPage(
        operLogApi.page({
          current: q.current,
          size: q.size,
          module: q.module || undefined,
          username: q.username || undefined,
          status: q.status,
        }),
      )
      if (!guard.isCurrent(id)) return
      setList(rows)
      setTotal(t)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      setList([])
      setTotal(0)
      toast.error(e instanceof Error ? e.message : '加载操作日志失败')
    } finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [guard, toast])

  useEffect(() => {
    void loadData(query)
  }, [query, loadData])

  const handleSearch = () => {
    setQuery((prev) => ({
      ...prev,
      current: 1,
      module: draft.module.trim(),
      username: draft.username.trim(),
      status: draft.status === '' ? undefined : Number(draft.status),
    }))
  }

  const handleReset = () => {
    setQuery({ current: 1, size: 10, module: '', username: '', status: undefined })
    setDraft({ module: '', username: '', status: '' })
  }

  const changePage = (next: number) => {
    setQuery((prev) => ({ ...prev, current: next }))
  }

  const handleClean = async () => {
    const ok = await confirm({
      title: '清空操作日志',
      message: '确定要清空所有操作日志吗？此操作不可恢复。',
      danger: true,
      confirmText: '清空',
    })
    if (!ok) return
    try {
      await operLogApi.clean()
      toast.success('操作日志已清空')
      setQuery((prev) => ({ ...prev, current: 1 }))
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '清空失败')
    }
  }

  const noAccess = !can(SYSTEM_PERMS.monitor.operlog)
  if (noAccess) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：操作日志
      </div>
    )
  }

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">操作行为审计</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">安全风控中心</span>
          </div>
          <p className="mt-2 text-[11px] font-medium text-slate-400 pl-4">
            * 记录所有系统操作行为，可按模块、用户、状态检索。清空权限仅限授权管理员。
          </p>
        </div>
        <PermGate perms={[SYSTEM_PERMS.monitor.operlogRemove]}>
          <button
            type="button"
            onClick={() => void handleClean()}
            className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow-sm transition"
          >
            清空日志
          </button>
        </PermGate>
      </div>

      {/* Search Filters */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">操作模块</label>
          <input
            placeholder="搜索模块"
            value={draft.module}
            onChange={(e) => setDraft((prev) => ({ ...prev, module: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-44 font-bold focus:ring-indigo-500"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">操作账号</label>
          <input
            placeholder="搜索用户名"
            value={draft.username}
            onChange={(e) => setDraft((prev) => ({ ...prev, username: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-44 font-bold focus:ring-indigo-500"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">操作状态</label>
          <select
            value={draft.status}
            onChange={(e) => setDraft((prev) => ({ ...prev, status: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-32 font-bold focus:ring-indigo-500"
          >
            <option value="">全部</option>
            <option value={1}>成功</option>
            <option value={0}>失败</option>
          </select>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleSearch}
            className="px-5 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold shadow-sm hover:bg-indigo-700 transition"
          >
            检索追踪
          </button>
          <button
            type="button"
            onClick={handleReset}
            className="px-5 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-200 transition"
          >
            重置
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
            <tr>
              <th className="px-8 py-5">操作模块</th>
              <th className="px-8 py-5">操作类型</th>
              <th className="px-8 py-5">操作人</th>
              <th className="px-8 py-5">请求地址</th>
              <th className="px-8 py-5">操作 IP</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5">耗时</th>
              <th className="px-8 py-5">操作时间</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 relative">
            {loading ? (
              <tr><td colSpan={8} className="px-8 py-20 text-center text-slate-400 font-bold italic">检索系统深层审计中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={8} className="px-8 py-20 text-center text-slate-300 font-black text-lg italic">暂无命中该条件的操作日志</td></tr>
            ) : (
              list.map((row) => (
                <tr
                  key={row.id}
                  className="hover:bg-slate-50 transition-colors cursor-pointer"
                  onClick={() => setDetail(row)}
                >
                  <td className="px-8 py-5 text-slate-900">{row.module || '--'}</td>
                  <td className="px-8 py-5 text-slate-700 text-xs">{row.operType || '--'}</td>
                  <td className="px-8 py-5 text-slate-900">{row.username || '--'}</td>
                  <td className="px-8 py-5 text-indigo-600 text-xs tabular-nums truncate max-w-[200px]" title={row.operUrl}>{row.operUrl || '--'}</td>
                  <td className="px-8 py-5 text-indigo-600 tabular-nums text-xs">{row.operIp || '--'}</td>
                  <td className="px-8 py-5">
                    <span className={`px-2 py-1 rounded-lg text-xs ${Number(row.status) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}`}>
                      {Number(row.status) === 1 ? '成功' : '失败'}
                    </span>
                  </td>
                  <td className="px-8 py-5 text-slate-500 text-xs tabular-nums">{row.costTime != null ? `${row.costTime}ms` : '--'}</td>
                  <td className="px-8 py-5 text-slate-500 text-xs tabular-nums">{formatDateTime(row.createTime)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <div className="px-8 py-4 bg-slate-50/50 border-t border-slate-50 flex justify-between items-center text-xs font-bold text-slate-500">
          <div>共检索到 <span className="text-slate-900">{total}</span> 条操作记录</div>
          <div className="flex items-center gap-2">
            <select
              value={query.size}
              onChange={(e) =>
                setQuery((prev) => ({
                  ...prev,
                  current: 1,
                  size: Number(e.target.value),
                }))
              }
              className="px-2 py-1 bg-white rounded-lg ring-1 ring-slate-200"
            >
              <option value={10}>10条/页</option>
              <option value={20}>20条/页</option>
              <option value={50}>50条/页</option>
            </select>
            <span>第 {query.current} / {Math.max(1, Math.ceil(total / query.size))} 页</span>
            <button
              onClick={() => changePage(query.current - 1)}
              disabled={query.current <= 1}
              className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50 hover:bg-slate-50"
            >
              前页
            </button>
            <span className="px-3 py-1 text-slate-900 bg-white rounded-lg ring-1 ring-indigo-200 font-black">
              {query.current}
            </span>
            <button
              onClick={() => changePage(query.current + 1)}
              disabled={query.current >= Math.max(1, Math.ceil(total / query.size))}
              className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50 hover:bg-slate-50"
            >
              后页
            </button>
          </div>
        </div>
      </div>

      {/* Detail Modal */}
      <Modal open={!!detail} onClose={() => setDetail(null)} title="操作日志详情" maxWidth="max-w-2xl">
        {detail && (
          <div className="space-y-4 text-sm">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">操作模块</div>
                <div className="font-bold text-slate-900">{detail.module || '--'}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">操作类型</div>
                <div className="font-bold text-slate-900">{detail.operType || '--'}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">操作人</div>
                <div className="font-bold text-slate-900">{detail.username || '--'}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">操作 IP</div>
                <div className="font-bold text-indigo-600 tabular-nums">{detail.operIp || '--'}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">请求地址</div>
                <div className="font-bold text-indigo-600 text-xs break-all">{detail.operUrl || '--'}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">状态</div>
                <span className={`px-2 py-1 rounded-lg text-xs ${Number(detail.status) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}`}>
                  {Number(detail.status) === 1 ? '成功' : '失败'}
                </span>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">耗时</div>
                <div className="font-bold text-slate-900 tabular-nums">{detail.costTime != null ? `${detail.costTime}ms` : '--'}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">操作时间</div>
                <div className="font-bold text-slate-500 tabular-nums">{formatDateTime(detail.createTime)}</div>
              </div>
            </div>

            {detail.errorMsg && (
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase mb-1">错误信息</div>
                <div className="font-bold text-rose-600 text-xs">{detail.errorMsg}</div>
              </div>
            )}

            <div>
              <div className="text-[10px] font-black text-slate-400 uppercase mb-1">请求参数</div>
              <pre className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-xs font-mono text-slate-700 overflow-auto max-h-48 whitespace-pre-wrap break-all">
                {detail.requestParam || '--'}
              </pre>
            </div>
            <div>
              <div className="text-[10px] font-black text-slate-400 uppercase mb-1">响应数据</div>
              <pre className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-xs font-mono text-slate-700 overflow-auto max-h-48 whitespace-pre-wrap break-all">
                {detail.responseData || '--'}
              </pre>
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={() => setDetail(null)}
                className="px-5 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-200 transition"
              >
                关闭
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
