package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;
    private String configKey;
    private String configValue;
    /** Y 内置 N 可改 */
    private String configType;
    private String remark;
}
