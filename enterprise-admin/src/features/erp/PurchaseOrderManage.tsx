import { useCallback, useEffect, useState } from 'react'
import { purchaseOrderApi, supplierApi, productApi } from '../../api/erp-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import type { PurchaseOrderRow, SupplierRow, ProductRow } from '../../types/erp-crud'

const STATUS_TABS = ['全部', '草稿', '已提交', '已审批', '已入库'] as const
const STATUS_MAP: Record<string, string | undefined> = {
  '全部': undefined, '草稿': '0', '已提交': '1', '已审批': '2', '已入库': '3',
}

export default function PurchaseOrderManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const [list, setList] = useState<PurchaseOrderRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [activeTab, setActiveTab] = useState('全部')
  const [loading, setLoading] = useState(false)
  const [formOpen, setFormOpen] = useState(false)

  // form state
  const [suppliers, setSuppliers] = useState<SupplierRow[]>([])
  const [products, setProducts] = useState<ProductRow[]>([])
  const [supplierId, setSupplierId] = useState<number>(0)
  const [remark, setRemark] = useState('')
  const [items, setItems] = useState<Array<{ productId: number; quantity: number; unitPrice: number }>>([])
  const [submitting, setSubmitting] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await purchaseOrderApi.page({ current, size, status: STATUS_MAP[activeTab] })
      setList(pickPageRecords(res))
      setTotal(res.total ?? 0)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载采购订单失败')
      setList([])
      setTotal(0)
    }
    finally { setLoading(false) }
  }, [current, activeTab])

  useEffect(() => { void loadData() }, [loadData])

  const openForm = async () => {
    setFormOpen(true)
    setSupplierId(0); setRemark(''); setItems([])
    try {
      const [sRes, pRes] = await Promise.all([
        supplierApi.page({ current: 1, size: 100 }),
        productApi.page({ current: 1, size: 100 }),
      ])
      setSuppliers(pickPageRecords(sRes))
      setProducts(pickPageRecords(pRes))
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载供应商/商品失败')
    }
  }

  const addItem = () => setItems(prev => [...prev, { productId: 0, quantity: 1, unitPrice: 0 }])
  const removeItem = (idx: number) => setItems(prev => prev.filter((_, i) => i !== idx))
  const updateItem = (idx: number, field: string, val: number) =>
    setItems(prev => prev.map((it, i) => i === idx ? { ...it, [field]: val } : it))

  const handleSubmit = async () => {
    if (!supplierId || items.length === 0) { toast.error('请选择供应商并添加商品'); return }
    setSubmitting(true)
    try {
      await purchaseOrderApi.create({ supplierId, remark, items })
      setFormOpen(false); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '创建失败') }
    finally { setSubmitting(false) }
  }

  const handleAction = async (id: number, action: 'submit' | 'approve' | 'reject' | 'confirmInbound') => {
    const ok = await confirm({ title: action === 'reject' ? '驳回' : action === 'approve' ? '审批通过' : action === 'confirmInbound' ? '确认入库' : '提交', message: '确认执行此操作？' })
    if (!ok) return
    try {
      if (action === 'submit') await purchaseOrderApi.submit(id)
      else if (action === 'approve') await purchaseOrderApi.approve(id)
      else if (action === 'reject') await purchaseOrderApi.reject(id)
      else if (action === 'confirmInbound') await purchaseOrderApi.confirmInbound(id)
      void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
  }

  const statusLabel = (s: number | string | undefined) => {
    const m: Record<string, [string, string]> = {
      '0': ['草稿', 'bg-slate-100 text-slate-500'],
      '1': ['已提交', 'bg-amber-50 text-amber-600'],
      '2': ['已审批', 'bg-blue-50 text-blue-600'],
      '3': ['已入库', 'bg-emerald-50 text-emerald-600'],
    }
    const [label, cls] = m[String(s)] ?? [String(s ?? '-'), 'bg-slate-50 text-slate-400']
    return <span className={`px-2 py-1 rounded-lg text-[10px] font-black ${cls}`}>{label}</span>
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-between items-center">
        <div className="flex gap-3">
          {STATUS_TABS.map(t => (
            <button key={t} onClick={() => { setActiveTab(t); setCurrent(1) }}
              className={`px-5 py-2 rounded-2xl text-xs font-black transition-all ${activeTab === t ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200' : 'bg-white shadow-sm ring-1 ring-slate-100 text-slate-400 hover:text-indigo-600'}`}
            >{t}</button>
          ))}
        </div>
        <button onClick={openForm} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建采购单</button>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">采购单号</th>
              <th className="px-8 py-5">供应商</th>
              <th className="px-8 py-5">总额</th>
              <th className="px-8 py-5">状态</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && !list.length ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">加载中...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="px-8 py-12 text-center text-slate-300 font-black">暂无数据</td></tr>
            ) : list.map(o => (
              <tr key={o.id} className="hover:bg-slate-50 transition-all">
                <td className="px-8 py-6 font-black text-slate-900">{o.orderNo ?? o.id}</td>
                <td className="px-8 py-6 text-slate-500">{o.supplierName ?? '-'}</td>
                <td className="px-8 py-6 text-indigo-600 font-black">¥{Number(o.totalAmount ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6">{statusLabel(o.status)}</td>
                <td className="px-8 py-6 text-right">
                  <div className="flex gap-3 justify-end">
                    {String(o.status) === '0' && <button onClick={() => handleAction(o.id!, 'submit')} className="text-indigo-600 text-xs font-black hover:underline">提交</button>}
                    {String(o.status) === '1' && (
                      <>
                        <button onClick={() => handleAction(o.id!, 'approve')} className="text-indigo-600 text-xs font-black hover:underline">审批</button>
                        <button onClick={() => handleAction(o.id!, 'reject')} className="text-rose-500 text-xs font-black hover:underline">驳回</button>
                      </>
                    )}
                    {String(o.status) === '2' && <button onClick={() => handleAction(o.id!, 'confirmInbound')} className="text-emerald-600 text-xs font-black hover:underline">确认入库</button>}
                  </div>
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

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title="新建采购单" maxWidth="max-w-2xl">
        <div className="p-8 space-y-6">
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">供应商</label>
            <select value={supplierId} onChange={e => setSupplierId(Number(e.target.value))}
              className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500">
              <option value={0}>请选择</option>
              {suppliers.map(s => <option key={s.id} value={s.id}>{s.supplierName}</option>)}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">备注</label>
            <input value={remark} onChange={e => setRemark(e.target.value)} className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          </div>
          <div>
            <div className="flex justify-between items-center mb-3">
              <label className="text-[10px] font-black text-slate-400 uppercase">商品明细</label>
              <button onClick={addItem} className="text-xs font-black text-indigo-600 hover:underline">+ 添加</button>
            </div>
            {items.map((it, idx) => (
              <div key={idx} className="flex gap-3 mb-3 items-center">
                <select value={it.productId} onChange={e => updateItem(idx, 'productId', Number(e.target.value))}
                  className="flex-[2] px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-xs font-bold">
                  <option value={0}>选择产品</option>
                  {products.map(p => <option key={p.id} value={p.id}>{p.productName}</option>)}
                </select>
                <input type="number" placeholder="数量" value={it.quantity} min={1}
                  onChange={e => updateItem(idx, 'quantity', Math.max(1, Number(e.target.value)))}
                  className="flex-1 px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-xs font-bold text-right" />
                <input type="number" placeholder="单价" value={it.unitPrice} min={0}
                  onChange={e => updateItem(idx, 'unitPrice', Math.max(0, Number(e.target.value)))}
                  className="flex-1 px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-xs font-bold text-right" />
                <button onClick={() => removeItem(idx)} className="text-rose-400 text-xs font-black">删除</button>
              </div>
            ))}
          </div>
        </div>
        <div className="p-8 border-t border-slate-100 flex gap-4">
          <button onClick={() => setFormOpen(false)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
          <button onClick={handleSubmit} disabled={submitting}
            className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all disabled:opacity-60">
            {submitting ? '提交中...' : '创建采购单'}
          </button>
        </div>
      </Modal>
    </div>
  )
}
