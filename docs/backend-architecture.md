# NexusERP-X 后端架构文档

## 技术栈

| 类别 | 技术 |
|---|---|
| 框架 | Spring Boot 3.3.4 + Spring Cloud 2023.0.3（微服务架构） |
| ORM | MyBatis Plus 3.5.7 + 动态数据源 |
| 安全 | JWT (jjwt 0.12.5) + Spring Security |
| 分布式 | Redisson 3.35（Redis 客户端）、XXL-JOB 2.4.1（定时任务） |
| 文件存储 | MinIO |
| API 文档 | SpringDoc OpenAPI + Knife4j |
| 构建 | Maven 多模块聚合，Java 17 |

---

## 模块结构总览

```
nexus-erp-x/
├── nexus-common       # 公共基础模块（所有服务共享）
├── nexus-gateway      # API 网关（唯一对外入口）
├── nexus-auth         # 认证模块（占位，逻辑已迁移至 nexus-system）
├── nexus-system       # 系统管理（用户/权限/组织/消息/登录日志）
├── nexus-erp          # 企业资源计划（进销存/客户/合同/财务）
├── nexus-oa           # 办公自动化（考勤/请假/审批/任务/文件）
└── nexus-wage         # 薪资管理（工资条）
```

---

## 1. nexus-common（公共基础模块）

所有其他模块共享的基础设施，不打包为独立服务。

### 子包结构

| 子包 | 功能说明 |
|---|---|
| `security/jwt/` | JWT 令牌签发（7天有效期）、解析、Redis 在线会话白名单校验 |
| `security/auth/` | `NexusPrincipal`（登录主体）、`SecurityUtils` 工具类 |
| `security/datascope/` | 数据权限范围模型（本人/本部门/全量等） |
| `security/config/` | 安全配置属性（白名单路径、JWT 密钥等） |
| `tenant/` | `TenantContext` — ThreadLocal 存租户 ID |
| `context/` | 请求上下文持有（当前用户/店铺/租户信息） |
| `mybatis/` | 分页插件、数据权限拦截器、多数据源自动切换、租户字段自动填充 |
| `integration/dingtalk/` | 钉钉 API 集成客户端 |
| `core/domain/` | 统一响应 `Result<T>`、标准错误码 `ResultCode` |
| `core/page/` | 通用分页查询 `PageQuery` / `PageResult` |
| `exception/` | `BusinessException` 业务异常 |
| `audit/` | 操作审计注解/拦截器 |
| `web/` | 全局异常处理器、响应包装 |

### 核心类说明

- **JwtTokenProvider**: 登录签发 JWT，包含 uid/tid/username/oid/sid/ds/asids/aoids/authorities/clientType 等 Claims
- **NexusPrincipal**: 登录主体，持有用户身份、租户、店铺、数据权限范围等完整上下文
- **TenantContext**: ThreadLocal 持有当前请求的 tenantId
- **DataScope**: 数据权限枚举（本人=1, 本部门=2, 本部门及下级=3, 全部=5）

---

## 2. nexus-gateway（API 网关）

唯一对外暴露的入口，端口由 `application.yml` 配置。

### 组件

| 组件 | 职责 |
|---|---|
| `AuthenticationGlobalFilter` | 网关层 JWT 校验 + Redis 白名单校验，将 `X-User-Id`、`X-Tenant-Id`、`X-Org-Id`、`X-Shop-Id`、`X-Data-Scope` 写入请求头传递给下游 |
| `DownstreamContextForwardingFilter` | 将网关解析出的上下文信息转发到下游微服务 |
| `GatewaySecurityConfiguration` | 路由白名单配置 |

### 公开路径（不鉴权）

- `/api/v1/auth/login`
- `/api/v1/system/captcha/*`
- `/doc.html`、`/swagger-ui/*`、`/swagger-resources/*`、`/v3/api-docs/*`
- `/actuator/*`

### 安全流程

```
请求 → AuthenticationGlobalFilter(JWT校验+Redis白名单)
                                      ↓
                           下游服务获取 X-User-Id 等 Header
                                      ↓
                           NexusSecurityFilter → JwtAuthenticationFilter
                                      ↓
                           OnlineTokenValidator（Redis二次校验）
                                      ↓
                           DataScopeInterceptor（数据权限拦截）
```

---

## 3. nexus-auth（认证模块）

> 占位模块，真实认证逻辑已迁移到 nexus-system。

保留健康检查接口 `/api/v1/auth-service/status`。

---

## 4. nexus-system（系统管理模块）

最核心的基础服务模块，处理用户、权限、组织架构、消息通知、登录日志等。

### Domain Models（数据实体）

