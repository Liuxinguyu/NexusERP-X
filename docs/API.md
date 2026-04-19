# NexusERP-X API Reference

## 项目概览

NexusERP-X 是一个基于 Spring Cloud 微服务架构 + Vue 3 前端的 SaaS 企业管理平台。

### 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3 + Spring Cloud Gateway + MyBatis-Plus |
| 网关 | Spring Cloud Gateway（端口 8080） |
| 认证 | JWT + Redis（Token 管理 / 在线用户会话） |
| 多租户 | TenantId 头部字段隔离 |
| 数据库 | MySQL 8 |
| 注册中心 | Consul（可选，本地 dev 跳过） |

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API 网关，所有前端请求入口 |
| nexus-system | 8081 | 系统管理（用户/角色/菜单/考勤/工作台等） |
| nexus-erp | 8082 | ERP 进销存（商品/订单/仓库/财务等） |
| nexus-auth | 8085 | 认证服务（健康检查，实际认证由 nexus-system 处理） |
| nexus-oa | - | OA 协同（考勤/审批/任务/日程等，与 system 共用数据库） |
| nexus-wage | - | 薪酬管理 |

---

## 统一请求 / 响应规范

### 请求头

| Header | 说明 |
|--------|------|
| `Authorization` | `Bearer {accessToken}`，除登录/验证码外全部需要 |
| `X-Tenant-Id` | 租户 ID（可选，部分接口从 Token 中解析） |

### 统一响应包装

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

- `code=200` 或 `code=0` 视为成功
- 其他 code 弹出 Element Plus 错误消息并 reject

### 分页响应

```json
{
  "records": [...],
  "total": 100,
  "current": 1,
  "size": 10,
  "pages": 10
}
```

> 注意：部分接口同时兼容 `records` 和 `list` 字段，前端需做兜底处理。

---

## 鉴权流程

### 两步登录

```
Step 1: POST /api/v1/auth/login
        ← { preAuthToken, tenantId, shops[], expiresInSeconds }

Step 2: POST /api/v1/auth/confirm-shop
        body: { preAuthToken, shopId }
        ← { accessToken, currentShopId, currentOrgId, dataScope }
```

### 令牌刷新

- Gateway 拦截器自动在 JWT 过期前 5 分钟通过 `POST /api/v1/auth/refresh` 刷新
- 刷新期间并发请求被队列化，只允许一个刷新请求

### 权限模型

| 角色 | 说明 |
|------|------|
| ADMIN | 店长，可管理用户/角色/菜单/店铺/公告等 |
| STAFF | 分店员工，仅可访问被授权的菜单 |

- 接口级：`@PreAuthorize("hasRole('ADMIN')")`
- 写操作记录操作日志（`@OpLog`）

---

## 认证相关 `/api/v1/auth`

> 前端 base URL：`/api/v1`（网关代理）

### 获取验证码图片

```
GET /api/v1/system/captcha/image
```

| 返回字段 | 类型 | 说明 |
|----------|------|------|
| uuid | string | 验证码 key |
| img | string | Base64 PNG 图片 |

### 验证验证码

```
POST /api/v1/system/captcha/validate
Content-Type: x-www-form-urlencoded
```

| 参数 | 说明 |
|------|------|
| uuid | 验证码 UUID |
| code | 用户输入的验证码 |

### 预登录

```
POST /api/v1/auth/login
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | ✓ | 用户名 |
| password | string | ✓ | 密码 |
| captcha | string | | 验证码 |
| captchaKey | string | | 验证码 UUID |

**返回**

```json
{
  "preAuthToken": "...",
  "tenantId": 1,
  "shops": [{ "shopId": 1, "shopName": "总店" }],
  "expiresInSeconds": 300,
  "requiresShopSelection": true
}
```

### 确认店铺

```
POST /api/v1/auth/confirm-shop
```

| 参数 | 类型 | 说明 |
|------|------|------|
| preAuthToken | string | 预登录票据 |
| shopId | number | 选择的店铺 ID |

**返回**

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "tenantId": 1,
  "currentShopId": 1,
  "currentOrgId": 2,
  "dataScope": 4,
  "accessibleShopIds": [1, 2],
  "accessibleOrgIds": [1, 2]
}
```

### 切换店铺（已登录）

```
PUT /api/v1/auth/switch-shop
```

| 参数 | 类型 | 说明 |
|------|------|------|
| shopId | number | 目标店铺 ID |

### 获取用户信息

```
GET /api/v1/system/user/info
```

**返回**

```json
{
  "profile": {
    "userId": 1,
    "username": "admin",
    "realName": "管理员",
    "avatarUrl": null,
    "tenantId": 1,
    "currentShopId": 1,
    "currentOrgId": 2,
    "dataScope": 4,
    "accessibleShopIds": [1, 2],
    "accessibleOrgIds": [1, 2]
  },
  "menus": [{ ... }],
  "latestNoticeTitle": "欢迎使用..."
}
```

