-- 目的：
-- 1) 清理历史重复库存键 (tenant_id, product_id, warehouse_id)
-- 2) 增加唯一索引，保证 upsert 语义可靠

-- 重要说明（MySQL）：
-- 唯一索引 (tenant_id, product_id, warehouse_id) 会覆盖 del_flag=1 的逻辑删除行；
-- 如果仅把重复行设为 del_flag=1，仍然会与唯一索引冲突，导致建索引失败。
-- 因此本迁移采用：qty 聚合保留 1 行 + 物理删除其余重复行，再创建唯一索引。

-- 1) 先把每个键的“保留行”修正为：del_flag=0，qty=所有未删除行 qty 求和（NULL 当 0）
UPDATE erp_stock s
JOIN (
    SELECT
        tenant_id,
        product_id,
        warehouse_id,
        MIN(id) AS keep_id,
        SUM(IFNULL(CASE WHEN del_flag = 0 THEN qty ELSE 0 END, 0)) AS total_qty
    FROM erp_stock
    GROUP BY tenant_id, product_id, warehouse_id
    HAVING COUNT(*) > 1
) d ON d.tenant_id = s.tenant_id
   AND d.product_id = s.product_id
   AND d.warehouse_id = s.warehouse_id
SET s.qty = CASE WHEN s.id = d.keep_id THEN d.total_qty ELSE s.qty END,
    s.del_flag = CASE WHEN s.id = d.keep_id THEN 0 ELSE s.del_flag END;

-- 2) 物理删除同键的非保留行（包括 del_flag=1 的历史行），确保唯一索引不会冲突
DELETE s
FROM erp_stock s
JOIN (
    SELECT tenant_id, product_id, warehouse_id, MIN(id) AS keep_id
    FROM erp_stock
    GROUP BY tenant_id, product_id, warehouse_id
    HAVING COUNT(*) > 1
) d ON d.tenant_id = s.tenant_id
   AND d.product_id = s.product_id
   AND d.warehouse_id = s.warehouse_id
WHERE s.id <> d.keep_id;

-- 增加唯一键；若历史仍有重复未处理，会在此处失败并阻断迁移
ALTER TABLE erp_stock
    ADD UNIQUE KEY uk_erp_stock_scope (tenant_id, product_id, warehouse_id);
