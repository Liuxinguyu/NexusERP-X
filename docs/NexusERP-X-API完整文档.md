# NexusERP-X 完整系统文档

> 文档生成日期：2026-04-14
> 技术栈：Spring Boot 3.3.x + MyBatis-Plus + Spring Cloud Gateway + Redis + MySQL

---

## 目录

1. [项目架构总览](#1-项目架构总览)
2. [模块目录结构](#2-模块目录结构)
3. [所有 API 接口清单](#3-所有-api-接口清单)
4. [实体（Domain Model）](#4-实体-domain-model)
5. [DTO 数据传输对象](#5-dto-数据传输对象)
6. [公共组件与安全机制](#6-公共组件与安全机制)
7. [数据库表清单](#7-数据库表清单)
8. [网关路由与安全配置](#8-网关路由与安全配置)

---

## 1. 项目架构总览

```
nexus-gateway  (8080)  ─── 网关：路由、CORS、JWT 在线校验
nexus-system   (8081)  ─── 系统管理：用户/角色/菜单/组织/店铺/登录日志/在线用户/公告/字典/消息/工作台
nexus-erp      (8082)  ─── ERP 业务：采购/销售/库存/客户/供应商/产品/CRM/财务报表
nexus-oa       (8083)  ─── OA 办公：员工/请假/考勤/审批中心/任务/日程/云空间
nexus-wage     (8084)  ─── 薪酬管理：薪资项配置/月工资单生成
nexus-auth     (8085)  ─── 认证服务占位（真实认证在 nexus-system）
nexus-common            ─── 公共库：Result/PageQuery/Security/JWT/Tenant/操作日志/异常处理
```

---

## 2. 模块目录结构

### 2.1 nexus-common（共享库，无端口）

```
nexus-common/
├── src/main/java/com/nexus/common/
│   ├── annotation/
│   │   └── OpLog.java                     # 操作日志注解
│   ├── audit/
│   │   ├── OpLogAspect.java               # 操作日志切面
│   │   ├── OperLogRecord.java              # 日志记录实体
│   │   └── OperLogRecorder.java            # 日志记录器
│   ├── autoconfigure/
│   │   └── NexusCommonAutoConfiguration.java
│   ├── context/
│   │   ├── DataScopeContext.java
│   │   ├── GatewayUserContext.java
│   │   ├── NexusRequestHeaders.java
│   │   ├── OrgContext.java
│   │   └── TenantContext.java
│   ├── core/domain/
│   │   ├── IResultCode.java               # 结果码接口
│   │   ├── Result.java                    # 统一响应包装
│   │   └── ResultCode.java                # 枚举实现
│   ├── core/page/
│   │   ├── PageQuery.java                 # 分页请求
│   │   └── PageResult.java                # 分页响应
│   ├── domain/model/
│   │   ├── AbstractAuditableEntity.java   # 审计字段基类
│   │   ├── BaseEntity.java                # 含租户+店铺
│   │   └── BaseTenantEntity.java          # 含租户
│   ├── exception/
│   │   └── BusinessException.java
│   ├── mybatis/config/
│   │   └── MybatisPlusConfig.java
│   ├── mybatis/datapermission/
│   │   └── DataScopeInterceptor.java      # 数据权限拦截器
│   ├── mybatis/handler/
│   │   └── NexusMetaObjectHandler.java   # 自动填充
│   ├── mybatis/tenant/
│   │   └── NexusTenantLineHandler.java
│   ├── security/
│   │   ├── BearerTokenResolver.java
│   │   ├── NexusPrincipal.java           # 认证主体（含 authorities）
│   │   └── SecurityUtils.java             # 安全工具
│   ├── security/auth/
│   │   └── NexusAuthenticationException.java
│   ├── security/config/
│   │   ├── NexusSecurityConfiguration.java # @EnableMethodSecurity
│   │   └── NexusSecurityProperties.java
│   ├── security/datascope/
│   │   └── DataScope.java                 # 数据范围枚举
│   ├── security/event/
│   │   └── JwtTokenAuthenticatedEvent.java
│   ├── security/jwt/
│   │   ├── JwtAuthenticationException.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java          # JWT 创建/解析
│   │   └── OnlineTokenValidator.java       # Redis 在线校验
│   ├── tenant/
│   │   └── NexusTenantProperties.java
│   ├── web/auth/
│   │   └── InternalAuthInterceptor.java
│   ├── web/config/
│   │   └── NexusInternalAuthWebMvcConfiguration.java
│   ├── web/exception/
│   │   └── GlobalExceptionHandler.java
│   └── web/filter/
│       └── TenantContextWebFilter.java
└── src/main/resources/
    └── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 2.2 nexus-gateway（端口 8080）

```
nexus-gateway/
├── src/main/java/com/nexus/gateway/
│   ├── NexusGatewayApplication.java
│   ├── config/
│   │   └── GatewaySecurityConfiguration.java
│   └── filter/
│       ├── AuthenticationGlobalFilter.java  # JWT 在线校验 + 公开路径放行
│       └── DownstreamContextForwardingFilter.java
└── src/main/resources/
    └── application.yml                      # 路由、CORS、JWT_SECRET
```

### 2.3 nexus-system（端口 8081）

```
nexus-system/
├── src/main/java/com/nexus/system/
│   ├── api/controller/
│   │   ├── AuthController.java             # 认证（登录/登出/刷新/店铺）
│   │   ├── SystemUserController.java        # 当前用户信息/切换组织
│   │   ├── SysUserAdminController.java      # 用户 CRUD（ADMIN）
│   │   ├── SysRoleController.java           # 角色 CRUD（ADMIN）
│   │   ├── SysOrgController.java            # 组织 CRUD（ADMIN 写）
│   │   ├── SysShopController.java           # 店铺 CRUD（ADMIN 写）
│   │   ├── SysMenuController.java           # 菜单树（ADMIN）
│   │   ├── SysLoginLogController.java       # 登录日志（ADMIN）
│   │   ├── SysOnlineUserController.java     # 在线用户/强退（ADMIN）
│   │   ├── SysNoticeController.java         # 公告 CRUD（ADMIN 写）
│   │   ├── SystemDictController.java        # 字典查询
│   │   ├── SysMessageController.java         # 消息通知
│   │   ├── WorkbenchController.java          # 工作台仪表盘
│   │   └── OaLeaveController.java            # 请假申请/审批（system 内）
│   ├── application/
│   │   ├── dto/
│   │   │   ├── AuthDtos.java
│   │   │   ├── OaLeaveDtos.java
│   │   │   ├── SysOrgTreeVO.java
│   │   │   ├── SystemAdminDtos.java
│   │   │   ├── SystemOrgDtos.java
│   │   │   ├── UserInfoDtos.java
│   │   │   └── WorkbenchDtos.java
│   │   └── service/
│   │       ├── AuthApplicationService.java          # 核心认证逻辑
│   │       ├── AuthRedisService.java                 # 认证会话 Redis
│   │       ├── OnlineUserRedisService.java            # 在线用户 Redis
│   │       ├── JwtOnlineSessionListener.java
│   │       ├── LoginCaptchaValidator.java             # 验证码校验
│   │       ├── SysLoginLogApplicationService.java    # 登录日志
│   │       ├── SysOperLogRecorderService.java        # 操作日志
│   │       ├── AdminAuthorizationService.java        # 管理员判断
│   │       ├── SysUserAdminApplicationService.java
│   │       ├── SysRoleApplicationService.java
│   │       ├── SysOrgApplicationService.java
│   │       ├── SysShopApplicationService.java
│   │       ├── SysMenuApplicationService.java
│   │       ├── SysNoticeApplicationService.java
│   │       ├── SysDictApplicationService.java
│   │       ├── SysConfigApplicationService.java
│   │       ├── SysMessageApplicationService.java
│   │       ├── SystemUserInfoService.java
│   │       ├── WorkbenchService.java
│   │       ├── OaLeaveApplicationService.java
│   │       └── OaLeaveDictValidator.java
│   ├── domain/model/
│   │   ├── SysUser.java          # 用户
│   │   ├── SysRole.java          # 角色
│   │   ├── SysMenu.java          # 菜单/权限
│   │   ├── SysOrg.java           # 组织架构
│   │   ├── SysPost.java          # 岗位
│   │   ├── SysShop.java          # 店铺
│   │   ├── SysUserPost.java      # 用户-岗位关联
│   │   ├── SysUserShopRole.java  # 用户-店铺-角色关联
│   │   ├── SysRoleMenu.java      # 角色-菜单关联
│   │   ├── SysOperLog.java       # 操作日志
│   │   ├── SysLoginLog.java      # 登录日志
│   │   ├── SysNotice.java        # 公告
│   │   ├── SysMessage.java       # 消息
│   │   ├── SysDictType.java      # 字典类型
│   │   ├── SysDictItem.java      # 字典项
│   │   ├── SysConfig.java        # 系统配置
│   │   ├── OaLeave.java          # 请假申请（system 内）
│   │   └── OaLeaveApproval.java  # 请假审批记录
│   ├── infrastructure/
│   │   ├── config/
│   │   │   └── PasswordConfig.java
│   │   └── mapper/
│   │       └── [17 个 Mapper 接口]
│   └── NexusSystemApplication.java
└── src/main/
    ├── resources/
    │   ├── application.yml               # nexus.captcha.enabled=true
    │   ├── application-dev.properties
    │   └── db/schema.sql                 # 系统表 + 种子数据
    └── mapper/WorkbenchMapper.xml        # 工作台自定义 SQL
```

### 2.4 nexus-erp（端口 8082）

```
nexus-erp/
├── src/main/java/com/nexus/erp/
│   ├── api/controller/
│   │   ├── ErpProductCategoryController.java   # 产品分类
│   │   ├── ErpSupplierController.java           # 供应商
│   │   ├── ErpWarehouseController.java          # 仓库
│   │   ├── ErpProductInfoController.java        # 产品信息
│   │   ├── ErpProductController.java            # 产品（基础）
│   │   ├── ErpPurchaseOrderController.java      # 采购入库
│   │   ├── ErpSaleOrderController.java          # 销售出库
│   │   ├── ErpCustomerController.java            # 客户
│   │   ├── ErpStockController.java              # 库存查询
│   │   ├── ErpOpportunityController.java        # 商机
│   │   ├── ErpContractController.java          # 合同
│   │   ├── ErpReportController.java             # 销售/库存报表
│   │   ├── FinReceivableController.java         # 应收账款
│   │   └── FinPayableController.java            # 应付账款
│   ├── application/
│   │   ├── dto/
│   │   │   ├── ErpDtos.java
│   │   │   ├── ErpOrderDtos.java
│   │   │   ├── ErpFoundationDtos.java
│   │   │   └── FinDtos.java
│   │   └── service/
│   │       └── [15 个 Service]
│   ├── domain/model/
│   │   ├── ErpProduct.java / ErpProductInfo.java / ErpProductCategory.java
│   │   ├── ErpCustomer.java
│   │   ├── ErpSupplier.java
│   │   ├── ErpWarehouse.java
│   │   ├── ErpStock.java
│   │   ├── ErpSaleOrder.java / ErpSaleOrderItem.java
│   │   ├── ErpPurchaseOrder.java / ErpPurchaseOrderItem.java
│   │   ├── ErpOpportunity.java
│   │   ├── ErpContract.java / ErpContractItem.java
│   │   ├── FinReceivable.java / FinReceivableRecord.java
│   │   ├── FinPayable.java / FinPayableRecord.java
│   │   └── ErpSysOperLog.java
│   └── infrastructure/mapper/
│       └── [17 个 Mapper 接口]
└── src/main/resources/
    └── db/
        ├── V1.0__erp_base_tables.sql
        ├── V1.1__erp_order_tables.sql
        ├── schema-erp-foundation.sql
        ├── schema-erp-opportunity.sql
        ├── schema-erp.sql
        ├── schema-fin-payable.sql
        └── schema-fin-receivable.sql
```

### 2.5 nexus-oa（端口 8083）

```
nexus-oa/
├── src/main/java/com/nexus/oa/
│   ├── api/controller/
│   │   ├── OaEmployeeController.java          # 员工档案
│   │   ├── OaLeaveRequestController.java       # 请假申请
│   │   ├── OaAttendanceController.java         # 考勤打卡/规则/加班
│   │   ├── OaApprovalCenterController.java     # 审批中心
│   │   ├── OaTaskController.java              # 任务看板
│   │   ├── OaScheduleController.java          # 日程管理
│   │   └── OaFileController.java              # 云空间/文件上传
│   ├── application/
│   │   ├── dto/OaDtos.java
│   │   └── service/ [7 个 Service]
│   ├── domain/model/
│   │   ├── OaEmployee.java
│   │   ├── OaLeave.java / OaLeaveDetail.java
│   │   ├── OaLeaveRequest.java
│   │   ├── OaOvertime.java
│   │   ├── OaAttendanceRecord.java / OaAttendanceRule.java
│   │   ├── OaTask.java / OaTaskComment.java
│   │   ├── OaApprovalTask.java
│   │   ├── OaSchedule.java
│   │   └── OaFile.java / OaFileFolder.java
│   ├── infrastructure/mapper/
│   └── workflow/
│       ├── LeaveRequestStateMachine.java
│       ├── LeaveApproverResolver.java
│       └── EmployeeBasedLeaveApproverResolver.java
└── src/main/resources/db/
    ├── V2.0__oa_tables.sql
    └── schema-oa-approval.sql / schema-oa-attendance.sql
```

### 2.6 nexus-wage（端口 8084）

```
nexus-wage/
├── src/main/java/com/nexus/wage/
│   ├── api/controller/
│   │   ├── WageItemConfigController.java      # 薪资项配置（ADMIN）
│   │   └── WageMonthlySlipController.java     # 月工资单（ADMIN）
│   ├── application/
│   │   ├── dto/WageDtos.java
│   │   └── service/ [2 个 Service]
│   ├── domain/model/
│   │   ├── WageItemConfig.java
│   │   └── WageMonthlySlip.java
│   └── infrastructure/mapper/ [2 个 Mapper]
└── src/main/resources/db/
    ├── V2.0__oa_employee_min.sql
    └── V3.0__wage_tables.sql
```

### 2.7 nexus-auth（端口 8085，已废弃）

```
nexus-auth/
├── src/main/java/com/nexus/auth/
│   ├── api/controller/AuthController.java   # 仅 /status 健康探针
│   └── application/service/AuthApplicationService.java
└── src/main/resources/application.yml
```
> 真实认证链路已迁移至 nexus-system，nexus-auth 仅作服务注册占位。

---

## 3. 所有 API 接口清单

> 统一响应格式：`Result<T>`，`code=0` 为成功
> 通用分页参数：`current`（默认1）、`size`（默认10）
> 所有接口均需 JWT 认证（`Authorization: Bearer <token>`），除非标注"公开"

### 3.1 认证模块 — `/api/v1/auth`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 登录（用户名+密码+可选验证码） | **公开** |
| POST | `/api/v1/auth/logout` | 登出 | 需认证 |
| POST | `/api/v1/auth/refresh` | Token 续期（Rotation） | 需认证 |
| GET | `/api/v1/auth/shops` | 获取当前用户可登录店铺列表 | 需认证 |
| PUT | `/api/v1/auth/switch-shop` | 切换当前店铺 | 需认证 |

#### 登录请求体（LoginRequest）

```json
{
  "username": "admin",
  "password": "your_password",
  "tenantId": 1,
  "captcha": "abc123",        // 可选，启用验证码时必填
  "captchaKey": "captcha:xxx" // 验证码 Key，启用验证码时必填
}
```

#### 登录响应体（LoginResponse）

```json
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "tenantId": 1,
  "currentShopId": 1,
  "currentOrgId": 1,
  "dataScope": 2,
  "accessibleShopIds": [1, 2],
  "accessibleOrgIds": [1]
}
```

### 3.2 系统模块 — `/api/v1/system`

#### 当前用户 — `/api/v1/system/user`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/user/info` | 获取当前用户信息+菜单树+最新公告标题 | 需认证 |
| GET | `/api/v1/system/user/list-by-org` | 按组织查询用户列表 | 需认证 |
| PUT | `/api/v1/system/user/change-org` | 修改当前用户所属组织 | 需认证 |

#### 用户管理 — `/api/v1/system/users`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/users/page` | 分页查询用户 | ADMIN |
| POST | `/api/v1/system/users` | 新增用户 | ADMIN |
| PUT | `/api/v1/system/users/{id}` | 修改用户 | ADMIN |
| GET | `/api/v1/system/users/{id}/shop-roles` | 查询用户在各个店铺的角色 | ADMIN |
| PUT | `/api/v1/system/users/{id}/shop-roles` | 分配用户在各个店铺的角色 | ADMIN |

#### 角色管理 — `/api/v1/system/roles`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/roles/page` | 分页查询角色 | ADMIN |
| GET | `/api/v1/system/roles/options` | 查询角色选项列表 | ADMIN |
| POST | `/api/v1/system/roles` | 新增角色 | ADMIN |
| PUT | `/api/v1/system/roles/{id}` | 修改角色 | ADMIN |
| GET | `/api/v1/system/roles/{id}/menu-ids` | 查询角色已授权菜单 ID | ADMIN |
| PUT | `/api/v1/system/roles/{id}/menus` | 分配角色菜单权限 | ADMIN |

#### 组织管理 — `/api/v1/system/org`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/org/tree` | 获取完整组织树 | 需认证 |
| GET | `/api/v1/system/org/tree-lazy` | 懒加载子节点 | 需认证 |
| POST | `/api/v1/system/org` | 新增组织 | ADMIN |
| PUT | `/api/v1/system/org` | 修改组织 | ADMIN |
| DELETE | `/api/v1/system/org/{id}` | 删除组织 | ADMIN |

#### 店铺管理 — `/api/v1/system/shops`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/shops/page` | 分页查询店铺 | 需认证 |
| GET | `/api/v1/system/shops/options` | 查询店铺选项 | 需认证 |
| POST | `/api/v1/system/shops` | 新增店铺 | ADMIN |
| PUT | `/api/v1/system/shops/{id}` | 修改店铺 | ADMIN |
| PUT | `/api/v1/system/shops/{id}/status` | 修改店铺状态 | ADMIN |

#### 菜单管理 — `/api/v1/system/menus`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/menus/tree` | 获取菜单树 | ADMIN |

#### 登录日志 — `/api/v1/system/login-log`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/login-log/page` | 分页查询登录日志 | ADMIN |

#### 在线用户 — `/api/v1/system/online-users`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/online-users` | 分页查询在线用户 | ADMIN |
| DELETE | `/api/v1/system/online-users/{userId}` | 强制下线指定用户 | ADMIN |

#### 公告管理 — `/api/v1/system/notice`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/notice/page` | 分页查询公告 | 需认证 |
| POST | `/api/v1/system/notice` | 新增公告 | ADMIN |
| PUT | `/api/v1/system/notice/{id}` | 修改公告 | ADMIN |
| PUT | `/api/v1/system/notice/{id}/publish` | 发布公告 | ADMIN |

#### 字典管理 — `/api/v1/system`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/dict-type/list` | 查询所有字典类型 | 需认证 |
| GET | `/api/v1/system/dict-item/list-by-type` | 按类型查询字典项 | 需认证 |

#### 消息管理 — `/api/v1/system/message`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/message/unread-count` | 未读消息数 | 需认证 |
| GET | `/api/v1/system/message/page` | 分页查询消息 | 需认证 |
| PUT | `/api/v1/system/message/read-all` | 全部标为已读 | 需认证 |

#### 工作台 — `/api/v1/system/workbench`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/system/workbench/dashboard` | 仪表盘摘要（今日销售/月采购/客户/供应商/待审批/库存预警） | 需认证 |
| GET | `/api/v1/system/workbench/sales-chart` | 销售趋势图 | 需认证 |
| GET | `/api/v1/system/workbench/purchase-chart` | 采购趋势图 | 需认证 |
| GET | `/api/v1/system/workbench/top-products` | 热销产品排行 | 需认证 |
| GET | `/api/v1/system/workbench/stock-alarms` | 库存预警列表 | 需认证 |

#### 请假管理（system 内） — `/api/v1/oa/leave`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/leave/page` | 分页查询请假单 | 需认证 |
| POST | `/api/v1/oa/leave/submit` | 提交请假申请 | 需认证 |
| POST | `/api/v1/oa/leave/approve` | 审批请假 | 需认证 |

### 3.3 ERP 模块 — `/api/v1/erp`

#### 产品分类 — `/api/v1/erp/product-categories`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/product-categories/list` | 列表 | 需认证 |
| GET | `/api/v1/erp/product-categories/tree` | 树形 | 需认证 |
| POST | `/api/v1/erp/product-categories` | 新增 | 需认证 |
| PUT | `/api/v1/erp/product-categories/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/product-categories/{id}` | 删除 | 需认证 |

#### 供应商 — `/api/v1/erp/suppliers`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/suppliers/page` | 分页查询 | 需认证 |
| POST | `/api/v1/erp/suppliers` | 新增 | 需认证 |
| PUT | `/api/v1/erp/suppliers/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/suppliers/{id}` | 删除 | 需认证 |

#### 仓库 — `/api/v1/erp/warehouses`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/warehouses/page` | 分页查询 | 需认证 |
| POST | `/api/v1/erp/warehouses` | 新增 | 需认证 |
| PUT | `/api/v1/erp/warehouses/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/warehouses/{id}` | 删除 | 需认证 |

#### 产品信息 — `/api/v1/erp/product-infos`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/product-infos/page` | 分页查询 | 需认证 |
| POST | `/api/v1/erp/product-infos` | 新增 | 需认证 |
| PUT | `/api/v1/erp/product-infos/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/product-infos/{id}` | 删除 | 需认证 |

#### 产品（基础） — `/api/v1/erp/products`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/products/page` | 分页查询 | 需认证 |
| POST | `/api/v1/erp/products` | 新增 | 需认证 |
| PUT | `/api/v1/erp/products/{id}` | 修改 | 需认证 |
| PUT | `/api/v1/erp/products/{id}/status` | 状态变更 | 需认证 |

#### 采购入库 — `/api/v1/erp/purchase-orders`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/purchase-orders/page` | 分页查询 | 需认证 |
| GET | `/api/v1/erp/purchase-orders/{id}/items` | 明细 | 需认证 |
| POST | `/api/v1/erp/purchase-orders` | 新增（草稿） | 需认证 |
| PUT | `/api/v1/erp/purchase-orders/{id}/confirm-inbound` | 确认入库（扣库存） | 需认证 |
| PUT | `/api/v1/erp/purchase-orders/{id}/submit` | 提交审核 | 需认证 |
| PUT | `/api/v1/erp/purchase-orders/{id}/approve` | 审核通过 | 需认证 |
| PUT | `/api/v1/erp/purchase-orders/{id}/reject` | 审核拒绝 | 需认证 |
| DELETE | `/api/v1/erp/purchase-orders/{id}` | 删除 | 需认证 |

#### 销售出库 — `/api/v1/erp/sale-order`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/sale-order/page` | 分页查询 | 需认证 |
| GET | `/api/v1/erp/sale-order/{id}/items` | 明细 | 需认证 |
| POST | `/api/v1/erp/sale-order` | 新增（草稿） | 需认证 |
| POST | `/api/v1/erp/sale-order/submit` | 一步提交出库（扣库存+生成应收） | 需认证 |
| PUT | `/api/v1/erp/sale-order/{id}/submit` | 草稿→待审核 | 需认证 |
| PUT | `/api/v1/erp/sale-order/{id}/approve` | 审核通过（扣库存） | 需认证 |
| PUT | `/api/v1/erp/sale-order/{id}/reject` | 审核拒绝 | 需认证 |
| DELETE | `/api/v1/erp/sale-order/{id}` | 删除（仅草稿/已拒绝） | 需认证 |

#### 客户 — `/api/v1/erp/customers`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/customers/page` | 分页查询 | 需认证 |
| POST | `/api/v1/erp/customers` | 新增 | 需认证 |
| PUT | `/api/v1/erp/customers/{id}` | 修改 | 需认证 |

#### 库存 — `/api/v1/erp/stocks`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/stocks/page` | 分页查询库存 | 需认证 |

#### 商机 — `/api/v1/erp/opportunities`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/opportunities/page` | 分页查询 | 需认证 |
| GET | `/api/v1/erp/opportunities/{id}` | 详情 | 需认证 |
| POST | `/api/v1/erp/opportunities` | 新增 | 需认证 |
| PUT | `/api/v1/erp/opportunities/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/opportunities/{id}` | 删除 | 需认证 |
| PUT | `/api/v1/erp/opportunities/{id}/stage` | 推进阶段 | 需认证 |

#### 合同 — `/api/v1/erp/contracts`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/contracts/page` | 分页查询 | 需认证 |
| GET | `/api/v1/erp/contracts/{id}` | 详情 | 需认证 |
| GET | `/api/v1/erp/contracts/{id}/items` | 合同条款 | 需认证 |
| POST | `/api/v1/erp/contracts` | 新增 | 需认证 |
| PUT | `/api/v1/erp/contracts/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/contracts/{id}` | 删除 | 需认证 |

#### 报表 — `/api/v1/erp/reports`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/reports/sales/monthly` | 月度销售汇总 | 需认证 |
| GET | `/api/v1/erp/reports/sales/trend` | 销售趋势（年） | 需认证 |
| GET | `/api/v1/erp/reports/sales/product-rank` | 产品销售排行 | 需认证 |
| GET | `/api/v1/erp/reports/sales/customer-rank` | 客户销售排行 | 需认证 |
| GET | `/api/v1/erp/reports/stock/alarm` | 库存预警 | 需认证 |
| GET | `/api/v1/erp/reports/stock/summary` | 库存总览 | 需认证 |

#### 应收账款 — `/api/v1/erp/receivables`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/receivables/page` | 分页查询 | 需认证 |
| GET | `/api/v1/erp/receivables/{id}` | 详情 | 需认证 |
| POST | `/api/v1/erp/receivables` | 新增 | 需认证 |
| PUT | `/api/v1/erp/receivables/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/receivables/{id}` | 删除 | 需认证 |
| POST | `/api/v1/erp/receivables/{id}/record` | 记录收款 | 需认证 |
| GET | `/api/v1/erp/receivables/{id}/records` | 收款记录列表 | 需认证 |
| GET | `/api/v1/erp/receivables/summary` | 按客户+月份汇总 | 需认证 |

#### 应付账款 — `/api/v1/erp/payables`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/erp/payables/page` | 分页查询 | 需认证 |
| GET | `/api/v1/erp/payables/{id}` | 详情 | 需认证 |
| POST | `/api/v1/erp/payables` | 新增 | 需认证 |
| PUT | `/api/v1/erp/payables/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/erp/payables/{id}` | 删除 | 需认证 |
| POST | `/api/v1/erp/payables/{id}/record` | 记录付款 | 需认证 |
| GET | `/api/v1/erp/payables/{id}/records` | 付款记录列表 | 需认证 |
| GET | `/api/v1/erp/payables/summary` | 按供应商+月份汇总 | 需认证 |

### 3.4 OA 模块 — `/api/v1/oa`

#### 员工档案 — `/api/v1/oa/employees`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/employees/page` | 分页查询 | 需认证 |
| GET | `/api/v1/oa/employees/{id}` | 详情 | 需认证 |
| POST | `/api/v1/oa/employees` | 新增 | 需认证 |
| PUT | `/api/v1/oa/employees/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/oa/employees/{id}` | 删除 | 需认证 |

#### 请假申请 — `/api/v1/oa/leave-requests`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/leave-requests/page` | 分页查询 | 需认证 |
| GET | `/api/v1/oa/leave-requests/{id}` | 详情 | 需认证 |
| POST | `/api/v1/oa/leave-requests` | 新增 | 需认证 |
| PUT | `/api/v1/oa/leave-requests/{id}` | 修改 | 需认证 |
| DELETE | `/api/v1/oa/leave-requests/{id}` | 删除 | 需认证 |
| PUT | `/api/v1/oa/leave-requests/{id}/submit` | 提交审批 | 需认证 |
| PUT | `/api/v1/oa/leave-requests/{id}/approve` | 审批（通过/拒绝） | 需认证 |

#### 考勤管理 — `/api/v1/oa/attendance`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/attendance/rules` | 考勤规则列表 | 需认证 |
| POST | `/api/v1/oa/attendance/rules` | 新增规则 | 需认证 |
| DELETE | `/api/v1/oa/attendance/rules/{id}` | 删除规则 | 需认证 |
| POST | `/api/v1/oa/attendance/check-in` | 打卡 | 需认证 |
| GET | `/api/v1/oa/attendance/my-today` | 今日打卡状态 | 需认证 |
| GET | `/api/v1/oa/attendance/records/page` | 打卡记录分页 | 需认证 |
| GET | `/api/v1/oa/attendance/leave/page` | 请假记录分页 | 需认证 |
| POST | `/api/v1/oa/attendance/leave` | 新增请假 | 需认证 |
| DELETE | `/api/v1/oa/attendance/leave/{id}` | 删除请假 | 需认证 |
| POST | `/api/v1/oa/attendance/leave/{id}/submit` | 提交请假审批 | 需认证 |
| POST | `/api/v1/oa/attendance/leave/{id}/approve` | 审批请假 | 需认证 |
| GET | `/api/v1/oa/attendance/overtime/page` | 加班记录分页 | 需认证 |
| POST | `/api/v1/oa/attendance/overtime` | 新增加班 | 需认证 |
| DELETE | `/api/v1/oa/attendance/overtime/{id}` | 删除加班 | 需认证 |
| POST | `/api/v1/oa/attendance/overtime/{id}/submit` | 提交加班审批 | 需认证 |
| POST | `/api/v1/oa/attendance/overtime/{id}/approve` | 审批加班 | 需认证 |
| GET | `/api/v1/oa/attendance/statistics/monthly` | 月度考勤统计 | 需认证 |

#### 审批中心 — `/api/v1/oa/approval`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/approval/tasks/my-apply` | 我发起的审批 | 需认证 |
| GET | `/api/v1/oa/approval/tasks/my-approve` | 待我审批 | 需认证 |
| GET | `/api/v1/oa/approval/tasks/{id}` | 审批详情 | 需认证 |
| POST | `/api/v1/oa/approval/tasks/{id}/approve` | 审批通过 | 需认证 |
| POST | `/api/v1/oa/approval/tasks/{id}/reject` | 审批拒绝 | 需认证 |

#### 任务看板 — `/api/v1/oa/tasks`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/tasks/page` | 分页查询 | 需认证 |
| GET | `/api/v1/oa/tasks/{id}` | 详情 | 需认证 |
| POST | `/api/v1/oa/tasks` | 新建任务 | 需认证 |
| PUT | `/api/v1/oa/tasks/{id}` | 修改任务 | 需认证 |
| DELETE | `/api/v1/oa/tasks/{id}` | 删除任务 | 需认证 |
| PUT | `/api/v1/oa/tasks/{id}/accept` | 接受任务 | 需认证 |
| PUT | `/api/v1/oa/tasks/{id}/progress` | 更新进度 | 需认证 |
| PUT | `/api/v1/oa/tasks/{id}/complete` | 完成任务 | 需认证 |
| PUT | `/api/v1/oa/tasks/{id}/cancel` | 取消任务 | 需认证 |
| GET | `/api/v1/oa/tasks/{id}/comments` | 评论列表 | 需认证 |
| POST | `/api/v1/oa/tasks/{id}/comment` | 添加评论 | 需认证 |

#### 日程管理 — `/api/v1/oa/schedules`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/schedules` | 按日期范围查询 | 需认证 |
| POST | `/api/v1/oa/schedules` | 新建日程 | 需认证 |
| PUT | `/api/v1/oa/schedules/{id}` | 修改日程 | 需认证 |
| DELETE | `/api/v1/oa/schedules/{id}` | 删除日程 | 需认证 |

#### 云空间 — `/api/v1/oa/files`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/oa/files/folders` | 文件夹列表 | 需认证 |
| POST | `/api/v1/oa/files/folders` | 新建文件夹 | 需认证 |
| DELETE | `/api/v1/oa/files/folders/{id}` | 删除文件夹 | 需认证 |
| GET | `/api/v1/oa/files` | 文件列表 | 需认证 |
| POST | `/api/v1/oa/files/upload` | 上传文件 | 需认证 |
| GET | `/api/v1/oa/files/{id}/download` | 下载文件 | 需认证 |
| DELETE | `/api/v1/oa/files/{id}` | 删除文件 | 需认证 |

### 3.5 薪酬模块 — `/api/v1/wage`

> 所有接口均需 ADMIN 角色

#### 薪资项配置 — `/api/v1/wage/item-configs`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/wage/item-configs` | 列表 | ADMIN |
| GET | `/api/v1/wage/item-configs/{id}` | 详情 | ADMIN |
| POST | `/api/v1/wage/item-configs` | 新增 | ADMIN |
| PUT | `/api/v1/wage/item-configs/{id}` | 修改 | ADMIN |
| DELETE | `/api/v1/wage/item-configs/{id}` | 删除 | ADMIN |

#### 月工资单 — `/api/v1/wage/monthly-slips`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/wage/monthly-slips` | 列表（按月份） | ADMIN |
| GET | `/api/v1/wage/monthly-slips/{id}` | 详情 | ADMIN |
| POST | `/api/v1/wage/monthly-slips/generate` | 一键生成月工资 | ADMIN |
| PUT | `/api/v1/wage/monthly-slips/{id}/adjust` | 调整工资 | ADMIN |
| POST | `/api/v1/wage/monthly-slips/confirm-pay` | 确认发放 | ADMIN |

---

## 4. 实体（Domain Model）

> 所有实体均继承 `BaseTenantEntity`（含 `tenantId`）或 `BaseEntity`（含 `tenantId + shopId`）
> 所有实体均有逻辑删除：`del_flag`（0=正常，1=删除），由 MyBatis-Plus `@TableLogic` 自动处理

### 4.1 系统实体

| 实体 | 表名 | 说明 |
|---|---|---|
| `SysUser` | `sys_user` | 用户（用户名/密码哈希/主店铺/主组织） |
| `SysRole` | `sys_role` | 角色（roleCode/roleName/dataScope） |
| `SysMenu` | `sys_menu` | 菜单/权限（perms 字段即权限标识） |
| `SysOrg` | `sys_org` | 组织架构（树形，parentId+ancestors） |
| `SysPost` | `sys_post` | 岗位 |
| `SysShop` | `sys_shop` | 店铺（属于某组织） |
| `SysUserPost` | `sys_user_post` | 用户-岗位关联 |
| `SysUserShopRole` | `sys_user_shop_role` | 用户-店铺-角色关联（三主键） |
| `SysRoleMenu` | `sys_role_menu` | 角色-菜单关联 |
| `SysLoginLog` | `sys_login_log` | 登录日志 |
| `SysOperLog` | `sys_oper_log` | 操作日志 |
| `SysNotice` | `sys_notice` | 公告（草稿/已发布） |
| `SysMessage` | `sys_message` | 消息通知 |
| `SysDictType` | `sys_dict_type` | 字典类型 |
| `SysDictItem` | `sys_dict_item` | 字典项 |
| `SysConfig` | `sys_config` | 系统配置 |
| `OaLeave` | `oa_leave` | 请假申请（system 内） |
| `OaLeaveApproval` | `oa_leave_approval` | 请假审批记录 |

### 4.2 ERP 实体

| 实体 | 表名 | 说明 |
|---|---|---|
| `ErpProduct` | `erp_product` | 基础产品 |
| `ErpProductInfo` | `erp_product_info` | 产品信息（含规格/库存/预警线） |
| `ErpProductCategory` | `erp_product_category` | 产品分类（树形） |
| `ErpCustomer` | `erp_customer` | 客户（含信用额度） |
| `ErpSupplier` | `erp_supplier` | 供应商（含银行账号） |
| `ErpWarehouse` | `erp_warehouse` | 仓库 |
| `ErpStock` | `erp_stock` | 库存（按产品+仓库） |
| `ErpSaleOrder` | `erp_sale_order` | 销售出库单（草稿/待审核/已审核） |
| `ErpSaleOrderItem` | `erp_sale_order_item` | 销售明细 |
| `ErpPurchaseOrder` | `erp_purchase_order` | 采购入库单 |
| `ErpPurchaseOrderItem` | `erp_purchase_order_item` | 采购明细 |
| `ErpOpportunity` | `erp_opportunity` | 商机（线索→成交全流程） |
| `ErpContract` | `erp_contract` | 合同（含关联商机） |
| `ErpContractItem` | `erp_contract_item` | 合同条款 |
| `FinReceivable` | `fin_receivable` | 应收账款（来源=销售单/合同） |
| `FinReceivableRecord` | `fin_receivable_record` | 收款记录 |
| `FinPayable` | `fin_payable` | 应付账款（来源=采购单） |
| `FinPayableRecord` | `fin_payable_record` | 付款记录 |
| `ErpSysOperLog` | `sys_oper_log` | ERP 操作日志 |

### 4.3 OA 实体

| 实体 | 表名 | 说明 |
|---|---|---|
| `OaEmployee` | `oa_employee` | 员工档案（绑定登录用户+直属上级） |
| `OaLeave` | `oa_leave_detail` | 请假明细（新表） |
| `OaLeaveRequest` | `oa_leave_request` | 请假申请（草稿/待审批/已通过/已拒绝） |
| `OaOvertime` | `oa_overtime` | 加班申请 |
| `OaAttendanceRecord` | `oa_attendance_record` | 打卡记录（含外勤） |
| `OaAttendanceRule` | `oa_attendance_rule` | 考勤规则 |
| `OaTask` | `oa_task` | 任务（待接受/进行中/已完成/已取消） |
| `OaTaskComment` | `oa_task_comment` | 任务评论 |
| `OaApprovalTask` | `oa_approval_task` | 统一审批任务 |
| `OaSchedule` | `oa_schedule` | 日程（私有/公开） |
| `OaFile` | `oa_file` | 文件 |
| `OaFileFolder` | `oa_file_folder` | 文件夹 |

### 4.4 薪酬实体

| 实体 | 表名 | 说明 |
|---|---|---|
| `WageItemConfig` | `wage_item_config` | 薪资项配置（固定值/手动录入/基本工资/补贴/扣款） |
| `WageMonthlySlip` | `wage_monthly_slip` | 月工资单（待确认/已发放） |

---

## 5. DTO 数据传输对象

### 5.1 核心公共 DTO

#### Result 统一响应

```java
Result<T> {
    int    code;      // 0=成功，非0=失败
    String message;   // 提示信息
    T      data;      // 数据体
    long   timestamp;
}
```

#### 分页请求

```java
PageQuery { Integer page=1; Integer size=10; }
PageResult<T> { List<T> list; Long total; Integer page; Integer size; }
```

### 5.2 认证 DTO

| DTO | 字段 |
|---|---|
| `LoginRequest` | username, password, tenantId, captcha, captchaKey |
| `LoginResponse` | accessToken, tokenType, tenantId, currentShopId, currentOrgId, dataScope, accessibleShopIds, accessibleOrgIds |
| `SwitchShopRequest` | shopId |
| `ShopItem` | shopId, shopName, shopType, orgId |

### 5.3 系统管理 DTO

| DTO | 说明 |
|---|---|
| `ShopCreateRequest` | orgId*, shopName*, shopType*, status |
| `ShopUpdateRequest` | orgId*, shopName*, shopType* |
| `RoleCreateRequest` | shopId, roleCode*, roleName*, dataScope* |
| `RoleUpdateRequest` | shopId, roleCode*, roleName*, dataScope* |
| `RoleMenuAssignRequest` | menuIds* |
| `UserCreateRequest` | username*, password*, realName, mainShopId*, status |
| `UserUpdateRequest` | realName, password(null=不变), mainShopId*, status |
| `UserShopRoleSaveRequest` | items: [{shopId*, roleId*}] |
| `NoticeSaveRequest` | id(Long|null), title*, content, noticeType, expireTime |
| `OrgCreateRequest` | parentId*, orgCode*, orgName*, orgType, sort, status |
| `OrgUpdateRequest` | id*, parentId*, orgCode*, orgName*, orgType, sort, status |
| `UserChangeOrgRequest` | userId*, newOrgId* |

### 5.4 ERP DTO

| DTO | 说明 |
|---|---|
| `SaleOrderCreateRequest` | customerId, customerName*, warehouseId*, items* |
| `SaleOrderLineRequest` | productId*, quantity*(≥1), unitPrice* |
| `PurchaseOrderCreateRequest` | supplierId*, warehouseId*, remark, items* |
| `PurchaseOrderLineRequest` | productId*, quantity*(≥1), unitPrice* |
| `ReceivableCreateRequest` | customerId*, customerName*, totalAmount*, sourceType, sourceId, invoiceNo, dueDate |
| `ReceivableRecordCreate` | amount*, paymentMethod, paymentAccount, paymentTime, receiptUrl |
| `PayableCreateRequest` | supplierId*, supplierName*, totalAmount*, sourceType, sourceId, invoiceNo, dueDate |

### 5.5 OA DTO

| DTO | 说明 |
|---|---|
| `LeaveRequestCreateRequest` | leaveType*, startTime*, endTime*, leaveDays*(≥0.5), reason |
| `LeaveApproveRequest` | approved*, opinion |
| `CheckInRequest` | type("in"/"out"), isOuter, outerAddress, outerReason |
| `AttendanceRuleCreateRequest` | ruleName*, checkInStart*, checkInEnd*, checkOutStart*, checkOutEnd* |
| `TaskCreateReq` | title*, description, priority, assigneeUserId, dueDate |
| `ScheduleCreateReq` | title*, startTime*, endTime*, isAllDay, visibility |

### 5.6 薪酬 DTO

| DTO | 说明 |
|---|---|
| `ItemConfigCreateRequest` | itemName*(max128), calcType*(1=固定值/2=手动录入), defaultAmount*, itemKind*(1=基本工资/2=补贴/3=扣款) |
| `GenerateMonthlyRequest` | belongMonth*(yyyy-MM格式), employeeIds(null=all) |
| `AdjustSlipRequest` | baseSalary*, subsidyTotal*, deductionTotal* |
| `ConfirmPayRequest` | slipIds* |

---

## 6. 公共组件与安全机制

### 6.1 JWT 认证链路

```
用户登录
  → AuthApplicationService.login()
    → 密码校验（BCrypt）
    → 验证码校验（Redis）
    → 查询用户店铺 + 数据权限
    → buildAuthorities(userId, shopId)  ← RBAC 角色+菜单权限
    → NexusPrincipal(..., authorities)
    → JwtTokenProvider.createAccessToken(principal)  ← authorities 写入 JWT claim
    → OnlineUserRedisService.recordLogin()  ← Redis 白名单
    → 返回 LoginResponse + JWT

请求验证
  → Gateway: AuthenticationGlobalFilter
    → 公开路径放行 /login /swagger /doc.html
    → 其他路径：解析 Authorization: Bearer <token>
    → OnlineTokenValidator: Redis 中查询 login:token:<rawToken>
    → token 不存在/已删除 → 401
  → Service: JwtAuthenticationFilter
    → JwtTokenProvider.parseToken()
    → 提取 authorities → UsernamePasswordAuthenticationToken → SecurityContextHolder
    → @PreAuthorize("hasRole('ADMIN')") 可用
```

### 6.2 Token Rotation

- **续期**：`POST /api/v1/auth/refresh` — 重新签发新 token，旧 token 从 Redis 删除
- **切换店铺**：`PUT /api/v1/auth/switch-shop` — 重新签发 token，旧 token 删除
- **登出**：`POST /api/v1/auth/logout` — 删除 Redis 白名单中的 token
- **强退**：`DELETE /api/v1/system/online-users/{userId}` — 删除该用户所有 Redis token

### 6.3 数据权限（DataScope）

| dataScope 值 | 含义 | 说明 |
|---|---|---|
| 1 | 仅本人数据 | |
| 2 | 本部门数据 | 含当前 Org |
| 3 | 本部门及以下数据 | 含当前 Org + 子 Org（递归查询） |
| 4 | 全部数据 | |

### 6.4 操作日志（@OpLog）

`@OpLog(module="模块名", type="操作类型", excludeParamNames={"敏感字段"})`
自动记录：模块/操作类型/URL/方法/HTTP方式/请求参数/响应数据/IP/耗时
当前系统：100 个接口已加 @OpLog

### 6.5 验证码机制

- 配置：`nexus.captcha.enabled=true`（生产环境默认）
- 验证码 Key 存入 Redis，过期 5 分钟
- 登录失败 3 次（可配置）强制要求验证码

---

## 7. 数据库表清单

### 系统表（nexus-system/src/main/resources/db/schema.sql）

| 表名 | 说明 |
|---|---|
| `sys_org` | 组织架构 |
| `sys_user` | 用户（admin 默认密码需初始化） |
| `sys_post` | 岗位 |
| `sys_user_post` | 用户-岗位 |
| `sys_shop` | 店铺 |
| `sys_role` | 角色（含 ADMIN/CASHIER） |
| `sys_user_shop_role` | 用户-店铺-角色 |
| `sys_menu` | 菜单+权限点 |
| `sys_role_menu` | 角色-菜单 |
| `oa_leave` | 请假申请 |
| `oa_leave_approval` | 请假审批记录 |
| `sys_dict_type` / `sys_dict_item` | 字典 |
| `sys_config` | 系统配置 |
| `sys_oper_log` | 操作日志 |
| `sys_login_log` | 登录日志 |
| `sys_notice` | 公告 |
| `sys_message` | 消息 |

### ERP 表（nexus-erp/src/main/resources/db/）

| 表名 | 说明 |
|---|---|
| `erp_product_category` | 产品分类 |
| `erp_product_info` | 产品信息 |
| `erp_product` | 基础产品 |
| `erp_customer` | 客户 |
| `erp_supplier` | 供应商 |
| `erp_warehouse` | 仓库 |
| `erp_stock` | 库存 |
| `erp_sale_order` / `erp_sale_order_item` | 销售出库 |
| `erp_purchase_order` / `erp_purchase_order_item` | 采购入库 |
| `erp_opportunity` | 商机 |
| `erp_contract` / `erp_contract_item` | 合同 |
| `fin_receivable` / `fin_receivable_record` | 应收账款 |
| `fin_payable` / `fin_payable_record` | 应付账款 |

### OA 表（nexus-oa/src/main/resources/db/）

| 表名 | 说明 |
|---|---|
| `oa_employee` | 员工档案 |
| `oa_leave_request` | 请假申请 |
| `oa_leave_detail` | 请假明细 |
| `oa_overtime` | 加班 |
| `oa_attendance_rule` | 考勤规则 |
| `oa_attendance_record` | 打卡记录 |
| `oa_task` / `oa_task_comment` | 任务+评论 |
| `oa_approval_task` | 审批任务 |
| `oa_schedule` | 日程 |
| `oa_file` / `oa_file_folder` | 云空间 |

### 薪酬表（nexus-wage/src/main/resources/db/）

| 表名 | 说明 |
|---|---|
| `wage_item_config` | 薪资项配置 |
| `wage_monthly_slip` | 月工资单 |

---

## 8. 网关路由与安全配置

### 8.1 路由配置

| 路径前缀 | 目标服务 | 端口 |
|---|---|---|
| `/api/v1/auth/**` | nexus-system | 8081 |
| `/api/v1/system/**` | nexus-system | 8081 |
| `/api/v1/oa/leave/**` | nexus-system | 8081 |
| `/api/v1/erp/**` | nexus-erp | 8082 |
| `/api/v1/oa/**`（不含 leave） | nexus-oa | 8083 |
| `/api/v1/wage/**` | nexus-wage | 8084 |

### 8.2 公开路径（无需 JWT）

| 路径 | 说明 |
|---|---|
| `/api/v1/auth/login` | 登录 |
| `/doc.html` | Knife4j 文档 |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI 3 文档 |
| `/actuator/health` | 健康检查 |

### 8.3 CORS 配置

```yaml
spring.cloud.gateway.globalcors.cors-configurations['[/**]']:
  allowedOrigins: "${CORS_ALLOWED_ORIGINS:http://localhost:5173}"
  allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
  allowedHeaders: "*"
  allowCredentials: true
  maxAge: 3600
```

### 8.4 JWT 密钥配置

所有模块：`nexus.security.jwt.secret: ${JWT_SECRET}`（无默认值，启动时强制校验）

---

## 9. 接口统计总览

| 模块 | Controller 数 | 接口数 | @OpLog 数 | @PreAuthorize(ADMIN) 数 |
|---|---|---|---|---|
| 系统 | 14 | 45 | 23 | 13 |
| ERP | 15 | 64 | 42 | 0 |
| OA | 7 | 41 | 28 | 0 |
| 薪酬 | 2 | 10 | 7 | 10（全） |
| 认证占位 | 1 | 1 | 0 | 0 |
| **合计** | **40** | **161** | **100** | **23** |