### 登出

```
POST /api/v1/auth/logout
```

---

## 系统管理 `/api/v1/system`

### 用户管理

```
GET  /api/v1/system/users/page       分页查询用户
POST /api/v1/system/users            新建用户
PUT  /api/v1/system/users/{id}       修改用户
GET  /api/v1/system/users/{id}/shop-roles   获取用户店铺角色
PUT  /api/v1/system/users/{id}/shop-roles   分配店铺角色
```

### 角色管理

```
GET  /api/v1/system/roles/page       分页查询角色
GET  /api/v1/system/roles/options    角色下拉选项
POST /api/v1/system/roles            新建角色
PUT  /api/v1/system/roles/{id}       修改角色
GET  /api/v1/system/roles/{id}/menu-ids    获取角色菜单ID列表
PUT  /api/v1/system/roles/{id}/menus      分配菜单权限
```

### 菜单管理

```
GET /api/v1/system/menus/tree   获取完整菜单树（ADMIN）
```

### 机构管理

```
GET    /api/v1/system/org/tree         获取机构树（全量）
GET    /api/v1/system/org/tree-lazy   懒加载子机构
POST   /api/v1/system/org             新建机构（ADMIN）
PUT    /api/v1/system/org             修改机构（ADMIN）
DELETE /api/v1/system/org/{id}         删除机构（ADMIN）
```

### 店铺管理

```
GET /api/v1/system/shops/page      分页查询店铺
GET /api/v1/system/shops/options    店铺下拉选项
POST /api/v1/system/shops           新建店铺（ADMIN）
PUT  /api/v1/system/shops/{id}      修改店铺（ADMIN）
PUT  /api/v1/system/shops/{id}/status  启用/禁用店铺（ADMIN）
```

### 通知公告

```
GET  /api/v1/system/notice/page      分页公告列表
POST /api/v1/system/notice            新建公告（ADMIN）
PUT  /api/v1/system/notice/{id}      修改公告（ADMIN）
PUT  /api/v1/system/notice/{id}/publish  发布公告（ADMIN）
```

### 登录日志

```
GET /api/v1/system/login-log/page   分页登录日志（ADMIN）
```

### 在线用户

```
GET    /api/v1/system/online-users   分页在线用户列表（ADMIN）
DELETE /api/v1/system/online-users/{userId}  强制下线（ADMIN）
```

### 数据字典

```
GET /api/v1/system/dict-type/list          获取字典类型列表
GET /api/v1/system/dict-item/list-by-type  获取字典项（需传 dictType）
```

### 站内消息

```
GET /api/v1/system/message/unread-count  未读消息数量
GET /api/v1/system/message/page          分页消息列表
PUT /api/v1/system/message/read-all      全部标记已读
```

### 工作台

```
GET /api/v1/system/workbench/dashboard     工作台摘要数据
GET /api/v1/system/workbench/sales-chart   销售图表
GET /api/v1/system/workbench/purchase-chart 采购图表
GET /api/v1/system/workbench/top-products  热销商品 TOP N
GET /api/v1/system/workbench/stock-alarms  库存预警列表
```

---

## OA 协同 `/api/v1/oa`

### 员工档案

```
GET    /api/v1/oa/employees/page    分页员工列表
GET    /api/v1/oa/employees/{id}    获取员工详情
POST   /api/v1/oa/employees         新建员工
PUT    /api/v1/oa/employees/{id}    修改员工
DELETE /api/v1/oa/employees/{id}    删除员工
```

### 请假申请

```
GET  /api/v1/oa/leave-requests/page   分页请假列表
GET  /api/v1/oa/leave-requests/{id}   请假详情
POST /api/v1/oa/leave-requests       新建请假
PUT  /api/v1/oa/leave-requests/{id}   修改请假
DELETE /api/v1/oa/leave-requests/{id} 删除请假
PUT  /api/v1/oa/leave-requests/{id}/submit   提交请假
PUT  /api/v1/oa/leave-requests/{id}/approve  审批请假
```

### 考勤打卡

