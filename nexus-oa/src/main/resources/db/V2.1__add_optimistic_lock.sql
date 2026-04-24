-- MySQL 8.x / Flyway
-- OA 核心表补充 version（幂等）

SET @db := DATABASE();

-- oa_employee.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oa_employee'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oa_employee' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `oa_employee` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- oa_leave_request.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oa_leave_request'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'oa_leave_request' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `oa_leave_request` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

