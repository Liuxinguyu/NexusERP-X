package com.nexus.erp.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.erp.domain.model.ErpStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ErpStockMapper extends BaseMapper<ErpStock> {

    /**
     * 乐观锁扣减库存：qty >= deductQty 时才扣减，返回影响行数。
     * 0 表示库存不足，扣减失败。
     */
    @Update("UPDATE erp_stock SET qty = qty - #{deductQty} WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND warehouse_id = #{warehouseId} AND qty >= #{deductQty}")
    int deductStock(@Param("tenantId") Long tenantId,
                    @Param("productId") Long productId,
                    @Param("warehouseId") Long warehouseId,
                    @Param("deductQty") int deductQty);

    /**
     * 库存预警查询：当前库存低于商品最低库存的记录
     */
    @Select("SELECT s.product_id AS productId, p.product_name AS productName, " +
            "w.warehouse_name AS warehouseName, s.qty AS currentQty, p.min_stock AS minStock " +
            "FROM erp_stock s " +
            "INNER JOIN erp_product_info p ON p.id = s.product_id AND p.tenant_id = #{tenantId} AND p.del_flag = 0 " +
            "INNER JOIN erp_warehouse w ON w.id = s.warehouse_id AND w.tenant_id = #{tenantId} AND w.del_flag = 0 " +
            "WHERE s.tenant_id = #{tenantId} AND s.del_flag = 0 AND p.min_stock > 0 AND s.qty < p.min_stock " +
            "ORDER BY (p.min_stock - s.qty) DESC")
    List<Map<String, Object>> selectStockAlarm(@Param("tenantId") Long tenantId);
}
