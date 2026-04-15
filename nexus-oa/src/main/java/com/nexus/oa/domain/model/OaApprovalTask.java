package com.nexus.oa.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_approval_task")
public class OaApprovalTask extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    /** 业务类型：leave / overtime / purchase */
    private String bizType;
    private Long bizId;
    private String title;
    private String contentSummary;
    private Long applicantUserId;
    private String applicantUserName;
    private Long approverUserId;
    private String approverUserName;
    /** 0待审批 1已通过 2已拒绝 */
    private Integer status;
    private String opinion;
    private LocalDateTime approveTime;
}
