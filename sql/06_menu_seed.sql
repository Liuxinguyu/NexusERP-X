-- =============================================================
-- 06_menu_seed.sql
-- 补全若依标准 RBAC 菜单 + 按钮权限 + 角色菜单授权
-- 对齐前端 component-registry.ts 的 component 字段
-- 管理员角色 role_id = 91001 (tenant_id=1)
-- =============================================================

-- -----------------------------------------------
-- 1. 更新现有菜单的 component 字段，对齐组件注册表
-- -----------------------------------------------
UPDATE sys_menu SET component = 'system/user'    WHERE tenant_id = 1 AND menu_name = '用户管理'  AND menu_type = 'C' AND parent_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND menu_name = '系统管理') t);
UPDATE sys_menu SET component = 'system/shop'    WHERE tenant_id = 1 AND menu_name = '店铺管理'  AND menu_type = 'C' AND parent_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND menu_name = '系统管理') t);
UPDATE sys_menu SET component = 'system/role'    WHERE tenant_id = 1 AND menu_name = '角色管理'  AND menu_type = 'C' AND parent_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND menu_name = '系统管理') t);

-- -----------------------------------------------
-- 2. 插入新的顶级目录和功能菜单 (tenant_id = 1)
-- -----------------------------------------------

-- 系统监控 目录
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (95001, 1, 0, 'M', '系统监控', 'monitor', NULL, NULL, 'monitor', 20, 1, 1, 0);

-- 系统管理下新增子菜单 (parent_id = 91002 = 系统管理)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95010, 1, 91002, 'C', '菜单管理', 'menu',   'system/menu',   'system:menu:query',   'tree-table', 3,  1, 1, 0),
(95011, 1, 91002, 'C', '组织管理', 'org',    'system/org',    'system:org:query',    'tree',       4,  1, 1, 0),
(95012, 1, 91002, 'C', '岗位管理', 'post',   'system/post',   'system:post:query',   'post',       5,  1, 1, 0),
(95013, 1, 91002, 'C', '数据字典', 'dict',   'system/dict',   'system:dict:query',   'dict',       6,  1, 1, 0),
(95014, 1, 91002, 'C', '参数配置', 'config', 'system/config', 'system:config:query', 'edit',       7,  1, 1, 0);

-- 系统监控下子菜单 (parent_id = 95001)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95020, 1, 95001, 'C', '登录日志', 'loginlog', 'monitor/loginlog', 'monitor:loginlog:list',   'logininfor', 1, 1, 1, 0),
(95021, 1, 95001, 'C', '操作日志', 'operlog',  'monitor/operlog',  'monitor:operlog:list',    'form',       2, 1, 1, 0),
(95022, 1, 95001, 'C', '在线用户', 'online',   'monitor/online',   'monitor:online:list',     'online',     3, 1, 1, 0);

-- -----------------------------------------------
-- 3. 插入按钮权限 (F 类型，不可见菜单)
-- -----------------------------------------------

-- 用户管理按钮 (parent = 91004)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95100, 1, 91004, 'F', '用户查询', NULL, NULL, 'system:user:query',  NULL, 1, 1, 1, 0),
(95101, 1, 91004, 'F', '用户新增', NULL, NULL, 'system:user:add',    NULL, 2, 1, 1, 0),
(95102, 1, 91004, 'F', '用户修改', NULL, NULL, 'system:user:edit',   NULL, 3, 1, 1, 0),
(95103, 1, 91004, 'F', '用户删除', NULL, NULL, 'system:user:remove', NULL, 4, 1, 1, 0);

-- 角色管理按钮 (parent = 91006)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95110, 1, 91006, 'F', '角色查询', NULL, NULL, 'system:role:query',  NULL, 1, 1, 1, 0),
(95111, 1, 91006, 'F', '角色新增', NULL, NULL, 'system:role:add',    NULL, 2, 1, 1, 0),
(95112, 1, 91006, 'F', '角色修改', NULL, NULL, 'system:role:edit',   NULL, 3, 1, 1, 0),
(95113, 1, 91006, 'F', '角色删除', NULL, NULL, 'system:role:remove', NULL, 4, 1, 1, 0);

-- 店铺管理按钮 (parent = 91005)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95120, 1, 91005, 'F', '店铺查询', NULL, NULL, 'system:shop:query',  NULL, 1, 1, 1, 0),
(95121, 1, 91005, 'F', '店铺新增', NULL, NULL, 'system:shop:add',    NULL, 2, 1, 1, 0),
(95122, 1, 91005, 'F', '店铺修改', NULL, NULL, 'system:shop:edit',   NULL, 3, 1, 1, 0),
(95123, 1, 91005, 'F', '店铺删除', NULL, NULL, 'system:shop:remove', NULL, 4, 1, 1, 0);

