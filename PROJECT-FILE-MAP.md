# NexusERP-X 项目文件清单与前后端对照

> 生成时间：2026-04-21
> 扫描范围：全部后端 Java 模块 + enterprise-admin 前端

---

## 一、nexus-common（公共基础模块）

### 1.1 核心领域

| 文件 | 说明 |
|------|------|
| `core/domain/Result.java` | 统一 API 响应包装（code + message + data） |
| `core/domain/ResultCode.java` | 标准业务状态码枚举（SUCCESS, BAD_REQUEST, UNAUTHORIZED 等） |
| `core/domain/IResultCode.java` | 状态码接口 |
| `core/page/PageQuery.java` | 分页请求 DTO |
| `core/page/PageResult.java` | 分页响应包装 |
| `domain/model/AbstractAuditableEntity.java` | 审计基类（createTime, updateTime, createBy, updateBy, delFlag） |
| `domain/model/BaseTenantEntity.java` | 租户基类，继承审计基类，增加 tenantId |
| `domain/model/BaseEntity.java` | 实体基类，继承租户基类，增加 shopId |
| `exception/BusinessException.java` | 业务异常，携带 ResultCode |

### 1.2 上下文（ThreadLocal）

| 文件 | 说明 |
|------|------|
| `context/TenantContext.java` | 当前租户 ID |
| `context/OrgContext.java` | 当前组织 ID、门店 ID、可访问组织/门店列表 |
| `context/GatewayUserContext.java` | 网关注入的 userId、username |
| `context/DataScopeContext.java` | 数据权限级别、deptId、userId |
| `context/NexusRequestHeaders.java` | 网关 HTTP 头常量 |

### 1.3 安全

| 文件 | 说明 |
|------|------|
| `security/NexusPrincipal.java` | UserDetails 实现，携带 userId/tenantId/shopId/orgId/dataScope/authorities |
| `security/SecurityUtils.java` | 静态工具：从 SecurityContext 提取当前用户信息 |
| `security/utils/SecurityUtils.java` | 组件版工具，委托 GatewayUserContext |
| `security/BearerTokenResolver.java` | 从 Authorization 头提取 JWT |
| `security/PermissionService.java` | `@ss.hasPermi()` 实现，检查权限集合 + Redis 缓存 |
| `security/datascope/DataScope.java` | 数据权限枚举：SELF / SHOP / ORG_AND_SUB_SHOPS / ALL |
| `security/event/JwtTokenAuthenticatedEvent.java` | JWT 认证成功事件 |
| `security/jwt/JwtTokenProvider.java` | JWT 创建与解析 |
| `security/jwt/JwtAuthenticationFilter.java` | OncePerRequestFilter，验证 JWT 并填充上下文 |
| `security/jwt/JwtAuthenticationException.java` | JWT 异常 |
| `security/jwt/OnlineTokenValidator.java` | 在线 token 白名单校验接口 |
| `security/jwt/JwtInvalidBeforeStore.java` | 用户级 token 失效时间戳接口 |
| `security/config/NexusSecurityProperties.java` | JWT 密钥、过期时间、放行路径等配置 |
| `security/config/NexusSecurityConfiguration.java` | Spring Security 过滤链配置 |

### 1.4 注解 & 切面

| 文件 | 说明 |
|------|------|
| `annotation/OpLog.java` | 操作审计日志注解 |
| `annotation/DataScope.java` | 组织级数据权限注解 |
| `annotation/ShopScope.java` | 门店级数据权限注解 |
| `annotation/Idempotent.java` | Redis 幂等控制注解 |
| `aspect/IdempotentAspect.java` | 幂等切面，Redis SET NX |
| `audit/OpLogAspect.java` | 操作日志切面 |
| `audit/OperLogRecord.java` | 操作日志载体 |
| `audit/OperLogRecorder.java` | 日志持久化接口 |

### 1.5 MyBatis-Plus

| 文件 | 说明 |
|------|------|
| `mybatis/MybatisPlusConfig.java` | MP 全局配置（分页插件、乐观锁、逻辑删除） |
| `mybatis/TenantFieldFillHandler.java` | 自动填充 tenantId / shopId / createBy / updateBy |
| `mybatis/datapermission/DataScopeInterceptor.java` | SQL 拦截器，按数据权限级别注入 WHERE 条件 |

### 1.6 Web / 拦截器

