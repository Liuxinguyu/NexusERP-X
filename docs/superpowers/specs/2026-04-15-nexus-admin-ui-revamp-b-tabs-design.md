# Nexus Admin（Vue+Element Plus）UI 改造设计稿（B 方案 + Tabs）

**日期**：2026-04-15  
**范围**：在保持现有技术栈（Vue3 + Vite + Pinia + Vue Router + Element Plus）前提下，借鉴 `enterprise-admin` 的“现代后台壳子”体验，完成：

- 全站统一视觉规范（tokens + element 覆盖）
- 新 Layout（侧边栏 + 顶栏 + 内容区）与“二级 Tabs 导航”
- 关键页面重做：Dashboard（Bento Grid 风格）+ 系统/ERP/OA/薪资主列表页统一骨架
- 路由稳定性根治：菜单 component ↔ 视图文件映射固定化；找不到视图不白屏

---

## 1. 目标与验收标准

### 1.1 目标
- **能用**：动态路由稳定，菜单点击必可进入页面（无白屏/无卡死）。
- **人性化**：点击区域更大；常用操作更显眼；列表页“搜—查—增—改—删”路径清晰。
- **好看且统一**：全站一致的卡片、间距、圆角、阴影、字体权重与色彩。

### 1.2 验收标准（最小可交付）
- 登录后侧边栏可用，一级模块切换顺畅；二级 Tabs 与当前路由同步。
- `menu.component` 能映射到 `/src/views/**/index.vue`；控制台不再出现 `view file not found`/`missing component(s)` 警告。
- Dashboard 具备 Bento Grid 风格布局（静态 + 少量真实数据接入）。
- 系统/ERP/OA/薪资至少各 1 个列表页完成“SearchCard + TableCard + Dialog/Drawer”统一外观与交互。

---

## 2. 视觉规范（借鉴 enterprise-admin 气质）

### 2.1 颜色
- 主色：Indigo 系（更接近参考项目），辅助色：Slate 灰阶
- 背景：`slate-50` + 微渐变到 `slate-100`
- 卡片：白底 + ring 细边框 + 轻阴影

### 2.2 圆角与阴影
- 大卡片：24–32px
- 输入/按钮：12–16px
- 标签/徽标：10–12px

### 2.3 排版与密度
- 标题更“重”（600–800），正文 400–500
- 统一间距体系：8/12/16/24/32

### 2.4 实现策略
- 新增 `src/styles/tokens.css`：设计 token（CSS 变量）
- 新增 `src/styles/element-overrides.css`：Element Plus 外观统一（尽量通过变量与低侵入选择器）
- `main.ts` 引入上述样式文件（与现有全局样式合并或替换）

---

## 3. Layout 信息架构（一级侧边栏 + 二级 Tabs）

### 3.1 Sidebar（只放一级模块）
Sidebar 显示：系统 / ERP / OA / 薪资 等一级模块（来自菜单树的第一层）。

- 顶部：品牌区（Logo + 名称）
- 中部：一级模块列表（按钮式、选中态强、点击区域大）
- 底部：上下文（店铺切换 / 租户信息 / 用户入口）

### 3.2 二级 Tabs（内容区顶部）
当选中某个一级模块时，在内容区顶部显示该模块下所有“叶子页面”的 Tabs（或分组 Tabs）。

要求：
- Tabs 与路由双向同步：
  - 点击 Tab → `router.push(tab.path)`
  - 路由变化 → 高亮当前 Tab
- Tabs 溢出时可横向滚动（带渐隐遮罩或左右按钮）

### 3.3 顶栏 Topbar
- 左侧：当前模块标题 + 可选面包屑
- 右侧：店铺切换 / 通知 / 用户菜单

---

## 4. 页面骨架规范（统一可用且好看）

### 4.1 列表页标准骨架
1) SearchCard（搜索卡）
- `el-card shadow="never"`，一行或两行条件
- 右侧：主按钮（新增/导出/批量）
- Enter 查询，Reset 复位并重新加载

2) TableCard（表格卡）
- header：标题 + 右侧操作（刷新/密度/列设置）
- table：行 hover、操作列靠右
- pagination：右下角对齐

3) Dialog/Drawer
- 表单项间距统一
- footer 固定：取消 / 保存

### 4.2 状态组件
- `RequestErrorState`：失败空态 + 重试（已存在）
- `EmptyState`：无数据空态（建议新增，以替代默认空表格）

---

## 5. 路由稳定性根治（不再猜路径）

### 5.1 唯一真源
- 以 **后端菜单 `menu.component`** 作为视图定位依据（例如 `views/system/user/index`）。

### 5.2 唯一映射规则
- 映射到 Vite glob key：`/src/${menu.component}.vue`
- 使用 `import.meta.glob('/src/views/**/index.vue')`，避免 alias key 不一致导致找不到模块。

### 5.3 诊断与兜底
- 控制台自检：列出菜单中找不到视图的条目（菜单名、fullPath、component、expectedViewKey）
- UI 兜底页面：`/route-fallback` 显示“期望文件路径/菜单信息/复制按钮”

---

## 6. 关键页面重做（B 范围）

### 6.1 Dashboard（工作台）
- Bento Grid：大卡 + 多个小卡
- 卡片类型：指标卡、待办卡、趋势卡、快捷入口
- 数据策略：先用真实接口能拿到的（用户/店铺/菜单），业务指标可先 mock，后续逐步接入

### 6.2 业务列表页统一外观
模块覆盖：
- 系统：用户/店铺/角色（优先）
- ERP：商品/客户/仓库/库存（优先）
- OA：员工/任务/考勤（优先）
- 薪资：薪资项/工资单（优先）

---

## 7. 非目标（本轮不做）
- 不从 Vue 改成 React
- 不全面替换 Element Plus 为 Tailwind 组件（只借鉴风格）
- 不做大规模业务逻辑重构（以 UI/路由稳定为主）

