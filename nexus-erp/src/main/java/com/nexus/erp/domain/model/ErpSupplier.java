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
@TableName("erp_supplier")
public class ErpSupplier extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("code")
    private String supplierCode;
    @TableField("name")
    private String supplierName;
    @TableField("contact_person")
    private String contactName;
    private String phone;
    private String email;
    /** 开户行 */
    private String bankName;
    /** 账号 */
    @TableField("bank_account")
    private String accountNo;
    private Integer status;
}
