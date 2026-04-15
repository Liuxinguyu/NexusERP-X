package com.nexus.common.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseEntity extends BaseTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(value = "shop_id", fill = FieldFill.INSERT)
    private Long shopId;
}

