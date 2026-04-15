package com.nexus.erp.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_contract")
public class ErpContract extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String contractNo;
    private String contractName;
    private Long customerId;
    private String customerName;
    private Long opportunityId;
    private LocalDate signDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private String signedBy;
    private String attachmentUrls;
    /** 1执行中 2到期 3终止 */
    private Integer status;
    private String remark;
}
