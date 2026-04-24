-- 应收账款主表
CREATE TABLE IF NOT EXISTS fin_receivable (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  receivable_no VARCHAR(64) NOT NULL COMMENT '应收单号',
  source_type VARCHAR(16) NOT NULL COMMENT '来源类型：sale_order/crm_contract',
  source_id BIGINT NOT NULL COMMENT '来源单据ID',
  customer_id BIGINT NOT NULL COMMENT '客户ID',
  customer_name VARCHAR(128) COMMENT '客户名称',
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应收总额',
  received_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  pending_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '待收金额',
  invoice_no VARCHAR(64) COMMENT '发票号',
  due_date DATE COMMENT '到期日期',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0未回款 1部分回款 2已结清',
  remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, receivable_no),
  KEY idx_customer (tenant_id, customer_id),
  KEY idx_status (tenant_id, status),
  UNIQUE KEY uk_receivable_source (tenant_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 收款记录表
CREATE TABLE IF NOT EXISTS fin_receivable_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  receivable_id BIGINT NOT NULL COMMENT '应收单ID',
  record_no VARCHAR(64) NOT NULL COMMENT '收款流水号',
  amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '收款金额',
  payment_method VARCHAR(32) COMMENT '付款方式：银行转账/现金/支票/微信/支付宝',
  payment_account VARCHAR(128) COMMENT '收款账户',
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
  KEY idx_receivable (tenant_id, receivable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
