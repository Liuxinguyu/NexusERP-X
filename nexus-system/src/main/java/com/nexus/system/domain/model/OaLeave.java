package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假单（状态机：0 草稿 1 部门审批中 2 总经理审批中 3 已通过 4 已驳回）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_leave")
public class OaLeave extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long orgId;
    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal days;
    private String reason;
    /** 0 草稿 1 部门审批中 2 总经理审批中 3 已通过 4 已驳回 */
    private Integer status;
}
