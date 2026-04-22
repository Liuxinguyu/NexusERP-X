import { useState } from 'react';

export default function Finance() {
  const [selectedMonth, setSelectedMonth] = useState('2026-04');

  // 模拟精细化薪酬数据
  const payrolls = [
    { id: 'PAY-001', name: '张三', dept: '研发部', base: 15000, bonus: 3500, deduct: 500, net: 18000, status: '已发放', date: '04-10' },
    { id: 'PAY-002', name: '李四', dept: '销售部', base: 8000, bonus: 12500, deduct: 0, net: 20500, status: '待发放', date: '-' },
    { id: 'PAY-003', name: '王五', dept: '人事部', base: 10000, bonus: 1000, deduct: 250, net: 10750, status: '已发放', date: '04-10' },
    { id: 'PAY-004', name: '赵六', dept: '大客户部', base: 9000, bonus: 0, deduct: 1200, net: 7800, status: '异常', date: '-' },
  ];

  return (
    <div className="min-h-full bg-slate-50/50 p-2 font-sans flex flex-col">
      {/* --- 顶部区域 --- */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">薪酬与财务中心</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">处理企业员工月度薪资发放与税务核算。</p>
        </div>
        <div className="flex gap-3">
          <button className="rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50 hover:shadow-sm">导出财务报表</button>
          <button className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-slate-900/20 transition-all hover:bg-slate-800 active:scale-95">一键发放薪资</button>
        </div>
      </div>

      {/* --- 财务大盘卡片 --- */}
      <div className="mb-6 flex items-center justify-between rounded-2xl bg-white p-6 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          </div>
          <div>
            <div className="text-sm font-semibold text-slate-500">计薪月份</div>
            <input type="month" value={selectedMonth} onChange={(e) => setSelectedMonth(e.target.value)} className="mt-1 cursor-pointer rounded-md border-0 text-lg font-bold text-slate-900 outline-none focus:ring-0 p-0" />
          </div>
        </div>
        <div className="flex gap-12">
          <div>
             <div className="text-sm font-semibold text-slate-500 mb-1">预计总支出 (元)</div>
             <div className="text-3xl font-black text-slate-900">¥ 57,050</div>
          </div>
          <div>
             <div className="text-sm font-semibold text-slate-500 mb-1">计薪总人数</div>
             <div className="text-3xl font-black text-slate-900">4 <span className="text-base font-semibold text-slate-500">人</span></div>
          </div>
        </div>
      </div>

      {/* --- 薪资明细表格 --- */}
      <div className="flex-1 rounded-2xl bg-white shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50/50">
            <tr className="border-b border-slate-100 text-slate-500">
              <th className="px-6 py-4 font-semibold">员工姓名</th>
              <th className="px-6 py-4 font-semibold">部门</th>
              <th className="px-6 py-4 text-right font-semibold">基本工资</th>
              <th className="px-6 py-4 text-right font-semibold">绩效/奖金</th>
              <th className="px-6 py-4 text-right font-semibold">社保/扣款</th>
              <th className="px-6 py-4 text-right font-semibold">实发总额</th>
              <th className="px-6 py-4 text-center font-semibold">状态</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {payrolls.map((row) => (
              <tr key={row.id} className="transition-colors hover:bg-slate-50/80 group">
                <td className="px-6 py-4 font-bold text-slate-900">{row.name} <span className="text-xs font-medium text-slate-400 ml-2 font-mono">{row.id}</span></td>
                <td className="px-6 py-4 font-medium text-slate-500">{row.dept}</td>
                <td className="px-6 py-4 text-right font-semibold text-slate-700">¥{row.base.toLocaleString()}</td>
                <td className="px-6 py-4 text-right font-bold text-emerald-600 bg-emerald-50/30">+ ¥{row.bonus.toLocaleString()}</td>
                <td className="px-6 py-4 text-right font-bold text-red-500 bg-red-50/30">- ¥{row.deduct.toLocaleString()}</td>
                <td className="px-6 py-4 text-right font-black text-slate-900 text-base">¥{row.net.toLocaleString()}</td>
                <td className="px-6 py-4">
                  <div className="flex justify-center">
                    <span className={`inline-flex items-center rounded-md px-2.5 py-1 text-xs font-bold ${
                      row.status === '已发放' ? 'bg-emerald-50 text-emerald-600 ring-1 ring-inset ring-emerald-600/20' : 
                      row.status === '异常' ? 'bg-red-50 text-red-600 ring-1 ring-inset ring-red-600/20' : 
                      'bg-slate-100 text-slate-600'
                    }`}>
                      {row.status}
                    </span>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