| 实体 | 说明 |
|---|---|
| `SysUser` | 系统用户 |
| `SysShop` | 店铺 |
| `SysOrg` | 组织架构 |
| `SysRole` | 角色 |
| `SysMenu` | 菜单/权限树 |
| `SysRoleMenu` | 角色-菜单关联 |
| `SysUserShopRole` | 用户-店铺-角色关联（三方关联表） |
| `SysPost` | 岗位 |
| `SysUserPost` | 用户-岗位关联 |
| `SysDictType` / `SysDictItem` | 字典类型/字典项 |
| `SysConfig` | 系统配置参数 |
| `SysNotice` | 系统公告 |
| `SysMessage` | 用户消息 |
| `SysLoginLog` | 登录日志 |
| `SysOperLog` | 操作日志 |
| `OaLeave` | 请假记录（极简版） |
| `OaLeaveApproval` | 请假审批记录 |

### API Controllers

| Controller | 端点前缀 | 功能 |
|---|---|---|
| `AuthController` | `/api/v1/auth/*` | 登录、Token 续期、店铺切换 |
| `SysUserAdminController` | `/api/v1/system/users/*` | 用户 CRUD、状态管理 |
| `SystemUserController` | `/api/v1/system/user/*` | 当前用户信息、密码修改 |
| `SysMenuController` | `/api/v1/system/menus/*` | 菜单权限树管理 |
| `SysRoleController` | `/api/v1/system/roles/*` | 角色 CRUD、分配菜单 |
| `SysOrgController` | `/api/v1/system/orgs/*` | 组织架构管理 |
| `SysShopController` | `/api/v1/system/shops/*` | 店铺管理 |
| `SystemDictController` | `/api/v1/system/dicts/*` | 字典数据管理 |
| `SysNoticeController` | `/api/v1/system/notices/*` | 公告发布 |
| `SysMessageController` | `/api/v1/system/messages/*` | 站内消息发送/查询 |
| `SysLoginLogController` | `/api/v1/system/login-logs/*` | 登录日志查询 |
| `SysCaptchaController` | `/api/v1/system/captcha/*` | 图形验证码生成/校验 |
| `SysOnlineUserController` | `/api/v1/system/online/*` | 在线用户管理（强制下线） |
| `OaLeaveController` | `/api/v1/system/leaves/*` | 极简请假申请 |
| `WorkbenchController` | `/api/v1/system/workbench/*` | 工作台首页数据聚合 |

### Application Services

| Service | 职责 |
|---|---|
| `AuthApplicationService` | 登录流程（密码校验 → 验证码 → 多店铺选择 → JWT 签发 → Redis 在线会话）、Token 续期（Rotation）、店铺切换 |
| `AuthRedisService` | 用户店铺列表缓存、当前会话状态（店铺+数据范围）缓存 |
| `OnlineUserRedisService` | 在线用户 Redis 记录，支持强制下线（删除 token） |
| `JwtOnlineSessionListener` | JWT 监听器，Token 注销时同步清除 Redis 记录 |
| `LoginCaptchaValidator` | 登录验证码校验 |
| `CaptchaImageService` | 图形验证码生成 |
| `SysOrgApplicationService` | 组织架构树查询（含子孙节点查询，用于数据权限） |
| `SysMenuApplicationService` | 菜单树构建、用户权限聚合 |
| `SysRoleApplicationService` | 角色授权 |
| `WorkbenchService` | 工作台数据聚合（待办数、通知数等） |

### 登录/授权流程

```
用户登录 → 验证码校验 → 密码校验 → 查询用户可登录店铺列表
                                      ↓
                          选择当前店铺 → 计算数据权限范围(scope)
                                      ↓
                          构建 NexusPrincipal → JwtTokenProvider 签发 JWT
                                      ↓
                          onlineUserRedisService.recordLogin() 写入 Redis
                                      ↓
                          返回 AccessToken + 当前店铺 + 数据范围
```

### Token 续期（Rotation）

前端在 access token 即将过期时调用续期接口，后端重新签发 token 并使旧 token 立即失效。

---

## 5. nexus-erp（企业资源计划模块）

进销存 + 客户关系 + 合同 + 商机 + 财务报表。

### Domain Models

