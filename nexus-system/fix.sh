#!/bin/bash
set -e
sed -i '' 's/SystemAdminDtos.UserShopRoleResponse/SystemAdminDtos.UserShopRoleItem/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
sed -i '' 's/SystemAdminDtos.UserShopRoleResponse/SystemAdminDtos.UserShopRoleItem/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java
sed -i '' 's/req.getMappings()/req.getItems()/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
