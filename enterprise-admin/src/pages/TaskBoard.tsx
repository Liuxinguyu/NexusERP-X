import { useState } from 'react';

export default function TaskBoard() {
  // Mock 数据：看板列与任务卡片
  const [columns] = useState([
    { id: 'todo', title: '待处理 (To Do)', color: 'bg-[#F3F4F6]' },
    { id: 'in-progress', title: '进行中 (In Progress)', color: 'bg-[#EFF6FF]' },
    { id: 'done', title: '已完成 (Done)', color: 'bg-[#ECFDF5]' },
  ]);

  const [tasks] = useState([
    { id: 'T1', title: '设计登录页面 UI', col: 'todo', priority: 'High', date: '04-15', avatar: '设' },
    { id: 'T2', title: '对接 ERP 销售出库接口', col: 'in-progress', priority: 'Urgent', date: '04-14', avatar: '开' },
    { id: 'T3', title: '修复薪酬模块计算精度 Bug', col: 'in-progress', priority: 'High', date: '04-14', avatar: '开' },
    { id: 'T4', title: '整理 Q1 客户报表', col: 'done', priority: 'Normal', date: '04-10', avatar: '财' },
  ]);

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="mb-6 flex shrink-0 items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-[#111827]">任务看板</h1>
          <p className="mt-1 text-sm text-[#6B7280]">跨部门协同与敏捷任务追踪</p>
        </div>
        <div className="flex gap-3">
          <div className="flex -space-x-2 mr-4">
            {/* 模拟团队头像叠放 */}
            <div className="h-9 w-9 rounded-full border-2 border-white bg-[#111827] flex items-center justify-center text-white text-xs">张</div>
            <div className="h-9 w-9 rounded-full border-2 border-white bg-[#6B7280] flex items-center justify-center text-white text-xs">李</div>
            <div className="h-9 w-9 rounded-full border-2 border-white bg-[#D1D5DB] flex items-center justify-center text-[#111827] text-xs">+3</div>
          </div>
          <button className="rounded-md bg-[#111827] px-4 py-2 text-sm font-medium text-white hover:bg-[#374151]">
            + 新建任务
          </button>
        </div>
      </div>

      {/* 看板主区域 (横向滚动) */}
      <div className="flex flex-1 gap-6 overflow-x-auto pb-4">
        {columns.map(col => (
          <div key={col.id} className={`flex w-80 shrink-0 flex-col rounded-xl ${col.color} p-4`}>
            <div className="mb-4 flex items-center justify-between">
              <h3 className="font-medium text-[#111827]">{col.title}</h3>
              <span className="text-sm text-[#6B7280]">
                {tasks.filter(t => t.col === col.id).length}
              </span>
            </div>
            
            {/* 任务卡片列表 */}
            <div className="flex flex-col gap-3 overflow-y-auto">
              {tasks.filter(t => t.col === col.id).map(task => (
                <div key={task.id} className="group cursor-grab rounded-lg border border-[#E5E7EB] bg-white p-4 shadow-sm transition-all hover:border-[#111827] hover:shadow-md">
                  <div className="mb-2 flex items-start justify-between">
                    <span className={`rounded px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wider ${
                      task.priority === 'Urgent' ? 'bg-[#FEF2F2] text-[#DC2626]' :
                      task.priority === 'High' ? 'bg-[#FFFBEB] text-[#D97706]' : 'bg-[#F3F4F6] text-[#6B7280]'
                    }`}>
                      {task.priority}
                    </span>
                    <span className="text-xs text-[#9CA3AF] opacity-0 transition-opacity group-hover:opacity-100">···</span>
                  </div>
                  <h4 className="mb-4 text-sm font-medium text-[#111827] leading-tight">{task.title}</h4>
                  <div className="flex items-center justify-between border-t border-[#F3F4F6] pt-3">
                    <div className="flex h-6 w-6 items-center justify-center rounded-full bg-[#F3F4F6] text-xs font-medium text-[#6B7280]">
                      {task.avatar}
                    </div>
                    <span className="text-xs text-[#6B7280]">{task.date}</span>
                  </div>
                </div>
              ))}
              
              {/* 底部快速添加按钮 */}
              <button className="mt-2 flex w-full items-center justify-center rounded-lg border border-dashed border-[#D1D5DB] py-2 text-sm text-[#6B7280] transition-colors hover:border-[#111827] hover:text-[#111827]">
                + 添加卡片
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
