import { useCallback, useEffect, useState } from 'react'
import { onlineUserApi, unwrapPage } from '../../api/system-crud'
import type { OnlineUserRow } from '../../types/system-crud'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import { useStaleGuard } from '../../hooks/useStaleGuard'

function formatTimestamp(ts?: number): string {
  if (!ts) return '--'
  try {
    const d = new Date(ts)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return '--'
  }
}

export default function OnlineUserManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()

  const [list, setList] = useState<OnlineUserRow[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)

  const [query, setQuery] = useState<{
    current: number
    size: number
    username: string
    ip: string
  }>({
    current: 1,
    size: 10,
    username: '',
    ip: '',
  })
  const [draft, setDraft] = useState({ username: '', ip: '' })
  const guard = useStaleGuard()

  const loadData = useCallback(async (q: typeof query) => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const { rows, total: t } = await unwrapPage(
        onlineUserApi.page({
          current: q.current,
          size: q.size,
          username: q.username.trim() ? q.username.trim() : undefined,
          ip: q.ip.trim() ? q.ip.trim() : undefined,
        }),
      )
      if (!guard.isCurrent(id)) return
      setList(rows)
      setTotal(t)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      setList([])
      setTotal(0)
      toast.error(e instanceof Error ? e.message : '加载在线用户失败')
    } finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [guard, toast])

  useEffect(() => {
    void loadData(query)
  }, [query, loadData])

  const handleKick = async (row: OnlineUserRow) => {
    if (row.userId == null) {
      toast.error('用户ID缺失，无法强退')
      return
    }
    const ok = await confirm({
      title: '强制下线',
      message: `确定要将用户「${row.username ?? '--'}」强制下线吗？`,
      danger: true,
      confirmText: '强退',
    })
    if (!ok) return
    try {
      await onlineUserApi.kick(row.userId)
      toast.success(`已将「${row.username}」强制下线`)
      void loadData(query)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '强退失败')
    }
  }

  const handleSearch = () => {
    setQuery((prev) => ({
      ...prev,
      current: 1,
      username: draft.username,
      ip: draft.ip,
    }))
  }

  const handleReset = () => {
    setQuery({ current: 1, size: 10, username: '', ip: '' })
    setDraft({ username: '', ip: '' })
  }

  const changePage = (next: number) => {
    setQuery((prev) => ({ ...prev, current: next }))
  }

  if (!can(SYSTEM_PERMS.monitor.online)) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：在线用户
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
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">在线用户监控</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">安全风控中心</span>
          </div>
          <p className="mt-2 text-[11px] font-medium text-slate-400 pl-4">
            * 实时展示当前在线用户列表，授权管理员可强制下线异常会话。
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadData(query)}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow-sm transition"
        >
          刷新
        </button>
      </div>

      {/* Search */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">用户名</label>
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
          <label className="text-[10px] font-black text-slate-400 uppercase">登录 IP</label>
          <input
            placeholder="搜索 IP（支持包含匹配）"
            value={draft.ip}
            onChange={(e) => setDraft((prev) => ({ ...prev, ip: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-44 font-bold focus:ring-indigo-500"
          />
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleSearch}
            className="px-5 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold shadow-sm hover:bg-indigo-700 transition"
          >
            检索
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
              <th className="px-8 py-5">用户名</th>
              <th className="px-8 py-5">登录 IP</th>
              <th className="px-8 py-5">登录时间</th>
              <th className="px-8 py-5">用户终端</th>
              <th className="px-8 py-5">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 relative">
            {loading ? (
              <tr><td colSpan={5} className="px-8 py-20 text-center text-slate-400 font-bold italic">正在获取在线用户数据...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="px-8 py-20 text-center text-slate-300 font-black text-lg italic">当前无在线用户</td></tr>
            ) : (
              list.map((row, idx) => (
                <tr key={row.userId ?? `${row.username ?? 'unknown'}-${idx}`} className="hover:bg-slate-50 transition-colors">
                  <td className="px-8 py-5 text-slate-900">{row.username || '--'}</td>
                  <td className="px-8 py-5 text-indigo-600 tabular-nums text-xs">{row.ip || '--'}</td>
                  <td className="px-8 py-5 text-slate-500 text-xs tabular-nums">{formatTimestamp(row.loginTime)}</td>
                  <td className="px-8 py-5 text-slate-700 text-xs max-w-[280px] truncate" title={row.userAgent}>{row.userAgent || '--'}</td>
                  <td className="px-8 py-5">
                    <PermGate perms={[SYSTEM_PERMS.monitor.onlineKick]}>
                      <button
                        type="button"
                        onClick={() => void handleKick(row)}
                        className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-xl text-xs font-bold transition"
                      >
                        强退
                      </button>
                    </PermGate>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <div className="px-8 py-4 bg-slate-50/50 border-t border-slate-50 flex justify-between items-center text-xs font-bold text-slate-500">
          <div>共 <span className="text-slate-900">{total}</span> 个在线用户</div>
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