-- 菜单管理按钮 (parent = 95010)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95130, 1, 95010, 'F', '菜单查询', NULL, NULL, 'system:menu:query',  NULL, 1, 1, 1, 0),
(95131, 1, 95010, 'F', '菜单新增', NULL, NULL, 'system:menu:add',    NULL, 2, 1, 1, 0),
(95132, 1, 95010, 'F', '菜单修改', NULL, NULL, 'system:menu:edit',   NULL, 3, 1, 1, 0),
(95133, 1, 95010, 'F', '菜单删除', NULL, NULL, 'system:menu:remove', NULL, 4, 1, 1, 0);

-- 组织管理按钮 (parent = 95011)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95140, 1, 95011, 'F', '组织查询', NULL, NULL, 'system:org:query',  NULL, 1, 1, 1, 0),
(95141, 1, 95011, 'F', '组织新增', NULL, NULL, 'system:org:add',    NULL, 2, 1, 1, 0),
(95142, 1, 95011, 'F', '组织修改', NULL, NULL, 'system:org:edit',   NULL, 3, 1, 1, 0),
(95143, 1, 95011, 'F', '组织删除', NULL, NULL, 'system:org:remove', NULL, 4, 1, 1, 0);

-- 岗位管理按钮 (parent = 95012)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95150, 1, 95012, 'F', '岗位查询', NULL, NULL, 'system:post:query',  NULL, 1, 1, 1, 0),
(95151, 1, 95012, 'F', '岗位新增', NULL, NULL, 'system:post:add',    NULL, 2, 1, 1, 0),
(95152, 1, 95012, 'F', '岗位修改', NULL, NULL, 'system:post:edit',   NULL, 3, 1, 1, 0),
(95153, 1, 95012, 'F', '岗位删除', NULL, NULL, 'system:post:remove', NULL, 4, 1, 1, 0);

-- 字典管理按钮 (parent = 95013)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95160, 1, 95013, 'F', '字典查询', NULL, NULL, 'system:dict:query',  NULL, 1, 1, 1, 0),
(95161, 1, 95013, 'F', '字典新增', NULL, NULL, 'system:dict:add',    NULL, 2, 1, 1, 0),
(95162, 1, 95013, 'F', '字典修改', NULL, NULL, 'system:dict:edit',   NULL, 3, 1, 1, 0),
(95163, 1, 95013, 'F', '字典删除', NULL, NULL, 'system:dict:remove', NULL, 4, 1, 1, 0);

-- 参数配置按钮 (parent = 95014)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95170, 1, 95014, 'F', '配置查询', NULL, NULL, 'system:config:query',  NULL, 1, 1, 1, 0),
(95171, 1, 95014, 'F', '配置新增', NULL, NULL, 'system:config:add',    NULL, 2, 1, 1, 0),
(95172, 1, 95014, 'F', '配置修改', NULL, NULL, 'system:config:edit',   NULL, 3, 1, 1, 0),
(95173, 1, 95014, 'F', '配置删除', NULL, NULL, 'system:config:remove', NULL, 4, 1, 1, 0);

-- 操作日志按钮 (parent = 95021)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95180, 1, 95021, 'F', '日志查询', NULL, NULL, 'monitor:operlog:list',   NULL, 1, 1, 1, 0),
(95181, 1, 95021, 'F', '日志清空', NULL, NULL, 'monitor:operlog:remove', NULL, 2, 1, 1, 0);

-- 在线用户按钮 (parent = 95022)
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95190, 1, 95022, 'F', '在线查询', NULL, NULL, 'monitor:online:list',        NULL, 1, 1, 1, 0),
(95191, 1, 95022, 'F', '强制退出', NULL, NULL, 'monitor:online:forceLogout', NULL, 2, 1, 1, 0);

-- -----------------------------------------------
-- 4. 为管理员角色授予所有新菜单权限
--    tenant_id=1 的管理员角色 role_id = 91001
-- -----------------------------------------------
INSERT IGNORE INTO sys_role_menu (tenant_id, role_id, menu_id, del_flag) VALUES
-- 系统监控目录
(1, 91001, 95001, 0),
-- 系统管理子菜单
(1, 91001, 95010, 0), (1, 91001, 95011, 0), (1, 91001, 95012, 0), (1, 91001, 95013, 0), (1, 91001, 95014, 0),
-- 系统监控子菜单
(1, 91001, 95020, 0), (1, 91001, 95021, 0), (1, 91001, 95022, 0),
-- 用户管理按钮
(1, 91001, 95100, 0), (1, 91001, 95101, 0), (1, 91001, 95102, 0), (1, 91001, 95103, 0),
-- 角色管理按钮
(1, 91001, 95110, 0), (1, 91001, 95111, 0), (1, 91001, 95112, 0), (1, 91001, 95113, 0),
-- 店铺管理按钮
(1, 91001, 95120, 0), (1, 91001, 95121, 0), (1, 91001, 95122, 0), (1, 91001, 95123, 0),
-- 菜单管理按钮
(1, 91001, 95130, 0), (1, 91001, 95131, 0), (1, 91001, 95132, 0), (1, 91001, 95133, 0),
-- 组织管理按钮
(1, 91001, 95140, 0), (1, 91001, 95141, 0), (1, 91001, 95142, 0), (1, 91001, 95143, 0),
-- 岗位管理按钮
(1, 91001, 95150, 0), (1, 91001, 95151, 0), (1, 91001, 95152, 0), (1, 91001, 95153, 0),
-- 字典管理按钮
(1, 91001, 95160, 0), (1, 91001, 95161, 0), (1, 91001, 95162, 0), (1, 91001, 95163, 0),
-- 参数配置按钮
(1, 91001, 95170, 0), (1, 91001, 95171, 0), (1, 91001, 95172, 0), (1, 91001, 95173, 0),
-- 操作日志按钮
(1, 91001, 95180, 0), (1, 91001, 95181, 0),
-- 在线用户按钮
(1, 91001, 95190, 0), (1, 91001, 95191, 0);

