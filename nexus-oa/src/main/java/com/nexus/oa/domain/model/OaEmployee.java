package com.nexus.oa.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_employee")
public class OaEmployee extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String empNo;
    private String name;
    private String dept;
    private String position;
    private LocalDate hireDate;
    private String phone;
    private Integer status;
    /** 绑定登录用户，用于请假申请人与审批人解析 */
    private Long userId;
    /** 直属上级用户 ID（审批人） */
    private Long directLeaderUserId;
}
