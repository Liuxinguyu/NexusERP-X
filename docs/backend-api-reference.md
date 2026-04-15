# NexusERP-X 前端开发 API 参考手册

> 基于代码实际 DTO/Controller 整理，供前端开发人员直接使用。

---

## 1. 通用约定

### 响应格式

所有接口统一包装在 `Result<T>` 中：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录 / Token 无效 |
| 403 | 无权限 |
| 500 | 服务器错误 |

### 分页请求

所有分页接口使用相同参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `current` | long | 1 | 页码（从1开始） |
| `size` | long | 10 | 每页条数 |

分页响应为 MyBatis Plus 的 `IPage<T>`：

```json
{
  "records": [...],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

### 认证方式

- 请求头：`Authorization: Bearer <accessToken>`
- Token 在登录成功后返回，有效期 7 天
- 前端需在 Token 即将过期前调用续期接口获取新 Token

### 公共路径（无需登录）

- `POST /api/v1/auth/login`
- `GET /api/v1/system/captcha/*`
- `/doc.html`, `/swagger-ui/*`, `/v3/api-docs/*`, `/actuator/*`

---

## 2. 认证模块 `/api/v1/auth`

### 2.1 登录

```
POST /api/v1/auth/login
Content-Type: application/json
```

**请求体：**

```json
{
  "username": "admin",
  "password": "123456",
  "tenantId": 1,
  "captcha": "abcd",
  "captchaKey": "uuid-xxx"
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `username` | 是 | 用户名 |
| `password` | 是 | 密码（明文，HTTPS传输） |
| `tenantId` | 否 | 多租户场景下指定租户 |
| `captcha` | 开启验证码时必填 | 验证码文字 |
| `captchaKey` | 开启验证码时必填 | 验证码 key，从 `/api/v1/system/captcha/image` 获取 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "tenantId": 1,
    "currentShopId": 1,
    "currentOrgId": 1,
    "dataScope": 2,
    "accessibleShopIds": [1, 2],
    "accessibleOrgIds": [1]
  }
}
```

### 2.2 Token 续期

```
POST /api/v1/auth/refresh
Authorization: Bearer <oldToken>
```

前端在 Token 即将过期时调用，旧 Token 立即失效。

**响应：** 同登录响应

### 2.3 获取可切换店铺列表

```
GET /api/v1/auth/shops
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "data": [
    { "shopId": 1, "shopName": "旗舰店", "shopType": 1, "orgId": 1 },
    { "shopId": 2, "shopName": "分店A", "shopType": 2, "orgId": 1 }
  ]
}
```

### 2.4 切换当前店铺

```
POST /api/v1/auth/switch-shop
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体：**

```json
{ "shopId": 2 }
```

**响应：** 同登录响应（返回新 Token 和店铺信息）

### 2.5 图形验证码

```
GET /api/v1/system/captcha/image
```

**响应：** 图片（Content-Type: image/png），Query 参数返回 key：

```
GET /api/v1/system/captcha/image?width=120&height=40
```

返回 cookie 或 body 中的 `captchaKey` 字段用于后续校验。

---

## 3. 当前用户 `/api/v1/system/user`

### 3.1 获取当前用户信息

```
GET /api/v1/system/user/info
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "admin",
    "realName": "管理员",
    "tenantId": 1,
    "orgId": 1,
    "orgName": "技术部",
    "shopId": 1,
    "shopName": "旗舰店",
    "roles": ["ADMIN", "MANAGER"],
    "menus": [...],
    "permissions": ["system:user:create", ...]
  }
}
```

### 3.2 按部门查询用户列表

```
GET /api/v1/system/user/list-by-org?orgId=1
Authorization: Bearer <token>
```

| 参数 | 必填 | 说明 |
|---|---|---|
| `orgId` | 否 | 部门ID，不传则查全部 |

---

## 4. 用户管理 `/api/v1/system/users`

> 需要 `ROLE_ADMIN` 权限

### 4.1 用户分页

```
GET /api/v1/system/users/page?current=1&size=10&username=admin
Authorization: Bearer <token>
```

### 4.2 新增用户

```
POST /api/v1/system/users
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "username": "zhangsan",
  "password": "Password123",
  "realName": "张三",
  "mainShopId": 1,
  "status": 1
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `username` | 是 | 用户名 |
| `password` | 是 | 初始密码 |
| `realName` | 否 | 真实姓名 |
| `mainShopId` | 是 | 主店铺ID |
| `status` | 否 | 0=停用 1=正常（默认1） |

### 4.3 修改用户

```
PUT /api/v1/system/users/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "realName": "张三（修改）",
  "mainShopId": 1,
  "status": 1,
  "password": ""
}
```

> 注：`password` 为空则不修改密码

### 4.4 查看用户店铺角色

```
GET /api/v1/system/users/{id}/shop-roles
Authorization: Bearer <token>
```

### 4.5 分配用户店铺角色

```
PUT /api/v1/system/users/{id}/shop-roles
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "items": [
    { "shopId": 1, "roleId": 1 },
    { "shopId": 2, "roleId": 2 }
  ]
}
```

---

## 5. 角色管理 `/api/v1/system/roles`

> 需要 `ROLE_ADMIN` 权限

### 5.1 角色列表

```
GET /api/v1/system/roles/page?current=1&size=10&roleName=管理员
Authorization: Bearer <token>
```

### 5.2 新增角色

```
POST /api/v1/system/roles
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "shopId": 1,
  "roleCode": "MANAGER",
  "roleName": "经理",
  "dataScope": 2
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `shopId` | 是 | 所属店铺 |
| `roleCode` | 是 | 角色代码（英文大写，存储时加 ROLE_ 前缀） |
| `roleName` | 是 | 角色名称 |
| `dataScope` | 是 | 数据权限：1=本人 2=本部门 3=本部门及下级 5=全部 |

### 5.3 分配菜单权限

```
PUT /api/v1/system/roles/{id}/menus
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{ "menuIds": [1, 2, 3, 4, 5] }
```

---

## 6. 菜单管理 `/api/v1/system/menus`

> 需要 `ROLE_ADMIN` 权限

### 6.1 获取完整菜单树

```
GET /api/v1/system/menus/tree
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "menuType": "C",
      "menuName": "系统管理",
      "path": "/system",
      "component": "Layout",
      "sort": 1,
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "menuType": "M",
          "menuName": "用户管理",
          "path": "user",
          "component": "system/user/index",
          "sort": 1,
          "children": []
        }
      ]
    }
  ]
}
```

`menuType` 说明：`M`=菜单 `C`=目录 `F`=按钮

---

## 7. 组织与店铺 `/api/v1/system/orgs` `/api/v1/system/shops`

### 7.1 组织树

```
GET /api/v1/system/orgs/tree
Authorization: Bearer <token>
```

### 7.2 店铺列表

```
GET /api/v1/system/shops/page?current=1&size=10
Authorization: Bearer <token>
```

### 7.3 新增店铺

```
POST /api/v1/system/shops
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "orgId": 1,
  "shopName": "新店铺",
  "shopType": 1,
  "status": 1
}
```

---

## 8. 工作台 `/api/v1/system/workbench`

### 8.1 工作台首页数据

```
GET /api/v1/system/workbench/dashboard
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "todaySaleAmount": 125000.00,
    "monthlyPurchaseAmount": 80000.00,
    "customerCount": 45,
    "supplierCount": 12,
    "pendingApprovalCount": 3,
    "stockAlarmCount": 5
  }
}
```

### 8.2 销售趋势图

```
GET /api/v1/system/workbench/sale-chart?months=6
Authorization: Bearer <token>
```

**响应：**

```json
{
  "data": [
    { "month": "2026-01", "amount": 98000.00 },
    { "month": "2026-02", "amount": 125000.00 }
  ]
}
```

### 8.3 热销商品

```
GET /api/v1/system/workbench/top-products?topN=10
Authorization: Bearer <token>
```

### 8.4 库存预警

```
GET /api/v1/system/workbench/stock-alarms
Authorization: Bearer <token>
```

---

## 9. ERP 模块

### 9.1 客户管理 `/api/v1/erp/customers`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/customers/page` | GET | 分页查询 |
| `/erp/customers` | POST | 新增客户 |
| `/erp/customers/{id}` | PUT | 修改客户 |
| `/erp/customers/{id}` | DELETE | 删除客户 |
| `/erp/customers/{id}` | GET | 客户详情 |

**新增客户请求：**

```json
{
  "customerName": "某某公司",
  "contactPerson": "李经理",
  "contactPhone": "13800138000",
  "address": "北京市朝阳区xxx"
}
```

### 9.2 供应商管理 `/api/v1/erp/suppliers`

> 结构同客户管理

### 9.3 产品分类 `/api/v1/erp/product-categories`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/product-categories/tree` | GET | 分类树 |
| `/erp/product-categories` | POST | 新增分类 |
| `/erp/product-categories/{id}` | PUT | 修改分类 |

### 9.4 产品管理 `/api/v1/erp/products`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/products/page` | GET | 分页（参数：`categoryId`, `productName`） |
| `/erp/products` | POST | 新增产品 |
| `/erp/products/{id}` | PUT | 修改产品 |

**新增产品请求：**

```json
{
  "categoryId": 1,
  "productName": "商品A",
  "unit": "件",
  "spec": "默认规格",
  "minStock": 10,
  "description": ""
}
```

### 9.5 产品明细/SKU `/api/v1/erp/product-infos`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/product-infos/page` | GET | 分页（参数：`productId`, `skuCode`） |
| `/erp/product-infos` | POST | 新增SKU |
| `/erp/product-infos/{id}` | PUT | 修改SKU |

### 9.6 仓库管理 `/api/v1/erp/warehouses`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/warehouses/page` | GET | 分页 |
| `/erp/warehouses` | POST | 新增仓库 |
| `/erp/warehouses/{id}` | PUT | 修改仓库 |

### 9.7 库存查询 `/api/v1/erp/stocks`

```
GET /api/v1/erp/stocks/page?current=1&size=10&productId=1&warehouseId=1
Authorization: Bearer <token>
```

**响应字段：** productId, productName, warehouseId, warehouseName, quantity, minStock, maxStock

### 9.8 销售订单 `/api/v1/erp/sale-orders`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/sale-orders/page` | GET | 分页（参数：`status`, `orderNo`） |
| `/erp/sale-orders` | POST | 草稿创建（不扣库存） |
| `/erp/sale-orders/submit` | POST | 提交出库（**扣库存+生成应收**） |
| `/erp/sale-orders/{id}` | GET | 订单详情 |
| `/erp/sale-orders/{id}/items` | GET | 订单明细 |

**草稿创建请求：**

```json
{
  "customerId": 1,
  "customerName": "某某公司",
  "warehouseId": 1,
  "items": [
    { "productId": 1, "quantity": 10, "unitPrice": 100.00 },
    { "productId": 2, "quantity": 5, "unitPrice": 50.00 }
  ]
}
```

> 草稿创建不扣库存，status=0（草稿）

**提交出库请求：** 同上，调用 `/submit` 接口
> 自动扣减库存（乐观锁），库存不足返回错误，扣库成功后 status=1（已出库）并自动生成应收记录

**订单状态：**

| status | 含义 |
|---|---|
| 0 | 草稿 |
| 1 | 已出库（已扣库） |
| -1 | 已取消 |

### 9.9 采购订单 `/api/v1/erp/purchase-orders`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/purchase-orders/page` | GET | 分页（参数：`status`, `orderNo`） |
| `/erp/purchase-orders` | POST | 创建采购单 |
| `/erp/purchase-orders/{id}` | GET | 采购单详情 |
| `/erp/purchase-orders/{id}/items` | GET | 采购明细 |

**创建采购单请求：**

```json
{
  "supplierId": 1,
  "warehouseId": 1,
  "remark": "急需补货",
  "items": [
    { "productId": 1, "quantity": 100, "unitPrice": 80.00 }
  ]
}
```

### 9.10 合同管理 `/api/v1/erp/contracts`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/contracts/page` | GET | 分页 |
| `/erp/contracts` | POST | 新增合同 |
| `/erp/contracts/{id}` | GET | 合同详情 |
| `/erp/contracts/{id}/items` | GET | 合同条款 |

### 9.11 商机管理 `/api/v1/erp/opportunities`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/opportunities/page` | GET | 分页 |
| `/erp/opportunities` | POST | 新增商机 |
| `/erp/opportunities/{id}` | PUT | 修改商机阶段 |

### 9.12 应收管理 `/api/v1/erp/receivables`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/receivables/page` | GET | 分页（参数：`customerId`, `status`, `dateFrom`, `dateTo`） |
| `/erp/receivables` | POST | 手工创建应收 |
| `/erp/receivables/{id}` | GET | 应收详情 |
| `/erp/receivables/{id}/records` | GET | 还款记录 |
| `/erp/receivables/{id}/record` | POST | 录入还款 |

**手工创建应收：**

```json
{
  "customerId": 1,
  "customerName": "某某公司",
  "totalAmount": 10000.00,
  "sourceType": "manual",
  "invoiceNo": "INV-2024-001",
  "dueDate": "2024-12-31",
  "remark": ""
}
```

**录入还款：**

```json
{
  "amount": 5000.00,
  "paymentMethod": "bank_transfer",
  "paymentAccount": "6222********1234",
  "paymentTime": "2024-06-15T10:00:00",
  "receiptUrl": "https://minio.xxx.com/xxx.jpg",
  "remark": ""
}
```

**应收状态：**

| status | 含义 |
|---|---|
| 0 | 未收款 |
| 1 | 部分收款 |
| 2 | 已结清 |

### 9.13 应付管理 `/api/v1/erp/payables`

> 结构同应收管理，替换 customer → supplier

### 9.14 ERP 报表 `/api/v1/erp/reports`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/reports/sale-summary` | GET | 销售汇总（参数：`dateFrom`, `dateTo`） |
| `/erp/reports/purchase-summary` | GET | 采购汇总 |

---

## 10. OA 模块

### 10.1 员工管理 `/api/v1/oa/employees`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/erp/employees/page` | GET | 分页 |
| `/erp/employees` | POST | 新增员工 |
| `/erp/employees/{id}` | PUT | 修改员工 |
| `/erp/employees/{id}` | DELETE | 删除员工 |

**新增员工请求：**

```json
{
  "empNo": "E001",
  "name": "张三",
  "dept": "销售部",
  "position": "销售经理",
  "hireDate": "2024-01-01",
  "phone": "13800138000",
  "status": 1,
  "userId": null,
  "directLeaderUserId": 1
}
```

### 10.2 考勤规则 `/api/v1/oa/attendance/rules`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/rules` | GET | 查规则列表 |
| `/rules` | POST | 新增规则 |
| `/rules/{id}` | PUT | 修改规则 |

**新增考勤规则：**

```json
{
  "ruleName": "标准工时",
  "checkInStart": "08:30",
  "checkInEnd": "09:30",
  "checkOutStart": "17:30",
  "checkOutEnd": "18:30",
  "remark": ""
}
```

### 10.3 考勤打卡 `/api/v1/oa/attendance`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/check-in` | POST | 打卡 |
| `/today` | GET | 今日打卡状态 |
| `/page` | GET | 考勤记录分页 |
| `/statistics` | GET | 考勤统计 |

**打卡请求：**

```json
{
  "type": "in",
  "isOuter": 0,
  "outerAddress": "",
  "outerReason": ""
}
```

| type | 含义 |
|---|---|
| `in` | 上班打卡 |
| `out` | 下班打卡 |

**今日状态响应：**

```json
{
  "checkedIn": true,
  "checkedOut": false,
  "checkInTime": "08:55",
  "checkOutTime": "",
  "status": 1,
  "statusLabel": "正常"
}
```

### 10.4 请假申请 `/api/v1/oa/leave-requests`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/page` | GET | 我的请假分页（参数：`status`） |
| `/` | POST | 提交请假申请 |
| `/{id}` | PUT | 修改请假申请 |
| `/{id}/submit` | POST | 提交草稿进入审批 |
| `/{id}` | DELETE | 删除草稿 |

**提交请假请求：**

```json
{
  "leaveType": "年假",
  "startDate": "2024-07-01",
  "endDate": "2024-07-03",
  "leaveDays": 3,
  "reason": "家人出行"
}
```

**请假状态：**

| status | 含义 |
|---|---|
| 0 | 草稿 |
| 1 | 待审批 |
| 2 | 审批通过 |
| -1 | 驳回 |

### 10.5 加班申请 `/api/v1/oa/overtimes`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/page` | GET | 我的加班分页 |
| `/` | POST | 提交加班申请 |

**提交加班请求：**

```json
{
  "startTime": "2024-07-01 18:00:00",
  "endTime": "2024-07-01 21:00:00",
  "hours": 3.0,
  "reason": "项目赶工"
}
```

### 10.6 排班管理 `/api/v1/oa/schedules`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/page` | GET | 排班分页 |
| `/` | POST | 新增排班 |
| `/{id}` | PUT | 修改排班 |

### 10.7 任务管理 `/api/v1/oa/tasks`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/page` | GET | 任务分页 |
| `/` | POST | 创建任务 |
| `/{id}` | PUT | 修改任务 |
| `/{id}/comment` | POST | 添加评论 |

### 10.8 文件管理 `/api/v1/oa/files`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/folders` | GET/POST/DELETE | 文件夹 CRUD |
| `/upload` | POST | 上传文件（`multipart/form-data`） |
| `/page` | GET | 文件列表（参数：`folderId`） |

**上传文件表单字段：**

| 字段 | 说明 |
|---|---|
| `file` | 文件 binary |
| `folderId` | 目标文件夹ID（根目录为null） |

### 10.9 审批中心 `/api/v1/oa/approval-center`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/my-apply/page` | GET | 我发起的（参数：`status`） |
| `/my-approve/page` | GET | 待我审批（参数：`status`） |
| `/{taskId}/approve` | POST | 审批操作 |

**审批请求：**

```json
{
  "approved": true,
  "opinion": "同意"
}
```

---

## 11. 薪资模块 `/api/v1/wage`

### 11.1 薪资项目配置 `/api/v1/wage/item-configs`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/` | GET | 薪资项目列表 |
| `/` | POST | 新增项目 |
| `/{id}` | PUT | 修改项目 |
| `/{id}` | DELETE | 删除项目 |

**新增薪资项目：**

```json
{
  "itemName": "基本工资",
  "calcType": 1,
  "defaultAmount": 8000.00,
  "itemKind": 1
}
```

| calcType | 含义 |
|---|---|
| 1 | 固定值 |
| 2 | 手动录入 |

| itemKind | 含义 |
|---|---|
| 1 | 基本工资（应发） |
| 2 | 补贴 |
| 3 | 扣款 |

### 11.2 工资条 `/api/v1/wage/monthly-slips`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/` | GET | 工资条列表（参数：`belongMonth` 如 `2024-10`） |
| `/generate` | POST | 生成月度工资条 |
| `/{id}` | GET | 工资条详情 |
| `/{id}` | PUT | 调整工资条 |
| `/confirm-pay` | PUT | 确认发放 |

**生成月度工资条：**

```json
{
  "belongMonth": "2024-10",
  "employeeIds": []
}
```

> `employeeIds` 为空则取当前租户全部在职员工

**调整工资条：**

```json
{
  "baseSalary": 10000.00,
  "subsidyTotal": 2000.00,
  "deductionTotal": 500.00
}
```

**确认发放：**

```json
{ "slipIds": [1, 2, 3] }
```

**工资条状态：**

| status | 含义 |
|---|---|
| 0 | 待确认 |
| 1 | 已发放 |

---

## 12. 系统字典 `/api/v1/system/dicts`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/types` | GET | 字典类型列表 |
| `/types` | POST | 新增类型 |
| `/types/{id}` | PUT | 修改类型 |
| `/items/{typeCode}` | GET | 查某类型下所有字典项 |

---

## 13. 通知与消息

### 13.1 公告 `/api/v1/system/notices`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/page` | GET | 公告分页 |
| `/` | POST | 发布公告 |
| `/{id}` | PUT | 修改公告 |
| `/{id}` | DELETE | 删除公告 |

**发布公告请求：**

```json
{
  "id": null,
  "title": "端午节放假通知",
  "content": "放假时间为...",
  "noticeType": "normal",
  "expireTime": "2024-06-15T23:59:59"
}
```

### 13.2 站内消息 `/api/v1/system/messages`

| 接口 | 方法 | 说明 |
|---|---|---|
| `/page` | GET | 我的消息分页 |
| `/send` | POST | 发送消息 |
| `/{id}/read` | PUT | 标记已读 |

**发送消息请求：**

```json
{
  "userIds": [1, 2, 3],
  "title": "系统通知",
  "content": "内容..."
}
```

---

## 14. 枚举值速查

### 状态类

| 字段 | 值 | 含义 |
|---|---|---|
| 用户状态 `status` | 0 | 停用 |
| | 1 | 正常 |
| 店铺状态 `status` | 0 | 停用 |
| | 1 | 正常 |
| 订单状态 `status` | -1 | 已取消 |
| | 0 | 草稿 |
| | 1 | 已出库/已入库 |
| 应收应付 `status` | 0 | 未收款/未付款 |
| | 1 | 部分 |
| | 2 | 已结清 |
| 工资条 `status` | 0 | 待确认 |
| | 1 | 已发放 |

### 数据权限 `dataScope`

| 值 | 含义 |
|---|---|
| 1 | 本人数据 |
| 2 | 本部门数据 |
| 3 | 本部门及下级 |
| 5 | 全部数据 |

### 菜单类型 `menuType`

| 值 | 含义 |
|---|---|
| `M` | 菜单 |
| `C` | 目录 |
| `F` | 按钮/权限 |

---

## 15. 前端开发注意事项

### 15.1 Token 刷新策略

```javascript
// 示例：在拦截器中处理 401
if (response.status === 401 && !isRefreshing) {
  isRefreshing = true;
  // 调用刷新接口
  const res = await refreshToken();
  // 重试所有失败的请求
  return Promise.all(failedRequests.map(fn => fn()));
}
```

### 15.2 多店铺切换

店铺切换后，接口返回新的 Token，前端需：
1. 更新本地存储的 token
2. 更新全局用户信息的 shopId
3. 刷新页面数据

### 15.3 文件上传

文件上传使用 `multipart/form-data`，MinIO 存储后返回文件访问 URL。

### 15.4 数据权限前端适配

`dataScope` 字段影响查询结果范围，前端无需特殊处理，后端自动过滤。

### 15.5 审批中心

`/my-approve/page` 返回的列表需要前端根据 `taskType` 字段区分是请假还是加班，显示不同的详情跳转链接。