| 文件 | 说明 |
|------|------|
| `web/auth/InternalAuthInterceptor.java` | 防绕过网关拦截器，从 X-Header 填充上下文 |
| `web/auth/TenantContextWebFilter.java` | Servlet Filter，为未认证请求填充租户上下文 |
| `web/GlobalExceptionHandler.java` | 全局异常处理 |
| `tenant/NexusTenantProperties.java` | 租户配置属性 |
| `utils/HttpRequestUtils.java` | HTTP 工具（clientIp 等） |

---

## 二、nexus-gateway（网关模块）

| 文件 | 说明 |
|------|------|
| `NexusGatewayApplication.java` | 网关启动类 |
| `filter/AuthenticationGlobalFilter.java` | 全局过滤器：JWT 验证 + 向下游注入 X-Header |
| `config/GatewayRouteConfig.java` | 路由配置 |

---

## 三、nexus-system（系统管理模块）

### 3.1 Controller

| 文件 | 说明 | 前端对应 |
|------|------|----------|
| `AuthController.java` | 登录/登出/刷新/切换门店 | `enterprise-admin/src/api/auth.ts` → App.tsx 登录流程 |
| `SysUserAdminController.java` | 用户 CRUD + 状态 + 门店角色分配 | `api/system-crud.ts` → `features/system/UserManage.tsx` |
| `SysRoleController.java` | 角色 CRUD + 菜单权限分配 | `api/system-crud.ts` → `features/system/RoleManage.tsx` |
| `SysShopController.java` | 门店 CRUD + 状态切换 | `api/system-crud.ts` → `features/system/ShopManage.tsx` |
| `SysMenuController.java` | 菜单树查询 | `api/system-crud.ts` → RoleManage 菜单分配 |
| `SysOrgController.java` | 组织树 CRUD | `api/system-crud.ts` → `features/system/components/OrgSidebar.tsx` |
| `SysDictTypeController.java` | 字典类型 CRUD | `api/dict.ts` → `features/system/DictManage.tsx` |
| `SysDictItemController.java` | 字典项 CRUD | `api/dict.ts` → `features/system/DictManage.tsx` |
| `SysLoginLogController.java` | 登录日志分页查询 | `api/monitor.ts` → `features/system/LoginLogManage.tsx` |
| `SysNoticeController.java` | 公告 CRUD | 前端暂未对接 |
| `SysOnlineUserController.java` | 在线用户管理 | 前端暂未对接 |
| `WorkbenchController.java` | 工作台仪表盘数据 | `api/system.ts` → App.tsx 工作台 |
| `CaptchaController.java` | 验证码生成与校验 | `api/auth.ts` → App.tsx 登录 |
| `SysUserInfoController.java` | 当前用户信息查询 | `api/auth.ts` → `context/PermissionsContext.tsx` |

### 3.2 Application Service

| 文件 | 说明 |
|------|------|
| `AuthApplicationService.java` | 登录认证、JWT 签发、门店确认、token 刷新 |
| `SysUserAdminApplicationService.java` | 用户管理业务逻辑 |
| `SysRoleApplicationService.java` | 角色管理 + 菜单权限绑定 |
| `SysShopApplicationService.java` | 门店管理 + 组织树过滤 |
| `SysMenuApplicationService.java` | 菜单树构建 |
| `SysOrgApplicationService.java` | 组织树 CRUD + 子孙节点查询 |
| `SysDictApplicationService.java` | 字典类型/项 CRUD |
| `SysConfigApplicationService.java` | 系统配置读写（带 Spring Cache） |
| `SysLoginLogApplicationService.java` | 登录日志记录与查询 |
| `SysNoticeApplicationService.java` | 公告管理 |
| `OnlineUserRedisService.java` | 在线用户 Redis 管理 |
| `WorkbenchApplicationService.java` | 工作台统计数据聚合 |

### 3.3 Domain Model

| 文件 | 说明 |
|------|------|
| `SysUser.java` | 用户（username, passwordHash, realName, avatarUrl, status, mainShopId, mainOrgId） |
| `SysRole.java` | 角色（roleCode, roleName, dataScope） |
| `SysShop.java` | 门店 |
| `SysOrg.java` | 组织/部门（树形） |
| `SysMenu.java` | 菜单/权限 |
| `SysConfig.java` | 系统配置 KV |
| `SysDictType.java` | 字典类型 |
| `SysDictItem.java` | 字典项 |
| `SysLoginLog.java` | 登录日志 |
| `SysOperLog.java` | 操作日志 |
| `SysNotice.java` | 公告 |
| `SysMessage.java` | 站内消息 |
| `SysPost.java` | 岗位 |
| `SysRoleMenu.java` | 角色-菜单关联 |
| `SysRoleOrg.java` | 角色-组织关联 |
| `SysRoleShop.java` | 角色-门店关联 |
| `SysUserPost.java` | 用户-岗位关联 |
| `SysUserShopRole.java` | 用户-门店-角色关联 |

