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
@TableName("oa_attendance_record")
public class OaAttendanceRecord extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate checkDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer workMinutes;
    /** 0正常 1迟到 2早退 3缺卡 4旷工 5加班 */
    private Integer status;
    private Integer isOuter;
    private String outerAddress;
    private String outerReason;
    private String remark;
}
