package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product_category")
public class ErpProductCategory extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;
    /** 父级 ID，0 表示根 */
    private Long parentId;
    @TableField("sort_order")
    private Integer sort;
    private Integer status;
}
