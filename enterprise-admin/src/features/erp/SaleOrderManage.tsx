import { useCallback, useEffect, useMemo, useState } from 'react'
import { saleOrderApi, productApi, customerApi } from '../../api/erp-crud'
import { pickPageRecords } from '../../lib/http-helpers'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { ERP_PERMS } from '../../lib/business-perms'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import type { SaleOrderRow, ProductRow, CustomerRow } from '../../types/erp-crud'

const STATUS_TABS = ['全部单据', '草稿', '已提交', '已出库', '已结清'] as const
const STATUS_MAP: Record<string, string | undefined> = {
  '全部单据': undefined, '草稿': '0', '已提交': '1', '已出库': '2', '已结清': '3',
}

export default function SaleOrderManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()
  const [list, setList] = useState<SaleOrderRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const size = 10
  const [activeTab, setActiveTab] = useState<string>('全部单据')
  const [loading, setLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const guard = useStaleGuard()

  const loadData = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    try {
      const res = await saleOrderApi.page({ current, size, status: STATUS_MAP[activeTab] })
      if (!guard.isCurrent(id)) return
      setList(pickPageRecords(res))
      setTotal(res.total ?? 0)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      toast.error(e instanceof Error ? e.message : '加载销售订单失败')
      setList([])
      setTotal(0)
    }
    finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, activeTab, guard, toast])

  useEffect(() => { void loadData() }, [loadData])

  const handleTabChange = (tab: string) => { setActiveTab(tab); setCurrent(1) }

  const handleAction = async (id: number, action: 'submit' | 'approve' | 'outbound' | 'reject') => {
    const ok = await confirm({ title: action === 'reject' ? '驳回' : action === 'approve' ? '审批通过' : action === 'outbound' ? '确认出库' : '提交', message: '确认执行此操作？' })
    if (!ok) return
    try {
      if (action === 'submit') await saleOrderApi.submitById(id)
      else if (action === 'approve') await saleOrderApi.approve(id)
      else if (action === 'outbound') await saleOrderApi.outbound(id)
      else if (action === 'reject') await saleOrderApi.reject(id)
      void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '操作失败') }
  }

  const handleDelete = async (id: number) => {
    const ok = await confirm({ title: '删除销售单', message: '确认删除此销售单？', danger: true })
    if (!ok) return
    try { await saleOrderApi.remove(id); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '删除失败') }
  }

  const statusLabel = (s: number | string | undefined) => {
    const m: Record<string, [string, string]> = {
      '0': ['草稿', 'bg-slate-100 text-slate-500'],
      '1': ['已提交', 'bg-amber-50 text-amber-600'],
      '2': ['已出库', 'bg-blue-50 text-blue-600'],
      '3': ['已结清', 'bg-emerald-50 text-emerald-600'],
    }
    const [label, cls] = m[String(s)] ?? [String(s ?? '-'), 'bg-slate-50 text-slate-400']
    return <span className={`px-2 py-1 rounded-lg text-[10px] font-black ${cls}`}>{label}</span>
  }

  if (!can(ERP_PERMS.saleOrder.list)) {
    return <div className="p-8 text-center text-slate-400 font-bold">暂无权限访问</div>
  }

  return (
    <div className="space-y-6 animate-in zoom-in-95 duration-500">
      <div className="flex justify-between items-center">
        <div className="flex gap-4">
          {STATUS_TABS.map(t => (
            <button key={t} onClick={() => handleTabChange(t)}
              className={`px-6 py-2 rounded-2xl text-xs font-black transition-all ${activeTab === t ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200' : 'bg-white shadow-sm ring-1 ring-slate-100 text-slate-400 hover:text-indigo-600'}`}
            >{t}</button>
          ))}
        </div>
        <PermGate perms={[ERP_PERMS.saleOrder.add]}>
          <button onClick={() => setDrawerOpen(true)} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl text-sm font-black shadow-xl shadow-indigo-200 hover:bg-indigo-500 transition-all">+ 新建销售单</button>
        </PermGate>
      </div>

      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-900 text-white/50 text-[10px] font-black uppercase tracking-widest">
            <tr>
              <th className="px-8 py-5">销售单号</th>
              <th className="px-8 py-5">客户</th>
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
                <td className="px-8 py-6 text-slate-500">{o.customerName ?? '-'}</td>
                <td className="px-8 py-6 text-indigo-600 font-black">¥{Number(o.totalAmount ?? 0).toLocaleString()}</td>
                <td className="px-8 py-6">{statusLabel(o.status)}</td>
                <td className="px-8 py-6 text-right">
                  <div className="flex gap-3 justify-end">
                    {String(o.status) === '0' && (
                      <>
                        <PermGate perms={[ERP_PERMS.saleOrder.submit]}>
                          <button onClick={() => void handleAction(o.id!, 'submit')} className="text-indigo-600 text-xs font-black hover:underline">提交</button>
                        </PermGate>
                        <PermGate perms={[ERP_PERMS.saleOrder.remove]}>
                          <button onClick={() => void handleDelete(o.id!)} className="text-rose-500 text-xs font-black hover:underline">删除</button>
                        </PermGate>
                      </>
                    )}
                    {String(o.status) === '1' && (
                      <>
                        <PermGate perms={[ERP_PERMS.saleOrder.approve]}>
                          <button onClick={() => void handleAction(o.id!, 'approve')} className="text-indigo-600 text-xs font-black hover:underline">审批</button>
                        </PermGate>
                        <PermGate perms={[ERP_PERMS.saleOrder.reject]}>
                          <button onClick={() => void handleAction(o.id!, 'reject')} className="text-rose-500 text-xs font-black hover:underline">驳回</button>
                        </PermGate>
                      </>
                    )}
                    {String(o.status) === '2' && (
                      <PermGate perms={[ERP_PERMS.saleOrder.outbound]}>
                        <button onClick={() => void handleAction(o.id!, 'outbound')} className="text-emerald-600 text-xs font-black hover:underline">确认出库</button>
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
            <button onClick={() => setCurrent(p => Math.max(1, p - 1))} disabled={current <= 1} className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50 hover:bg-slate-50">前页</button>
            <span className="px-3 py-1 text-slate-900 bg-white rounded-lg ring-1 ring-indigo-200 font-black">{current}</span>
            <button onClick={() => setCurrent(p => p + 1)} disabled={current * size >= total} className="px-3 py-1 bg-white rounded-lg ring-1 ring-slate-200 disabled:opacity-50 hover:bg-slate-50">后页</button>
          </div>
        </div>
      </div>

      {drawerOpen && <NewOrderDrawer onClose={() => setDrawerOpen(false)} onSuccess={() => { setDrawerOpen(false); void loadData() }} />}
    </div>
  )
}

function NewOrderDrawer({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const toast = useToast()
  const [submitting, setSubmitting] = useState(false)
  const [customers, setCustomers] = useState<CustomerRow[]>([])
  const [products, setProducts] = useState<ProductRow[]>([])
  const [customerId, setCustomerId] = useState<number>(0)
  const [cart, setCart] = useState<Array<{ product: ProductRow; qty: number }>>([])
  const [productSearch, setProductSearch] = useState('')

  useEffect(() => {
    void customerApi.page({ current: 1, size: 100 }).then(r => setCustomers(pickPageRecords(r))).catch(() => {
      toast.error('加载客户列表失败')
    })
    void productApi.page({ current: 1, size: 100 }).then(r => setProducts(pickPageRecords(r))).catch(() => {
      toast.error('加载商品列表失败')
    })
  }, [toast])

  const filteredProducts = useMemo(() =>
    products.filter(p => !cart.some(c => c.product.id === p.id) &&
      (!productSearch || (p.productName ?? '').toLowerCase().includes(productSearch.toLowerCase()))),
    [products, cart, productSearch])

  const addToCart = (p: ProductRow) => setCart(prev => [...prev, { product: p, qty: 1 }])
  const removeFromCart = (idx: number) => setCart(prev => prev.filter((_, i) => i !== idx))

  const handleQtyChange = (idx: number, val: string) => {
    const newQty = Math.max(0, parseInt(val) || 0)
    setCart(prev => {
      const next = [...prev]
      const maxStock = next[idx].product.stock ?? 99999
      next[idx] = { ...next[idx], qty: Math.min(newQty, maxStock) }
      return next
    })
  }

  const totals = useMemo(() => {
    const subtotal = cart.reduce((sum, item) => sum + (item.product.unitPrice ?? 0) * item.qty, 0)
    return { subtotal, tax: subtotal * 0.13, grand: subtotal * 1.13 }
  }, [cart])

  const handleSubmit = async () => {
    if (!customerId) { toast.error('请选择客户'); return }
    if (cart.length === 0) { toast.error('请添加产品'); return }
    if (submitting) return
    setSubmitting(true)
    try {
      await saleOrderApi.submit({
        customerId,
        items: cart.map(c => ({ productId: c.product.id!, quantity: c.qty, unitPrice: c.product.unitPrice ?? 0 })),
      })
      onSuccess()
    } catch (e) { toast.error(e instanceof Error ? e.message : '提交失败') }
    finally { setSubmitting(false) }
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end" onClick={onClose}>
      <div className="absolute inset-0 bg-slate-900/30 backdrop-blur-sm" />
      <div className="relative w-full max-w-2xl bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-500" onClick={e => e.stopPropagation()}>
        <div className="p-8 bg-slate-900 text-white">
          <div className="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em] mb-2">ERP / Sale Order</div>
          <h2 className="text-2xl font-black tracking-tighter">新建销售单</h2>
        </div>

        <div className="flex-1 overflow-y-auto p-8 space-y-8">
          <section>
            <label className="text-[10px] font-black text-slate-400 uppercase mb-2 block">选择客户</label>
            <select value={customerId} onChange={e => setCustomerId(Number(e.target.value))}
              className="w-full bg-slate-50 rounded-xl px-4 py-3 text-sm font-bold ring-1 ring-slate-200 focus:ring-indigo-500">
              <option value={0}>-- 请选择 --</option>
              {customers.map(c => <option key={c.id} value={c.id}>{c.customerName}</option>)}
            </select>
          </section>

          <section>
            <label className="text-[10px] font-black text-slate-400 uppercase mb-2 block">添加产品</label>
            <input placeholder="搜索产品名称..." value={productSearch} onChange={e => setProductSearch(e.target.value)}
              className="w-full bg-slate-50 rounded-xl px-4 py-3 text-sm font-bold ring-1 ring-slate-200 focus:ring-indigo-500 mb-3" />
            <div className="max-h-40 overflow-y-auto space-y-2">
              {filteredProducts.slice(0, 20).map(p => (
                <div key={p.id} onClick={() => addToCart(p)}
                  className="flex justify-between items-center p-3 bg-slate-50 rounded-xl hover:bg-indigo-50 cursor-pointer transition-all">
                  <div>
                    <div className="text-xs font-black text-slate-900">{p.productName}</div>
                    <div className="text-[10px] text-slate-400">{p.sku} · 库存 {p.stock ?? 0}</div>
                  </div>
                  <span className="text-xs font-black text-indigo-600">¥{(p.unitPrice ?? 0).toLocaleString()}</span>
                </div>
              ))}
            </div>
          </section>

          {cart.length > 0 && (
            <section>
              <label className="text-[10px] font-black text-slate-400 uppercase mb-2 block">购物车</label>
              <div className="space-y-3">
                {cart.map((item, idx) => (
                  <div key={idx} className="flex justify-between items-center p-4 bg-slate-50 rounded-xl">
                    <div>
                      <div className="text-xs font-black text-slate-900">{item.product.productName}</div>
                      <div className="text-[10px] text-slate-400">¥{(item.product.unitPrice ?? 0).toLocaleString()} × {item.qty}</div>
                    </div>
                    <div className="flex items-center gap-3">
                      <input type="number" value={item.qty} onChange={e => handleQtyChange(idx, e.target.value)} min={0}
                        className="w-20 bg-white rounded-xl px-3 py-2 text-right text-xs font-black ring-1 ring-slate-200 focus:ring-indigo-500" />
                      <button onClick={() => removeFromCart(idx)} className="text-rose-400 text-xs font-black hover:text-rose-600">移除</button>
                    </div>
                  </div>
                ))}
                <div className="pt-4 space-y-2 border-t border-slate-100">
                  <div className="flex justify-between text-[10px] font-bold text-slate-500"><span>小计</span><span>¥{totals.subtotal.toLocaleString()}</span></div>
                  <div className="flex justify-between text-[10px] font-bold text-slate-500"><span>税 (13%)</span><span>¥{totals.tax.toLocaleString()}</span></div>
                  <div className="flex justify-between text-xl font-black text-indigo-600 pt-4"><span>合计</span><span>¥{totals.grand.toLocaleString()}</span></div>
                </div>
              </div>
            </section>
          )}
        </div>

        <div className="p-8 bg-slate-50/50 border-t border-slate-100 flex gap-4">
          <button onClick={onClose} className="flex-1 py-4 bg-white rounded-2xl font-black text-xs text-slate-400 hover:text-slate-900 transition-all uppercase">取消</button>
          <PermGate perms={[ERP_PERMS.saleOrder.add]}>
            <button onClick={() => void handleSubmit()} disabled={submitting || cart.length === 0}
              className="flex-[2] py-4 bg-indigo-600 text-white rounded-2xl font-black text-xs shadow-2xl shadow-indigo-100 hover:bg-indigo-500 transition-all uppercase flex items-center justify-center gap-2">
              {submitting ? <span className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : '提交销售单'}
            </button>
          </PermGate>
        </div>
      </div>
    </div>
  )
}
