package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_payable")
public class FinPayable extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payableNo;
    /** 来源类型：purchase_order */
    private String sourceType;
    private Long sourceId;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private String invoiceNo;
    private LocalDate dueDate;
    /** 0未付款 1部分付款 2已结清 */
    private Integer status;
    private String remark;

    @Version
    private Integer version;
}
