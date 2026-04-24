-- Finance source uniqueness: pre-checks and post-checks for V1.6 migration
-- Run on MySQL before and after applying V1.6__dedupe_and_add_unique_fin_source.sql.

-- ===================== pre-check: duplicate active receivables =====================
SELECT tenant_id,
       source_type,
       source_id,
       COUNT(*) AS duplicate_count,
       GROUP_CONCAT(id ORDER BY id) AS receivable_ids
FROM fin_receivable
WHERE del_flag = 0
GROUP BY tenant_id, source_type, source_id
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, tenant_id, source_type, source_id;

-- ===================== pre-check: duplicate active payables =====================
SELECT tenant_id,
       source_type,
       source_id,
       COUNT(*) AS duplicate_count,
       GROUP_CONCAT(id ORDER BY id) AS payable_ids
FROM fin_payable
WHERE del_flag = 0
GROUP BY tenant_id, source_type, source_id
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, tenant_id, source_type, source_id;

-- ===================== pre-check: child rows pointing at duplicate receivables =====================
SELECT d.tenant_id,
       d.source_type,
       d.source_id,
       COUNT(*) AS record_rows,
       GROUP_CONCAT(DISTINCT d.id ORDER BY d.id) AS duplicate_receivable_ids,
       GROUP_CONCAT(r.id ORDER BY r.id) AS receipt_record_ids
FROM fin_receivable_record r
JOIN fin_receivable d ON d.id = r.receivable_id
WHERE d.del_flag = 0
  AND EXISTS (
      SELECT 1
      FROM fin_receivable x
      WHERE x.tenant_id = d.tenant_id
        AND x.source_type = d.source_type
        AND x.source_id = d.source_id
        AND x.del_flag = 0
      GROUP BY x.tenant_id, x.source_type, x.source_id
      HAVING COUNT(*) > 1
  )
GROUP BY d.tenant_id, d.source_type, d.source_id
ORDER BY d.tenant_id, d.source_type, d.source_id;

-- ===================== pre-check: child rows pointing at duplicate payables =====================
SELECT d.tenant_id,
       d.source_type,
       d.source_id,
       COUNT(*) AS record_rows,
       GROUP_CONCAT(DISTINCT d.id ORDER BY d.id) AS duplicate_payable_ids,
       GROUP_CONCAT(r.id ORDER BY r.id) AS payment_record_ids
FROM fin_payable_record r
JOIN fin_payable d ON d.id = r.payable_id
WHERE d.del_flag = 0
  AND EXISTS (
      SELECT 1
      FROM fin_payable x
      WHERE x.tenant_id = d.tenant_id
        AND x.source_type = d.source_type
        AND x.source_id = d.source_id
        AND x.del_flag = 0
      GROUP BY x.tenant_id, x.source_type, x.source_id
      HAVING COUNT(*) > 1
  )
GROUP BY d.tenant_id, d.source_type, d.source_id
ORDER BY d.tenant_id, d.source_type, d.source_id;

-- ===================== post-check: no duplicate active receivables =====================
SELECT COUNT(*) AS duplicate_receivable_group_count
FROM (
    SELECT 1
    FROM fin_receivable
    WHERE del_flag = 0
    GROUP BY tenant_id, source_type, source_id
    HAVING COUNT(*) > 1
) t;

-- ===================== post-check: no duplicate active payables =====================
SELECT COUNT(*) AS duplicate_payable_group_count
FROM (
    SELECT 1
    FROM fin_payable
    WHERE del_flag = 0
    GROUP BY tenant_id, source_type, source_id
    HAVING COUNT(*) > 1
) t;

-- ===================== post-check: unique indexes exist =====================
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('fin_receivable', 'fin_payable')
  AND INDEX_NAME IN ('uk_receivable_source', 'uk_payable_source')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ===================== post-check: no child rows reference missing masters =====================
SELECT COUNT(*) AS dangling_receivable_record_count
FROM fin_receivable_record r
LEFT JOIN fin_receivable m ON m.id = r.receivable_id
WHERE m.id IS NULL;

SELECT COUNT(*) AS dangling_payable_record_count
FROM fin_payable_record r
LEFT JOIN fin_payable m ON m.id = r.payable_id
WHERE m.id IS NULL;
