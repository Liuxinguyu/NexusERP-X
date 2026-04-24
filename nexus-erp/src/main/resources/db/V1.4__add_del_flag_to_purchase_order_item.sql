-- 为采购明细补齐逻辑删除字段，与模块 del_flag=0 查询风格一致
ALTER TABLE erp_purchase_order_item
    ADD COLUMN del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常 1删除';

CREATE INDEX idx_purchase_order_item_tenant_order_del
    ON erp_purchase_order_item (tenant_id, order_id, del_flag);
