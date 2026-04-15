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
@TableName("erp_warehouse")
public class ErpWarehouse extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("code")
    private String warehouseCode;
    @TableField("name")
    private String warehouseName;
    /** 负责人 */
    @TableField("manager")
    private String managerName;
    /** 联系方式 */
    @TableField("phone")
    private String contactInfo;
    private String address;
    private Integer status;
}
