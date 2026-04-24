-- 为销售单补齐 customer_id 字段，并补充索引以匹配当前领域模型
SET @db = DATABASE();

SET @sale_customer_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_sale_order' AND COLUMN_NAME = 'customer_id'
);
SET @sale_customer_sql = IF(
  @sale_customer_exists = 0,
  'ALTER TABLE `erp_sale_order` ADD COLUMN `customer_id` BIGINT NULL COMMENT ''客户ID'' AFTER `order_no`',
  'SELECT 1'
);
PREPARE stmt FROM @sale_customer_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sale_customer_idx_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'erp_sale_order' AND INDEX_NAME = 'idx_customer'
);
SET @sale_customer_idx_sql = IF(
  @sale_customer_idx_exists = 0,
  'CREATE INDEX `idx_customer` ON `erp_sale_order` (`customer_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sale_customer_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
