package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_shop")
public class SysRoleShop {
    private Long roleId;
    private Long shopId;
}
