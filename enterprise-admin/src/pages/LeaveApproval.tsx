import { useState } from 'react';

export default function LeaveApproval() {
  const [activeTab, setActiveTab] = useState('pending');

  // 模拟带有多维度状态的审批数据
  const [approvals, setApprovals] = useState([
    { id: 'OA-2026-001', applicant: '张三', dept: '研发部', type: '年假', days: 2, reason: '回老家探亲，处理个人家庭事务', status: 'pending', date: '2026-04-12 09:30', avatar: '张', color: 'from-blue-500 to-indigo-500' },
    { id: 'OA-2026-002', applicant: '李思思', dept: '大客户部', type: '病假', days: 1, reason: '急性肠胃炎，去市医院挂水', status: 'pending', date: '2026-04-13 08:15', avatar: '李', color: 'from-emerald-400 to-teal-500' },
    { id: 'OA-2026-003', applicant: '王五', dept: '财务部', type: '事假', days: 0.5, reason: '办理个人证件换发', status: 'approved', date: '2026-04-10 14:20', avatar: '王', color: 'from-amber-400 to-orange-500' },
  ]);

  const handleApprove = (id: string) => {
    setApprovals(approvals.map(app => app.id === id ? { ...app, status: 'approved' } : app));
  };

  const filteredList = approvals.filter(app => app.status === activeTab);

  return (
    <div className="min-h-full bg-slate-50/50 p-2 font-sans">
      {/* --- 顶部区域 --- */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">审批中心</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">处理员工请假、报销与日常流程审批。</p>
        </div>
        <button className="flex items-center gap-2 rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-slate-900/20 transition-all hover:bg-slate-800 hover:shadow-lg hover:shadow-slate-900/30 active:scale-95">
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          发起新申请
        </button>
      </div>

      {/* --- 现代化分段控制器 (Segmented Control) --- */}
      <div className="mb-6 flex w-max rounded-xl bg-slate-100/80 p-1 ring-1 ring-slate-200/50">
        <button 
          onClick={() => setActiveTab('pending')}
          className={`flex items-center gap-2 rounded-lg px-6 py-2 text-sm font-semibold transition-all ${
            activeTab === 'pending' ? 'bg-white text-slate-900 shadow-sm ring-1 ring-slate-200' : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          待我审批
          <span className={`rounded-full px-2 py-0.5 text-xs ${activeTab === 'pending' ? 'bg-red-50 text-red-600' : 'bg-slate-200 text-slate-500'}`}>
            {approvals.filter(a => a.status === 'pending').length}
          </span>
        </button>
        <button 
          onClick={() => setActiveTab('approved')}
          className={`flex items-center gap-2 rounded-lg px-6 py-2 text-sm font-semibold transition-all ${
            activeTab === 'approved' ? 'bg-white text-slate-900 shadow-sm ring-1 ring-slate-200' : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          已审批记录
        </button>
      </div>

      {/* --- 审批卡片列表 --- */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {filteredList.length === 0 ? (
           <div className="col-span-full rounded-2xl border border-dashed border-slate-300 bg-slate-50 py-20 text-center flex flex-col items-center justify-center">
             <span className="text-4xl mb-4 opacity-50">🍵</span>
             <p className="text-slate-500 font-medium">太棒了，所有审批均已处理完毕！</p>
           </div>
        ) : (
          filteredList.map(app => (
            <div key={app.id} className="group relative flex flex-col overflow-hidden rounded-2xl bg-white p-6 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-200/50">
              
              {/* 卡片头部信息 */}
              <div className="mb-4 flex items-start gap-4">
                <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr ${app.color} text-lg font-bold text-white shadow-sm`}>
                  {app.avatar}
                </div>
                <div className="flex-1 overflow-hidden">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-slate-900">{app.applicant}</h3>
                    <span className="text-xs font-semibold text-slate-400">{app.date}</span>
                  </div>
                  <p className="mt-0.5 text-xs font-medium text-slate-500">{app.dept} · {app.id}</p>
                </div>
              </div>

              {/* 核心内容区 */}
              <div className="mb-5 rounded-xl bg-slate-50 p-4 ring-1 ring-inset ring-slate-100">
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-500">申请类型</span>
                  <span className="rounded-md bg-white px-2 py-1 text-xs font-bold text-slate-700 ring-1 ring-slate-200 shadow-sm">{app.type} ({app.days} 天)</span>
                </div>
                <div className="text-sm font-medium text-slate-700 leading-relaxed">
                  "{app.reason}"
                </div>
              </div>
              
              {/* 底部操作区 */}
              <div className="mt-auto flex items-center justify-end border-t border-slate-100 pt-4">
                {app.status === 'pending' ? (
                  <div className="flex gap-3 w-full">
                    <button className="flex-1 rounded-xl bg-white py-2.5 text-sm font-semibold text-slate-600 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50 hover:text-red-600">驳回申请</button>
                    <button onClick={() => handleApprove(app.id)} className="flex-1 rounded-xl bg-slate-900 py-2.5 text-sm font-semibold text-white shadow-md shadow-slate-900/10 transition-all hover:bg-slate-800 active:scale-95">同意通过</button>
                  </div>
                ) : (
                  <div className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-50 py-2 text-emerald-600 ring-1 ring-inset ring-emerald-600/20">
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                    <span className="text-sm font-bold">审批已通过</span>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
