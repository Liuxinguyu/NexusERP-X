package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_order")
public class ErpSaleOrder extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    /** 客户ID（先留字段，可为空） */
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private BigDecimal totalAmount;
    /** 0-草稿 1-待审核 2-已审核 3-已出库 -1-已拒绝 */
    private Integer status;

    @Version
    private Integer version;
}
