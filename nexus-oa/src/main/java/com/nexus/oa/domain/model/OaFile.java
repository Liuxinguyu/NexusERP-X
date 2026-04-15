package com.nexus.oa.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_file")
public class OaFile extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long folderId;
    private String fileName;
    private String fileKey;
    private Long fileSize;
    private String fileType;
    private Integer downloadCount;
    /** 0私有 1公开 */
    private Integer visibility;
    private Long ownerUserId;
}
