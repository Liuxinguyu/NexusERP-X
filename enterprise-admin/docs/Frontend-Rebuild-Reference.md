# NexusERP-X 前端重建参考文档 (Cursor Rebuild)

本文档包含重建前端所需的全部架构信息、技术细节和代码规范。

---

## 1. 项目概述

> **本仓库 `enterprise-admin` 说明**：当前目录下的实现为 **React 19 + Vite + Tailwind** 的管理端（与上文「nexus-admin-web / Vue 3」为平行参考）。按钮级权限在前端通过 `PermissionsProvider` + `PermGate`（见 `src/context/PermissionsContext.tsx`）实现，语义对齐若依 `v-hasPermi`，权限字符串常量见 `src/lib/system-perms.ts`，数据来自 `GET /system/user/info` 返回的 `permissions` / `perms` 等字段。

- **项目名**: nexus-admin-web
- **框架**: Vue 3 + TypeScript + Vite
- **UI 库**: Element Plus（全局注册，zh-CN 中文）
- **状态管理**: Pinia
- **路由**: Vue Router 4（动态路由）
- **HTTP**: Axios + 拦截器
- **图标**: @element-plus/icons-vue（全局注册）

### 启动

```bash
cd nexus-admin-web
npm install
npm run dev        # 默认 5173
```

---

## 2. 目录结构

```
nexus-admin-web/src/
├── main.ts                        # 入口：注册 pinia/router/ElementPlus/全局图标
├── App.vue                        # 根组件：只有 <router-view />
├── api/
│   ├── request.ts                # Axios 实例，baseURL='/api/v1'，拦截器
│   ├── auth.ts                    # 登录/登出/店铺/个人信息
│   ├── system.ts                  # 用户/角色/菜单/机构/工作台
│   ├── oa.ts                      # OA：员工/考勤/请假/审批/任务/日程
│   ├── erp.ts                     # ERP：商品/客户/供应商/仓库/订单/财务
│   └── wage.ts                    # 薪酬配置/工资条
├── stores/
│   ├── user.ts                    # 用户态、登录、动态路由
│   └── app.ts                     # 当前模块、Tab 状态
├── router/
│   └── index.ts                   # 静态路由 + 路由守卫
├── layout/
│   ├── index.vue                  # 主布局：Sidebar + Topbar + SubTabs + <router-view>
│   └── components/
│       ├── AppSidebar.vue         # 左侧图标导航（72px）
│       ├── AppTopbar.vue          # 顶部栏：标题/公告/店铺切换/用户菜单
│       └── SubTabs.vue            # 二级 Tab 栏
├── views/
│   ├── login/index.vue            # 两步登录页
│   ├── dashboard/index.vue        # Bento Grid 工作台
│   ├── system/hub/index.vue       # 系统设置枢纽页
│   ├── erp/                      # ERP 各模块页（动态加载）
│   └── oa/                       # OA 各模块页（动态加载）
├── components/
│   ├── NexusCard/index.vue        # 通用卡片
│   ├── NexusTableCard/index.vue   # 表格+分页封装
│   └── RequestErrorState.vue      # 请求错误状态
├── hooks/
│   └── useDict.ts                # 字典数据 hook
└── styles/
    ├── tokens.css                # CSS 变量（设计 Token）
    ├── common.css                # 全局样式（滚动条/工具类）
    └── element-overrides.css     # Element Plus 样式覆盖
```

---

## 3. 设计 Token (tokens.css)

所有 CSS 变量定义在 `:root` 中，全局生效：

```css
:root {
  /* 品牌色 */
  --color-primary: #4f46e5;
  --color-primary-hover: #4338ca;
  --color-primary-soft: #eef2ff;

  /* 布局尺寸 */
  --sidebar-width: 72px;       /* 左侧图标导航宽度 */
  --header-height: 64px;       /* 顶部栏高度 */

  /* 语义色 */
  --color-success: #16a34a;
  --color-warning: #d97706;
  --color-danger: #dc2626;

  /* 圆角 */
  --radius-sm: 12px;
  --radius-md: 16px;

  /* 文字 */
  --text-primary: #0f172a;
  --text-secondary: #334155;
  --text-muted: #64748b;

  /* Element Plus 覆盖 */
  --el-color-primary: #4f46e5;
  --el-border-radius-base: 12px;
}
```

