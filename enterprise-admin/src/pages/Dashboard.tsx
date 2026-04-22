export default function Dashboard() {
  // Mock 数据
  const stats = [
    { label: '今日营收', value: '¥24,599', trend: '+14.5%', isUp: true, icon: '💰', color: 'text-emerald-600', bg: 'bg-emerald-100' },
    { label: '新增订单', value: '142', trend: '+5.2%', isUp: true, icon: '📦', color: 'text-blue-600', bg: 'bg-blue-100' },
    { label: '待办审批', value: '12', trend: '-2.1%', isUp: false, icon: '⏳', color: 'text-amber-600', bg: 'bg-amber-100' },
    { label: '活跃客户', value: '89', trend: '+18.4%', isUp: true, icon: '👥', color: 'text-indigo-600', bg: 'bg-indigo-100' },
  ];

  const activities = [
    { id: 1, user: '张经理', action: '创建了出库单', target: 'OUT-2026-001', time: '10分钟前', avatar: '张' },
    { id: 2, user: '李财务', action: '审批通过了', target: '3月份工资单', time: '1小时前', avatar: '李' },
    { id: 3, user: '王销售', action: '新增了重点客户', target: '星辰科技', time: '2小时前', avatar: '王' },
  ];

  // 模拟平滑的渐变柱状图数据
  const chartData = [30, 45, 60, 50, 75, 90, 65, 80, 55, 70, 85, 100];
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];

  return (
    // 整个页面背景采用极微弱的蓝灰渐变，提升高级感
    <div className="min-h-full bg-gradient-to-br from-slate-50 via-white to-blue-50/30 p-2 font-sans">
      
      {/* 头部欢迎区 */}
      <div className="mb-8 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-500 text-xl font-bold text-white shadow-lg shadow-blue-500/30">
            E
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">早上好，Admin 👋</h1>
            <p className="mt-1 text-sm font-medium text-slate-500">这是您企业今天的实时运营数据。</p>
          </div>
        </div>
        <button className="rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 shadow-sm ring-1 ring-inset ring-slate-200 transition-all hover:bg-slate-50 hover:shadow">
          导出本月报告
        </button>
      </div>

      {/* 核心数据卡片区 */}
      <div className="mb-8 grid grid-cols-4 gap-6">
        {stats.map((stat, idx) => (
          <div key={idx} className="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100 transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_8px_24px_-8px_rgba(0,0,0,0.1)]">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm font-semibold text-slate-500">{stat.label}</span>
              <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${stat.bg} ${stat.color} transition-transform group-hover:scale-110`}>
                {stat.icon}
              </div>
            </div>
            <div className="flex items-baseline gap-3">
              <span className="text-3xl font-extrabold tracking-tight text-slate-900">{stat.value}</span>
              <span className={`flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${stat.isUp ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'}`}>
                {stat.trend}
              </span>
            </div>
            {/* 卡片底部的装饰性光晕 */}
            <div className="absolute -bottom-6 -right-6 h-24 w-24 rounded-full bg-gradient-to-br from-slate-100 to-transparent opacity-50 blur-2xl"></div>
          </div>
        ))}
      </div>

      {/* 图表与动态区 */}
      <div className="grid grid-cols-3 gap-6">
        
        {/* 左侧：精美的纯 CSS 趋势图 */}
        <div className="col-span-2 flex flex-col rounded-2xl bg-white p-7 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100">
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-slate-900">年度营收趋势</h2>
              <p className="text-sm text-slate-500">2026年累计营收对比</p>
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-slate-50 p-1 ring-1 ring-slate-200">
              <button className="rounded bg-white px-3 py-1.5 text-xs font-semibold text-slate-800 shadow-sm">收入</button>
              <button className="rounded px-3 py-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800">利润</button>
            </div>
          </div>
          
          <div className="flex h-64 flex-1 items-end gap-3 border-b border-slate-100 pb-2">
            {chartData.map((h, i) => (
              <div key={i} className="group relative flex flex-1 flex-col items-center justify-end h-full">
                {/* 悬浮提示框 */}
                <div className="absolute -top-10 hidden w-max rounded-lg bg-slate-900 px-3 py-2 text-xs font-semibold text-white shadow-xl group-hover:block">
                  ¥{(h * 1530).toLocaleString()}
                  <div className="absolute -bottom-1 left-1/2 h-2 w-2 -translate-x-1/2 rotate-45 bg-slate-900"></div>
                </div>
                {/* 渐变圆角柱子 */}
                <div 
                  className="w-full max-w-[48px] rounded-t-xl bg-gradient-to-t from-blue-100 to-blue-50 transition-all duration-300 group-hover:from-blue-500 group-hover:to-indigo-500"
                  style={{ height: `${h}%` }}
                ></div>
              </div>
            ))}
          </div>
          <div className="mt-3 flex justify-between text-xs font-medium text-slate-400">
            {months.map((m, i) => <span key={i} className="flex-1 text-center">{m}</span>)}
          </div>
        </div>

        {/* 右侧：实时动态 Feed */}
        <div className="flex flex-col rounded-2xl bg-white p-7 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.05)] ring-1 ring-slate-100">
          <div className="mb-6 flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900">系统动态</h2>
            <button className="text-sm font-semibold text-blue-600 hover:text-blue-700">全部动态 →</button>
          </div>
          
          <div className="relative flex-1 space-y-6 overflow-y-auto">
            {/* 左侧的时间线连接轴 */}
            <div className="absolute bottom-0 left-5 top-2 w-[2px] bg-slate-100"></div>
            
            {activities.map((activity) => (
              <div key={activity.id} className="relative flex gap-4">
                <div className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-slate-50 ring-4 ring-white">
                  <span className="text-sm font-bold text-slate-600">{activity.avatar}</span>
                </div>
                <div className="flex flex-col pt-1">
                  <p className="text-sm text-slate-600">
                    <span className="font-semibold text-slate-900">{activity.user}</span> {activity.action} <span className="font-semibold text-slate-900">{activity.target}</span>
                  </p>
                  <span className="mt-1 text-xs font-medium text-slate-400">{activity.time}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
