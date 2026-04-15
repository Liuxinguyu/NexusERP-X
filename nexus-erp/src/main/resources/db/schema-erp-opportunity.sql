-- 商机表
CREATE TABLE IF NOT EXISTS erp_opportunity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL COMMENT '客户ID',
  customer_name VARCHAR(128) COMMENT '客户名称',
  opportunity_name VARCHAR(256) NOT NULL COMMENT '商机名称',
  amount DECIMAL(18,2) DEFAULT 0 COMMENT '预估金额',
  stage VARCHAR(32) NOT NULL DEFAULT '线索' COMMENT '阶段：线索/需求确认/方案/报价/成交/失败',
  probability INT DEFAULT 0 COMMENT '赢单概率 0-100',
  expect_close_date DATE COMMENT '预计成交日期',
  owner_user_id BIGINT COMMENT '负责人用户ID',
  contact_id BIGINT COMMENT '联系人ID',
  remark TEXT,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1进行中 0已关闭',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id),
  KEY idx_customer (tenant_id, customer_id),
  KEY idx_owner (tenant_id, owner_user_id),
  KEY idx_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 合同主表
CREATE TABLE IF NOT EXISTS erp_contract (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  contract_no VARCHAR(64) NOT NULL COMMENT '合同编号',
  contract_name VARCHAR(256) NOT NULL COMMENT '合同名称',
  customer_id BIGINT NOT NULL COMMENT '客户ID',
  customer_name VARCHAR(128) COMMENT '客户名称',
  opportunity_id BIGINT COMMENT '关联商机ID',
  sign_date DATE COMMENT '签订日期',
  start_date DATE COMMENT '生效日期',
  end_date DATE COMMENT '到期日期',
  amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '合同金额',
  signed_by VARCHAR(128) COMMENT '签约人',
  attachment_urls TEXT COMMENT '附件URL，逗号分隔',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1执行中 2到期 3终止',
  remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, contract_no),
  KEY idx_customer (tenant_id, customer_id),
  KEY idx_opportunity (tenant_id, opportunity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 合同明细表
CREATE TABLE IF NOT EXISTS erp_contract_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  contract_id BIGINT NOT NULL,
  product_id BIGINT,
  product_name VARCHAR(256),
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(18,2),
  subtotal DECIMAL(18,2),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_contract (tenant_id, contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
