import { useCallback, useEffect, useState } from 'react'
import { stockApi, warehouseApi } from '../../api/erp-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import type { StockRow, WarehouseRow } from '../../types/erp-crud'

export default function StockQuery() {
  const [list, setList] = useState<StockRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 20
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [productName, setProductName] = useState('')
  const [warehouseId, setWarehouseId] = useState<number | undefined>()
  const [warehouses, setWarehouses] = useState<WarehouseRow[]>([])

  useEffect(() => {
    warehouseApi.page({ current: 1, size: 100 }).then(res => setWarehouses(pickPageRecords(res))).catch(() => {})
  }, [])

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await stockApi.page({ current, size, productName: productName.trim() || undefined, warehouseId })
      setList(pickPageRecords(res))
      setTotal(res.total ?? 0)
    } catch { setList([]); setTotal(0); setError('加载库存数据失败，请重试') }
    finally { setLoading(false) }
  }, [current, productName, warehouseId])

  useEffect(() => { void loadData() }, [loadData])

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-[2.5rem] p-6 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-4 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">产品名称</label>
          <input value={productName} onChange={e => setProductName(e.target.value)} placeholder="搜索产品"
            className="px-4 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold w-48 focus:ring-indigo-500" />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">仓库</label>
          <select value={warehouseId ?? ''} onChange={e => setWarehouseId(e.target.value ? Number(e.target.value) : undefined)}
            className="px-4 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold w-40 focus:ring-indigo-500">
            <option value="">全部仓库</option>
            {warehouses.map(w => <option key={w.id} value={w.id}>{w.warehouseName}</option>)}
          </select>
        </div>
        <button onClick={() => { setCurrent(1); void loadData() }} className="px-6 py-2 bg-indigo-600 text-white rounded-xl text-xs font-black hover:bg-indigo-500 transition-all">查询</button>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">产品名称</th>
              <th className="px-8 py-5">仓库</th>
              <th className="px-8 py-5 text-right">库存数量</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && !list.length ? (
              <tr><td colSpan={3} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : error ? (
              <tr><td colSpan={3} className="px-8 py-12 text-center">
                <div className="text-rose-500 font-black text-sm mb-3">{error}</div>
                <button onClick={() => void loadData()} className="px-5 py-2 bg-indigo-600 text-white rounded-xl text-xs font-black hover:bg-indigo-500 transition-all">重试</button>
              </td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={3} className="px-8 py-12 text-center text-slate-300 font-black">暂无库存数据</td></tr>
            ) : list.map(s => (
              <tr key={s.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{s.productName ?? '-'}</td>
                <td className="px-8 py-6 text-slate-500">{s.warehouseName ?? '-'}</td>
                <td className="px-8 py-6 text-right">
                  <span className={`text-lg font-black ${(s.quantity ?? 0) < 10 ? 'text-rose-500' : 'text-slate-900'}`}>
                    {s.quantity?.toLocaleString() ?? 0}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-8 py-4 bg-slate-50/50 border-t border-slate-50 flex justify-between items-center text-xs font-bold text-slate-500">
          <div>共 <span className="text-slate-900">{total}</span> 条</div>
          <div className="flex gap-2">
            <button onClick={() => setCurrent(p => Math.max(1, p - 1))} disabled={current <= 1} className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50 hover:bg-slate-50">前页</button>
            <span className="px-3 py-1 text-slate-900 bg-white rounded-lg ring-1 ring-indigo-200 font-black">{current}</span>
            <button onClick={() => setCurrent(p => p + 1)} disabled={current * size >= total} className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50 hover:bg-slate-50">后页</button>
          </div>
        </div>
      </div>
    </div>
  )
}
