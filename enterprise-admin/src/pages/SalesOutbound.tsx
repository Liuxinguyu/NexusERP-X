import { useState, useMemo } from 'react';

// --- 模拟后端返回的数据结构 ---
type Product = { id: string; name: string; sku: string; price: number; stock: number };
const inventory: Product[] = [
  { id: 'P001', name: 'MacBook Pro 16"', sku: 'MBP-16-M3', price: 2499, stock: 45 },
  { id: 'P002', name: 'Magic Keyboard', sku: 'MK-WHT-01', price: 149, stock: 120 },
  { id: 'P003', name: 'Studio Display 27"', sku: 'SD-27-5K', price: 1599, stock: 12 },
];

export default function SalesOutbound() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const outboundList = [
    { id: 'OUT-20260413-001', customer: 'Acme Corp', amount: 5147, status: '已出库', date: '2026-04-13', operator: 'Admin' },
    { id: 'OUT-20260413-002', customer: 'Global Tech', amount: 2499, status: '待发货', date: '2026-04-12', operator: '张三' },
    { id: 'OUT-20260410-008', customer: '星辰科技', amount: 12850, status: '已取消', date: '2026-04-10', operator: '李四' },
  ];

  return (
    <div className="min-h-full bg-slate-50/50 p-2 font-sans">
      {/* --- 顶部操作区 --- */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">销售出库单</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">管理企业商品出库、打单与发货全流程。</p>
        </div>
        <button 
          onClick={() => setIsModalOpen(true)}
          className="flex items-center gap-2 rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-slate-900/20 transition-all hover:bg-slate-800 hover:shadow-lg hover:shadow-slate-900/30 active:scale-95"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          新建出库单
        </button>
      </div>

      {/* --- 列表与工具栏卡片 --- */}
      <div className="flex flex-col rounded-2xl bg-white shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 overflow-hidden">
        
        {/* 工具栏 */}
        <div className="flex items-center justify-between border-b border-slate-100 p-5 bg-white/50">
          <div className="relative w-80">
            <svg className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input 
              type="text" 
              placeholder="搜索单号或客户名称..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-lg border-0 bg-slate-50 py-2 pl-10 pr-4 text-sm text-slate-900 ring-1 ring-inset ring-slate-200 transition-all placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-blue-600 outline-none"
            />
          </div>
          <div className="flex gap-2">
            <button className="rounded-lg bg-white px-4 py-2 text-sm font-semibold text-slate-700 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50">筛选</button>
            <button className="rounded-lg bg-white px-4 py-2 text-sm font-semibold text-slate-700 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50">导出</button>
          </div>
        </div>

        {/* 高级表格 */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50/50">
              <tr className="border-b border-slate-100 text-slate-500">
                <th className="px-6 py-4 font-semibold">单据编号</th>
                <th className="px-6 py-4 font-semibold">客户名称</th>
                <th className="px-6 py-4 font-semibold">出库日期</th>
                <th className="px-6 py-4 font-semibold">制单人</th>
                <th className="px-6 py-4 font-semibold">当前状态</th>
                <th className="px-6 py-4 text-right font-semibold">订单总额</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {outboundList.map((order) => (
                <tr key={order.id} className="transition-colors hover:bg-slate-50/80 cursor-pointer group">
                  <td className="px-6 py-4 font-medium text-slate-900 group-hover:text-blue-600 transition-colors">{order.id}</td>
                  <td className="px-6 py-4 text-slate-600">{order.customer}</td>
                  <td className="px-6 py-4 text-slate-500">{order.date}</td>
                  <td className="px-6 py-4 text-slate-600">{order.operator}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      order.status === '已出库' ? 'bg-emerald-50 text-emerald-600 ring-1 ring-inset ring-emerald-600/20' : 
                      order.status === '待发货' ? 'bg-amber-50 text-amber-600 ring-1 ring-inset ring-amber-600/20' : 
                      'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-500/20'
                    }`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right font-bold text-slate-900">¥{order.amount.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* --- 高级磨砂玻璃弹窗：新建出库单 --- */}
      {isModalOpen && <NewOutboundModal onClose={() => setIsModalOpen(false)} />}
    </div>
  );
}

// ==========================================
// 核心复杂组件：包含全套联动的出库单表单
// ==========================================
function NewOutboundModal({ onClose }: { onClose: () => void }) {
  // 购物车状态
  const [cart, setCart] = useState<{ product: Product; qty: number }[]>([]);

  // 添加商品逻辑
  const addProduct = (prod: Product) => {
    if (cart.find(item => item.product.id === prod.id)) return; // 避免重复添加
    setCart([...cart, { product: prod, qty: 1 }]);
  };

  // 删除商品逻辑
  const removeProduct = (idx: number) => {
    setCart(cart.filter((_, index) => index !== idx));
  };

  // 更新数量逻辑
  const updateQty = (index: number, newQty: number) => {
    const newCart = [...cart];
    // 限制最小为1，最大不能超过库存
    newCart[index].qty = Math.min(Math.max(1, newQty), newCart[index].product.stock);
    setCart(newCart);
  };

  // 动态总价计算 (自动包含 10% 模拟税费)
  const totals = useMemo(() => {
    const subtotal = cart.reduce((sum, item) => sum + item.product.price * item.qty, 0);
    const tax = subtotal * 0.1; 
    return { subtotal, tax, grandTotal: subtotal + tax };
  }, [cart]);

  return (
    // Backdrop 磨砂玻璃背景
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      
      {/* 弹窗主体 */}
      <div className="flex w-full max-w-4xl max-h-[90vh] flex-col rounded-2xl bg-white shadow-2xl ring-1 ring-slate-900/5 animate-in zoom-in-95 duration-200 overflow-hidden">
        
        {/* 弹窗头部 */}
        <div className="flex items-center justify-between border-b border-slate-100 px-8 py-5 bg-slate-50/50">
          <div>
            <h2 className="text-xl font-bold text-slate-900">创建出库单</h2>
            <p className="text-sm font-medium text-slate-500 mt-1">选择商品并自动计算库存与总价</p>
          </div>
          <button onClick={onClose} className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        {/* 弹窗内容区 (可滚动) */}
        <div className="flex-1 overflow-y-auto px-8 py-6">
          
          {/* 基础信息录入 */}
          <div className="grid grid-cols-2 gap-6 mb-8">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">客户名称 <span className="text-red-500">*</span></label>
              <select className="w-full rounded-lg border-0 bg-slate-50 py-2.5 px-4 text-sm text-slate-900 ring-1 ring-inset ring-slate-200 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-blue-600 outline-none cursor-pointer">
                <option>选择合作客户...</option>
                <option>Acme Corp</option>
                <option>Global Tech</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">发货日期 <span className="text-red-500">*</span></label>
              <input type="date" className="w-full rounded-lg border-0 bg-slate-50 py-2.5 px-4 text-sm text-slate-900 ring-1 ring-inset ring-slate-200 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-blue-600 outline-none" />
            </div>
          </div>

          {/* 快捷添加商品区 */}
          <div className="mb-4 flex gap-2">
            {inventory.map(prod => (
              <button 
                key={prod.id} 
                onClick={() => addProduct(prod)}
                disabled={cart.some(item => item.product.id === prod.id)}
                className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 transition-colors hover:border-blue-600 hover:text-blue-600 disabled:opacity-50 disabled:hover:border-slate-200 disabled:hover:text-slate-600"
              >
                + 添加 {prod.name}
              </button>
            ))}
          </div>

          {/* 动态商品明细表 */}
          <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm">
            <div className="grid grid-cols-12 gap-4 border-b border-slate-200 bg-slate-50 px-4 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <div className="col-span-5">商品明细</div>
              <div className="col-span-2 text-right">单价</div>
              <div className="col-span-2 text-right">出库数量</div>
              <div className="col-span-2 text-right">小计</div>
              <div className="col-span-1 text-center">操作</div>
            </div>
            
            {cart.length === 0 ? (
              <div className="px-4 py-10 text-center text-sm font-medium text-slate-400 bg-slate-50/50">
                请从上方添加需要出库的商品
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {cart.map((item, idx) => (
                  <div key={idx} className="grid grid-cols-12 items-center gap-4 px-4 py-3 group">
                    <div className="col-span-5">
                      <div className="font-bold text-slate-900">{item.product.name}</div>
                      <div className="text-xs font-medium text-slate-500 mt-0.5">SKU: {item.product.sku} | 剩余库存: {item.product.stock}</div>
                    </div>
                    <div className="col-span-2 text-right text-sm font-semibold text-slate-700">¥{item.product.price.toLocaleString()}</div>
                    <div className="col-span-2 flex justify-end">
                      <input 
                        type="number" 
                        value={item.qty}
                        onChange={(e) => updateQty(idx, parseInt(e.target.value) || 1)}
                        className="w-20 rounded-md border-0 bg-slate-50 px-3 py-1.5 text-right text-sm font-semibold text-slate-900 ring-1 ring-inset ring-slate-200 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-blue-600 outline-none"
                      />
                    </div>
                    <div className="col-span-2 text-right text-sm font-bold text-blue-600">
                      ¥{(item.product.price * item.qty).toLocaleString()}
                    </div>
                    <div className="col-span-1 flex justify-center">
                      <button onClick={() => removeProduct(idx)} className="text-slate-300 hover:text-red-500 transition-colors p-1">
                        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 弹窗底部汇总与操作 */}
        <div className="border-t border-slate-100 bg-slate-50 px-8 py-5 flex items-end justify-between">
          <div className="text-sm font-medium text-slate-500">
            共 <span className="font-bold text-slate-900">{cart.reduce((sum, item) => sum + item.qty, 0)}</span> 件商品
          </div>
          
          <div className="flex items-end gap-8">
            <div className="text-right">
              <div className="text-xs font-semibold text-slate-500 mb-1">金额明细</div>
              <div className="text-sm text-slate-600">合计: ¥{totals.subtotal.toLocaleString()}</div>
              <div className="text-sm text-slate-600">税费 (10%): ¥{totals.tax.toLocaleString()}</div>
            </div>
            <div className="text-right">
              <div className="text-xs font-semibold text-slate-500 mb-1">应收总额</div>
              <div className="text-3xl font-black tracking-tight text-blue-600">
                ¥{totals.grandTotal.toLocaleString()}
              </div>
            </div>
          </div>
        </div>
        
        <div className="bg-white px-8 py-4 border-t border-slate-100 flex justify-end gap-3 rounded-b-2xl">
          <button onClick={onClose} className="rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50">取消</button>
          <button className="rounded-xl bg-blue-600 px-6 py-2.5 text-sm font-semibold text-white shadow-md shadow-blue-500/20 transition-all hover:bg-blue-700 hover:shadow-lg hover:shadow-blue-600/30 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed" disabled={cart.length === 0}>
            提交出库并生成单据
          </button>
        </div>

      </div>
    </div>
  );
}