| 模块 | 实体 | 说明 |
|---|---|---|
| 客户/供应商 | `ErpCustomer` | 客户档案 |
| | `ErpSupplier` | 供应商档案 |
| 产品 | `ErpProduct` | 产品主数据 |
| | `ErpProductInfo` | 产品明细/SKU |
| | `ErpProductCategory` | 产品分类 |
| 仓库 | `ErpWarehouse` | 仓库档案 |
| 库存 | `ErpStock` | 库存余额（乐观锁更新） |
| 销售 | `ErpSaleOrder` | 销售订单 |
| | `ErpSaleOrderItem` | 销售订单明细 |
| 采购 | `ErpPurchaseOrder` | 采购订单 |
| | `ErpPurchaseOrderItem` | 采购订单明细 |
| 合同 | `ErpContract` | 合同主表 |
| | `ErpContractItem` | 合同条款明细 |
| 商机 | `ErpOpportunity` | 商机/销售线索 |
| 财务 | `FinReceivable` | 应收账单 |
| | `FinReceivableRecord` | 应收还款明细 |
| | `FinPayable` | 应付账单 |
| | `FinPayableRecord` | 应付付款明细 |
| 日志 | `ErpSysOperLog` | ERP 操作日志 |

### API Controllers（共 13 个）

| Controller | 功能 |
|---|---|
| `ErpCustomerController` | 客户管理 |
| `ErpSupplierController` | 供应商管理 |
| `ErpProductController` | 产品管理 |
| `ErpProductInfoController` | 产品明细/SKU 管理 |
| `ErpProductCategoryController` | 产品分类管理 |
| `ErpWarehouseController` | 仓库管理 |
| `ErpStockController` | 库存查询、盘点 |
| `ErpSaleOrderController` | 销售订单 |
| `ErpPurchaseOrderController` | 采购订单 |
| `ErpContractController` | 合同管理 |
| `ErpOpportunityController` | 商机管理 |
| `FinReceivableController` | 应收管理 |
| `FinPayableController` | 应付管理 |
| `ErpReportController` | ERP 数据报表 |

### Application Services

| Service | 职责 |
|---|---|
| `ErpSaleOrderApplicationService` | 销售订单：草稿创建、提交出库（**乐观锁扣减库存 → 自动生成应收记录**） |
| `ErpPurchaseOrderApplicationService` | 采购订单流程 |
| `FinReceivableApplicationService` | 应收管理：销售出库自动生成应收 → 分期还款记录 |
| `FinPayableApplicationService` | 应付管理：采购入库生成应付 → 分期付款记录 |
| `ErpStockApplicationService` | 库存查询、盘点 |
| `ErpCustomerApplicationService` | 客户管理、商机转化 |
| `ErpOpportunityApplicationService` | 商机阶段管理 |
| `ErpContractApplicationService` | 合同起草/审批 |
| `ErpReportService` | ERP 数据报表 |

### 销售出库流程

```
SaleOrderCreateRequest（客户+商品+仓库）
        ↓
校验商品存在性 + 计算总金额
        ↓
乐观锁扣减库存（stockMapper.deductStock，version 乐观锁）
        ↓  任一商品库存不足 → 事务回滚
插入主表（status=1 已出库）
        ↓
批量插入订单明细
        ↓
自动创建应收记录（finReceivableService.createFromSaleOrder）
```

---

## 6. nexus-oa（办公自动化模块）

考勤、请假、排班、审批中心、任务、日程、文件管理。

### Domain Models

| 实体 | 说明 |
|---|---|
| `OaEmployee` | 员工档案 |
| `OaAttendanceRecord` | 考勤打卡记录 |
| `OaAttendanceRule` | 考勤规则配置 |
| `OaLeaveRequest` | 请假申请（状态机驱动） |
| `OaOvertime` | 加班申请 |
| `OaSchedule` | 排班表 |
| `OaTask` | 任务 |
| `OaTaskComment` | 任务评论 |
| `OaFile` | 文件（MinIO 存储） |
| `OaFileFolder` | 文件夹 |
| `OaApprovalTask` | 审批任务记录（请假/加班的统一审批视图） |

### API Controllers（共 8 个）

| Controller | 功能 |
|---|---|
| `OaEmployeeController` | 员工档案管理 |
| `OaAttendanceController` | 考勤打卡、排班规则 |
| `OaLeaveRequestController` | 请假申请（状态机版） |
| `OaScheduleController` | 排班管理 |
| `OaTaskController` | 任务创建/分配/评论 |
| `OaFileController` | 文件上传（MinIO）、目录管理 |
| `OaApprovalCenterController` | 统一审批中心（我的申请/待我审批） |

另有 `nexus-system` 中的 `OaLeaveController` 提供极简版请假功能。

### Application Services

| Service | 职责 |
|---|---|
| `OaApprovalCenterService` | 统一审批中心：我发起的审批、待我审批、审批通过/拒绝 |
| `OaLeaveRequestApplicationService` | 请假申请：草稿 → 提交 → 审批 → 状态变更 |
| `OaAttendanceApplicationService` | 考勤打卡、排班、排班规则 |
| `OaEmployeeApplicationService` | 员工档案管理 |
| `OaScheduleApplicationService` | 排班管理 |
| `OaTaskApplicationService` | 任务创建/分配/评论 |
| `OaFileApplicationService` | 文件上传（MinIO）、目录管理 |

