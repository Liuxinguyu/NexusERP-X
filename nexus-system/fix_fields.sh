#!/bin/bash
set -e

# Fix SystemDictDtos.java
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/dto/SystemDictDtos.java
package com.nexus.system.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class SystemDictDtos {

    @Data
    public static class DictTypeCreateRequest {
        @NotBlank(message = "字典名称不能为空")
        private String dictName;
        @NotBlank(message = "字典类型不能为空")
        private String dictType;
        private Integer status;
        private String remark;
    }

    @Data
    public static class DictTypeUpdateRequest {
        @NotBlank(message = "字典名称不能为空")
        private String dictName;
        @NotBlank(message = "字典类型不能为空")
        private String dictType;
        private Integer status;
        private String remark;
    }

    @Data
    public static class DictItemCreateRequest {
        @NotBlank(message = "字典类型不能为空")
        private String dictType;
        @NotBlank(message = "字典标签不能为空")
        private String label;
        @NotBlank(message = "字典键值不能为空")
        private String value;
        private Integer sort;
        private Integer status;
        private String remark;
    }

    @Data
    public static class DictItemUpdateRequest {
        @NotBlank(message = "字典标签不能为空")
        private String label;
        @NotBlank(message = "字典键值不能为空")
        private String value;
        private Integer sort;
        private Integer status;
        private String remark;
    }
}
INNER_EOF

# Fix SysDictApplicationService.java (remove cssClass, listClass)
sed -i '' '/item.setCssClass/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysDictApplicationService.java
sed -i '' '/item.setListClass/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysDictApplicationService.java

# Fix SysRoleApplicationService.java (remove sort, status, remark)
sed -i '' '/role.setSort(/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
sed -i '' '/role.setStatus(/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
sed -i '' '/role.setRemark(/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
sed -i '' 's/SysRole::getSort/SysRole::getId/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
sed -i '' '/eq(SysRole::getStatus, 1)/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
# Also dataScope mapped
sed -i '' '/role.setRoleCode/a\
        role.setDataScope(req.getDataScope() != null ? req.getDataScope() : 1);
' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
sed -i '' '/role.setRoleCode(req.getRoleCode());/a\
        if (req.getDataScope() != null) role.setDataScope(req.getDataScope());
' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java

# Fix SysShopApplicationService.java (remove shopCode, address, contactName, contactPhone)
sed -i '' '/SysShop::getShopCode/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' '/req.getShopCode()/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' '/shop.setShopCode/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' '/shop.setAddress/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' '/shop.setContactName/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' '/shop.setContactPhone/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' 's/, String shopCode//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' 's/, SysShop::getShopCode//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' 's/, r.getShopCode()//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' 's/, s.getShopCode()//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
sed -i '' '/shop.setShopName/a\
        shop.setOrgId(req.getOrgId());\
        shop.setShopType(req.getShopType() != null ? req.getShopType() : 1);
' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java

