package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_org")
public class SysRoleOrg {
    private Long roleId;
    private Long orgId;
}