```
GET /api/v1/oa/attendance/rules             考勤规则列表
POST /api/v1/oa/attendance/rules            新建考勤规则（ADMIN）
DELETE /api/v1/oa/attendance/rules/{id}     删除考勤规则（ADMIN）
POST /api/v1/oa/attendance/check-in         员工打卡
GET  /api/v1/oa/attendance/my-today         今日打卡状态
GET  /api/v1/oa/attendance/records/page    分页考勤记录
GET  /api/v1/oa/attendance/leave/page      分页请假记录
POST /api/v1/oa/attendance/leave           新建请假申请
DELETE /api/v1/oa/attendance/leave/{id}    删除请假
POST /api/v1/oa/attendance/leave/{id}/submit   提交请假
POST /api/v1/oa/attendance/leave/{id}/approve  审批请假
GET  /api/v1/oa/attendance/overtime/page    分页加班记录
POST /api/v1/oa/attendance/overtime         新建加班申请
DELETE /api/v1/oa/attendance/overtime/{id}   删除加班
POST /api/v1/oa/attendance/overtime/{id}/submit  提交加班
POST /api/v1/oa/attendance/overtime/{id}/approve 审批加班
GET  /api/v1/oa/attendance/statistics/monthly   月度考勤统计
```

### 审批中心

```
GET /api/v1/oa/approval/tasks/my-apply      我发起的审批
GET /api/v1/oa/approval/tasks/my-approve    待我审批的任务
GET /api/v1/oa/approval/tasks/{id}          审批任务详情
POST /api/v1/oa/approval/tasks/{id}/approve  通过
POST /api/v1/oa/approval/tasks/{id}/reject  驳回
```

### 任务管理

```
GET    /api/v1/oa/tasks/page          分页任务列表
GET    /api/v1/oa/tasks/{id}         任务详情
POST   /api/v1/oa/tasks              新建任务
PUT    /api/v1/oa/tasks/{id}         修改任务
DELETE /api/v1/oa/tasks/{id}         删除任务
PUT    /api/v1/oa/tasks/{id}/accept          接受任务
PUT    /api/v1/oa/tasks/{id}/progress         更新进度（?progress=50）
PUT    /api/v1/oa/tasks/{id}/complete         完成任务
PUT    /api/v1/oa/tasks/{id}/cancel           取消任务
GET    /api/v1/oa/tasks/{id}/comments         评论列表
POST   /api/v1/oa/tasks/{id}/comment          添加评论
```

### 排班日程

```
GET    /api/v1/oa/schedules           日程列表
POST   /api/v1/oa/schedules           新建日程
PUT    /api/v1/oa/schedules/{id}      修改日程
DELETE /api/v1/oa/schedules/{id}      删除日程
```

### 文件管理

```
GET    /api/v1/oa/files/folders        文件夹列表
POST   /api/v1/oa/files/folders        新建文件夹
DELETE /api/v1/oa/files/folders/{id}   删除文件夹
GET    /api/v1/oa/files                文件列表
POST   /api/v1/oa/files/upload         上传文件
GET    /api/v1/oa/files/{id}/download  下载文件
DELETE /api/v1/oa/files/{id}           删除文件
```

---

## ERP 进销存 `/api/v1/erp`

### 商品管理

```
GET  /api/v1/erp/products/page        分页商品列表
POST /api/v1/erp/products             新建商品
PUT  /api/v1/erp/products/{id}       修改商品
PUT  /api/v1/erp/products/{id}/status  修改商品状态

GET  /api/v1/erp/product-infos/page   分页商品信息列表
POST /api/v1/erp/product-infos        新建商品信息
PUT  /api/v1/erp/product-infos/{id}   修改商品信息
DELETE /api/v1/erp/product-infos/{id} 删除商品信息
```

### 商品分类

```
GET    /api/v1/erp/product-categories/tree   分类树
GET    /api/v1/erp/product-categories/list   分类列表（扁平）
POST   /api/v1/erp/product-categories       新建分类
PUT    /api/v1/erp/product-categories/{id}  修改分类
DELETE /api/v1/erp/product-categories/{id}  删除分类
```

### 客户管理

```
GET  /api/v1/erp/customers/page   分页客户列表
POST /api/v1/erp/customers        新建客户
PUT  /api/v1/erp/customers/{id}   修改客户
```

### 供应商管理

```
GET    /api/v1/erp/suppliers/page    分页供应商列表
POST   /api/v1/erp/suppliers         新建供应商
PUT    /api/v1/erp/suppliers/{id}    修改供应商
DELETE /api/v1/erp/suppliers/{id}    删除供应商
```

### 仓库管理

```
GET    /api/v1/erp/warehouses/page   分页仓库列表
POST   /api/v1/erp/warehouses        新建仓库
PUT    /api/v1/erp/warehouses/{id}   修改仓库
DELETE /api/v1/erp/warehouses/{id}   删除仓库
```

### 库存查询

```
GET /api/v1/erp/stocks/page   分页库存列表（支持 productId/warehouseId 过滤）
```

### 销售订单

