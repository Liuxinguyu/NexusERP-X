package com.nexus.wage.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wage_monthly_slip")
public class WageMonthlySlip extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属月份，如 2024-10 */
    private String belongMonth;

    private Long employeeId;

    private BigDecimal baseSalary;

    private BigDecimal subsidyTotal;

    private BigDecimal deductionTotal;

    private BigDecimal netPay;

    /** 0 待确认 1 已发放 */
    private Integer status;
}
