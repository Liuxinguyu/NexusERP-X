package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位（与组织树解耦，通过 {@link SysUserPost} 与用户关联）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPost extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String postCode;
    private String postName;
    private Integer status;
}
