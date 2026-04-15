package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dictType;
    private String label;
    @TableField("item_value")
    private String value;
    private Integer sort;
    private Integer status;
    private String remark;
}