-- -----------------------------------------------
-- 5. ERP / OA / 薪酬 / CRM 目录及菜单
-- -----------------------------------------------

-- ERP 管理 目录
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (95200, 1, 0, 'M', 'ERP 管理', 'erp', NULL, NULL, 'shopping', 30, 1, 1, 0);

-- ERP 子菜单
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95210, 1, 95200, 'C', '销售订单', 'sale-order',      'erp/sale-order',      'erp:sale-order:list',      'shopping', 1, 1, 1, 0),
(95211, 1, 95200, 'C', '采购订单', 'purchase-order',   'erp/purchase-order',  'erp:purchase-order:list',  'component', 2, 1, 1, 0),
(95212, 1, 95200, 'C', '库存查询', 'stock',            'erp/stock',           'erp:stock:list',           'tab',      3, 1, 1, 0),
(95213, 1, 95200, 'C', '应收应付', 'finance',          'erp/finance',         'erp:finance:list',         'money',    4, 1, 1, 0),
(95214, 1, 95200, 'C', '报表分析', 'report',           'erp/report',          'erp:report:list',          'chart',    5, 1, 1, 0);

-- OA 办公 目录
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (95300, 1, 0, 'M', 'OA 办公', 'oa', NULL, NULL, 'peoples', 40, 1, 1, 0);

-- OA 子菜单
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95310, 1, 95300, 'C', '考勤打卡', 'attendance', 'oa/attendance', 'oa:attendance:list', 'date',     1, 1, 1, 0),
(95311, 1, 95300, 'C', '审批中心', 'approval',   'oa/approval',   'oa:approval:list',   'checkbox', 2, 1, 1, 0),
(95312, 1, 95300, 'C', '任务协同', 'task',       'oa/task',       'oa:task:list',       'cascader', 3, 1, 1, 0),
(95313, 1, 95300, 'C', '企业云盘', 'cloud-disk', 'oa/cloud-disk', 'oa:cloud-disk:list', 'upload',   4, 1, 1, 0),
(95314, 1, 95300, 'C', '内部公告', 'notice',     'oa/notice',     'oa:notice:list',     'message',  5, 1, 1, 0);

-- 薪酬管理 目录
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (95400, 1, 0, 'M', '薪酬管理', 'wage', NULL, NULL, 'money', 50, 1, 1, 0);

-- 薪酬子菜单
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95410, 1, 95400, 'C', '工资单',   'slip',   'wage/slip',   'wage:slip:list',   'documentation', 1, 1, 1, 0),
(95411, 1, 95400, 'C', '薪酬配置', 'config', 'wage/config', 'wage:config:list', 'edit',          2, 1, 1, 0);

-- CRM 关系 目录
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES (95500, 1, 0, 'M', 'CRM 关系', 'crm', NULL, NULL, 'people', 60, 1, 1, 0);

-- CRM 子菜单
INSERT IGNORE INTO sys_menu (id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status, del_flag)
VALUES
(95510, 1, 95500, 'C', '商机跟进', 'opportunity', 'crm/opportunity', 'crm:opportunity:list', 'star',  1, 1, 1, 0),
(95511, 1, 95500, 'C', '合同管理', 'contract',    'crm/contract',    'crm:contract:list',    'form',  2, 1, 1, 0);

-- 为管理员角色授予 ERP/OA/薪酬/CRM 菜单权限
INSERT IGNORE INTO sys_role_menu (tenant_id, role_id, menu_id, del_flag) VALUES
(1, 91001, 95200, 0), (1, 91001, 95210, 0), (1, 91001, 95211, 0), (1, 91001, 95212, 0), (1, 91001, 95213, 0), (1, 91001, 95214, 0),
(1, 91001, 95300, 0), (1, 91001, 95310, 0), (1, 91001, 95311, 0), (1, 91001, 95312, 0), (1, 91001, 95313, 0), (1, 91001, 95314, 0),
(1, 91001, 95400, 0), (1, 91001, 95410, 0), (1, 91001, 95411, 0),
(1, 91001, 95500, 0), (1, 91001, 95510, 0), (1, 91001, 95511, 0);

-- -----------------------------------------------
-- 6. 同步更新 tenant_id=2 的菜单（可选）
--    如需多租户同步，复制上述 INSERT 并将 tenant_id 改为 2，id 改为 96xxx 系列
-- -----------------------------------------------