```
GET  /api/v1/erp/sale-orders/page      分页销售订单
GET  /api/v1/erp/sale-orders/{id}     订单详情
GET  /api/v1/erp/sale-orders/{id}/items 订单明细
POST /api/v1/erp/sale-order            新建销售订单
POST /api/v1/erp/sale-order/submit    一键提交（建单+扣库存+发货）
PUT  /api/v1/erp/sale-orders/{id}/submit   提交订单
PUT  /api/v1/erp/sale-order/{id}/approve   审批通过
PUT  /api/v1/erp/sale-order/{id}/reject    审批拒绝
DELETE /api/v1/erp/sale-order/{id}          删除订单
```

### 采购订单

```
GET  /api/v1/erp/purchase-orders/page      分页采购订单
GET  /api/v1/erp/purchase-orders/{id}     采购订单详情
GET  /api/v1/erp/purchase-orders/{id}/items 订单明细
POST /api/v1/erp/purchase-orders           新建采购订单
PUT  /api/v1/erp/purchase-orders/{id}     修改采购订单
PUT  /api/v1/erp/purchase-orders/{id}/confirm-inbound  确认入库
PUT  /api/v1/erp/purchase-orders/{id}/submit   提交审批
PUT  /api/v1/erp/purchase-orders/{id}/approve   审批通过
PUT  /api/v1/erp/purchase-orders/{id}/reject    审批拒绝
DELETE /api/v1/erp/purchase-orders/{id}          删除订单
```

### 应收款

```
GET /api/v1/erp/receivables/page       分页应收款列表
GET /api/v1/erp/receivables/{id}      应收款详情
POST /api/v1/erp/receivables          新建应收款
PUT  /api/v1/erp/receivables/{id}     修改应收款
DELETE /api/v1/erp/receivables/{id}   删除应收款
POST /api/v1/erp/receivables/{id}/record   记录收款
GET  /api/v1/erp/receivables/{id}/records  收款记录列表
GET  /api/v1/erp/receivables/summary      应收汇总（按客户/月）
```

### 应付款

```
GET /api/v1/erp/payables/page       分页应付款列表
GET /api/v1/erp/payables/{id}      应付款详情
POST /api/v1/erp/payables          新建应付款
PUT  /api/v1/erp/payables/{id}     修改应付款
DELETE /api/v1/erp/payables/{id}   删除应付款
POST /api/v1/erp/payables/{id}/record   记录付款
GET  /api/v1/erp/payables/{id}/records  付款记录列表
GET  /api/v1/erp/payables/summary      应付款汇总（按供应商/月）
```

### 报表

```
GET /api/v1/erp/reports/sales/monthly       月度销售汇总
GET /api/v1/erp/reports/sales/trend         年度销售趋势
GET /api/v1/erp/reports/sales/product-rank  商品销售排行
GET /api/v1/erp/reports/sales/customer-rank 客户销售排行
GET /api/v1/erp/reports/stock/alarm         库存预警
GET /api/v1/erp/reports/stock/summary       库存分类汇总
```

### 商机 & 合同

```
GET    /api/v1/erp/opportunities/page   分页商机列表
GET    /api/v1/erp/opportunities/{id}   商机详情
POST   /api/v1/erp/opportunities        新建商机
PUT    /api/v1/erp/opportunities/{id}   修改商机
DELETE /api/v1/erp/opportunities/{id}   删除商机
PUT    /api/v1/erp/opportunities/{id}/stage  推进商机阶段

GET    /api/v1/erp/contracts/page       分页合同列表
GET    /api/v1/erp/contracts/{id}        合同详情
GET    /api/v1/erp/contracts/{id}/items  合同明细
POST   /api/v1/erp/contracts             新建合同
PUT    /api/v1/erp/contracts/{id}        修改合同
DELETE /api/v1/erp/contracts/{id}         删除合同
```

---

## 薪酬管理 `/api/v1/wage`

> 需要 ADMIN 角色

### 薪酬项目配置

```
GET    /api/v1/wage/item-configs          配置列表
GET    /api/v1/wage/item-configs/{id}    详情
POST   /api/v1/wage/item-configs         新建配置
PUT    /api/v1/wage/item-configs/{id}    修改配置
DELETE /api/v1/wage/item-configs/{id}    删除配置
```

### 月度工资条

```
GET  /api/v1/wage/monthly-slips           月度工资列表
GET  /api/v1/wage/monthly-slips/{id}     工资条详情
POST /api/v1/wage/monthly-slips/generate 生成指定月工资
PUT  /api/v1/wage/monthly-slips/{id}/adjust  调整工资
POST /api/v1/wage/monthly-slips/confirm-pay 确认发放
DELETE /api/v1/wage/monthly-slips/{id}   删除工资条
```

---

## 数据库表结构

> 所有表均继承 `BaseTenantEntity`，包含 `tenant_id`、`create_time`、`update_time`、`create_by`、`update_by`、`del_flag` 通用字段（省略标注）。

