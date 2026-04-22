import { useState } from 'react';

export default function SystemSettings() {
  const [activeDept, setActiveDept] = useState('dept-1');

  // 模拟组织架构与用户数据
  const departments = [
    { id: 'dept-1', name: '总经办', count: 3 },
    { id: 'dept-2', name: '产品研发中心', count: 45 },
    { id: 'dept-3', name: '全国销售部', count: 28 },
    { id: 'dept-4', name: '财务与结算中心', count: 6 },
  ];

  const users = [
    { id: 'U001', name: 'Admin', account: 'admin', role: '超级管理员', phone: '138****5678', status: '正常' },
    { id: 'U002', name: '张经理', account: 'zhang_dev', role: '研发主管', phone: '139****1122', status: '正常' },
    { id: 'U003', name: '离职员工', account: 'wang_old', role: '普通员工', phone: '-', status: '停用' },
  ];

  return (
    <div className="min-h-full bg-slate-50/50 p-2 font-sans flex flex-col">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">系统设置</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">管理组织架构树与平台账号权限。</p>
        </div>
      </div>

      {/* 左右黄金分割布局 */}
      <div className="flex flex-1 gap-6 min-h-0">
        
        {/* 左侧：精美卡片式组织架构树 */}
        <div className="flex w-72 flex-col rounded-2xl bg-white shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 overflow-hidden">
          <div className="border-b border-slate-100 p-5 bg-slate-50/50 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">组织架构</h3>
            <button className="flex h-6 w-6 items-center justify-center rounded-md bg-white text-slate-500 ring-1 ring-slate-200 hover:text-blue-600"><svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg></button>
          </div>
          <div className="flex-1 overflow-y-auto p-3 space-y-1">
            {departments.map(dept => (
              <button 
                key={dept.id}
                onClick={() => setActiveDept(dept.id)}
                className={`flex w-full items-center justify-between rounded-xl px-4 py-3 transition-all ${
                  activeDept === dept.id ? 'bg-blue-50 text-blue-700 font-bold ring-1 ring-blue-500/20' : 'text-slate-600 font-semibold hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <div className="flex items-center gap-3">
                  <svg className={`h-4 w-4 ${activeDept === dept.id ? 'text-blue-500' : 'text-slate-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002 2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                  {dept.name}
                </div>
                <span className={`rounded-full px-2 py-0.5 text-xs ${activeDept === dept.id ? 'bg-blue-100/50 text-blue-600' : 'bg-slate-100 text-slate-500'}`}>{dept.count}</span>
              </button>
            ))}
          </div>
        </div>

        {/* 右侧：用户列表区 */}
        <div className="flex flex-1 flex-col rounded-2xl bg-white shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-100 p-5 bg-white/50">
             <div className="relative w-72">
               <svg className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
               <input type="text" placeholder="搜索成员姓名或账号..." className="w-full rounded-lg border-0 bg-slate-50 py-2 pl-10 pr-4 text-sm text-slate-900 ring-1 ring-inset ring-slate-200 outline-none focus:bg-white focus:ring-2 focus:ring-blue-600" />
             </div>
             <button className="rounded-xl bg-slate-900 px-5 py-2 text-sm font-semibold text-white shadow-md shadow-slate-900/10 hover:bg-slate-800">添加成员</button>
          </div>
          
          <div className="flex-1 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50/50">
                <tr className="border-b border-slate-100 text-slate-500">
                  <th className="px-6 py-4 font-semibold">账号信息</th>
                  <th className="px-6 py-4 font-semibold">角色权限</th>
                  <th className="px-6 py-4 font-semibold">联系方式</th>
                  <th className="px-6 py-4 font-semibold">账号状态</th>
                  <th className="px-6 py-4 text-right font-semibold">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((user) => (
                  <tr key={user.id} className="transition-colors hover:bg-slate-50/80 group">
                    <td className="px-6 py-4">
                      <div className="font-bold text-slate-900">{user.name}</div>
                      <div className="text-xs font-medium text-slate-500 mt-0.5">@{user.account}</div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="rounded-md bg-slate-100 px-2.5 py-1 text-xs font-bold text-slate-600">{user.role}</span>
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-600">{user.phone}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${user.status === '正常' ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'}`}>
                        <span className={`h-1.5 w-1.5 rounded-full ${user.status === '正常' ? 'bg-emerald-500' : 'bg-red-500'}`}></span>
                        {user.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right font-semibold">
                      <button className="text-blue-600 hover:text-blue-800 mr-4 transition-colors">编辑</button>
                      <button className="text-red-500 hover:text-red-700 transition-colors">停用</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
