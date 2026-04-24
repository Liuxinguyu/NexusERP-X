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
@TableName("fin_receivable")
public class FinReceivable extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String receivableNo;
    /** 来源类型：sale_order / crm_contract */
    private String sourceType;
    private Long sourceId;
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal pendingAmount;
    private String invoiceNo;
    private LocalDate dueDate;
    /** 0未回款 1部分回款 2已结清 */
    private Integer status;
    private String remark;

    @Version
    private Integer version;
}
