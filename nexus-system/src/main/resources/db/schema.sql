DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS oa_leave_approval;
DROP TABLE IF EXISTS oa_leave;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user_shop_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_shop;
DROP TABLE IF EXISTS sys_user_post;
DROP TABLE IF EXISTS sys_post;
DROP TABLE IF EXISTS sys_org;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_org (
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
);

CREATE TABLE sys_user (
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
);

CREATE TABLE sys_post (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  post_code VARCHAR(64) NOT NULL,
  post_name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

CREATE TABLE sys_user_post (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

CREATE TABLE sys_shop (
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
);

CREATE TABLE sys_role (
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
);

CREATE TABLE sys_user_shop_role (
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
);

CREATE TABLE sys_menu (
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
);

CREATE TABLE sys_role_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

-- 极简请假（状态机驱动，无外部工作流引擎）
CREATE TABLE oa_leave (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  leave_type VARCHAR(32) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  days DECIMAL(10,2) NOT NULL,
  reason VARCHAR(512),
  status TINYINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

CREATE TABLE oa_leave_approval (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  leave_id BIGINT NOT NULL,
  approver_user_id BIGINT NOT NULL,
  post_code VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL,
  opinion VARCHAR(512),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approve_time TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

-- 数据字典
CREATE TABLE sys_dict_type (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  dict_name VARCHAR(128) NOT NULL,
  dict_type VARCHAR(64) NOT NULL,
  status TINYINT DEFAULT 1,
  remark VARCHAR(512),
  del_flag TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  UNIQUE (tenant_id, dict_type)
);

CREATE TABLE sys_dict_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  dict_type VARCHAR(64) NOT NULL,
  label VARCHAR(128) NOT NULL,
  item_value VARCHAR(128) NOT NULL,
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  remark VARCHAR(512),
  del_flag TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT
);

-- 系统参数
CREATE TABLE sys_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_name VARCHAR(128) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value VARCHAR(1024),
  config_type CHAR(1) DEFAULT 'Y',
  remark VARCHAR(512),
  del_flag TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  UNIQUE (tenant_id, config_key)
);

-- 操作日志
CREATE TABLE sys_oper_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT,
  username VARCHAR(64),
  module VARCHAR(128),
  oper_type VARCHAR(32),
  oper_url VARCHAR(512),
  oper_method VARCHAR(16),
  oper_ip VARCHAR(64),
  request_param VARCHAR(4000),
  response_data VARCHAR(4000),
  status TINYINT,
  error_msg VARCHAR(4000),
  cost_time BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 登录日志
CREATE TABLE sys_login_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT,
  username VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL,
  ip VARCHAR(64),
  user_agent VARCHAR(512),
  msg VARCHAR(512),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 通知公告
CREATE TABLE sys_notice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  content TEXT,
  notice_type VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL,
  expire_time TIMESTAMP,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

-- 站内消息
CREATE TABLE sys_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  content TEXT,
  message_type VARCHAR(32) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag TINYINT DEFAULT 0
);

-- 组织树：根 + 子节点（ancestors 含虚拟根 0 至本节点 id）
INSERT INTO sys_org(id, tenant_id, parent_id, ancestors, org_code, org_name, org_type, sort, status)
VALUES (1, 1, 0, '0,1', 'ROOT', '总公司', 1, 0, 1);
INSERT INTO sys_org(id, tenant_id, parent_id, ancestors, org_code, org_name, org_type, sort, status)
VALUES (2, 1, 1, '0,1,2', 'EAST', '华东分公司', 2, 1, 1);

INSERT INTO sys_shop(id, tenant_id, org_id, shop_name, shop_type, status) VALUES (1, 1, 1, '总店', 1, 1);
INSERT INTO sys_user(id, tenant_id, username, password_hash, real_name, status, main_shop_id, main_org_id) VALUES (1, 1, 'admin', '{noop}admin', '管理员', 1, 1, 2);
INSERT INTO sys_post(id, tenant_id, post_code, post_name, status) VALUES (1, 1, 'DEPT_MANAGER', '部门经理', 1);
INSERT INTO sys_post(id, tenant_id, post_code, post_name, status) VALUES (2, 1, 'GM', '总经理', 1);
INSERT INTO sys_user_post(id, tenant_id, user_id, post_id) VALUES (1, 1, 1, 2);
INSERT INTO sys_role(id, tenant_id, shop_id, role_code, role_name, data_scope) VALUES (1, 1, 1, 'ADMIN', '店长', 4);
INSERT INTO sys_user_shop_role(id, tenant_id, user_id, shop_id, role_id) VALUES (1, 1, 1, 1, 1);

INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (1, 1, 0, 'M', '系统管理', 'system', NULL, 'system:manage', 'Setting', 1, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (2, 1, 1, 'C', '用户管理', 'user', 'views/system/user/index', 'system:user:manage', 'User', 2, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (3, 1, 1, 'C', '店铺管理', 'shop', 'views/system/shop/index', 'system:shop:manage', 'Shop', 3, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (4, 1, 1, 'C', '角色管理', 'role', 'views/system/role/index', 'system:role:manage', 'Stamp', 4, 1, 1);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (1, 1, 1, 1);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (2, 1, 1, 2);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (3, 1, 1, 3);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (4, 1, 1, 4);

-- 第二店铺挂在华东分公司下；分店员工 data_scope=2
INSERT INTO sys_shop(id, tenant_id, org_id, shop_name, shop_type, status) VALUES (2, 1, 2, '分店', 1, 1);
INSERT INTO sys_role(id, tenant_id, shop_id, role_code, role_name, data_scope) VALUES (2, 1, 2, 'STAFF', '分店员工', 2);
INSERT INTO sys_user(id, tenant_id, username, password_hash, real_name, status, main_shop_id, main_org_id) VALUES (2, 1, 'staff', '{noop}staff', '店员', 1, 2, 2);
INSERT INTO sys_user_shop_role(id, tenant_id, user_id, shop_id, role_id) VALUES (2, 1, 2, 2, 2);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (5, 1, 2, 1);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (6, 1, 2, 2);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (7, 1, 2, 3);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (8, 1, 2, 4);

-- ERP 业务菜单（挂在「系统管理」下）
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (5, 1, 1, 'M', 'ERP管理', 'erp', NULL, 'erp:manage', 'Briefcase', 5, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (6, 1, 5, 'C', '商品管理', 'product', 'views/erp/product/index', 'erp:product:manage', 'Goods', 1, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (7, 1, 5, 'C', '客户管理', 'customer', 'views/erp/customer/index', 'erp:customer:manage', 'Avatar', 2, 1, 1);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (9, 1, 1, 5);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (10, 1, 1, 6);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (11, 1, 1, 7);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (12, 1, 2, 5);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (13, 1, 2, 6);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (14, 1, 2, 7);

INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (20, 1, 5, 'M', 'ERP基础设置', 'basic', NULL, 'erp:basic', 'FolderOpened', 3, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (21, 1, 20, 'C', '仓库管理', 'warehouse', 'views/erp/warehouse/index', 'erp:warehouse:list', 'OfficeBuilding', 1, 1, 1);
INSERT INTO sys_menu(id, tenant_id, parent_id, menu_type, menu_name, path, component, perms, icon, sort, visible, status)
VALUES (22, 1, 20, 'C', '库存查询', 'stock', 'views/erp/stock/index', 'erp:stock:list', 'Box', 2, 1, 1);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (15, 1, 1, 20);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (16, 1, 1, 21);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (17, 1, 1, 22);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (18, 1, 2, 20);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (19, 1, 2, 21);
INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id) VALUES (20, 1, 2, 22);

-- 数据字典：请假类型
INSERT INTO sys_dict_type(id, tenant_id, dict_name, dict_type, status, remark)
VALUES (1, 1, '请假类型', 'oa_leave_type', 1, 'OA 请假');
INSERT INTO sys_dict_item(id, tenant_id, dict_type, label, item_value, sort, status)
VALUES (1, 1, 'oa_leave_type', '年假', 'ANNUAL', 1, 1);
INSERT INTO sys_dict_item(id, tenant_id, dict_type, label, item_value, sort, status)
VALUES (2, 1, 'oa_leave_type', '事假', 'PERSONAL', 2, 1);
INSERT INTO sys_dict_item(id, tenant_id, dict_type, label, item_value, sort, status)
VALUES (3, 1, 'oa_leave_type', '病假', 'SICK', 3, 1);

-- 系统参数（config_type: Y 内置）
INSERT INTO sys_config(id, tenant_id, config_name, config_key, config_value, config_type, remark)
VALUES (1, 1, '是否启用验证码', 'sys.account.captchaEnabled', 'true', 'Y', '登录验证码开关；Redis 未命中时可填 888888 开发绕过');
INSERT INTO sys_config(id, tenant_id, config_name, config_key, config_value, config_type, remark)
VALUES (2, 1, '是否允许自注册', 'sys.account.registerUser', 'false', 'Y', '开放注册开关');

-- 示例公告（已发布、未过期）
INSERT INTO sys_notice(id, tenant_id, title, content, notice_type, status, expire_time, create_by)
VALUES (1, 1, '欢迎使用 NexusERP', '系统已就绪，祝使用愉快。', 'NOTICE', 1, NULL, 1);
