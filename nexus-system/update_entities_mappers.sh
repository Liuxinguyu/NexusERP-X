#!/bin/bash
set -e

# Update SysRole to add shopScope
sed -i '' '/private Integer dataScope;/a\
    private Integer shopScope;
' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/domain/model/SysRole.java

# Create SysRoleOrg and SysRoleShop models
mkdir -p /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/domain/model
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/domain/model/SysRoleOrg.java
package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_org")
public class SysRoleOrg {
    private Long roleId;
    private Long orgId;
}
INNER_EOF

cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/domain/model/SysRoleShop.java
package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_shop")
public class SysRoleShop {
    private Long roleId;
    private Long shopId;
}
INNER_EOF

# Create Mappers
mkdir -p /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/infrastructure/mapper
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/infrastructure/mapper/SysRoleOrgMapper.java
package com.nexus.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.system.domain.model.SysRoleOrg;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleOrgMapper extends BaseMapper<SysRoleOrg> {
}
INNER_EOF

cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/infrastructure/mapper/SysRoleShopMapper.java
package com.nexus.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.system.domain.model.SysRoleShop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleShopMapper extends BaseMapper<SysRoleShop> {
}
INNER_EOF