### 系统模块

#### sys_user — 系统用户
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | 租户ID |
| username | VARCHAR(64) | 登录用户名 |
| password_hash | VARCHAR(255) | 密码（BCrypt） |
| real_name | VARCHAR(64) | 真实姓名 |
| avatar_url | VARCHAR(512) | 头像URL |
| status | TINYINT | 1正常 0停用 |
| main_shop_id | BIGINT | 主店铺 |
| main_org_id | BIGINT | 主机构 |

#### sys_role — 角色
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| shop_id | BIGINT | 所属店铺（可空） |
| role_code | VARCHAR(64) | 角色代码（唯一） |
| role_name | VARCHAR(128) | 角色名称 |
| data_scope | TINYINT | 数据权限范围，4=全部 |

#### sys_org — 组织机构
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| parent_id | BIGINT | 父机构ID，0为根 |
| ancestors | VARCHAR(512) | 祖籍链，逗号分隔 |
| org_code | VARCHAR(64) | 机构编码 |
| org_name | VARCHAR(128) | 机构名称 |
| org_type | TINYINT | 1总公司 2分公司 |
| sort | INT | 排序 |

#### sys_shop — 店铺
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| org_id | BIGINT | 所属机构 |
| shop_name | VARCHAR(128) | 店铺名称 |
| shop_type | TINYINT | 店铺类型 |
| status | TINYINT | 1正常 0停用 |

#### sys_menu — 菜单
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| parent_id | BIGINT | 父菜单ID，0为根 |
| menu_type | CHAR(1) | M目录 C菜单 F按钮 |
| menu_name | VARCHAR(128) | 菜单名称 |
| path | VARCHAR(256) | 路由路径 |
| component | VARCHAR(256) | 前端组件路径 |
| perms | VARCHAR(128) | 权限标识 |
| icon | VARCHAR(128) | 图标 |
| sort | INT | 排序 |
| visible | TINYINT | 1显示 0隐藏 |
| status | TINYINT | 1正常 0停用 |

#### sys_user_shop_role — 用户店铺角色关联
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| user_id | BIGINT | |
| shop_id | BIGINT | |
| role_id | BIGINT | |

#### sys_notice — 通知公告
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| title | VARCHAR(256) | 标题 |
| content | TEXT | 内容 |
| notice_type | VARCHAR(32) | 类型 |
| status | TINYINT | 状态 |
| expire_time | TIMESTAMP | 过期时间 |

#### sys_message — 站内消息
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| user_id | BIGINT | 接收用户 |
| title | VARCHAR(256) | |
| content | TEXT | |
| message_type | VARCHAR(32) | |
| is_read | TINYINT | 0未读 1已读 |

#### sys_dict_type — 字典类型
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| dict_name | VARCHAR(128) | 字典名称 |
| dict_type | VARCHAR(64) | 字典类型（唯一） |
| status | TINYINT | |
| remark | VARCHAR(512) | |

#### sys_dict_item — 字典项
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| dict_type | VARCHAR(64) | 所属字典类型 |
| label | VARCHAR(128) | 显示标签 |
| item_value | VARCHAR(128) | 存储值 |
| sort | INT | 排序 |

#### sys_config — 系统参数
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| config_name | VARCHAR(128) | 配置名称 |
| config_key | VARCHAR(128) | 配置键（唯一） |
| config_value | VARCHAR(1024) | 配置值 |
| config_type | CHAR(1) | Y内置 N自定义 |

#### sys_oper_log — 操作日志
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| user_id | BIGINT | |
| username | VARCHAR(64) | |
| module | VARCHAR(128) | 模块名 |
| oper_type | VARCHAR(64) | 操作类型 |
| oper_url | VARCHAR(512) | 请求URL |
| oper_method | VARCHAR(16) | HTTP方法 |
| oper_ip | VARCHAR(64) | IP地址 |
| request_param | CLOB | 请求参数 |
| status | INT | 0失败 1成功 |
| error_msg | VARCHAR(1024) | 错误信息 |
| cost_time | BIGINT | 耗时ms |

#### sys_login_log — 登录日志
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| username | VARCHAR(64) | |
| status | TINYINT | 0失败 1成功 |
| ip | VARCHAR(64) | |
| user_agent | VARCHAR(512) | |
| msg | VARCHAR(512) | |

---

### OA 模块

#### oa_employee — 员工档案
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| emp_no | VARCHAR(32) | 工号（唯一） |
| name | VARCHAR(64) | 姓名 |
| dept | VARCHAR(128) | 部门 |
| position | VARCHAR(64) | 职位 |
| hire_date | DATE | 入职日期 |
| phone | VARCHAR(32) | |
| status | TINYINT | 1在职 0离职 |
| user_id | BIGINT | 关联系统用户 |
| direct_leader_user_id | BIGINT | 直接上级 |

