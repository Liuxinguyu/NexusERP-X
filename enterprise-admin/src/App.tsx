import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { approvalApi } from './api/oa-crud';
import { systemApi } from './api/system-crud';
import { authApi } from './api/auth';
import { beginPreAuthLogin, completeShopLogin } from './lib/auth-flow';
import { pickPageRecords } from './lib/http-helpers';
import { resolveComponent } from './lib/component-registry';
import { PermissionsProvider, usePermissions, type MenuNode } from './context/PermissionsContext';
import { RouteErrorBoundary } from './components/RouteErrorBoundary';
import {
  clearAuthSession,
  clearPendingLoginContext,
  getAccessToken,
  getCurrentShopId,
  getTenantId,
  setAuthSession,
  setPendingLoginContext,
} from './lib/storage';
import type { ApprovalTaskRow } from './types/oa-crud';
import type { DashboardSummary } from './types/system-crud';

// ==========================================
// Fallback menu config (used when API menus haven't loaded)
// ==========================================
const FALLBACK_MENU: MenuNode[] = [
  {
    id: -1, parentId: 0, menuType: 'M', menuName: '系统基础', icon: '⚙️', path: 'system',
    children: [
      { id: -11, parentId: -1, menuType: 'C', menuName: '用户管理', component: 'system/user' },
      { id: -12, parentId: -1, menuType: 'C', menuName: '角色管理', component: 'system/role' },
      { id: -16, parentId: -1, menuType: 'C', menuName: '菜单管理', component: 'system/menu' },
      { id: -17, parentId: -1, menuType: 'C', menuName: '组织管理', component: 'system/org' },
      { id: -18, parentId: -1, menuType: 'C', menuName: '岗位管理', component: 'system/post' },
      { id: -19, parentId: -1, menuType: 'C', menuName: '参数配置', component: 'system/config' },
      { id: -13, parentId: -1, menuType: 'C', menuName: '店铺管理', component: 'system/shop' },
      { id: -14, parentId: -1, menuType: 'C', menuName: '数据字典', component: 'system/dict' },
    ],
  },
  {
    id: -6, parentId: 0, menuType: 'M', menuName: '系统监控', icon: '📡', path: 'monitor',
    children: [
      { id: -61, parentId: -6, menuType: 'C', menuName: '登录日志', component: 'monitor/loginlog' },
      { id: -62, parentId: -6, menuType: 'C', menuName: '操作日志', component: 'monitor/operlog' },
      { id: -63, parentId: -6, menuType: 'C', menuName: '在线用户', component: 'monitor/online' },
    ],
  },
  {
    id: -2, parentId: 0, menuType: 'M', menuName: 'ERP 管理', icon: '📦', path: 'erp',
    children: [
      { id: -21, parentId: -2, menuType: 'C', menuName: '销售订单', component: 'erp/sale-order' },
      { id: -22, parentId: -2, menuType: 'C', menuName: '采购订单', component: 'erp/purchase-order' },
      { id: -23, parentId: -2, menuType: 'C', menuName: '库存查询', component: 'erp/stock' },
      { id: -24, parentId: -2, menuType: 'C', menuName: '应收应付', component: 'erp/finance' },
      { id: -25, parentId: -2, menuType: 'C', menuName: '报表分析', component: 'erp/report' },
    ],
  },
  {
    id: -3, parentId: 0, menuType: 'M', menuName: 'OA 办公', icon: '🤝', path: 'oa',
    children: [
      { id: -31, parentId: -3, menuType: 'C', menuName: '考勤打卡', component: 'oa/attendance' },
      { id: -32, parentId: -3, menuType: 'C', menuName: '审批中心', component: 'oa/approval' },
      { id: -33, parentId: -3, menuType: 'C', menuName: '任务协同', component: 'oa/task' },
      { id: -34, parentId: -3, menuType: 'C', menuName: '企业云盘', component: 'oa/cloud-disk' },
      { id: -35, parentId: -3, menuType: 'C', menuName: '内部公告', component: 'oa/notice' },
    ],
  },
  {
    id: -4, parentId: 0, menuType: 'M', menuName: '薪酬管理', icon: '💰', path: 'finance',
    children: [
      { id: -41, parentId: -4, menuType: 'C', menuName: '工资单', component: 'wage/slip' },
      { id: -42, parentId: -4, menuType: 'C', menuName: '薪酬配置', component: 'wage/config' },
    ],
  },
  {
    id: -5, parentId: 0, menuType: 'M', menuName: 'CRM 关系', icon: '👥', path: 'crm',
    children: [
      { id: -51, parentId: -5, menuType: 'C', menuName: '商机跟进', component: 'crm/opportunity' },
      { id: -52, parentId: -5, menuType: 'C', menuName: '合同管理', component: 'crm/contract' },
    ],
  },
];

