#!/bin/bash
set -e

# Fix context user id
for file in /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/*.java; do
    sed -i '' 's/TenantContext.getUserId()/com.nexus.common.context.GatewayUserContext.getUserId()/g' "$file"
done

# Fix User properties
sed -i '' 's/getNickname()/getRealName()/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/SysUser::getNickname/SysUser::getRealName/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/user.setNickname/user.setRealName/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java

sed -i '' 's/user.setPhone(req.getPhone());//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/user.setEmail(req.getEmail());//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/user.setOrgId(req.getOrgId());//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/.eq(orgId != null, SysUser::getOrgId, orgId)//g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java

# Note: ShopOption has id, shopName. We map it with option.setId(s.getId()); option.setShopName(s.getShopName());
# RoleOption has id, roleName, roleCode. We map similarly.
sed -i '' '/RoleOption/d' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
cat << 'INNER_EOF' >> /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java

    public List<SystemAdminDtos.RoleOption> listOptions() {
        Long tenantId = requireTenantId();
        return roleMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getDelFlag, 0)
                        .orderByDesc(SysRole::getId))
                .stream()
                .map(r -> {
                    SystemAdminDtos.RoleOption o = new SystemAdminDtos.RoleOption();
                    o.setId(r.getId()); o.setRoleName(r.getRoleName()); o.setRoleCode(r.getRoleCode());
                    return o;
                }).collect(java.util.stream.Collectors.toList());
    }
INNER_EOF

# Fix UserShopRole mapping errors
sed -i '' 's/new SystemAdminDtos.UserShopRoleItem(e.getKey(), e.getValue())/new SystemAdminDtos.UserShopRoleItem()/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/mapping.getRoleIds()/java.util.Collections.singletonList(mapping.getRoleId())/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java

