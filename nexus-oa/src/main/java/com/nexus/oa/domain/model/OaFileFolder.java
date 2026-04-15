package com.nexus.oa.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_file_folder")
public class OaFileFolder extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String folderName;
    /** 0私有 1公开 */
    private Integer visibility;
    private Long ownerUserId;
}