const ICON_MAP: Record<string, string> = {
  system: '⚙️', monitor: '📡', erp: '📦', oa: '🤝', finance: '💰', wage: '💰', crm: '👥', settings: '🛠️',
};

function getMenuIcon(node: MenuNode): string {
  if (node.icon && !node.icon.startsWith('#')) return node.icon;
  const seg = (node.path ?? '').split('/')[0];
  return ICON_MAP[seg] ?? '📂';
}

// ==========================================
// Workbench (Dashboard) Overview
// ==========================================

const getGreeting = () => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return 'Good morning'
  if (h < 18) return 'Good afternoon'
  return 'Good evening'
}

const Workbench = ({
  revenueText,
  dashboardData,
  actionItems,
  displayName,
}: {
  revenueText: string
  dashboardData: DashboardSummary
  actionItems: Array<{ t: string; d: string; c: string }>
  displayName: string
}) => (
  <div className="min-h-full space-y-10 animate-in fade-in duration-1000">

    {/* 顶部：欢迎与核心上下文 */}
    <div className="flex items-end">
      <div>
        <div className="flex items-center gap-3 mb-2">
          <span className="h-2 w-2 rounded-full bg-indigo-500 animate-pulse"></span>
          <span className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em]">System Online: Nexus-Gateway 8080</span>
        </div>
        <h1 className="text-4xl font-black text-slate-900 tracking-tighter">
          {getGreeting()}, <span className="italic underline decoration-indigo-500 decoration-8 underline-offset-4">{displayName}.</span>
        </h1>
      </div>
    </div>

    {/* 核心大盘：Bento Grid 布局 */}
    <div className="grid grid-cols-12 gap-6">

      {/* A. 营收走势大卡片 */}
      <div className="col-span-8 bg-slate-900 rounded-[3.5rem] p-10 text-white shadow-2xl relative overflow-hidden group">
        <div className="relative z-10 flex flex-col h-full">
          <div className="flex justify-between items-start mb-12">
            <div>
              <h3 className="text-xl font-black italic tracking-tighter uppercase">Revenue Overview</h3>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-4xl font-black text-indigo-400">{revenueText}</span>
                <span className="text-xs text-emerald-400 font-bold">+14.2% ↑</span>
              </div>
            </div>
            <div className="bg-white/5 backdrop-blur-md px-4 py-2 rounded-2xl border border-white/10 text-[10px] font-black tracking-widest uppercase">
              Real-time Sync
            </div>
          </div>

          <div className="flex-1 flex items-end gap-3 px-4">
            {[40, 70, 45, 90, 65, 80, 40, 100, 50, 75, 60, 85].map((h, i) => (
              <div key={i} className="flex-1 flex flex-col justify-end group/bar cursor-pointer">
                <div className="h-full w-full bg-indigo-500/20 rounded-t-xl group-hover/bar:bg-indigo-500 transition-all duration-500" style={{ height: `${h}%` }}></div>
              </div>
            ))}
          </div>
        </div>
        <div className="absolute -bottom-20 -left-20 h-64 w-64 bg-indigo-500/10 rounded-full blur-3xl group-hover:bg-indigo-500/20 transition-all duration-1000"></div>
      </div>

      {/* B. 待办快捷处理 */}
      <div className="col-span-4 bg-white rounded-[3.5rem] p-10 shadow-sm ring-1 ring-slate-100 flex flex-col">
        <h3 className="text-lg font-black text-slate-900 mb-8 border-b border-slate-50 pb-4 italic">Action Items.</h3>
        <div className="flex-1 space-y-4">
          {actionItems.map((task, i) => (
            <div key={i} className="flex justify-between items-center p-4 bg-slate-50 rounded-2xl hover:bg-slate-100 transition-all cursor-pointer group">
              <div>
                <div className="text-xs font-black text-slate-900">{task.t}</div>
                <div className="text-[10px] font-bold text-slate-400 mt-0.5">{task.d}</div>
              </div>
              <span className={`px-2 py-1 rounded-lg text-[9px] font-black uppercase ${task.c}`}>Handle</span>
            </div>
          ))}
        </div>
        <button className="mt-8 w-full py-4 bg-slate-900 text-white rounded-2xl font-black text-xs hover:bg-indigo-600 transition-all">View All Task</button>
      </div>

      {/* 底部次级指标卡片 */}
      {[
        { l: '今日营收', v: dashboardData.revenueToday != null ? `¥${Number(dashboardData.revenueToday).toLocaleString()}` : '-', d: 'ERP', i: '💰', bg: 'bg-emerald-50' },
        { l: '今日订单', v: dashboardData.orderToday != null ? String(dashboardData.orderToday) : '-', d: 'ORDER', i: '📦', bg: 'bg-indigo-50' },
        { l: '待审批', v: dashboardData.pendingApproval != null ? String(dashboardData.pendingApproval) : '-', d: 'OA', i: '📋', bg: 'bg-amber-50' },
        { l: '活跃客户', v: dashboardData.activeCustomer != null ? String(dashboardData.activeCustomer).replace(/\B(?=(\d{3})+(?!\d))/g, ',') : '-', d: 'CRM', i: '👥', bg: 'bg-blue-50' },
      ].map((s, i) => (
        <div key={i} className="col-span-3 bg-white p-8 rounded-[3rem] shadow-sm ring-1 ring-slate-100 flex flex-col justify-between group hover:ring-indigo-500 transition-all duration-500">
          <div className="flex justify-between items-start">
            <span className={`h-12 w-12 rounded-2xl ${s.bg} flex items-center justify-center text-xl`}>{s.i}</span>
            <span className="text-[10px] font-black text-slate-300 uppercase tracking-widest">{s.d}</span>
          </div>
          <div className="mt-8">
            <div className="text-[10px] font-black text-slate-400 uppercase mb-1">{s.l}</div>
            <div className="text-2xl font-black text-slate-900">{s.v}</div>
          </div>
        </div>
      ))}

    </div>
  </div>
);