#### oa_leave_request — 简易请假（老版本）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| applicant_user_id | BIGINT | 申请人 |
| leave_type | VARCHAR(32) | 请假类型 |
| start_time | TIMESTAMP | 开始时间 |
| end_time | TIMESTAMP | 结束时间 |
| leave_days | DECIMAL(10,2) | 请假天数 |
| reason | VARCHAR(512) | 原因 |
| status | TINYINT | 状态 |
| approver_user_id | BIGINT | 审批人 |

#### oa_attendance_rule — 考勤规则
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| rule_name | VARCHAR(64) | 规则名称 |
| check_in_start | TIME | 上班签到开始 |
| check_in_end | TIME | 上班签到截止 |
| check_out_start | TIME | 下班签退开始 |
| check_out_end | TIME | 下班签退截止 |
| is_enable | TINYINT | 1启用 0禁用 |

#### oa_attendance_record — 考勤记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| user_id | BIGINT | 关联 sys_user |
| check_date | DATE | 考勤日期 |
| check_in_time | DATETIME | 上班打卡时间 |
| check_out_time | DATETIME | 下班打卡时间 |
| work_minutes | INT | 工作时长（分钟） |
| status | TINYINT | 0正常 1迟到 2早退 3缺卡 4旷工 5加班 |
| is_outer | TINYINT | 0否 1外勤 |
| outer_address | VARCHAR(256) | 外勤地址 |

#### oa_leave_detail — 请假明细（新版本）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| leave_no | VARCHAR(64) | 请假编号（唯一） |
| user_id | BIGINT | 申请人 |
| user_name | VARCHAR(64) | |
| leave_type | VARCHAR(32) | 年假/病假/事假/婚假/产假/其他 |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| leave_days | DECIMAL(5,1) | 天数 |
| reason | TEXT | 原因 |
| status | TINYINT | 0草稿 1待审批 2已通过 3已拒绝 |
| approver_user_id | BIGINT | 审批人 |
| approver_opinion | TEXT | 审批意见 |
| approver_time | DATETIME | 审批时间 |

#### oa_overtime — 加班记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| overtime_no | VARCHAR(64) | 加班单号（唯一） |
| user_id | BIGINT | 申请人 |
| user_name | VARCHAR(64) | |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| hours | DECIMAL(5,1) | 加班时长（小时） |
| reason | TEXT | |
| status | TINYINT | 0草稿 1待审批 2已通过 3已拒绝 |
| approver_user_id | BIGINT | 审批人 |

#### oa_approval_task — 通用审批任务
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| task_no | VARCHAR(64) | 任务编号 |
| biz_type | VARCHAR(32) | 业务类型（leave/overtime/purchase） |
| biz_id | BIGINT | 业务数据ID |
| title | VARCHAR(256) | 审批标题 |
| content_summary | VARCHAR(512) | 内容摘要 |
| applicant_user_id | BIGINT | 申请人 |
| applicant_user_name | VARCHAR(64) | |
| approver_user_id | BIGINT | 审批人 |
| approver_user_name | VARCHAR(64) | |
| status | TINYINT | 0待审批 1已通过 2已拒绝 |
| opinion | TEXT | 审批意见 |
| approve_time | DATETIME | |

#### oa_task — 任务看板
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| task_no | VARCHAR(64) | 任务编号（唯一） |
| title | VARCHAR(256) | 标题 |
| description | TEXT | 描述 |
| priority | TINYINT | 1紧急 2高 3中 4低 |
| status | TINYINT | 0待接受 1进行中 2已完成 3已取消 |
| assignee_user_id | BIGINT | 负责人 |
| assignee_user_name | VARCHAR(64) | |
| creator_user_id | BIGINT | 创建人 |
| due_date | DATE | 截止日期 |
| progress | INT | 进度 0-100 |
| tags | VARCHAR(256) | 标签 |

#### oa_task_comment — 任务评论
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| task_id | BIGINT | |
| user_id | BIGINT | 评论人 |
| user_name | VARCHAR(64) | |
| content | TEXT | 评论内容 |

#### oa_schedule — 日程管理
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| title | VARCHAR(256) | 日程标题 |
| content | TEXT | |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| is_all_day | TINYINT | 0否 1是 |
| location | VARCHAR(256) | 地点 |
| color | VARCHAR(16) | 日历颜色 |
| visibility | TINYINT | 0私有 1公开 |
| creator_user_id | BIGINT | 创建人 |

#### oa_file_folder — 云空间文件夹
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| parent_id | BIGINT | 父文件夹，0为根 |
| folder_name | VARCHAR(128) | |
| visibility | TINYINT | 0私有 1公开 |
| owner_user_id | BIGINT | 所有者 |

