package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 树状组织单元（公司 / 分公司 / 门店归属节点等），与 {@code sys_shop.org_id} 关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父组织 id，根节点为 0 */
    private Long parentId;

    /**
     * 祖级列表：自虚拟根 0 起至本节点 id，逗号分隔，如根组织 id=1 为 {@code 0,1}，其子 id=5 为 {@code 0,1,5}。
     */
    private String ancestors;

    private String orgCode;
    private String orgName;

    /** 1 公司 2 分公司 3 其他 */
    private Integer orgType;

    private Integer sort;
    private Integer status;
}
