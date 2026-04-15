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
@TableName("oa_schedule")
public class OaSchedule extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isAllDay;
    private Integer reminderMinutes;
    private String location;
    private String color;
    /** 0私有 1公开 */
    private Integer visibility;
    private Long creatorUserId;
}
