-- MySQL：薪酬薪资项配置、月度工资单（与 BaseTenantEntity / 逻辑删除字段对齐）

DROP TABLE IF EXISTS wage_monthly_slip;
DROP TABLE IF EXISTS wage_item_config;

CREATE TABLE wage_item_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  item_name VARCHAR(128) NOT NULL COMMENT '薪资项名称',
  calc_type TINYINT NOT NULL COMMENT '1固定值 2手动录入',
  default_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '默认金额',
  item_kind TINYINT NOT NULL DEFAULT 2 COMMENT '1基本工资 2补贴 3扣款',
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_wic_tenant (tenant_id)
);

CREATE TABLE wage_monthly_slip (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  belong_month VARCHAR(7) NOT NULL COMMENT '归属月份 如2024-10',
  employee_id BIGINT NOT NULL COMMENT 'OA员工ID',
  base_salary DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  subsidy_total DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  deduction_total DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  net_pay DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已发放',
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wms_tenant_month_emp (tenant_id, belong_month, employee_id),
  KEY idx_wms_tenant_month (tenant_id, belong_month),
  KEY idx_wms_employee (employee_id)
);
