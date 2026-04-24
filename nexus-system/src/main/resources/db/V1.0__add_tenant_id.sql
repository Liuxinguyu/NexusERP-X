-- MySQL 8.x / Flyway
-- 为 sys_role_menu 补充 tenant_id 字段（幂等）

SET @db := DATABASE();

SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_role_menu'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_role_menu' AND COLUMN_NAME = 'tenant_id'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `sys_role_menu` ADD COLUMN `tenant_id` BIGINT NULL COMMENT ''租户ID''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