**覆盖方式**：Element Plus 的 `--el-xxx` 变量会自动响应 tokens.css 中的同名变量。

---

## 4. HTTP 层 (request.ts)

### 核心配置

```ts
const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',   // 所有请求前缀
  timeout: 15000,
})
```

### 请求拦截器行为

1. **白名单免授权**：以下路径跳过 Token 注入
   - `/auth/login`
   - `/auth/confirm-shop`
   - `/system/captcha/image`
   - `/system/captcha/validate`

2. **自动附加 JWT**：`Authorization: Bearer {token}`

3. **主动刷新**：JWT 过期前 5 分钟自动刷新，刷新期间并发请求被队列化

4. **错误处理**：
   - 业务 code ≠ 200/0 → `ElMessage.error(msg)` + reject
   - 401 → 尝试刷新 Token → 刷新失败 → 跳转 `/login`

### 返回值约定

后端返回 `{ code, msg, data }`，拦截器直接返回 `result.data`。因此：

```ts
// 业务代码直接拿 data
const data = await systemApi.getUserPage(params)  // 返回 PageResult<User>
```

---

## 5. 登录流程 (两步登录)

```
用户输入账号密码
  → POST /api/v1/auth/login
  → 返回 { preAuthToken, shops[] } 存入 sessionStorage
  → 展示店铺选择器（el-tree-select）
  → 选店后 POST /api/v1/auth/confirm-shop
  → 返回 { accessToken, currentShopId }
  → 存入 localStorage
  → GET /api/v1/system/user/info（获取用户信息+菜单树）
  → userStore.addAccessibleRoutes() 注册动态路由
  → router.push('/dashboard')
```

### 路由守卫

```ts
// router/index.ts
router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const token = userStore.getToken()
  const shopId = userStore.getCurrentShopId()

  if (to.path !== '/login') {
    // 业务页必须有 JWT + 已选店铺
    if (!token || shopId === null) return { path: '/login', query: { redirect: to.fullPath } }
    return
  }

  // 已登录 → 回首页
  if (token && shopId !== null) return { path: '/' }
})
```

### Token 存储

| Key | 位置 | 说明 |
|-----|------|------|
| `nexus_token` | localStorage | JWT |
| `nexus_current_shop_id` | localStorage | 当前店铺ID |
| `nexus_pre_auth_token` | sessionStorage | 预登录票据 |
| `nexus_pending_shops` | sessionStorage | 预登录店铺列表 |

---

## 6. 动态路由机制

### 后端菜单结构

后端返回菜单树 `UserMenuNode[]`：

```ts
interface UserMenuNode {
  id: number
  parentId: number
  menuType: string      // 'M'=菜单
  menuName: string
  path: string           // 相对路径，如 'user'
  fullPath: string       // 完整路径，如 '/system/user'
  component: string | null  // 如 'system/user/index'
  icon: string           // Element Plus 图标名
  perms: string
  sort: number
  children: UserMenuNode[]
}
```

### 组件映射规则

```ts
// user.ts 中的映射
'user'              → /src/views/{module}/user/index.vue
'user/list'         → /src/views/{module}/user/list.vue
'system/user'       → /src/views/system/user/index.vue

// fallback（找不到时）
→ /src/views/route-fallback/index.vue
```

### 注册流程

```ts
// user.ts addAccessibleRoutes()
function addAccessibleRoutes() {
  const existing = router.getRoutes()
  // 先清理旧动态路由
  for (const r of existing) {
    if (String(r.name).startsWith('__dyn_')) {
      try { router.removeRoute(r.name) } catch {}
    }
  }
  // 全部挂到 Layout 下
  for (const r of buildFlatRoutes(menus.value)) {
    router.addRoute('Layout', r)
  }
}
```

所有动态路由挂在 `Layout` 路由的 children 下。

