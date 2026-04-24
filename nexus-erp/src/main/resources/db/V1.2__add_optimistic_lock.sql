-- MySQL 8.x / Flyway
-- 增加乐观锁版本号字段（幂等）

SET @db := DATABASE();

-- erp_stock.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_stock'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_stock' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `erp_stock` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fin_payable.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_payable'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_payable' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `fin_payable` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fin_receivable.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_receivable'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_receivable' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `fin_receivable` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- erp_sale_order.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_sale_order'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_sale_order' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `erp_sale_order` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- erp_purchase_order.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_purchase_order'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_purchase_order' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `erp_purchase_order` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- erp_product_category.tenant_id（按需求补齐；若已存在则跳过）
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_product_category'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_product_category' AND COLUMN_NAME = 'tenant_id'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `erp_product_category` ADD COLUMN `tenant_id` BIGINT NULL COMMENT ''租户ID''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