### 3.4 Mapper

| 文件 | 说明 |
|------|------|
| `SysUserMapper.java / .xml` | 用户查询（含组织 JOIN） |
| `SysRoleMapper.java / .xml` | 角色列表查询 |
| `WorkbenchMapper.java / .xml` | 工作台仪表盘聚合查询 |
| 其余 Mapper | 各实体标准 BaseMapper |

---

## 四、nexus-erp（ERP 业务模块）

### 4.1 Controller

| 文件 | 说明 | 前端对应 |
|------|------|----------|
| `ErpProductController.java` | 产品 CRUD + 状态变更 | `pages/SalesOutbound.tsx`（mock） |
| `ErpProductInfoController.java` | 产品信息 CRUD | 前端暂未对接 |
| `ErpProductCategoryController.java` | 产品分类树/列表 CRUD | 前端暂未对接 |
| `ErpCustomerController.java` | 客户 CRUD | `pages/CustomerList.tsx`（mock） |
| `ErpSupplierController.java` | 供应商 CRUD（已脱敏银行信息） | 前端暂未对接 |
| `ErpWarehouseController.java` | 仓库 CRUD | 前端暂未对接 |
| `ErpStockController.java` | 库存分页查询 | 前端暂未对接 |
| `ErpPurchaseOrderController.java` | 采购订单 CRUD + 入库确认 | 前端暂未对接 |
| `ErpSaleOrderController.java` | 销售订单 CRUD + 出库确认 | `api/erp.ts` → App.tsx 工作台 |
| `ErpOpportunityController.java` | CRM 商机管理 + 阶段推进 | 前端暂未对接 |
| `ErpContractController.java` | CRM 合同管理 | 前端暂未对接 |
| `FinReceivableController.java` | 应收账款 + 回款登记 | 前端暂未对接 |
| `FinPayableController.java` | 应付账款 + 付款登记 | 前端暂未对接 |
| `ErpReportController.java` | ERP 报表（销售/采购/库存统计） | 前端暂未对接 |

### 4.2 Application Service

| 文件 | 说明 |
|------|------|
| `ErpProductApplicationService.java` | 产品管理（含分类关联） |
| `ErpProductInfoApplicationService.java` | 产品信息管理 |
| `ErpProductCategoryApplicationService.java` | 产品分类树构建 |
| `ErpCustomerApplicationService.java` | 客户管理 |
| `ErpSupplierApplicationService.java` | 供应商管理（page 返回 VO 脱敏） |
| `ErpWarehouseApplicationService.java` | 仓库管理 |
| `ErpStockApplicationService.java` | 库存查询 + 预警 |
| `ErpPurchaseOrderApplicationService.java` | 采购订单 + 入库触发应付 |
| `ErpSaleOrderApplicationService.java` | 销售订单 + 出库触发应收 |
| `ErpOpportunityApplicationService.java` | 商机管理 + 阶段状态机 |
| `ErpContractApplicationService.java` | 合同管理 |
| `FinReceivableApplicationService.java` | 应收管理 + 回款状态机（乐观锁） |
| `FinPayableApplicationService.java` | 应付管理 + 付款状态机（乐观锁） |
| `ErpReportApplicationService.java` | 报表聚合 |

### 4.3 Domain Model

| 文件 | 说明 |
|------|------|
| `ErpProduct.java` | 产品（productCode, productName, category, price, status） |
| `ErpProductInfo.java` | 产品详细信息 |
| `ErpProductCategory.java` | 产品分类（树形） |
| `ErpCustomer.java` | 客户 |
| `ErpSupplier.java` | 供应商（含银行信息） |
| `ErpWarehouse.java` | 仓库 |
| `ErpStock.java` | 库存（productId + warehouseId + qty） |
| `ErpPurchaseOrder.java` | 采购订单 |
| `ErpPurchaseOrderItem.java` | 采购订单明细 |
| `ErpSaleOrder.java` | 销售订单 |
| `ErpSaleOrderItem.java` | 销售订单明细 |
| `ErpOpportunity.java` | CRM 商机 |
| `ErpContract.java` | CRM 合同 |
| `ErpContractItem.java` | 合同明细 |
| `FinReceivable.java` | 应收单 |
| `FinReceivableRecord.java` | 回款记录 |
| `FinPayable.java` | 应付单 |
| `FinPayableRecord.java` | 付款记录 |