---

## 7. 布局架构

### Layout (layout/index.vue)

```
┌──────────┬──────────────────────────────────────┐
│          │  AppTopbar                           │
│ Sidebar  ├──────────────────────────────────────┤
│ (72px)   │  SubTabs                             │
│ 图标导航 │  二级菜单 Tab 栏                      │
│          ├──────────────────────────────────────┤
│          │  <router-view> (content-shell)       │
│          │  padding: 24px                       │
└──────────┴──────────────────────────────────────┘
```

### Sidebar (AppSidebar.vue)

**不是 el-menu**，是纯按钮组：

- 每个一级模块一个圆形图标按钮
- 点击 → `appStore.setActiveModule(base)` → `router.push('/{module}/hub')`
- 底部 logo: "N" 字样

### SubTabs (SubTabs.vue)

- 展示当前一级模块下的二级菜单（叶子节点）
- 第一个 Tab 是"概览"（hub 页）
- 横向滚动，自动 scroll 到当前激活 Tab
- 样式：底部 2px 指示条，颜色跟随 `--color-primary`

### AppTopbar (AppTopbar.vue)

- 左侧：当前模块标题
- 右侧：公告图标、店铺切换下拉、用户头像下拉

---

## 8. 模块路由映射（Hub 页）

| 模块 | Hub 路由 | Sidebar 点击行为 |
|------|----------|----------------|
| 工作台 | `/dashboard` | Sidebar 顶部按钮 → `/dashboard` |
| 系统设置 | `/system/hub` | `/system` → `/system/hub` |
| ERP | `/erp/hub` | `/erp` → `/erp/hub` |
| OA | `/oa/hub` | `/oa` → `/oa/hub` |

Hub 页是各模块的概览/导航页，用 `grid` 布局展示子模块入口卡片。

---

## 9. API 模块速查

### baseURL: `/api/v1`

| 文件 | 前缀 | 说明 |
|------|------|------|
| `auth.ts` | `/auth` | 登录/登出/店铺 |
| `system.ts` | `/system` | 用户/角色/菜单/机构/工作台 |
| `oa.ts` | `/oa` | OA 模块（员工/考勤/请假/审批等）|
| `erp.ts` | `/erp` | ERP 模块（商品/客户/供应商/仓库/订单/财务）|
| `wage.ts` | `/wage` | 薪酬模块 |

### 核心 API 签名

```ts
// Auth
authApi.getCaptchaImage()                        // GET /system/captcha/image
authApi.loginPreAuth({ username, password, captcha?, captchaKey? })  // POST /auth/login
authApi.getCurrentUserInfo()                     // GET /system/user/info
authApi.logout()                                 // POST /auth/logout

// 店铺
confirmShop({ preAuthToken, shopId })            // POST /auth/confirm-shop
switchShop(shopId)                               // PUT /auth/switch-shop

// Workbench
systemApi.getDashboard()                         // GET /system/workbench/dashboard
systemApi.getSalesChart()                        // GET /system/workbench/sales-chart

// Attendance
oaApi.submitClockIn({ lat, lng, type })          // POST /oa/attendance/clock-in
oaApi.getAttendanceTodayStatus()                 // GET /oa/attendance/today

// Approval
oaApi.getMyApprove(current, size, status?)      // GET /oa/approval/tasks/my-approve
oaApi.approveTask(id, { approved, opinion })    // POST /oa/approval/tasks/{id}/approve
```

---

## 10. Element Plus 使用规范

### 全局注册

```ts
// main.ts
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(ElementPlus, { locale: zhCn })
```

**不需要手动 import 组件**（unplugin-vue-components 自动导入），也不需要手动 import 图标。

### 样式覆盖原则

统一在 `element-overrides.css` 中处理：

```css
/* 圆角统一 */
.el-button { border-radius: 12px !important; }

/* 表格只保留底边线 */
.el-table { border: none !important; }
.el-table th.el-table__cell { border-bottom: 1px solid var(--border-color) !important; }
.el-table td.el-table__cell { border-bottom: 1px solid var(--border-color-soft) !important; }
```

