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
@TableName("erp_opportunity")
public class ErpOpportunity extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private String customerName;
    private String opportunityName;
    private BigDecimal amount;
    /** 线索 / 需求确认 / 方案 / 报价 / 成交 / 失败 */
    private String stage;
    private Integer probability;
    private LocalDate expectCloseDate;
    private Long ownerUserId;
    private Long contactId;
    private String remark;
    /** 1进行中 0已关闭 */
    private Integer status;
}
