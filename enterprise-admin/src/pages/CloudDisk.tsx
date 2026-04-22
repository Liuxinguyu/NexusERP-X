import { useState } from 'react';

export default function CloudDisk() {
  const [currentPath] = useState(['全部文件', '2026 项目文档', 'Q2 产品研发']);

  // 模拟文件系统数据
  const files = [
    { id: '1', name: 'UI 设计图源文件', type: 'folder', size: '-', date: '10分钟前', icon: '📁', color: 'text-blue-500 bg-blue-50' },
    { id: '2', name: '产品需求文档 V2.pdf', type: 'pdf', size: '2.4 MB', date: '昨天 14:30', icon: '📄', color: 'text-red-500 bg-red-50' },
    { id: '3', name: 'Q1 营收财务报表.xlsx', type: 'excel', size: '856 KB', date: '2026-04-05', icon: '📊', color: 'text-emerald-500 bg-emerald-50' },
    { id: '4', name: '公司团建合影.jpg', type: 'image', size: '5.1 MB', date: '2026-04-01', icon: '🖼️', color: 'text-purple-500 bg-purple-50' },
  ];

  return (
    <div className="min-h-full bg-slate-50/50 p-2 font-sans flex flex-col">
      {/* --- 顶部区域 --- */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">企业云盘</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">安全、高速的团队文档共享与协作空间。</p>
        </div>
        <div className="flex gap-3">
          <button className="rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50">新建文件夹</button>
          <button className="flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-blue-600/20 transition-all hover:bg-blue-700 active:scale-95">
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}><path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg>
            上传文件
          </button>
        </div>
      </div>

      {/* --- 面包屑与搜索条 --- */}
      <div className="mb-6 flex items-center justify-between rounded-2xl bg-white p-4 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-600">
          {currentPath.map((path, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <span className={`cursor-pointer transition-colors hover:text-blue-600 ${idx === currentPath.length - 1 ? 'text-slate-900' : ''}`}>
                {path}
              </span>
              {idx < currentPath.length - 1 && <svg className="h-4 w-4 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>}
            </div>
          ))}
        </div>
        <div className="relative w-64">
           <svg className="absolute left-3 top-2 h-4 w-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
           <input type="text" placeholder="搜索此文件夹..." className="w-full rounded-lg border-0 bg-slate-50 py-1.5 pl-9 pr-4 text-sm text-slate-900 ring-1 ring-inset ring-slate-200 outline-none focus:bg-white focus:ring-2 focus:ring-blue-600" />
        </div>
      </div>

      {/* --- 高级网格视图 (Grid) --- */}
      <div className="grid grid-cols-2 gap-6 md:grid-cols-4 lg:grid-cols-5">
        {files.map((file) => (
          <div key={file.id} className="group relative flex cursor-pointer flex-col rounded-2xl bg-white p-5 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.02)] ring-1 ring-slate-100 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-200/50 hover:ring-blue-100">
            
            {/* 图标区 */}
            <div className={`mb-4 flex h-16 w-16 items-center justify-center rounded-2xl ${file.color} transition-transform group-hover:scale-105`}>
              <span className="text-3xl drop-shadow-sm">{file.icon}</span>
            </div>

            {/* 信息区 */}
            <h3 className="truncate text-sm font-bold text-slate-900 transition-colors group-hover:text-blue-600" title={file.name}>
              {file.name}
            </h3>
            <div className="mt-1 flex items-center justify-between text-xs font-medium text-slate-400">
              <span>{file.date}</span>
              <span>{file.size}</span>
            </div>

            {/* 悬浮操作菜单 (磨砂质感) */}
            <div className="absolute inset-0 flex items-center justify-center gap-2 rounded-2xl bg-slate-900/5 backdrop-blur-[1px] opacity-0 transition-opacity group-hover:opacity-100">
              <button className="rounded-lg bg-white p-2 text-slate-700 shadow-sm ring-1 ring-slate-200 hover:text-blue-600 hover:ring-blue-200"><svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg></button>
              <button className="rounded-lg bg-white p-2 text-slate-700 shadow-sm ring-1 ring-slate-200 hover:text-red-600 hover:ring-red-200"><svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg></button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
