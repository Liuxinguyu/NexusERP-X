package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 请假审批记录（0 待处理 1 已同意 2 已驳回）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_leave_approval")
public class OaLeaveApproval extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long leaveId;
    private Long approverUserId;
    private String postCode;
    /** 0 待处理 1 已同意 2 已驳回 */
    private Integer status;
    private String opinion;

    @TableField("approve_time")
    private LocalDateTime approveTime;
}
