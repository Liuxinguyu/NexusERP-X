import { useEffect, useState } from 'react'
import { reportApi } from '../../api/erp-crud'
import { useToast } from '../../components/Toast'
import type { SalesMonthly, RankItem, StockAlarm } from '../../types/erp-crud'

export default function ReportView() {
  const toast = useToast()
  const [trend, setTrend] = useState<SalesMonthly[]>([])
  const [productRank, setProductRank] = useState<RankItem[]>([])
  const [customerRank, setCustomerRank] = useState<RankItem[]>([])
  const [alarms, setAlarms] = useState<StockAlarm[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const year = new Date().getFullYear()
  const month = new Date().getMonth() + 1

  useEffect(() => {
    setLoading(true)
    setError('')
    Promise.all([
      reportApi.salesTrend({ year }),
      reportApi.productRank({ year, month, limit: 10 }),
      reportApi.customerRank({ year, month, limit: 10 }),
      reportApi.stockAlarm(),
    ]).then(([t, p, c, a]) => {
      setTrend(t); setProductRank(p); setCustomerRank(c); setAlarms(a)
    }).catch(() => {
      setError('加载报表数据失败，请刷新重试')
      toast.error('加载报表数据失败')
    }).finally(() => setLoading(false))
  }, [year, month])

  const maxTrend = Math.max(...trend.map(t => t.totalAmount ?? 0), 1)

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {loading && <div className="text-center text-slate-300 font-black py-12">报表加载中...</div>}

      {error && (
        <div className="bg-rose-50 rounded-[2rem] p-6 ring-1 ring-rose-100 flex items-center justify-between">
          <span className="text-sm font-black text-rose-600">{error}</span>
          <button onClick={() => window.location.reload()} className="px-5 py-2 bg-rose-600 text-white rounded-xl text-xs font-black hover:bg-rose-500 transition-all">刷新重试</button>
        </div>
      )}

      {/* 销售趋势 */}
      <div className="bg-white rounded-[2.5rem] p-8 shadow-sm ring-1 ring-slate-100">
        <h3 className="text-sm font-black text-slate-900 mb-6 uppercase tracking-wider">年度销售趋势</h3>
        <div className="flex items-end gap-3 h-48">
          {trend.map((t, i) => (
            <div key={i} className="flex-1 flex flex-col items-center gap-2">
              <div className="w-full bg-indigo-100 rounded-t-xl transition-all hover:bg-indigo-500 group cursor-pointer"
                style={{ height: `${((t.totalAmount ?? 0) / maxTrend) * 100}%`, minHeight: 4 }}>
                <div className="opacity-0 group-hover:opacity-100 text-[9px] text-white font-black text-center pt-1 transition-opacity">
                  ¥{((t.totalAmount ?? 0) / 10000).toFixed(1)}万
                </div>
              </div>
              <span className="text-[9px] font-black text-slate-400">{t.month?.slice(5) ?? (i + 1) + '月'}</span>
            </div>
          ))}
          {trend.length === 0 && <div className="flex-1 text-center text-slate-300 font-bold text-sm">暂无趋势数据</div>}
        </div>
      </div>

      {/* 排行 */}
      <div className="grid grid-cols-2 gap-6">
        <div className="bg-white rounded-[2.5rem] p-8 shadow-sm ring-1 ring-slate-100">
          <h3 className="text-sm font-black text-slate-900 mb-4 uppercase tracking-wider">产品销售排行</h3>
          <div className="space-y-3">
            {productRank.map((r, i) => (
              <div key={i} className="flex justify-between items-center p-3 bg-slate-50 rounded-xl">
                <div className="flex items-center gap-3">
                  <span className={`h-6 w-6 rounded-lg flex items-center justify-center text-[10px] font-black ${i < 3 ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-500'}`}>{i + 1}</span>
                  <span className="text-xs font-black text-slate-900">{r.name}</span>
                </div>
                <span className="text-xs font-black text-indigo-600">¥{(r.amount ?? 0).toLocaleString()}</span>
              </div>
            ))}
            {productRank.length === 0 && <div className="text-center text-slate-300 text-xs font-bold py-4">暂无数据</div>}
          </div>
        </div>
        <div className="bg-white rounded-[2.5rem] p-8 shadow-sm ring-1 ring-slate-100">
          <h3 className="text-sm font-black text-slate-900 mb-4 uppercase tracking-wider">客户销售排行</h3>
          <div className="space-y-3">
            {customerRank.map((r, i) => (
              <div key={i} className="flex justify-between items-center p-3 bg-slate-50 rounded-xl">
                <div className="flex items-center gap-3">
                  <span className={`h-6 w-6 rounded-lg flex items-center justify-center text-[10px] font-black ${i < 3 ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-500'}`}>{i + 1}</span>
                  <span className="text-xs font-black text-slate-900">{r.name}</span>
                </div>
                <span className="text-xs font-black text-emerald-600">¥{(r.amount ?? 0).toLocaleString()}</span>
              </div>
            ))}
            {customerRank.length === 0 && <div className="text-center text-slate-300 text-xs font-bold py-4">暂无数据</div>}
          </div>
        </div>
      </div>

      {/* 库存预警 */}
      <div className="bg-white rounded-[2.5rem] p-8 shadow-sm ring-1 ring-slate-100">
        <h3 className="text-sm font-black text-slate-900 mb-4 uppercase tracking-wider">库存预警</h3>
        {alarms.length === 0 ? (
          <div className="text-center text-emerald-500 text-xs font-black py-6">所有库存正常，无预警</div>
        ) : (
          <div className="grid grid-cols-3 gap-4">
            {alarms.map((a, i) => (
              <div key={i} className="p-4 bg-rose-50 rounded-2xl ring-1 ring-rose-100">
                <div className="text-xs font-black text-slate-900">{a.productName}</div>
                <div className="text-[10px] text-slate-500 mt-1">{a.warehouseName}</div>
                <div className="flex justify-between mt-3 text-[10px] font-black">
                  <span className="text-rose-600">当前: {a.quantity}</span>
                  <span className="text-slate-400">最低: {a.minStock}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
