CREATE TABLE IF NOT EXISTS sys_org (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  ancestors VARCHAR(512) NOT NULL DEFAULT '0',
  org_code VARCHAR(64) NOT NULL,
  org_name VARCHAR(128) NOT NULL,
  org_type TINYINT NOT NULL DEFAULT 1,
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_shop (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  shop_name VARCHAR(128) NOT NULL,
  shop_type TINYINT NOT NULL,
  status TINYINT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64),
  avatar_url VARCHAR(512),
  status TINYINT DEFAULT 1,
  main_shop_id BIGINT,
  main_org_id BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(128) NOT NULL,
  data_scope TINYINT NOT NULL DEFAULT 2,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user_shop_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0,
  menu_type CHAR(1) NOT NULL,
  menu_name VARCHAR(128) NOT NULL,
  path VARCHAR(256),
  component VARCHAR(256),
  perms VARCHAR(128),
  icon VARCHAR(128),
  sort INT DEFAULT 0,
  visible TINYINT DEFAULT 1,
  status TINYINT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_tenant (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_name VARCHAR(128) NOT NULL,
  status TINYINT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT DEFAULT NULL,
  update_by BIGINT DEFAULT NULL,
  del_flag TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_tenant (id, tenant_name, status, del_flag) VALUES
  (1, '集团总部', 1, 0),
  (2, '华南分公司', 1, 0);

-- sys_org / sys_shop：登录需 sys_user_shop_role.shop_id 有效（见 SysUserShopRole、AuthApplicationService）
INSERT IGNORE INTO sys_org (id, tenant_id, parent_id, ancestors, org_code, org_name, org_type, sort, status, del_flag)
VALUES (91001, 1, 0, '0,91001', 'HQ_ROOT', '集团总部', 1, 0, 1, 0);

INSERT IGNORE INTO sys_shop (id, tenant_id, org_id, shop_name, shop_type, status, del_flag)
VALUES (91001, 1, 91001, '集团总部总店', 1, 1, 0);

INSERT IGNORE INTO sys_org (id, tenant_id, parent_id, ancestors, org_code, org_name, org_type, sort, status, del_flag)
VALUES (92001, 2, 0, '0,92001', 'HN_ROOT', '华南分公司', 1, 0, 1, 0);

INSERT IGNORE INTO sys_shop (id, tenant_id, org_id, shop_name, shop_type, status, del_flag)
VALUES (92001, 2, 92001, '华南分公司总店', 1, 1, 0);

INSERT IGNORE INTO sys_user (id, tenant_id, username, password_hash, real_name, status, main_shop_id, main_org_id, del_flag)
VALUES
  (91001, 1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 1, 91001, 91001, 0),
  (91002, 2, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 1, 92001, 92001, 0);

INSERT IGNORE INTO sys_role (id, tenant_id, shop_id, role_code, role_name, data_scope, del_flag)
VALUES
  (91001, 1, NULL, 'SUPER_ADMIN', '超级管理员', 4, 0),
  (91002, 2, NULL, 'SUPER_ADMIN', '超级管理员', 4, 0);

INSERT IGNORE INTO sys_user_shop_role (id, tenant_id, user_id, shop_id, role_id, del_flag)
VALUES
  (91001, 1, 91001, 91001, 91001, 0),
  (91002, 2, 91002, 92001, 91002, 0);

-- 3 条菜单：工作台(顶级)；系统管理(顶级)；ERP进销存 为系统管理子节点（parent_id=系统管理 id）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (91001, 1, 0, 'C', '工作台', 'welcome', 'views/dashboard/index', 'dashboard:view', 'HomeFilled', 1, 1, 1, 0),
  (91002, 1, 0, 'M', '系统管理', 'system', NULL, 'system:manage', 'Setting', 2, 1, 1, 0),
  (91003, 1, 91002, 'M', 'ERP进销存', 'erp', NULL, 'erp:manage', 'Briefcase', 1, 1, 1, 0);

-- 系统管理下：用户 / 店铺 / 角色（path 为末段，前端路由为 /system/user 等）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (91004, 1, 91002, 'C', '用户管理', 'user', 'views/system/user/index', 'system:user:manage', 'User', 2, 1, 1, 0),
  (91005, 1, 91002, 'C', '店铺管理', 'shop', 'views/system/shop/index', 'system:shop:manage', 'Shop', 3, 1, 1, 0),
  (91006, 1, 91002, 'C', '角色管理', 'role', 'views/system/role/index', 'system:role:manage', 'Stamp', 4, 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, del_flag)
VALUES
  (91001, 1, 91001, 91001, 0),
  (91002, 1, 91001, 91002, 0),
  (91003, 1, 91001, 91003, 0),
  (91013, 1, 91001, 91004, 0),
  (91014, 1, 91001, 91005, 0),
  (91015, 1, 91001, 91006, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (92001, 2, 0, 'C', '工作台', 'welcome', 'views/dashboard/index', 'dashboard:view', 'HomeFilled', 1, 1, 1, 0),
  (92002, 2, 0, 'M', '系统管理', 'system', NULL, 'system:manage', 'Setting', 2, 1, 1, 0),
  (92003, 2, 92002, 'M', 'ERP进销存', 'erp', NULL, 'erp:manage', 'Briefcase', 1, 1, 1, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (92004, 2, 92002, 'C', '用户管理', 'user', 'views/system/user/index', 'system:user:manage', 'User', 2, 1, 1, 0),
  (92005, 2, 92002, 'C', '店铺管理', 'shop', 'views/system/shop/index', 'system:shop:manage', 'Shop', 3, 1, 1, 0),
  (92006, 2, 92002, 'C', '角色管理', 'role', 'views/system/role/index', 'system:role:manage', 'Stamp', 4, 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, del_flag)
VALUES
  (92001, 2, 91002, 92001, 0),
  (92002, 2, 91002, 92002, 0),
  (92003, 2, 91002, 92003, 0),
  (92113, 2, 91002, 92004, 0),
  (92114, 2, 91002, 92005, 0),
  (92115, 2, 91002, 92006, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (91100, 1, 91003, 'M', 'ERP基础设置', 'basic', NULL, 'erp:basic', 'FolderOpened', 2, 1, 1, 0),
  (91101, 1, 91100, 'C', '仓库管理', 'warehouse', 'views/erp/warehouse/index', 'erp:warehouse:list', 'OfficeBuilding', 1, 1, 1, 0),
  (91102, 1, 91100, 'C', '库存查询', 'stock', 'views/erp/stock/index', 'erp:stock:list', 'Box', 2, 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, del_flag)
VALUES
  (91110, 1, 91001, 91100, 0),
  (91111, 1, 91001, 91101, 0),
  (91112, 1, 91001, 91102, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (92100, 2, 92003, 'M', 'ERP基础设置', 'basic', NULL, 'erp:basic', 'FolderOpened', 2, 1, 1, 0),
  (92101, 2, 92100, 'C', '仓库管理', 'warehouse', 'views/erp/warehouse/index', 'erp:warehouse:list', 'OfficeBuilding', 1, 1, 1, 0),
  (92102, 2, 92100, 'C', '库存查询', 'stock', 'views/erp/stock/index', 'erp:stock:list', 'Box', 2, 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, del_flag)
VALUES
  (92110, 2, 91002, 92100, 0),
  (92111, 2, 91002, 92101, 0),
  (92112, 2, 91002, 92102, 0);

-- ============================================================
-- 新增菜单：租户1（继续从 93001 开始）
-- ============================================================

-- CRM 商机管理（挂到 ERP进销存 91003 下）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (93001, 1, 91003, 'C', '商机管理', 'opportunity', 'views/erp/opportunity/index', 'erp:opportunity:list', 'TrendCharts', 3, 1, 1, 0);

-- CRM 合同管理
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (93002, 1, 91003, 'C', '合同管理', 'contract', 'views/erp/contract/index', 'erp:contract:list', 'Document', 4, 1, 1, 0);

-- 应收账款
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (93003, 1, 91003, 'C', '应收账款', 'receivable', 'views/fin/receivable/index', 'fin:receivable:list', 'Money', 5, 1, 1, 0);

-- 应付账款
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (93004, 1, 91003, 'C', '应付账款', 'payable', 'views/fin/payable/index', 'fin:payable:list', 'Wallet', 6, 1, 1, 0);

-- 协同办公（顶级目录）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (93010, 1, 0, 'M', '协同办公', 'oa', NULL, 'oa:manage', 'Coin', 4, 1, 1, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (93011, 1, 93010, 'C', '考勤打卡', 'attendance', 'views/oa/attendance/index', 'oa:attendance:checkin', 'Timer', 1, 1, 1, 0),
  (93012, 1, 93010, 'C', '请假申请', 'leave', 'views/oa/leave/index', 'oa:leave:manage', 'Tickets', 2, 1, 1, 0),
  (93013, 1, 93010, 'C', '加班申请', 'overtime', 'views/oa/overtime/index', 'oa:overtime:manage', 'Clock', 3, 1, 1, 0),
  (93014, 1, 93010, 'C', '审批中心', 'approval', 'views/oa/approval/index', 'oa:approval:manage', 'Stamp', 4, 1, 1, 0),
  (93015, 1, 93010, 'C', '任务看板', 'task', 'views/oa/task/index', 'oa:task:manage', 'List', 5, 1, 1, 0),
  (93016, 1, 93010, 'C', '日程管理', 'schedule', 'views/oa/schedule/index', 'oa:schedule:manage', 'Calendar', 6, 1, 1, 0),
  (93017, 1, 93010, 'C', '内部公告', 'notice', 'views/oa/notice/index', 'oa:notice:view', 'Bell', 7, 1, 1, 0),
  (93018, 1, 93010, 'C', '云空间', 'file', 'views/oa/file/index', 'oa:file:manage', 'Folder', 8, 1, 1, 0);

-- 报表分析（顶级目录）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (93020, 1, 0, 'M', '报表分析', 'report', NULL, 'report:manage', 'DataAnalysis', 5, 1, 1, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (93021, 1, 93020, 'C', '销售分析', 'sales', 'views/report/index', 'report:sales:view', 'TrendCharts', 1, 1, 1, 0),
  (93022, 1, 93020, 'C', '库存分析', 'stock', 'views/report/index', 'report:stock:view', 'Box', 2, 1, 1, 0);

-- 角色菜单关联（租户1超级管理员 91001）
INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, del_flag)
VALUES
  (93001, 1, 91001, 93001, 0),
  (93002, 1, 91001, 93002, 0),
  (93003, 1, 91001, 93003, 0),
  (93004, 1, 91001, 93004, 0),
  (93010, 1, 91001, 93010, 0),
  (93011, 1, 91001, 93011, 0),
  (93012, 1, 91001, 93012, 0),
  (93013, 1, 91001, 93013, 0),
  (93014, 1, 91001, 93014, 0),
  (93015, 1, 91001, 93015, 0),
  (93016, 1, 91001, 93016, 0),
  (93017, 1, 91001, 93017, 0),
  (93018, 1, 91001, 93018, 0),
  (93020, 1, 91001, 93020, 0),
  (93021, 1, 91001, 93021, 0),
  (93022, 1, 91001, 93022, 0);

-- ============================================================
-- 新增菜单：租户2（继续从 94001 开始）
-- ============================================================

-- CRM 商机管理（挂到 ERP进销存 92003 下）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (94001, 2, 92003, 'C', '商机管理', 'opportunity', 'views/erp/opportunity/index', 'erp:opportunity:list', 'TrendCharts', 3, 1, 1, 0);

-- CRM 合同管理
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (94002, 2, 92003, 'C', '合同管理', 'contract', 'views/erp/contract/index', 'erp:contract:list', 'Document', 4, 1, 1, 0);

-- 应收账款
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (94003, 2, 92003, 'C', '应收账款', 'receivable', 'views/fin/receivable/index', 'fin:receivable:list', 'Money', 5, 1, 1, 0);

-- 应付账款
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (94004, 2, 92003, 'C', '应付账款', 'payable', 'views/fin/payable/index', 'fin:payable:list', 'Wallet', 6, 1, 1, 0);

-- 协同办公（顶级目录）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (94010, 2, 0, 'M', '协同办公', 'oa', NULL, 'oa:manage', 'Coin', 4, 1, 1, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (94011, 2, 94010, 'C', '考勤打卡', 'attendance', 'views/oa/attendance/index', 'oa:attendance:checkin', 'Timer', 1, 1, 1, 0),
  (94012, 2, 94010, 'C', '请假申请', 'leave', 'views/oa/leave/index', 'oa:leave:manage', 'Tickets', 2, 1, 1, 0),
  (94013, 2, 94010, 'C', '加班申请', 'overtime', 'views/oa/overtime/index', 'oa:overtime:manage', 'Clock', 3, 1, 1, 0),
  (94014, 2, 94010, 'C', '审批中心', 'approval', 'views/oa/approval/index', 'oa:approval:manage', 'Stamp', 4, 1, 1, 0),
  (94015, 2, 94010, 'C', '任务看板', 'task', 'views/oa/task/index', 'oa:task:manage', 'List', 5, 1, 1, 0),
  (94016, 2, 94010, 'C', '日程管理', 'schedule', 'views/oa/schedule/index', 'oa:schedule:manage', 'Calendar', 6, 1, 1, 0),
  (94017, 2, 94010, 'C', '内部公告', 'notice', 'views/oa/notice/index', 'oa:notice:view', 'Bell', 7, 1, 1, 0),
  (94018, 2, 94010, 'C', '云空间', 'file', 'views/oa/file/index', 'oa:file:manage', 'Folder', 8, 1, 1, 0);

-- 报表分析（顶级目录）
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (94020, 2, 0, 'M', '报表分析', 'report', NULL, 'report:manage', 'DataAnalysis', 5, 1, 1, 0);

INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
  (94021, 2, 94020, 'C', '销售分析', 'sales', 'views/report/index', 'report:sales:view', 'TrendCharts', 1, 1, 1, 0),
  (94022, 2, 94020, 'C', '库存分析', 'stock', 'views/report/index', 'report:stock:view', 'Box', 2, 1, 1, 0);

-- 角色菜单关联（租户2超级管理员 91002）
INSERT IGNORE INTO sys_role_menu (id, tenant_id, role_id, menu_id, del_flag)
VALUES
  (94001, 2, 91002, 94001, 0),
  (94002, 2, 91002, 94002, 0),
  (94003, 2, 91002, 94003, 0),
  (94004, 2, 91002, 94004, 0),
  (94010, 2, 91002, 94010, 0),
  (94011, 2, 91002, 94011, 0),
  (94012, 2, 91002, 94012, 0),
  (94013, 2, 91002, 94013, 0),
  (94014, 2, 91002, 94014, 0),
  (94015, 2, 91002, 94015, 0),
  (94016, 2, 91002, 94016, 0),
  (94017, 2, 91002, 94017, 0),
  (94018, 2, 91002, 94018, 0),
  (94020, 2, 91002, 94020, 0),
  (94021, 2, 91002, 94021, 0),
  (94022, 2, 91002, 94022, 0);

-- ============================================================
-- ALTER TABLE：erp_product_info 增加库存预警字段
-- ============================================================
-- erp_product_info 预警字段已在上面手动 ALTER，此处不再重复执行

