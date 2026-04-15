package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_customer")
public class ErpCustomer extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;
    private String name;
    private String contactName;
    private String contactPhone;
    private String level;
    private BigDecimal creditLimit;
}
