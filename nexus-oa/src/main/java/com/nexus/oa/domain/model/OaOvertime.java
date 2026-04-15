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
@TableName("oa_overtime")
public class OaOvertime extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String overtimeNo;
    private Long userId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal hours;
    private String reason;
    /** 0草稿 1待审批 2已通过 3已拒绝 */
    private Integer status;
    private Long approverUserId;
    private String approverOpinion;
    private LocalDateTime approverTime;
}
