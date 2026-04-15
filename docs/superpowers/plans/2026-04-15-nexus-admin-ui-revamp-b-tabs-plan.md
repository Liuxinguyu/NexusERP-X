# Nexus Admin UI Revamp (B + Tabs) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不更换技术栈（Vue3 + Element Plus）的前提下，完成“企业级现代后台壳子”改造：一级侧边栏 + 二级 Tabs、Dashboard Bento、列表页统一骨架，并彻底修复动态路由导致的“页面打不开/白屏”。

**Architecture:** 以 `menu.component` 为视图唯一真源，统一映射到 `/src/views/**.vue`；Layout 用组件化拆分（Sidebar/Topbar/SubTabs/PageShell）；视觉 token 与 Element 覆盖集中到 `src/styles/`，各业务页只做结构与间距调整。

**Tech Stack:** Vue 3 + Vite + Pinia + Vue Router + Element Plus + TypeScript

---

## File Structure（锁定改动边界）

**Create:**
- `nexus-admin-web/src/styles/tokens.css`（设计 token：颜色/圆角/阴影/间距/字体）
- `nexus-admin-web/src/styles/element-overrides.css`（Element Plus 统一外观）
- `nexus-admin-web/src/layout/components/AppSidebar.vue`（一级菜单）
- `nexus-admin-web/src/layout/components/AppTopbar.vue`（顶栏：用户/店铺/通知）
- `nexus-admin-web/src/layout/components/SubTabs.vue`（二级 Tabs：随路由与模块切换）
- `nexus-admin-web/src/layout/components/PageShell.vue`（内容区通用容器：max-width、背景、间距）
- `nexus-admin-web/src/views/dashboard/overview.vue`（新 Bento 工作台组件，供 `dashboard/index.vue` 引用）

**Modify:**
- `nexus-admin-web/src/main.ts`（引入新样式）
- `nexus-admin-web/src/layout/index.vue`（替换为组件化 Layout + Tabs 区）
- `nexus-admin-web/src/stores/user.ts`（动态路由：以 menu.component 精确映射；诊断输出）
- `nexus-admin-web/src/views/route-fallback/index.vue`（兜底页显示期望路径/复制按钮）
- `nexus-admin-web/src/views/login/index.vue`（验证码：后端关闭时不强制；更现代风格）
- 选定 8–10 个列表页：统一 SearchCard + TableCard 骨架与按钮布局（不改业务逻辑）

---

## Task 1: 路由稳定性“硬化”（先把能用做稳）

**Files:**
- Modify: `nexus-admin-web/src/stores/user.ts`
- Modify: `nexus-admin-web/src/views/route-fallback/index.vue`

- [ ] **Step 1: 在 route-fallback 页面展示诊断信息**
  - 显示：菜单名、fullPath、后端 component、expectedViewKey
  - 提供“一键复制 expectedViewKey”按钮
  - 允许跳回 Dashboard

- [ ] **Step 2: 动态路由生成只使用 menu.component**
  - 规则：`/src/${component}.vue`
  - glob：`import.meta.glob('/src/views/**/index.vue')`

- [ ] **Step 3: 启动自检输出完整 missing table**
  - 输出字段：menu、fullPath、component、expectedView
  - 只在 dev 输出（避免生产污染日志）

- [ ] **Step 4: 验证**
  - 操作：登录后打开控制台
  - 预期：无 `missing component(s)`；若缺文件能落到兜底页并展示期望路径

---

## Task 2: 新视觉 Token + Element 覆盖（统一好看）

**Files:**
- Create: `nexus-admin-web/src/styles/tokens.css`
- Create: `nexus-admin-web/src/styles/element-overrides.css`
- Modify: `nexus-admin-web/src/main.ts`

- [ ] **Step 1: 写 tokens.css（变量）**
  - 颜色：主色 indigo、slate 背景、danger/success/warn 辅助
  - 圆角：card/input/button/tag
  - 阴影：card / hover
  - 字体：Inter 优先（fallback system）

- [ ] **Step 2: 写 element-overrides.css**
  - `el-card`：ring + shadow
  - `el-table`：header 更轻、row hover 更柔和
  - `el-button`：primary 更现代、text/link 统一
  - `el-dialog/el-drawer`：圆角/标题区间距统一

- [ ] **Step 3: main.ts 引入顺序**
  - tokens → element-overrides → 现有 global（如需合并则移除旧冲突）

- [ ] **Step 4: 验证**
  - 运行：`npm run build`
  - 预期：构建通过；全站卡片/按钮视觉明显统一

---

## Task 3: Layout 组件化 + 一级 Sidebar + 顶部栏（壳子成型）

