package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_shop")
public class SysShop extends BaseTenantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;
    private String shopName;
    private Integer shopType;
    private Integer status;
}

