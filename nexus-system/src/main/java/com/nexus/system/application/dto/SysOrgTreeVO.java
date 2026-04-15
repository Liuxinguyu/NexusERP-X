package com.nexus.system.application.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构树节点（含子树人数聚合），供管理端树形展示。
 */
@Data
public class SysOrgTreeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String orgName;
    private String orgCode;
    /** 本节点直属用户人数 + 所有子孙节点 userCount 之和（内存聚合） */
    private Integer userCount;
    private List<SysOrgTreeVO> children = new ArrayList<>();
}