**Files:**
- Create: `nexus-admin-web/src/layout/components/AppSidebar.vue`
- Create: `nexus-admin-web/src/layout/components/AppTopbar.vue`
- Create: `nexus-admin-web/src/layout/components/PageShell.vue`
- Modify: `nexus-admin-web/src/layout/index.vue`

- [ ] **Step 1: AppSidebar.vue**
  - 输入：menus（树）、当前 route
  - 输出：一级模块列表（/system /erp /oa /wage …）
  - 交互：选中态强；支持折叠（保留现有 store 控制）

- [ ] **Step 2: AppTopbar.vue**
  - 右侧：店铺切换、通知、用户菜单
  - 左侧：当前模块标题

- [ ] **Step 3: PageShell.vue**
  - 背景渐变、max-width、padding 统一

- [ ] **Step 4: layout/index.vue 重组**
  - 顶：Topbar
  - 左：Sidebar
  - 中：SubTabs（Task4）
  - 内容：PageShell + router-view

- [ ] **Step 5: 验证**
  - 登录后：侧边栏一级切换可用，页面不抖动，内容区不撑满

---

## Task 4: 二级 SubTabs（像 enterprise-admin 顶部 sub buttons）

**Files:**
- Create: `nexus-admin-web/src/layout/components/SubTabs.vue`
- Modify: `nexus-admin-web/src/layout/index.vue`

- [ ] **Step 1: 从 userStore.menus 计算 tabs**
  - 当前一级模块 key：取 route 的第一段（如 `/system/**` → `system`）
  - tabs 列表：收集该模块下所有叶子菜单（有 component 的节点），按 sort 排序

- [ ] **Step 2: Tabs UI**
  - 横向滚动
  - 选中态：pill + ring
  - 点击：push 到对应 path

- [ ] **Step 3: 路由同步**
  - route change → activeTab 更新

- [ ] **Step 4: 验证**
  - `/system/user` 等页面打开时，tabs 正确高亮并可切换

---

## Task 5: Dashboard Bento 重做（关键页面）

**Files:**
- Create: `nexus-admin-web/src/views/dashboard/overview.vue`
- Modify: `nexus-admin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: 设计 Bento Grid**
  - 大卡：系统在线/收入趋势（可 mock）
  - 小卡：菜单快捷入口、待办/审批（可 mock）
  - 指标卡：店铺/员工/库存等（先静态）

- [ ] **Step 2: 接入少量真实上下文**
  - 展示：当前用户、当前店铺、系统在线（来自现有 store）

- [ ] **Step 3: 验证**
  - Dashboard 进入无报错，布局紧凑现代

---

## Task 6: 列表页骨架统一（系统/ERP/OA/薪资）

**Files:**
- Modify: 选定页面（例如）
  - `nexus-admin-web/src/views/system/user/index.vue`
  - `nexus-admin-web/src/views/system/shop/index.vue`
  - `nexus-admin-web/src/views/system/role/index.vue`
  - `nexus-admin-web/src/views/erp/product/index.vue`
  - `nexus-admin-web/src/views/erp/customer/index.vue`
  - `nexus-admin-web/src/views/erp/warehouse/index.vue`
  - `nexus-admin-web/src/views/oa/employee/index.vue`
  - `nexus-admin-web/src/views/wage/monthly-slip/index.vue`

- [ ] **Step 1: SearchCard + TableCard 结构调整**
  - 不改 API 逻辑，只调整容器与按钮布局
  - 将错误态/空态放到 TableCard 内统一区域

- [ ] **Step 2: 操作按钮规范**
  - 主按钮在右侧（新增/导出）
  - 次按钮（重置/刷新）用 text 或轻量按钮

- [ ] **Step 3: 验证**
  - 每个页面都能打开，样式一致、间距一致

---

## Task 7: 登录页体验修复（验证码与观感）

**Files:**
- Modify: `nexus-admin-web/src/views/login/index.vue`

- [ ] **Step 1: 验证码显示逻辑改为“后端要求才显示”**
  - 方式：请求验证码失败时不阻塞登录；或者通过后端开关接口（如无则走“失败隐藏”策略）
  - dev：后端已关闭 captcha 时，前端不再强制必填

- [ ] **Step 2: 登录页视觉贴近新 tokens**
  - 卡片、输入、按钮与全站一致

- [ ] **Step 3: 验证**
  - 空白账号不会提交
  - 正确账号可登录

---

## Final Verification（完成前强制）
- [ ] `npm run build`（前端构建通过）
- [ ] 登录 → 菜单 → 关键页面逐个打开（至少：系统用户、ERP商品、OA员工、薪资工资单、Dashboard）
- [ ] 控制台无 route missing / dynamic import 警告（允许少量无关 warning）

