-- 应付账款主表
CREATE TABLE IF NOT EXISTS fin_payable (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  payable_no VARCHAR(64) NOT NULL COMMENT '应付单号',
  source_type VARCHAR(16) NOT NULL COMMENT '来源类型：purchase_order',
  source_id BIGINT NOT NULL COMMENT '来源单据ID',
  supplier_id BIGINT NOT NULL COMMENT '供应商ID',
  supplier_name VARCHAR(128) COMMENT '供应商名称',
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应付总额',
  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
  pending_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '待付金额',
  invoice_no VARCHAR(64) COMMENT '发票号',
  due_date DATE COMMENT '到期日期',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0未付款 1部分付款 2已结清',
  remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, payable_no),
  KEY idx_supplier (tenant_id, supplier_id),
  KEY idx_status (tenant_id, status),
  UNIQUE KEY uk_payable_source (tenant_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 付款记录表
CREATE TABLE IF NOT EXISTS fin_payable_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  payable_id BIGINT NOT NULL COMMENT '应付单ID',
  record_no VARCHAR(64) NOT NULL COMMENT '付款流水号',
  amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '付款金额',
  payment_method VARCHAR(32) COMMENT '付款方式：银行转账/现金/支票',
  payment_account VARCHAR(128) COMMENT '付款账户',
  payment_time DATETIME COMMENT '付款时间',
  handler_user_id BIGINT COMMENT '经办人',
  receipt_url VARCHAR(512) COMMENT '凭证附件URL',
  remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, record_no),
  KEY idx_payable (tenant_id, payable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
