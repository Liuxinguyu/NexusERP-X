import { useState } from 'react';

export default function CustomerList() {
  const [searchTerm, setSearchTerm] = useState('');

  // 模拟 CRM 详尽客户数据
  const customers = [
    { id: 'C-001', name: 'Acme Corporation', industry: 'SaaS / 软件', contact: '王老板', title: 'CEO', phone: '138-0000-1111', level: 'VIP', status: '成单', date: '2026-04-12', logo: 'A' },
    { id: 'C-002', name: 'Global Tech Inc.', industry: '跨境电商', contact: '李总', title: '采购总监', phone: '139-1111-2222', level: '普通', status: '跟进中', date: '2026-04-10', logo: 'G' },
    { id: 'C-003', name: '星辰科技有限责任公司', industry: '人工智能', contact: '赵经理', title: '技术负责人', phone: '137-2222-3333', level: '重点', status: '初期沟通', date: '2026-04-13', logo: '星' },
    { id: 'C-004', name: '蓝海物流', industry: '供应链', contact: '陈主管', title: '运营主管', phone: '136-4444-5555', level: '普通', status: '流失', date: '2026-03-25', logo: '蓝' },
  ];

  const filtered = customers.filter(c => c.name.includes(searchTerm) || c.contact.includes(searchTerm));

  return (
    <div className="min-h-full bg-slate-50/50 p-2 font-sans flex flex-col">
      {/* --- 顶部区域 --- */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">客户档案库</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">维护企业核心客户资产与销售生命周期。</p>
        </div>
        <button className="flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-blue-600/20 transition-all hover:bg-blue-700 hover:shadow-lg hover:shadow-blue-600/30 active:scale-95">
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
             <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          新增企业客户
        </button>
      </div>

      {/* --- 主体表格卡片 --- */}
      <div className="flex flex-1 flex-col rounded-2xl bg-white shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 overflow-hidden">
        
        {/* 工具栏：搜索与高级筛选 */}
        <div className="flex items-center justify-between border-b border-slate-100 p-5 bg-white/50">
          <div className="flex items-center gap-4">
             <div className="relative w-80">
               <svg className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                 <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
               </svg>
               <input 
                 type="text" 
                 placeholder="搜索企业名称、联系人..." 
                 value={searchTerm}
                 onChange={(e) => setSearchTerm(e.target.value)}
                 className="w-full rounded-lg border-0 bg-slate-50 py-2 pl-10 pr-4 text-sm text-slate-900 ring-1 ring-inset ring-slate-200 transition-all placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-blue-600 outline-none"
               />
             </div>
             <span className="text-sm font-medium text-slate-400">共找到 {filtered.length} 家企业</span>
          </div>
          <div className="flex gap-2">
            <button className="flex items-center gap-2 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-slate-700 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50">
               <svg className="h-4 w-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" /></svg>
               高级筛选
            </button>
          </div>
        </div>

        {/* 客户明细表格 */}
        <div className="flex-1 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50/50">
              <tr className="border-b border-slate-100 text-slate-500">
                <th className="px-6 py-4 font-semibold">企业名称</th>
                <th className="px-6 py-4 font-semibold">关键联系人</th>
                <th className="px-6 py-4 font-semibold">客户级别</th>
                <th className="px-6 py-4 font-semibold">生命周期状态</th>
                <th className="px-6 py-4 text-right font-semibold">最后跟进时间</th>
                <th className="px-6 py-4 text-center font-semibold">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {filtered.map((customer) => (
                <tr key={customer.id} className="transition-colors hover:bg-slate-50/80 group">
                  {/* 企业名称带图标 */}
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 font-bold text-slate-500">
                        {customer.logo}
                      </div>
                      <div>
                        <div className="font-bold text-slate-900 group-hover:text-blue-600 transition-colors cursor-pointer">{customer.name}</div>
                        <div className="text-xs font-medium text-slate-500 mt-0.5">{customer.industry}</div>
                      </div>
                    </div>
                  </td>
                  {/* 联系人信息 */}
                  <td className="px-6 py-4">
                    <div className="font-semibold text-slate-700">{customer.contact} <span className="text-xs font-medium text-slate-400 ml-1">({customer.title})</span></div>
                    <div className="text-xs font-medium text-slate-500 mt-0.5">{customer.phone}</div>
                  </td>
                  {/* 客户级别 Badge */}
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-bold ${
                      customer.level === 'VIP' ? 'bg-red-50 text-red-600 ring-1 ring-inset ring-red-600/10' : 
                      customer.level === '重点' ? 'bg-indigo-50 text-indigo-600 ring-1 ring-inset ring-indigo-600/10' : 
                      'bg-slate-100 text-slate-600'
                    }`}>
                      {customer.level}
                    </span>
                  </td>
                  {/* 状态指示器 */}
                  <td className="px-6 py-4">
                     <div className="flex items-center gap-2">
                        <div className={`h-2 w-2 rounded-full ${
                           customer.status === '成单' ? 'bg-emerald-500' :
                           customer.status === '跟进中' ? 'bg-blue-500' :
                           customer.status === '初期沟通' ? 'bg-amber-500' : 'bg-slate-300'
                        }`}></div>
                        <span className="font-semibold text-slate-700">{customer.status}</span>
                     </div>
                  </td>
                  {/* 日期 */}
                  <td className="px-6 py-4 text-right font-medium text-slate-500">{customer.date}</td>
                  {/* 操作按钮 (悬浮显示) */}
                  <td className="px-6 py-4 text-center">
                    <button className="opacity-0 group-hover:opacity-100 text-sm font-semibold text-blue-600 hover:text-blue-800 transition-all bg-blue-50 px-3 py-1.5 rounded-lg">
                      写跟进
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
