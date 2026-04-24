import { useCallback, useEffect, useState } from 'react'
import { attendanceApi } from '../../api/oa-crud'
import { useToast } from '../../components/Toast'
import { usePermissions } from '../../context/PermissionsContext'
import { OA_PERMS } from '../../lib/business-perms'
import { formatDateTime } from '../../lib/format'
import { pickPageRecords } from '../../lib/http-helpers'
import type { TodayStatus, AttendanceRecord, AttendanceStatistics } from '../../types/oa-crud'

export default function AttendanceManage() {
  const toast = useToast()
  const { can } = usePermissions()
  const [now, setNow] = useState(new Date())
  const [todayStatus, setTodayStatus] = useState<TodayStatus>({})
  const [records, setRecords] = useState<AttendanceRecord[]>([])
  const [stats, setStats] = useState<AttendanceStatistics>({})
  const [checking, setChecking] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(timer)
  }, [])

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [today, recs, st] = await Promise.all([
        attendanceApi.myToday().catch(() => {
          toast.error('加载今日打卡状态失败')
          return {} as TodayStatus
        }),
        attendanceApi.recordsPage({ current: 1, size: 10 }).then(r => pickPageRecords(r)).catch(() => {
          toast.error('加载考勤记录失败')
          return []
        }),
        attendanceApi.monthlyStatistics({ year: now.getFullYear(), month: now.getMonth() + 1 }).catch(() => {
          toast.error('加载月度统计失败')
          return {} as AttendanceStatistics
        }),
      ])
      setTodayStatus(today); setRecords(recs); setStats(st)
    } finally { setLoading(false) }
  }, [now.getFullYear(), now.getMonth(), toast])

  useEffect(() => { void loadData() }, [loadData])

  const handleCheckIn = async () => {
    if (checking) return
    setChecking(true)
    try {
      await attendanceApi.checkIn()
      toast.success('打卡成功')
      void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '打卡失败') }
    finally { setChecking(false) }
  }

  const timeStr = now.toLocaleTimeString('zh-CN', { hour12: false })
  const dateStr = now.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })

  if (!can(OA_PERMS.attendance.view)) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="grid grid-cols-12 gap-8 animate-in slide-in-from-left duration-500">
      <div className="col-span-4 bg-white p-12 rounded-[3.5rem] shadow-sm ring-1 ring-slate-100 flex flex-col items-center">
        <div className="text-5xl font-black text-slate-900 mb-2 tracking-tighter tabular-nums">{timeStr}</div>
        <div className="text-xs font-black text-slate-300 mb-12 uppercase tracking-[0.3em]">{dateStr}</div>
        <button onClick={() => void handleCheckIn()} disabled={checking}
          className="h-52 w-52 rounded-full bg-slate-900 text-white font-black text-xl shadow-2xl ring-8 ring-slate-50 hover:bg-indigo-600 transition-all active:scale-95 group relative overflow-hidden disabled:opacity-60">
          <span className="relative z-10">{checking ? '打卡中...' : '签到打卡'}</span>
          <div className="absolute inset-0 bg-gradient-to-tr from-indigo-600 to-violet-500 opacity-0 group-hover:opacity-100 transition-opacity"></div>
        </button>
        {todayStatus.checkInTime && (
          <div className="mt-8 text-center">
            <div className="text-[10px] font-black text-slate-400 uppercase">今日签到</div>
            <div className="text-sm font-black text-emerald-600 mt-1">{formatDateTime(todayStatus.checkInTime)}</div>
            {todayStatus.checkOutTime && <div className="text-sm font-black text-emerald-600">{formatDateTime(todayStatus.checkOutTime)}</div>}
          </div>
        )}
        <div className="mt-8 flex items-center gap-2 bg-emerald-50 text-emerald-600 px-6 py-2 rounded-2xl font-black text-[10px] italic">
          <span className="h-2 w-2 bg-emerald-500 rounded-full animate-ping"></span>
          系统时钟同步中
        </div>
      </div>

      <div className="col-span-8 space-y-6">
        {/* 月统计 */}
        <div className="bg-white p-8 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
          <h4 className="font-black text-slate-900 mb-6 text-sm uppercase tracking-wider">本月统计</h4>
          <div className="grid grid-cols-4 gap-4">
            {[
              { l: '正常', v: stats.normalDays ?? 0, c: 'text-emerald-600' },
              { l: '迟到', v: stats.lateDays ?? 0, c: 'text-amber-600' },
              { l: '早退', v: stats.earlyDays ?? 0, c: 'text-orange-600' },
              { l: '缺勤', v: stats.absentDays ?? 0, c: 'text-rose-600' },
            ].map(s => (
              <div key={s.l} className="bg-slate-50 p-4 rounded-2xl text-center">
                <div className="text-[10px] font-black text-slate-400 uppercase">{s.l}</div>
                <div className={`text-2xl font-black ${s.c} mt-1`}>{s.v}</div>
              </div>
            ))}
          </div>
        </div>

        {/* 考勤记录 */}
        <div className="bg-white p-8 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
          <h4 className="font-black text-slate-900 mb-6 text-sm uppercase tracking-wider">近期考勤记录</h4>
          {loading ? (
            <div className="text-center text-slate-300 font-black py-6">加载中...</div>
          ) : records.length === 0 ? (
            <div className="text-center text-slate-300 font-black py-6">暂无记录</div>
          ) : (
            <div className="space-y-3">
              {records.map(r => (
                <div key={r.id} className="flex justify-between items-center p-4 bg-slate-50 rounded-[1.5rem] hover:bg-white hover:ring-1 ring-slate-200 transition-all">
                  <div className="flex gap-4 items-center">
                    <div className="h-10 w-10 bg-white rounded-xl flex items-center justify-center text-lg shadow-sm">🕒</div>
                    <div>
                      <div className="text-[10px] font-black text-slate-300 uppercase">{formatDateTime(r.date)}</div>
                      <div className="text-sm font-black text-slate-900">{formatDateTime(r.checkInTime)} — {formatDateTime(r.checkOutTime)}</div>
                    </div>
                  </div>
                  <span className={`px-3 py-1 rounded-xl text-[10px] font-black ${r.status === '正常' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>
                    {r.status ?? '-'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