// ==========================================
// Main App + Dynamic Menu Architecture
// ==========================================

export default function App() {
  const [authenticated, setAuthenticated] = useState(
    () => Boolean(getAccessToken()),
  );

  const handleLogout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      /* 仍清本地，避免 token 残留 */
    }
    clearAuthSession();
    setAuthenticated(false);
  }, []);

  useEffect(() => {
    const onAuthExpired = () => setAuthenticated(false);
    window.addEventListener('nexus-auth-expired', onAuthExpired);
    return () => window.removeEventListener('nexus-auth-expired', onAuthExpired);
  }, []);

  if (!authenticated) {
    return <LoginScreen onLoginSuccess={() => setAuthenticated(true)} />;
  }

  return (
    <PermissionsProvider>
      <AppInner onLogout={handleLogout} />
    </PermissionsProvider>
  );
}

function AppInner({ onLogout }: { onLogout: () => void }) {
  const { user, menus: apiMenus } = usePermissions();
  const [activeGroupId, setActiveGroupId] = useState<number | null>(null);
  const [activePageId, setActivePageId] = useState<number | null>(null);
  const [isDashboard, setIsDashboard] = useState(true);
  const [revenueText, setRevenueText] = useState('-');
  const [dashboardData, setDashboardData] = useState<DashboardSummary>({});
  const [currentShopIdState, setCurrentShopIdState] = useState<number | null>(
    () => getCurrentShopId(),
  );
  const [tenantIdState, setTenantIdState] = useState<number | null>(
    () => getTenantId(),
  );
  const [shopSwitching, setShopSwitching] = useState(false);
  const shopSwitchingRef = useRef(false);
  const [actionItems, setActionItems] = useState<Array<{ t: string; d: string; c: string }>>([]);

  // Merge API menus with fallback: API menus take priority, fallback fills gaps
  const menuGroups = useMemo(() => {
    if (apiMenus.length === 0) {
      return FALLBACK_MENU.filter(m => m.menuType === 'M');
    }
    const fallbackByPath = new Map(
      FALLBACK_MENU.filter(m => m.menuType === 'M').map(m => [m.path, m]),
    );
    const apiDirs = apiMenus.filter(m => m.menuType === 'M').map(dir => {
      const cChildren = (dir.children ?? []).filter(c => c.menuType === 'C');
      if (cChildren.length > 0) return dir;
      const fb = fallbackByPath.get(dir.path);
      if (fb?.children) return { ...dir, children: fb.children };
      return dir;
    });
    const apiPaths = new Set(apiDirs.map(m => m.path));
    const missingFallbacks = FALLBACK_MENU
      .filter(m => m.menuType === 'M' && !apiPaths.has(m.path));
    return [...apiDirs, ...missingFallbacks];
  }, [apiMenus]);

  // Get visible page children (C nodes) for the active group
  const activeGroup = useMemo(
    () => menuGroups.find(m => m.id === activeGroupId) ?? null,
    [menuGroups, activeGroupId],
  );

  const pageChildren = useMemo(() => {
    if (!activeGroup?.children) return [];
    return activeGroup.children.filter(c => c.menuType === 'C');
  }, [activeGroup]);

  const activePage = useMemo(
    () => pageChildren.find(p => p.id === activePageId) ?? null,
    [pageChildren, activePageId],
  );

  // Auto-select first group and first page
  useEffect(() => {
    if (!isDashboard && activeGroupId == null && menuGroups.length > 0) {
      const first = menuGroups[0];
      setActiveGroupId(first.id);
      const firstChild = first.children?.filter(c => c.menuType === 'C')[0];
      if (firstChild) setActivePageId(firstChild.id);
    }
  }, [isDashboard, activeGroupId, menuGroups]);

  // Resolve the component for the active page
  const ActiveComponent = useMemo(() => {
    if (!activePage?.component) return null;
    return resolveComponent(activePage.component);
  }, [activePage]);

  // Extract user display info
  const displayName = String(user.realName ?? user.username ?? user.nickName ?? 'Admin');
  const displayInitials = displayName.slice(0, 2).toUpperCase();

  const userShops: Array<{ shopId: number; shopName: string }> = (() => {
    const raw = user.shops ?? user.userShops;
    if (Array.isArray(raw)) return raw as Array<{ shopId: number; shopName: string }>;
    return [];
  })();

  useEffect(() => {
    void (async () => {
      try {
        const dashboard = await systemApi.getDashboard();
        const revenue = Number(dashboard.revenueToday ?? dashboard.revenue ?? 0);
        setRevenueText(Number.isNaN(revenue) ? '-' : `¥${revenue.toLocaleString()}`);
        setDashboardData(dashboard);
      } catch {
        setRevenueText('-');
        setDashboardData({});
      }
    })();
  }, []);

  useEffect(() => {
    void (async () => {
      try {
        const tasks = await approvalApi.myApprove({ current: 1, size: 3 });
        const rows = pickPageRecords(tasks);
        const mapped = rows.slice(0, 3).map((row: ApprovalTaskRow, idx: number) => ({
          t: row.title ? `审批：${row.title}` : '审批：流程待处理',
          d: row.applicantUserName ?? `User-${idx + 1}`,
          c: idx % 2 === 0 ? 'bg-amber-50 text-amber-600' : 'bg-indigo-50 text-indigo-600',
        }));
        if (mapped.length > 0) setActionItems(mapped);
      } catch {
        setActionItems([]);
      }
    })();
  }, []);

  const handleSwitchShop = async (shopId: number) => {
    if (shopId === currentShopIdState) return;
    if (shopSwitchingRef.current) return;
    shopSwitchingRef.current = true;
    setShopSwitching(true);
    try {
      const result = await authApi.switchShop(shopId);
      setAuthSession({
        accessToken: result.accessToken,
        currentShopId: result.currentShopId,
        tenantId: result.tenantId,
      });
      setCurrentShopIdState(result.currentShopId);
      setTenantIdState(result.tenantId ?? null);
      window.location.reload();
    } catch {
        /* 切店失败保持原店铺上下文，避免写入未授权 shopId */
    } finally {
      shopSwitchingRef.current = false;
      setShopSwitching(false);
    }
  };

  const switchToGroup = (group: MenuNode) => {
    setIsDashboard(false);
    setActiveGroupId(group.id);
    const firstChild = group.children?.filter(c => c.menuType === 'C')[0];
    setActivePageId(firstChild?.id ?? null);
  };

  return (
    <div className="flex h-screen w-full bg-[#f8fafc] text-slate-900 font-sans antialiased overflow-hidden">
      <aside className="w-80 bg-white border-r border-slate-100 flex flex-col shadow-2xl z-20">
        <div className="h-24 flex items-center px-10 gap-4">
          <div className="h-12 w-12 bg-indigo-600 rounded-2xl shadow-2xl shadow-indigo-100 flex items-center justify-center text-white font-black text-2xl italic">N</div>
          <div className="flex flex-col">
            <span className="font-black text-xl tracking-tighter">NEXUS PRO</span>
            <span className="text-[9px] font-black text-slate-300 tracking-[0.4em] uppercase">Enterprise Suite</span>
          </div>
        </div>

        <nav className="flex-1 p-6 space-y-1.5 overflow-y-auto">
          {/* Dashboard button */}
          <button
            onClick={() => { setIsDashboard(true); setActiveGroupId(null); setActivePageId(null); }}
            className={`w-full flex items-center gap-4 px-6 py-4 rounded-[1.5rem] text-sm font-black transition-all duration-500 ${
              isDashboard ? 'bg-indigo-600 text-white shadow-2xl shadow-indigo-200 -translate-y-1' : 'text-slate-400 hover:bg-slate-50'
            }`}
          >
            <span className="text-xl">📊</span> 工作台概览
          </button>

          {/* Dynamic menu groups */}
          {menuGroups.map(group => (
            <button
              key={group.id}
              onClick={() => switchToGroup(group)}
              className={`w-full flex items-center gap-4 px-6 py-4 rounded-[1.5rem] text-sm font-black transition-all duration-500 ${
                !isDashboard && activeGroupId === group.id ? 'bg-indigo-600 text-white shadow-2xl shadow-indigo-200 -translate-y-1' : 'text-slate-400 hover:bg-slate-50'
              }`}
            >
              <span className="text-xl">{getMenuIcon(group)}</span> {group.menuName}
            </button>
          ))}
        </nav>

        <div className="p-6 border-t border-slate-50 bg-slate-50/50 space-y-3">
           <div className="bg-white p-4 rounded-3xl shadow-sm ring-1 ring-slate-200/50 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                 <span className="text-[10px] font-black text-slate-300">当前店铺 (X-Shop-Id)</span>
                 <span className="text-[8px] px-2 py-0.5 bg-emerald-50 text-emerald-600 rounded-lg">{shopSwitching ? 'Switching...' : 'LIVE'}</span>
              </div>
              <select
                value={currentShopIdState ?? ''}
                onChange={(e) => {
                  const v = Number(e.target.value);
                  if (v) handleSwitchShop(v);
                }}
                disabled={shopSwitching}
                className="w-full bg-slate-50 border-0 rounded-xl text-xs font-black p-2 outline-none disabled:opacity-50"
              >
                {userShops.length > 0 ? (
                  userShops.map((s) => (
                    <option key={s.shopId} value={s.shopId}>{s.shopName}</option>
                  ))
                ) : (
                  <option value={currentShopIdState ?? ''}>Shop #{currentShopIdState ?? '—'}</option>
                )}
              </select>
           </div>
            <button
              type="button"
              onClick={() => void onLogout()}
              className="w-full rounded-2xl bg-slate-200 py-3 text-xs font-black text-slate-600 transition hover:bg-rose-100 hover:text-rose-700"
            >
              退出登录
            </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col min-w-0 overflow-hidden relative">
        <header className="h-24 bg-white/70 backdrop-blur-3xl border-b border-slate-100 px-12 flex items-center justify-between shrink-0 z-10">
          <div className="flex items-center gap-10">
            <h2 className="text-xl font-black text-slate-900 uppercase tracking-tighter">
              {isDashboard ? '工作台概览' : activeGroup?.menuName ?? ''}
            </h2>
            {!isDashboard && pageChildren.length > 0 && (
            <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-[1.2rem]">
              {pageChildren.map(sub => (
                <button
                  key={sub.id}
                  onClick={() => setActivePageId(sub.id)}
                  className={`px-6 py-2.5 rounded-xl text-[10px] font-black transition-all duration-300 ${
                    activePageId === sub.id ? 'bg-white text-indigo-600 shadow-md ring-1 ring-slate-100' : 'text-slate-400 hover:text-slate-600'
                  }`}
                >
                  {sub.menuName}
                </button>
              ))}
            </div>
            )}
          </div>
          <div className="flex items-center gap-8">
            <div className="flex items-center gap-2 bg-indigo-50 px-4 py-2 rounded-2xl ring-1 ring-indigo-100">
               <span className="h-2 w-2 rounded-full bg-indigo-500 animate-pulse"></span>
               <span className="text-[10px] font-black text-indigo-600">TENANT_{tenantIdState ?? '—'}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="text-right hidden md:block">
                <div className="text-xs font-black text-slate-800">{displayName}</div>
                <div className="text-[9px] text-slate-400">Shop #{currentShopIdState ?? '—'}</div>
              </div>
              <div className="h-12 w-12 rounded-2xl bg-slate-900 flex items-center justify-center text-white font-black shadow-lg">{displayInitials}</div>
            </div>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto p-12 bg-gradient-to-br from-[#f8fafc] to-[#f1f5f9]">
          <div className="max-w-7xl mx-auto h-full">
            {isDashboard && (
              <Workbench revenueText={revenueText} dashboardData={dashboardData} actionItems={actionItems} displayName={displayName} />
            )}

            {!isDashboard && ActiveComponent && (
              <RouteErrorBoundary>
                <Suspense fallback={<div className="flex items-center justify-center h-64"><div className="text-slate-400 font-bold animate-pulse">加载中...</div></div>}>
                  <ActiveComponent />
                </Suspense>
              </RouteErrorBoundary>
            )}

            {!isDashboard && !ActiveComponent && activePage && (
              <div className="p-20 text-center font-black text-slate-200 text-4xl uppercase tracking-[0.5em]">
                {activePage.menuName} 接口就绪
              </div>
            )}

            {!isDashboard && !activePage && (
              <div className="p-20 text-center font-black text-slate-200 text-4xl uppercase tracking-[0.5em]">
                请选择功能模块
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

function LoginScreen({ onLoginSuccess }: { onLoginSuccess: () => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [captcha, setCaptcha] = useState('');
  const [captchaKey, setCaptchaKey] = useState('');
  const [captchaImg, setCaptchaImg] = useState('');
  const [preAuthToken, setPreAuthToken] = useState('');
  const [shops, setShops] = useState<Array<{ shopId: number; shopName: string }>>([]);
  const [selectedShopId, setSelectedShopId] = useState<number | null>(null);
  const [step, setStep] = useState<'credential' | 'shop'>('credential');
  const [captchaLoading, setCaptchaLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [tenantIdInput, setTenantIdInput] = useState('');

  const resolveTenantId = (): number | null => {
    const inputTenant = Number(tenantIdInput.trim())
    if (!Number.isNaN(inputTenant) && inputTenant > 0) return inputTenant
    const urlTenantRaw = new URLSearchParams(window.location.search).get('tenantId')
    const urlTenant = Number(urlTenantRaw)
    if (!Number.isNaN(urlTenant) && urlTenant > 0) return urlTenant
    const hostFirst = window.location.hostname.split('.')[0]
    const hostTenant = Number(hostFirst)
    if (!Number.isNaN(hostTenant) && hostTenant > 0) return hostTenant
    return null
  }

  const normalizeCaptchaSrc = (raw: string): string => {
    const value = raw.trim();
    if (!value) return '';
    const duplicatedPrefixPattern =
      /^data:image\/[^;]+;base64,data:image\/[^;]+;base64,/i;
    const cleaned = value.replace(duplicatedPrefixPattern, 'data:image/png;base64,');
    if (cleaned.startsWith('data:image')) return cleaned;
    return `data:image/png;base64,${cleaned}`;
  };

  const captchaSrc = normalizeCaptchaSrc(captchaImg);

  const refreshCaptcha = async () => {
    setCaptchaLoading(true);
    try {
      const result = await authApi.getCaptchaImage();
      setCaptchaKey(result.uuid);
      setCaptchaImg(result.img);
    } catch {
      setCaptchaKey('');
      setCaptchaImg('');
      setError('验证码加载失败，请确认网关和后端服务可用');
    } finally {
      setCaptchaLoading(false);
    }
  };

  useEffect(() => {
    clearPendingLoginContext();
    void refreshCaptcha();
  }, []);

  const getFriendlyLoginError = (raw: string): string => {
    if (!raw) return '登录失败，请稍后重试';
    if (raw === '请求失败' || raw.toLowerCase().includes('network')) {
      return '登录失败：请检查网关服务、账号密码，或后端是否要求验证码'
    }
    if (raw.includes('验证码')) return '验证码错误或已过期，请重新输入';
    if (raw.includes('账号') || raw.includes('用户名') || raw.includes('密码')) {
      return '账号或密码错误，请重新输入';
    }
    return raw;
  };

  const handleCredentialSubmit = async () => {
    const normalizedUsername = username.trim();
    const normalizedPassword = password.trim();
    if (!normalizedUsername) { setError('请输入用户名'); return; }
    if (!normalizedPassword) { setError('请输入密码'); return; }
    if (!captcha.trim()) { setError('请输入验证码'); return; }
    if (!captchaKey) { setError('验证码未就绪，请刷新验证码后再试'); return; }
    const tenantId = resolveTenantId()
    if (tenantId == null) { setError('缺少租户信息，请输入租户ID或通过链接传入 tenantId'); return }

    setLoading(true);
    setError('');
    try {
      try {
        const captchaValid = await authApi.validateCaptcha(captchaKey, captcha.trim());
        if (!captchaValid) {
          setError('验证码错误或已过期，请重新输入');
          setCaptcha('');
          await refreshCaptcha();
          return;
        }
      } catch {
        setError('验证码错误或已过期，请重新输入');
        setCaptcha('');
        await refreshCaptcha();
        return;
      }

      const preAuth = await beginPreAuthLogin({
        username: normalizedUsername,
        password: normalizedPassword,
        captcha: captcha.trim(),
        captchaKey,
        tenantId,
      });
      setPendingLoginContext(preAuth.preAuthToken, preAuth.shops);
      setPreAuthToken(preAuth.preAuthToken);
      setShops(preAuth.shops);
      setSelectedShopId(preAuth.shops[0]?.shopId ?? null);
      setStep('shop');
      setCaptcha('');
    } catch (e) {
      const message = e instanceof Error ? e.message : '登录失败，请重试';
      setError(getFriendlyLoginError(message));
      setCaptcha('');
      await refreshCaptcha();
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmShop = async () => {
    if (!preAuthToken) {
      setError('预登录状态失效，请重新输入账号密码');
      setStep('credential');
      void refreshCaptcha();
      return;
    }
    if (selectedShopId === null) { setError('请选择店铺后继续'); return; }

    setLoading(true);
    setError('');
    try {
      await completeShopLogin(preAuthToken, selectedShopId);
      clearPendingLoginContext();
      onLoginSuccess();
    } catch (e) {
      setError(e instanceof Error ? e.message : '店铺确认失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const backToCredentialStep = () => {
    setStep('credential');
    setPreAuthToken('');
    setShops([]);
    setSelectedShopId(null);
    clearPendingLoginContext();
    void refreshCaptcha();
  };

  return (
    <div className="min-h-screen w-full bg-[#f8fafc] flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl ring-1 ring-slate-100 p-8 space-y-6">
        <div className="space-y-2">
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">欢迎登录 Nexus Pro</h1>
          <p className="text-sm text-slate-500">
            {step === 'credential' ? '请先登录后再进入系统。' : '请选择进入系统的店铺。'}
          </p>
        </div>
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); step === 'credential' ? handleCredentialSubmit() : handleConfirmShop() }}>
          {step === 'credential' ? (
            <>
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-500 uppercase tracking-wider">用户名</label>
                <input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  className="w-full px-4 py-3 rounded-2xl bg-slate-50 border-0 ring-1 ring-slate-200 outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-500 uppercase tracking-wider">密码</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  className="w-full px-4 py-3 rounded-2xl bg-slate-50 border-0 ring-1 ring-slate-200 outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-500 uppercase tracking-wider">租户ID</label>
                <input
                  value={tenantIdInput}
                  onChange={(e) => setTenantIdInput(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl bg-slate-50 border-0 ring-1 ring-slate-200 outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="可选：优先于 URL tenantId / 子域名"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black text-slate-500 uppercase tracking-wider">验证码</label>
                <div className="flex items-center gap-3">
                  <input
                    value={captcha}
                    onChange={(e) => setCaptcha(e.target.value)}
                    className="flex-1 px-4 py-3 rounded-2xl bg-slate-50 border-0 ring-1 ring-slate-200 outline-none focus:ring-2 focus:ring-indigo-500"
                    placeholder="请输入验证码"
                  />
                  <button
                    type="button"
                    onClick={() => void refreshCaptcha()}
                    className="w-28 h-12 rounded-xl bg-slate-50 ring-1 ring-slate-200 overflow-hidden text-xs font-semibold text-slate-500 hover:bg-slate-100 transition-all"
                    disabled={captchaLoading}
                  >
                    {captchaImg ? (
                      <img alt="captcha" src={captchaSrc} className="h-full w-full object-cover" />
                    ) : (
                      <span>{captchaLoading ? '加载中...' : '点击刷新'}</span>
                    )}
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="space-y-2">
              <label className="text-xs font-black text-slate-500 uppercase tracking-wider">店铺选择</label>
              <select
                value={selectedShopId ?? ''}
                onChange={(e) => setSelectedShopId(Number(e.target.value))}
                className="w-full px-4 py-3 rounded-2xl bg-slate-50 border-0 ring-1 ring-slate-200 outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {shops.map((shop) => (
                  <option key={shop.shopId} value={shop.shopId}>{shop.shopName}</option>
                ))}
              </select>
            </div>
          )}
          {error ? (
            <div role="alert" className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{error}</div>
          ) : null}
          {step === 'credential' ? (
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-2xl bg-slate-900 text-white font-black text-sm hover:bg-indigo-600 transition-all disabled:opacity-60"
            >
              {loading ? '登录中...' : '下一步：选择店铺'}
            </button>
          ) : (
            <div className="flex gap-3">
              <button
                type="button"
                onClick={backToCredentialStep}
                disabled={loading}
                className="flex-1 py-3 rounded-2xl bg-white text-slate-700 ring-1 ring-slate-200 font-black text-sm hover:bg-slate-50 transition-all disabled:opacity-60"
              >
                返回
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 py-3 rounded-2xl bg-slate-900 text-white font-black text-sm hover:bg-indigo-600 transition-all disabled:opacity-60"
              >
                {loading ? '确认中...' : '确认店铺并进入'}
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