#### oa_file — 云空间文件
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| folder_id | BIGINT | 所属文件夹，0为根 |
| file_name | VARCHAR(256) | 文件名 |
| file_key | VARCHAR(256) | 存储KEY |
| file_size | BIGINT | 大小（字节） |
| file_type | VARCHAR(64) | 扩展名 |
| download_count | INT | 下载次数 |
| visibility | TINYINT | 0私有 1公开 |
| owner_user_id | BIGINT | |

---

### ERP 模块

#### erp_product_category — 商品分类
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| name | VARCHAR(100) | 分类名称 |
| parent_id | BIGINT | 父分类，0为根 |
| sort_order | INT | 排序 |
| status | TINYINT | 1正常 0停用 |

#### erp_product_info — 商品信息（SKU）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| code | VARCHAR(50) | 商品编码 |
| name | VARCHAR(100) | 商品名称 |
| category_id | BIGINT | 关联分类 |
| spec | VARCHAR(200) | 规格型号 |
| unit | VARCHAR(20) | 单位 |
| price | DECIMAL(10,2) | 单价 |
| stock | INT | 库存数量 |
| status | TINYINT | 1正常 0停用 |

#### erp_warehouse — 仓库
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| code | VARCHAR(50) | 仓库编码 |
| name | VARCHAR(100) | 仓库名称 |
| manager | VARCHAR(50) | 负责人 |
| phone | VARCHAR(20) | 联系电话 |
| address | VARCHAR(255) | 地址 |
| status | TINYINT | 1正常 0停用 |

#### erp_supplier — 供应商
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| code | VARCHAR(50) | 供应商编码 |
| name | VARCHAR(100) | 供应商名称 |
| contact_person | VARCHAR(50) | 联系人 |
| phone | VARCHAR(20) | 电话 |
| email | VARCHAR(100) | |
| bank_name | VARCHAR(100) | 开户行 |
| bank_account | VARCHAR(50) | 银行账号 |
| status | TINYINT | 1正常 0停用 |

#### erp_stock — 库存
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| product_id | BIGINT | 关联商品 |
| warehouse_id | BIGINT | 关联仓库 |
| qty | INT | 库存数量 |
| UNIQUE | (tenant_id, product_id, warehouse_id) | |

#### erp_sale_order — 销售订单
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| order_no | VARCHAR(64) | 订单号（唯一） |
| customer_name | VARCHAR(128) | 客户名称 |
| warehouse_id | BIGINT | 出库仓库 |
| total_amount | DECIMAL(18,2) | 订单总额 |
| status | TINYINT | 订单状态 |
| UNIQUE | (tenant_id, order_no) | |

#### erp_sale_order_item — 销售订单明细
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| order_id | BIGINT | 关联销售订单 |
| product_id | BIGINT | 关联商品 |
| quantity | INT | 数量 |
| unit_price | DECIMAL(18,2) | 单价 |
| subtotal | DECIMAL(18,2) | 小计金额 |

#### erp_purchase_order — 采购订单
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| order_no | VARCHAR(64) | 订单号（唯一） |
| supplier_id | BIGINT | 关联供应商 |
| warehouse_id | BIGINT | 入库仓库 |
| total_amount | DECIMAL(18,2) | 订单总额 |
| status | TINYINT | 订单状态 |
| remark | VARCHAR(512) | 备注 |
| UNIQUE | (tenant_id, order_no) | |

#### erp_purchase_order_item — 采购订单明细
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| order_id | BIGINT | 关联采购订单 |
| product_id | BIGINT | 关联商品 |
| quantity | INT | 数量 |
| unit_price | DECIMAL(18,2) | 单价 |
| subtotal | DECIMAL(18,2) | 小计金额 |

#### fin_receivable — 应收账款
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| receivable_no | VARCHAR(64) | 应收单号（唯一） |
| source_type | VARCHAR(16) | 来源（sale_order/crm_contract） |
| source_id | BIGINT | 来源单据ID |
| customer_id | BIGINT | 关联客户 |
| customer_name | VARCHAR(128) | |
| total_amount | DECIMAL(18,2) | 应收总额 |
| received_amount | DECIMAL(18,2) | 已收金额 |
| pending_amount | DECIMAL(18,2) | 待收金额 |
| invoice_no | VARCHAR(64) | 发票号 |
| due_date | DATE | 到期日期 |
| status | TINYINT | 0未回款 1部分回款 2已结清 |

