package com.nexus.oa.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_attendance_rule")
public class OaAttendanceRule extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;
    private LocalTime checkInStart;
    private LocalTime checkInEnd;
    private LocalTime checkOutStart;
    private LocalTime checkOutEnd;
    private Integer isEnable;
    private String remark;
}
