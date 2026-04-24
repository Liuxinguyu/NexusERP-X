import { useCallback, useEffect, useState } from 'react'
import { loginLogApi, unwrapPage } from '../../api/system-crud'
import type { LoginLogRow } from '../../types/system-crud'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { formatDateTime } from '../../lib/format'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { useStaleGuard } from '../../hooks/useStaleGuard'

type LoginLogQuery = {
  current: number
  size: number
  username?: string
  status?: number
}

export default function LoginLogManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()

  const [list, setList] = useState<LoginLogRow[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)

  // Query conditions
  const [query, setQuery] = useState<LoginLogQuery>({
    current: 1,
    size: 10,
    username: '',
    status: undefined,
  })
  const [draft, setDraft] = useState({ username: '', status: '' })
  const guard = useStaleGuard()

  const loadData = useCallback(async (q: LoginLogQuery) => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const { rows, total: t } = await unwrapPage(loginLogApi.page(q))
      if (!guard.isCurrent(id)) return
      setList(rows)
      setTotal(t)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      setList([])
      setTotal(0)
      toast.error(e instanceof Error ? e.message : '加载登录日志失败')
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
      username: draft.username.trim(),
      status: draft.status === '' ? undefined : Number(draft.status),
    }))
  }

  const handleReset = () => {
    setQuery({ current: 1, size: 10, username: '', status: undefined })
    setDraft({ username: '', status: '' })
  }

  const changePage = (nextCurrent: number) => {
    setQuery(prev => ({ ...prev, current: nextCurrent }))
  }

  const handleClean = async () => {
    const ok = await confirm({
      title: '清空登录日志',
      message: '确认清空当前租户下的全部登录日志吗？此操作不可恢复。',
      danger: true,
      confirmText: '清空',
    })
    if (!ok) return
    try {
      await loginLogApi.clean()
      toast.success('已清空登录日志')
      setQuery((prev) => ({ ...prev, current: 1 }))
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '清空失败')
    }
  }

  const noAccess = !can(SYSTEM_PERMS.monitor.loginlog)

  if (noAccess) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：登录日志
      </div>
    )
  }

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">登录行为审计</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">安全风控中心</span>
          </div>
          <p className="mt-2 text-[11px] font-medium text-slate-400 pl-4">
            * 记录所有系统成员的授权访问行为。删除权保留给系统超管。
          </p>
        </div>
        <PermGate perms={[SYSTEM_PERMS.monitor.loginlogRemove]}>
          <button
            type="button"
            onClick={() => void handleClean()}
            className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow-sm transition"
          >
            清空
          </button>
        </PermGate>
      </div>

      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
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
          <label className="text-[10px] font-black text-slate-400 uppercase">登录状态</label>
          <select
            value={draft.status}
            onChange={(e) => setDraft((prev) => ({ ...prev, status: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-32 font-bold focus:ring-indigo-500"
          >
            <option value="">全部状态</option>
            <option value={1}>✅ 成功</option>
            <option value={0}>❌ 失败</option>
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

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
            <tr>
              <th className="px-8 py-5">行为时间</th>
              <th className="px-8 py-5">登录账号</th>
              <th className="px-8 py-5">网络 IP 层</th>
              <th className="px-8 py-5">用户终端与内核</th>
              <th className="px-8 py-5">鉴权结果</th>
              <th className="px-8 py-5">反馈提示</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 relative">
            {loading ? (
              <tr><td colSpan={6} className="px-8 py-20 text-center text-slate-400 font-bold italic">检索系统深层审计中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={6} className="px-8 py-20 text-center text-slate-300 font-black text-lg italic">暂无命中该条件的登录轨迹</td></tr>
            ) : (
              list.map((log) => (
                <tr key={log.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-8 py-5 text-slate-500 text-xs tabular-nums">{formatDateTime(log.createTime)}</td>
                  <td className="px-8 py-5 text-slate-900 border-l border-transparent">{log.username}</td>
                  <td className="px-8 py-5 text-indigo-600 tabular-nums text-xs">{log.ip}</td>
                  <td className="px-8 py-5">
                    <div className="text-slate-700 text-xs max-w-[240px] truncate" title={log.userAgent}>
                      {log.userAgent ?? '--'}
                    </div>
                  </td>
                  <td className="px-8 py-5">
                     <span className={`px-2 py-1 rounded-lg text-xs ${Number(log.status) === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}`}>
                        {Number(log.status) === 1 ? '成功' : '失败'}
                     </span>
                  </td>
                  <td className="px-8 py-5 text-xs text-slate-400 max-w-[200px] truncate" title={log.msg}>{log.msg || '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* 基础分页占位（配合后端补全） */}
        <div className="px-8 py-4 bg-slate-50/50 border-t border-slate-50 flex justify-between items-center text-xs font-bold text-slate-500">
          <div> 共检索到 <span className="text-slate-900">{total}</span> 条轨迹线索 </div>
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
      
    </div>
  )
}
