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
@TableName("erp_purchase_order")
public class ErpPurchaseOrder extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long supplierId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    /** 0-草稿 1-待审核 2-已审核 3-已入库 -1-已拒绝 */
    private Integer status;
    private String remark;

    @Version
    private Integer version;
}
