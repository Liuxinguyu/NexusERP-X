package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product_info")
public class ErpProductInfo extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("code")
    private String productCode;
    @TableField("name")
    private String productName;
    private Long categoryId;
    /** 规格型号 */
    @TableField("spec")
    private String specModel;
    private String unit;
    private BigDecimal price;
    /** 库存数量（列 stock） */
    @TableField("stock")
    private Integer stockQty;
    private Integer status;
    /** 最低库存预警 */
    private Integer minStock;
    /** 最高库存预警 */
    private Integer maxStock;
}