### 工作流引擎（手写状态机）

```
LeaveRequestStateMachine
  DRAFT   →(提交)→  PENDING   →(审批通过)→  APPROVED
                     ↓(审批拒绝)
                  REJECTED
```

- 审批人解析策略：`LeaveApproverResolver` 接口
- 实现类 `EmployeeBasedLeaveApproverResolver`：从员工表找直接主管作为审批人

### 审批中心

- **我发起的审批**：查询当前用户提交的 `OaApprovalTask`，按状态筛选
- **待我审批**：查询审批人=当前用户的待审批任务，支持通过/拒绝操作

---

## 7. nexus-wage（薪资模块）

工资条管理。

### Domain Models

| 实体 | 说明 |
|---|---|
| `WageItemConfig` | 薪资项目配置（类型：1=应发 2=补贴 3=扣款） |
| `WageMonthlySlip` | 月度工资条（关联员工+归属月份，状态：0=待确认 1=已发放） |

### API Controllers

| Controller | 功能 |
|---|---|
| `WageItemConfigController` | 薪资项目配置管理 |
| `WageMonthlySlipController` | 工资条生成/查询/确认发放 |

### 工资条计算逻辑

```
归属月份 + 员工列表 → 读取 OaEmployee（员工基础薪资）
                          + WageItemConfig（薪资项目配置）
                          → 计算应发/补贴/扣款
                          → 汇总实发工资
                          → 生成 WageMonthlySlip
```

---

## 关键架构特性

### 多租户 + 多店铺

- **租户隔离**：所有 Mapper 自动携带 `tenant_id` 条件
- **店铺隔离**：用户可绑定多个店铺，登录时选择当前店铺，JWT 中携带 `shopId` 和 `accessibleShopIds`
- **数据权限分级**：本人(1)/本部门(2)/本部门及下级(3)/全部(5)

### 安全架构

```
请求 → Gateway(JWT校验+Redis白名单) → 下游服务(X-User-Id等Header)
                                            ↓
                              NexusSecurityFilter(JwtAuthenticationFilter)
                                            ↓
                              OnlineTokenValidator(Redis二次校验)
                                            ↓
                              数据权限拦截器(DataScopeInterceptor)
```

### 数据库访问

- **多数据源**：动态数据源，按模块切换
- **乐观锁**：库存扣减使用 `version` 字段防止超卖
- **逻辑删除**：所有表含 `del_flag`，MyBatis Plus 自动拼接
- **自动填充**：tenantId、createTime、updateTime、creator 等字段自动注入

### JWT Claims 结构

| Claim | 字段 | 说明 |
|---|---|---|
| `uid` | CLAIM_USER_ID | 用户 ID |
| `tid` | CLAIM_TENANT_ID | 租户 ID |
| `sub` | username | 用户名 |
| `oid` | CLAIM_ORG_ID | 当前组织 ID |
| `sid` | shopId | 当前店铺 ID |
| `ds` | CLAIM_DATA_SCOPE | 数据权限范围代码 |
| `asids` | accessibleShopIds | 可访问店铺 ID 列表 |
| `aoids` | accessibleOrgIds | 可访问组织 ID 列表 |
| `authorities` | 角色+菜单权限标识 | 格式：`ROLE_xxx` 或 `system:user:create` |
| `clientType` | 登录端标识 | web / miniapp / app |

---

## 数据库表概览

| 模块 | 主要表 |
|---|---|
| system | sys_user, sys_shop, sys_org, sys_role, sys_menu, sys_role_menu, sys_user_shop_role, sys_post, sys_dict_type, sys_dict_item, sys_config, sys_notice, sys_message, sys_login_log, sys_oper_log, oa_leave, oa_leave_approval |
| erp | erp_customer, erp_supplier, erp_product, erp_product_info, erp_product_category, erp_warehouse, erp_stock, erp_sale_order, erp_sale_order_item, erp_purchase_order, erp_purchase_order_item, erp_contract, erp_contract_item, erp_opportunity, fin_receivable, fin_receivable_record, fin_payable, fin_payable_record |
| oa | oa_employee, oa_attendance_record, oa_attendance_rule, oa_leave_request, oa_overtime, oa_schedule, oa_task, oa_task_comment, oa_file, oa_file_folder, oa_approval_task |
| wage | wage_item_config, wage_monthly_slip |
