package com.nexus.oa.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_task")
public class OaTask extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private String title;
    private String description;
    /** 1紧急 2高 3中 4低 */
    private Integer priority;
    /** 0待接受 1进行中 2已完成 3已取消 */
    private Integer status;
    private Long assigneeUserId;
    private String assigneeUserName;
    private Long creatorUserId;
    private LocalDate dueDate;
    private LocalDate startDate;
    private LocalDateTime completedTime;
    private Integer progress;
    private String tags;
}
