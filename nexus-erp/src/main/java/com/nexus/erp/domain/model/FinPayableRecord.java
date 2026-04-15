package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_payable_record")
public class FinPayableRecord extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long payableId;
    private String recordNo;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentAccount;
    private LocalDateTime paymentTime;
    private Long handlerUserId;
    private String receiptUrl;
    private String remark;
}