#### fin_receivable_record — 收款记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| receivable_id | BIGINT | 关联应收单 |
| record_no | VARCHAR(64) | 收款流水号（唯一） |
| amount | DECIMAL(18,2) | 收款金额 |
| payment_method | VARCHAR(32) | 银行转账/现金/支票/微信/支付宝 |
| payment_time | DATETIME | 收款时间 |
| handler_user_id | BIGINT | 经办人 |
| receipt_url | VARCHAR(512) | 凭证URL |

#### fin_payable — 应付账款
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| payable_no | VARCHAR(64) | 应付单号（唯一） |
| source_type | VARCHAR(16) | 来源（purchase_order） |
| source_id | BIGINT | 来源单据ID |
| supplier_id | BIGINT | 关联供应商 |
| supplier_name | VARCHAR(128) | |
| total_amount | DECIMAL(18,2) | 应付总额 |
| paid_amount | DECIMAL(18,2) | 已付金额 |
| pending_amount | DECIMAL(18,2) | 待付金额 |
| invoice_no | VARCHAR(64) | 发票号 |
| due_date | DATE | 到期日期 |
| status | TINYINT | 0未付款 1部分付款 2已结清 |

#### fin_payable_record — 付款记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| payable_id | BIGINT | 关联应付单 |
| record_no | VARCHAR(64) | 付款流水号（唯一） |
| amount | DECIMAL(18,2) | 付款金额 |
| payment_method | VARCHAR(32) | 银行转账/现金/支票 |
| payment_account | VARCHAR(128) | 付款账户 |
| payment_time | DATETIME | |
| handler_user_id | BIGINT | 经办人 |

#### erp_opportunity — 商机
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| customer_id | BIGINT | 关联客户 |
| customer_name | VARCHAR(128) | |
| opportunity_name | VARCHAR(256) | 商机名称 |
| amount | DECIMAL(18,2) | 预估金额 |
| stage | VARCHAR(32) | 阶段：线索/需求确认/方案/报价/成交/失败 |
| probability | INT | 赢单概率 0-100 |
| expect_close_date | DATE | 预计成交日期 |
| owner_user_id | BIGINT | 负责人 |
| status | TINYINT | 1进行中 0已关闭 |

#### erp_contract — 合同
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| contract_no | VARCHAR(64) | 合同编号（唯一） |
| contract_name | VARCHAR(256) | 合同名称 |
| customer_id | BIGINT | 关联客户 |
| opportunity_id | BIGINT | 关联商机 |
| sign_date | DATE | 签订日期 |
| start_date | DATE | 生效日期 |
| end_date | DATE | 到期日期 |
| amount | DECIMAL(18,2) | 合同金额 |
| signed_by | VARCHAR(128) | 签约人 |
| attachment_urls | TEXT | 附件URL |
| status | TINYINT | 1执行中 2到期 3终止 |

#### erp_contract_item — 合同明细
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| contract_id | BIGINT | 关联合同 |
| product_id | BIGINT | 关联商品 |
| product_name | VARCHAR(256) | 商品名称 |
| quantity | INT | 数量 |
| unit_price | DECIMAL(18,2) | 单价 |
| subtotal | DECIMAL(18,2) | 小计 |

---

### Wage 模块

#### wage_item_config — 薪酬项目配置
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| item_name | VARCHAR(128) | 薪资项名称 |
| calc_type | TINYINT | 1固定值 2手动录入 |
| default_amount | DECIMAL(18,2) | 默认金额 |
| item_kind | TINYINT | 1基本工资 2补贴 3扣款 |

#### wage_monthly_slip — 月度工资单
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| belong_month | VARCHAR(7) | 归属月份，如 2024-10 |
| employee_id | BIGINT | 关联 OA 员工 |
| base_salary | DECIMAL(18,2) | 基本工资 |
| subsidy_total | DECIMAL(18,2) | 补贴合计 |
| deduction_total | DECIMAL(18,2) | 扣款合计 |
| net_pay | DECIMAL(18,2) | 实发工资 |
| status | TINYINT | 0待确认 1已发放 |
| UNIQUE | (tenant_id, belong_month, employee_id) | |

---


## 附录：已知注意事项

1. **部分 ERP 接口路径前缀混用**：`/erp/sale-order`（无 s）和 `/erp/sale-orders`（有 s）同时存在，前端 `erpApi` 中已做兼容。
2. **oaApi 接口冗余**：`oa.ts` 中存在同名方法别名（如 `getLeavePage` / `getLeaveRequestPage`），为兼容历史调用。
3. **登录两步分开**：`preAuthLogin` 不返回 JWT，JWT 在 `confirmShopEntry` 之后才下发。
4. **ADMIN 角色硬编码**：`role_code = 'ADMIN'` 的角色才有后台管理权限。
5. **多租户隔离**：所有写操作均通过 `TenantContext` 或 SQL 条件 `tenant_id = ?` 隔离。
