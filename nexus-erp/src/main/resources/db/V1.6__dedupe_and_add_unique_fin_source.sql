-- 清理历史应收/应付来源重复数据，并为来源三元组加唯一约束
SET @db = DATABASE();

-- ===================== fin_receivable 去重 =====================
DROP TEMPORARY TABLE IF EXISTS tmp_fin_receivable_keep;
CREATE TEMPORARY TABLE tmp_fin_receivable_keep AS
SELECT tenant_id,
       source_type,
       source_id,
       MIN(id) AS keep_id
FROM fin_receivable
WHERE del_flag = 0
GROUP BY tenant_id, source_type, source_id
HAVING COUNT(*) > 1;

UPDATE fin_receivable_record r
JOIN fin_receivable d
  ON d.id = r.receivable_id
JOIN tmp_fin_receivable_keep k
  ON k.tenant_id = d.tenant_id
 AND k.source_type = d.source_type
 AND k.source_id = d.source_id
SET r.receivable_id = k.keep_id
WHERE d.id <> k.keep_id;

DELETE d
FROM fin_receivable d
JOIN tmp_fin_receivable_keep k
  ON k.tenant_id = d.tenant_id
 AND k.source_type = d.source_type
 AND k.source_id = d.source_id
WHERE d.id <> k.keep_id
  AND d.del_flag = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_fin_receivable_keep;

SET @receivable_source_idx_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_receivable' AND INDEX_NAME = 'idx_source'
);
SET @receivable_source_drop_sql = IF(
  @receivable_source_idx_exists > 0,
  'ALTER TABLE `fin_receivable` DROP INDEX `idx_source`',
  'SELECT 1'
);
PREPARE stmt FROM @receivable_source_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @receivable_source_unique_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_receivable' AND INDEX_NAME = 'uk_receivable_source'
);
SET @receivable_source_unique_sql = IF(
  @receivable_source_unique_exists = 0,
  'ALTER TABLE `fin_receivable` ADD UNIQUE KEY `uk_receivable_source` (`tenant_id`, `source_type`, `source_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @receivable_source_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================== fin_payable 去重 =====================
DROP TEMPORARY TABLE IF EXISTS tmp_fin_payable_keep;
CREATE TEMPORARY TABLE tmp_fin_payable_keep AS
SELECT tenant_id,
       source_type,
       source_id,
       MIN(id) AS keep_id
FROM fin_payable
WHERE del_flag = 0
GROUP BY tenant_id, source_type, source_id
HAVING COUNT(*) > 1;

UPDATE fin_payable_record r
JOIN fin_payable d
  ON d.id = r.payable_id
JOIN tmp_fin_payable_keep k
  ON k.tenant_id = d.tenant_id
 AND k.source_type = d.source_type
 AND k.source_id = d.source_id
SET r.payable_id = k.keep_id
WHERE d.id <> k.keep_id;

DELETE d
FROM fin_payable d
JOIN tmp_fin_payable_keep k
  ON k.tenant_id = d.tenant_id
 AND k.source_type = d.source_type
 AND k.source_id = d.source_id
WHERE d.id <> k.keep_id
  AND d.del_flag = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_fin_payable_keep;

SET @payable_source_idx_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_payable' AND INDEX_NAME = 'idx_source'
);
SET @payable_source_drop_sql = IF(
  @payable_source_idx_exists > 0,
  'ALTER TABLE `fin_payable` DROP INDEX `idx_source`',
  'SELECT 1'
);
PREPARE stmt FROM @payable_source_drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payable_source_unique_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'fin_payable' AND INDEX_NAME = 'uk_payable_source'
);
SET @payable_source_unique_sql = IF(
  @payable_source_unique_exists = 0,
  'ALTER TABLE `fin_payable` ADD UNIQUE KEY `uk_payable_source` (`tenant_id`, `source_type`, `source_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @payable_source_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
