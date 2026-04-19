# NexusERP-X 前端开发指南

## 网关 Base URL

```
开发环境: http://localhost:8080
前端代理: /api → http://localhost:8080（vite.config.ts 配置）
前端实际: baseURL = '/api/v1'（request.ts 硬编码）
```

**完整请求路径示例**：
```
前端调用: systemApi.getUserPage()
  → axios.get('/api/v1/system/users/page')
  → Vite 代理 /api → http://localhost:8080
  → Gateway: GET /api/v1/system/users/page
```

---

## 网关路由映射

| 前端路径前缀 | 网关路由 | 目标服务 | 端口 |
|-------------|----------|----------|------|
| `/api/v1/auth/**` | `/api/v1/auth/**` | nexus-system | 8081 |
| `/api/v1/system/**` | `/api/v1/system/**` | nexus-system | 8081 |
| `/api/v1/oa/leave/**` | `/api/v1/oa/leave/**` | nexus-system | 8081 |
| `/api/v1/oa/**` | `/api/v1/oa/**` | nexus-oa | **8083** |
| `/api/v1/erp/**` | `/api/v1/erp/**` | nexus-erp | 8082 |
| `/api/v1/wage/**` | `/api/v1/wage/**` | nexus-wage | **8084** |

> ⚠️ `nexus-oa` 默认端口 **8083**，`nexus-wage` 默认端口 **8084**，与 `nexus-system` (8081) 不同。

---

## 本地启动命令

### 后端（4 个服务）

```bash
# 必须在 NexusERP-X 根目录执行
cd /Users/liuxingyu/NexusERP-X

# 端口: gateway=8080 system=8081 erp=8082 auth=8085
# 注意：OA(8083) 和 Wage(8084) 需要单独启动

mvn spring-boot:run -pl nexus-gateway -Dspring-boot.run.jvmArguments="-Dserver.port=8080"
mvn spring-boot:run -pl nexus-system  -Dspring-boot.run.jvmArguments="-Dserver.port=8081"
mvn spring-boot:run -pl nexus-erp     -Dspring-boot.run.jvmArguments="-Dserver.port=8082"
mvn spring-boot:run -pl nexus-auth    -Dspring-boot.run.jvmArguments="-Dserver.port=8085"
# OA 和 Wage（非必须，但 /api/v1/oa 和 /api/v1/wage 路由会失败）
mvn spring-boot:run -pl nexus-oa     -Dspring-boot.run.jvmArguments="-Dserver.port=8083"
mvn spring-boot:run -pl nexus-wage    -Dspring-boot.run.jvmArguments="-Dserver.port=8084"
```

### 前端

```bash
cd /Users/liuxingyu/NexusERP-X/nexus-admin-web
npm run dev          # 默认 5173
# 或指定端口
npm run dev -- --port 5174
```

---

## 端口占用排查

```bash
# 查看所有相关端口
lsof -i:8080 -i:8081 -i:8082 -i:8083 -i:8084 -i:8085 -i:5173 -P -n

# 杀掉指定端口进程
kill -9 $(lsof -ti:8080 -P -n)
```

---

## 前端 API 模块

所有 API 位于 `src/api/` 目录，`baseURL = '/api/v1'`。

| 文件 | 对应后端模块 | 说明 |
|------|-------------|------|
| `api/auth.ts` | `/api/v1/auth` | 登录/登出/店铺切换 |
| `api/system.ts` | `/api/v1/system` | 用户/角色/菜单/机构/店铺/工作台 |
| `api/oa.ts` | `/api/v1/oa` | 员工/考勤/请假/审批/任务/日程/文件 |
| `api/erp.ts` | `/api/v1/erp` | 商品/客户/供应商/仓库/库存/销采订单/财务 |
| `api/wage.ts` | `/api/v1/wage` | 薪酬配置/工资条 |

---

## 认证流程

### 两步登录

```
1. POST /api/v1/auth/login
   → 返回 { preAuthToken, shops[] }

2. POST /api/v1/auth/confirm-shop  (body: { preAuthToken, shopId })
   → 返回 { accessToken, currentShopId, ... }
   → 前端存储 accessToken，进入系统
```

### Token 存储

| Key | 位置 | 说明 |
|-----|------|------|
| `accessToken` | localStorage `nexus_token` | JWT |
| `currentShopId` | localStorage `nexus_current_shop_id` | 当前店铺ID |
| `preAuthToken` | sessionStorage | 预登录票据 |
| `shops[]` | sessionStorage | 预登录店铺列表 |

### 请求拦截器行为

- 自动在 `Authorization` 头附加 `Bearer {token}`
- JWT 过期前 5 分钟自动刷新（`/api/v1/auth/refresh`）
- 刷新期间并发请求被队列化
- 401 时重定向到 `/login`

