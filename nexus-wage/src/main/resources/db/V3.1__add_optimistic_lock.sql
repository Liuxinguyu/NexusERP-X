-- MySQL 8.x / Flyway
-- Wage 核心表补充 version（幂等）；并覆盖薪酬模块引用到的最小 oa_employee

SET @db := DATABASE();

-- wage_item_config.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'wage_item_config'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'wage_item_config' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `wage_item_config` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- wage_monthly_slip.version
SET @table_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'wage_monthly_slip'
);
SET @exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'wage_monthly_slip' AND COLUMN_NAME = 'version'
);
SET @sql := IF(@table_exists = 1 AND @exists = 0,
  'ALTER TABLE `wage_monthly_slip` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- oa_employee.version（薪酬模块联调/查询员工时会用到）
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

