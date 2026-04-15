package com.nexus.oa.domain.model;

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
@TableName("oa_leave_request")
public class OaLeaveRequest extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantUserId;
    /** 事假/病假/年假等，可与字典对齐 */
    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal leaveDays;
    private String reason;
    /** 0 草稿 1 待审批 2 已通过 3 已驳回 */
    private Integer status;
    private Long approverUserId;
}
