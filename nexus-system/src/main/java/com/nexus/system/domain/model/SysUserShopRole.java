package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_shop_role")
public class SysUserShopRole extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long shopId;
    private Long roleId;
}

