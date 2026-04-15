package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseTenantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    @JsonIgnore
    private String passwordHash;
    private String realName;
    private String avatarUrl;
    private Integer status;
    private Long mainShopId;
    /** 主属组织/部门 */
    private Long mainOrgId;
}