### 4.4 DTO

| 文件 | 说明 |
|------|------|
| `ErpDtos.java` | 产品/客户/商机/销售订单等请求与 VO |
| `ErpFoundationDtos.java` | 产品分类/产品信息/仓库/供应商/库存 VO |
| `FinDtos.java` | 应收/应付/回款/付款请求与 VO |

### 4.5 Mapper XML

| 文件 | 说明 |
|------|------|
| `ErpStockMapper.xml` | 乐观锁扣减库存 + UPSERT 增加库存 |
| 其余 Mapper | 各实体标准 BaseMapper |

---

## 五、nexus-oa（OA 办公模块）

### 5.1 Controller

| 文件 | 说明 | 前端对应 |
|------|------|----------|
| `OaEmployeeController.java` | 员工档案 CRUD | 前端暂未对接 |
| `OaAttendanceController.java` | 考勤规则/打卡/请假/加班/统计 | 前端暂未对接 |
| `OaLeaveRequestController.java` | 请假申请 CRUD + 审批 | `pages/LeaveApproval.tsx`（mock） |
| `OaApprovalCenterController.java` | 审批中心（我发起/待我审批） | `api/oa.ts` → App.tsx 工作台 |
| `OaTaskController.java` | 任务看板 CRUD + 状态流转 + 评论 | `pages/TaskBoard.tsx`（mock） |
| `OaScheduleController.java` | 日程管理 CRUD | 前端暂未对接 |
| `OaFileController.java` | 云空间（文件夹/文件上传下载） | `pages/CloudDisk.tsx`（mock） |

### 5.2 Application Service

| 文件 | 说明 |
|------|------|
| `OaEmployeeApplicationService.java` | 员工档案管理 |
| `OaAttendanceApplicationService.java` | 考勤全流程（规则/打卡/请假/加班/统计） |
| `OaLeaveRequestApplicationService.java` | 请假申请 + 审批流 |
| `OaApprovalCenterService.java` | 审批中心聚合查询 |
| `OaTaskApplicationService.java` | 任务看板 + 评论 |
| `OaScheduleApplicationService.java` | 日程管理 |
| `OaFileApplicationService.java` | 文件管理（本地存储） |

### 5.3 Domain Model

| 文件 | 说明 |
|------|------|
| `OaEmployee.java` | 员工档案 |
| `OaAttendanceRule.java` | 考勤规则 |
| `OaAttendanceRecord.java` | 打卡记录 |
| `OaLeave.java`（nexus-oa） | 请假明细（表 oa_leave_detail） |
| `OaLeaveRequest.java` | 请假申请单 |
| `OaOvertime.java` | 加班申请 |
| `OaApprovalTask.java` | 审批任务 |
| `OaTask.java` | 任务 |
| `OaTaskComment.java` | 任务评论 |
| `OaSchedule.java` | 日程 |
| `OaFile.java` | 文件 |
| `OaFileFolder.java` | 文件夹 |

---

## 六、nexus-wage（薪资模块）

### 6.1 Controller

| 文件 | 说明 | 前端对应 |
|------|------|----------|
| `WageItemConfigController.java` | 薪资项配置 CRUD | 前端暂未对接 |
| `WageMonthlySlipController.java` | 月度工资条 CRUD + 批量生成 | `pages/Finance.tsx`（mock） |

### 6.2 Application Service

| 文件 | 说明 |
|------|------|
| `WageItemConfigApplicationService.java` | 薪资项配置管理 |
| `WageMonthlySlipApplicationService.java` | 工资条管理 + 批量生成 |

### 6.3 Domain Model

| 文件 | 说明 |
|------|------|
| `WageItemConfig.java` | 薪资项配置 |
| `WageMonthlySlip.java` | 月度工资条 |

---

## 七、enterprise-admin（前端 React/TypeScript）

### 7.1 API 层