---

## 动态路由

登录后 `userStore.addAccessibleRoutes()` 读取后端菜单树，自动注册所有页面路由：

```ts
// 路由格式
{
  path: '/erp/sale-order',     // fullPath 去掉前导 /
  name: '__dyn_销售订单',
  component: () => import('@/views/erp/sale-order/index.vue'),
  meta: { title, icon, perms, modulePath }
}
```

前端组件约定路径规则：
- 后端 `component: 'erp/sale-order/index'`
- 映射到 `src/views/erp/sale-order/index.vue`

**fallback**：无匹配时渲染 `src/views/route-fallback/index.vue`

---

## 登录页操作流程

```
用户输入账号密码 → preAuthLogin()
  → preAuthToken 存入 sessionStorage
  → 展示店铺选择器（el-tree-select）
  → confirmShopEntry(shopId)
    → /confirm-shop 获取 accessToken
    → fetchProfileAndAcl() 拉取用户信息和菜单树
    → addAccessibleRoutes() 注册动态路由
    → router.push('/dashboard')
```

---

## 切换店铺

```
userStore.switchShop(newShopId)
  → PUT /auth/switch-shop
  → 返回新 accessToken + newShopId
  → 更新本地存储
  → fetchProfileAndAcl() 重新拉取菜单
```

---

## 常用接口速查

### 登录
```ts
authApi.getCaptchaImage()                         // GET /system/captcha/image
authApi.loginPreAuth({ username, password })      // POST /auth/login
authApi.getCurrentUserInfo()                      // GET /system/user/info
authApi.logout()                                  // POST /auth/logout
```

### 工作台
```ts
systemApi.getDashboard()                          // GET /system/workbench/dashboard
systemApi.getSalesChart()                         // GET /system/workbench/sales-chart
systemApi.getStockAlarms()                        // GET /system/workbench/stock-alarms
```

### 考勤打卡
```ts
oaApi.checkIn('in'|'out')                         // POST /oa/attendance/check-in
oaApi.getMyTodayStatus()                          // GET /oa/attendance/my-today
oaApi.getAttendanceRecords(current, size, params)  // GET /oa/attendance/records/page
oaApi.getAttendanceTodayStatus()                   // GET /oa/attendance/today
oaApi.submitClockIn(data)                         // POST /oa/attendance/clock-in
```

### 审批
```ts
oaApi.getMyApprove(current, size, status)        // GET /oa/approval/tasks/my-approve
oaApi.getMyApply(current, size, status)          // GET /oa/approval/tasks/my-apply
oaApi.approveTask(id, { approved, opinion })      // POST /oa/approval/tasks/{id}/approve
oaApi.rejectTask(id, opinion)                     // POST /oa/approval/tasks/{id}/reject
```

### ERP 销售订单
```ts
erpApi.getSaleOrderPage(params)                   // GET /erp/sale-orders/page
erpApi.createSaleOrder(data)                       // POST /erp/sale-order
erpApi.submitDraftSaleOrder(id)                   // PUT /erp/sale-orders/{id}/submit
erpApi.approveSaleOrder(id)                       // PUT /erp/sale-order/{id}/approve
erpApi.rejectSaleOrder(id)                       // PUT /erp/sale-order/{id}/reject
```

### ERP 采购订单
```ts
erpApi.getPurchaseOrderPage(params)              // GET /erp/purchase-orders/page
erpApi.createPurchaseOrder(data)                  // POST /erp/purchase-orders
erpApi.confirmInbound(id)                        // PUT /erp/purchase-orders/{id}/confirm-inbound
erpApi.approvePurchaseOrder(id)                   // PUT /erp/purchase-orders/{id}/approve
```

---

## 开发注意事项

1. **OA / Wage 路由**：`/api/v1/oa/**` 走 8083，`/api/v1/wage/**` 走 8084，确保对应服务已启动
2. **预登录票据有效期**：`preAuthToken` 有效期 300 秒，超时需重新输入账号密码
3. **店铺未分配**：`confirmShopEntry` 时若店铺列表为空，后端返回错误，前端提示"账号未分配可访问店铺"
4. **多租户隔离**：后端根据 Token 解析 tenantId，前端无需手动传 `X-Tenant-Id`
5. **分页兼容**：`PageResult` 同时支持 `records` 和 `list` 字段，前端做了兜底
6. **操作日志**：`@OpLog` 注解的接口后端会记录操作轨迹，包含 URL / 参数 / 耗时
7. **ADMIN 角色**：`role_code = 'ADMIN'` 才允许访问管理接口（用户/角色/菜单等写操作）