### 通用包装组件

```vue
<!-- NexusTableCard: 表格 + 分页封装 -->
<NexusTableCard
  v-model:current="query.current"
  v-model:size="query.size"
  :total="total"
  :loading="loading"
  @pagination-change="loadData"
>
  <el-table :data="list" />
</NexusTableCard>
```

---

## 11. 页面开发规范

### 典型列表页结构

```vue
<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="search-card card-header">
      <el-form inline>
        <el-form-item><el-input v-model="query.name" /></el-form-item>
        <el-form-item><el-button @click="loadData">查询</el-button></el-form-item>
      </el-form>
      <el-button type="primary" @click="openDialog()">新增</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { erpApi } from '@/api/erp'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, name: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await erpApi.getProductPage(query)
    list.value = res.records ?? res.list ?? []
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
```

### 分页兼容

后端 `PageResult` 同时支持 `records` 和 `list` 字段，前端需兜底：

```ts
list.value = res.records ?? res.list ?? []
```

---

## 12. Hub 页布局规范

```vue
<template>
  <div class="page-container">
    <div class="section-title mb-4">{{ 模块名 }}</div>
    <div class="card-grid">
      <div
        v-for="item in cards"
        :key="item.path"
        class="hub-card nexus-card"
        @click="router.push(item.path)"
      >
        <el-icon :size="24"><component :is="item.icon" /></el-icon>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}
.hub-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.hub-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
</style>
```

---

## 13. 关键文件完整代码

### main.ts

```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@/styles/variables.css'
import '@/styles/common.css'
import '@/styles/tokens.css'
import '@/styles/element-overrides.css'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
```

### App.vue

```vue
<template>
  <router-view />
</template>
```

### useDict.ts

```ts
import { ref } from 'vue'
import { systemApi } from '@/api/system'

const cache = new Map<string, any[]>()

export function useDict<T extends readonly string[]>(...typeCodes: T) {
  const dictMap = {} as Record<T[number], any[]>
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      for (const code of typeCodes) {
        if (cache.has(code)) {
          dictMap[code] = cache.get(code)!
        } else {
          const res = await systemApi.getDictItemsByTypeCode(code)
          dictMap[code] = res ?? []
          cache.set(code, dictMap[code])
        }
      }
    } catch (e: any) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  load()
  return { dictMap, loading, error, refresh: load }
}

export function preloadDict(...typeCodes: string[]) {
  return Promise.all(typeCodes.map((code) => systemApi.getDictItemsByTypeCode(code)))
}

export function clearDictCache(typeCode?: string) {
  if (typeCode) cache.delete(typeCode)
  else cache.clear()
}
```

---

## 14. Vite 配置

```ts
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Gateway
        changeOrigin: true,
      },
    },
  },
})
```

---

## 15. 后端网关路由（供参考）

```
/api/v1/auth/**       → nexus-system  (8081)
/api/v1/system/**     → nexus-system  (8081)
/api/v1/oa/leave/**   → nexus-system  (8081)
/api/v1/oa/**         → nexus-oa      (8083)
/api/v1/erp/**        → nexus-erp     (8082)
/api/v1/wage/**       → nexus-wage    (8084)
```

Vite Dev Server 通过 `/api` 代理到 `http://localhost:8080`（Gateway），由 Gateway 再路由到各微服务。

---

## 16. 快速开发清单

- [ ] 创建 Vue 组件时，放在 `src/views/{module}/{name}/index.vue`
- [ ] API 函数放在 `src/api/{module}.ts`，用 `get/post/put/del` 包装
- [ ] 分页参数：`withPageParams(current, size, extra?)` 兼容所有后端
- [ ] 表格数据：`res.records ?? res.list ?? []` 兜底
- [ ] 图标：直接用字符串 `<el-icon><User /></el-icon>`，无需 import
- [ ] Element Plus 组件：直接用，unplugin 自动导入
- [ ] 新增页面后无需手动注册路由，由后端菜单树动态注入
- [ ] Hub 页放在 `src/views/{module}/hub/index.vue`