| 文件 | 对接后端 |
|------|----------|
| `api/auth.ts` | AuthController, CaptchaController, SysUserInfoController |
| `api/system.ts` | WorkbenchController |
| `api/system-crud.ts` | SysUserAdmin, SysRole, SysShop, SysMenu, SysOrg, SysDictType, SysDictItem |
| `api/dict.ts` | SysDictType, SysDictItem |
| `api/erp.ts` | ErpSaleOrderController |
| `api/oa.ts` | OaApprovalCenterController |
| `api/monitor.ts` | SysLoginLogController |

### 7.2 功能页面（已对接后端）

| 文件 | 说明 |
|------|------|
| `features/system/UserManage.tsx` | 用户管理 |
| `features/system/RoleManage.tsx` | 角色管理 + 菜单权限 |
| `features/system/ShopManage.tsx` | 门店管理 |
| `features/system/DictManage.tsx` | 字典管理 |
| `features/system/LoginLogManage.tsx` | 登录日志 |
| `features/system/components/OrgSidebar.tsx` | 组织树侧边栏 |
| `context/PermissionsContext.tsx` | RBAC 权限上下文 |
| `App.tsx` | 应用壳：登录流程 + 工作台 + 路由 |

### 7.3 展示页面（纯 Mock，未对接后端）

| 文件 | 说明 | 待对接后端 |
|------|------|------------|
| `pages/Dashboard.tsx` | 仪表盘 | WorkbenchController |
| `pages/SalesOutbound.tsx` | 销售出库 | ErpSaleOrderController |
| `pages/TaskBoard.tsx` | 任务看板 | OaTaskController |
| `pages/CloudDisk.tsx` | 云空间 | OaFileController |
| `pages/LeaveApproval.tsx` | 请假审批 | OaLeaveRequestController, OaApprovalCenterController |
| `pages/CustomerList.tsx` | 客户档案 | ErpCustomerController |
| `pages/Finance.tsx` | 薪资中心 | WageMonthlySlipController |
| `pages/SystemSettings.tsx` | 系统设置 | 已被 features/system/ 替代 |

---

## 八、本次扫描修复汇总

### 已修复 Bug 列表

| # | 模块 | 文件 | 修复内容 |
|---|------|------|----------|
| 1 | system | `WorkbenchMapper.xml` | `p.name` → `p.product_name`，`w.name` → `w.warehouse_name`，应收状态过滤修正 |
| 2 | system | `SysUserAdminApplicationService.java` | `getDelFlag() == 1` NPE → `Objects.equals()` |
| 3 | system | `SysRoleApplicationService.java` | 同上 NPE 修复 |
| 4 | system | `SysShopApplicationService.java` | NPE 修复 + orgId 无效时返回空页 |
| 5 | system | `SysDictApplicationService.java` | 8 处 `getDelFlag() == 1` NPE 修复 |
| 6 | system | `SysConfigApplicationService.java` | update/delete 添加 `@CacheEvict` |
| 7 | system | `SysUserMapper.xml` | 列名修正（nickname→real_name, avatar→avatar_url），移除不存在字段，tenant_id 强制过滤，sys_org JOIN 加 del_flag |
| 8 | system | `SysRoleMapper.xml` | 移除不存在的 roleSort/status 属性，tenant_id 强制过滤 |
| 9 | system | `AuthController.java` | login 添加 `@Valid` |
| 10 | erp | `FinPayableApplicationService.java` | BigDecimal null 安全 + Integer 拆箱 NPE 修复 |
| 11 | erp | `FinReceivableApplicationService.java` | 同上 |
| 12 | erp | `ErpOpportunityApplicationService.java` | `status == 0` → `Objects.equals()` |
| 13 | erp | `ErpStockMapper.xml` | deductStock 添加 `AND del_flag = 0` |
| 14 | erp | `ErpStockMapper.java` | `p.name` → `p.product_name`，`w.name` → `w.warehouse_name` |
| 15 | erp | `ErpSupplierController.java` | 返回 SupplierVO 脱敏银行信息 + 添加 `@PreAuthorize` |
| 16 | erp | 7 个 ERP Controller | 全部添加 `@PreAuthorize` 权限注解 |
| 17 | oa | 7 个 OA Controller | 全部添加 `@PreAuthorize` 权限注解 |
| 18 | oa | `OaTaskComment.java` | 移除 createTime 字段遮蔽父类 |
| 19 | common | `InternalAuthInterceptor.java` | OrgContext/DataScopeContext 仅在未被 JWT 设置时才从 Header 填充，防止跨租户伪造 |
