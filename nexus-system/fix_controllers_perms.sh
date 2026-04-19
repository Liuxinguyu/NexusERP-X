#!/bin/bash
set -e

# Change `@PreAuthorize("hasRole('ADMIN')")` to proper @ss.hasPermi in controllers
# For SysRoleController
sed -i '' "s/@PreAuthorize(\"hasRole('ADMIN')\")//g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysRoleController.java
sed -i '' "s/@GetMapping(\"\/page\")/@PreAuthorize(\"@ss.hasPermi('system:role:list')\")\n    @GetMapping(\"\/page\")/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysRoleController.java
sed -i '' "s/public Result<Long> create(/@PreAuthorize(\"@ss.hasPermi('system:role:add')\")\n    public Result<Long> create(/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysRoleController.java
sed -i '' "s/public Result<Void> update(@PathVariable/@PreAuthorize(\"@ss.hasPermi('system:role:edit')\")\n    public Result<Void> update(@PathVariable/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysRoleController.java
sed -i '' "s/public Result<Void> delete(/@PreAuthorize(\"@ss.hasPermi('system:role:remove')\")\n    public Result<Void> delete(/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysRoleController.java

# For SysUserController
sed -i '' "s/@PreAuthorize(\"hasRole('ADMIN')\")//g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java
sed -i '' "s/@GetMapping(\"\/page\")/@PreAuthorize(\"@ss.hasPermi('system:user:list')\")\n    @GetMapping(\"\/page\")/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java
sed -i '' "s/public Result<Long> create(/@PreAuthorize(\"@ss.hasPermi('system:user:add')\")\n    public Result<Long> create(/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java
sed -i '' "s/public Result<Void> update(@PathVariable/@PreAuthorize(\"@ss.hasPermi('system:user:edit')\")\n    public Result<Void> update(@PathVariable/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java
sed -i '' "s/public Result<Void> delete(/@PreAuthorize(\"@ss.hasPermi('system:user:remove')\")\n    public Result<Void> delete(/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java

# SystemDictController
sed -i '' "s/@PreAuthorize(\"hasRole('ADMIN')\")/@PreAuthorize(\"@ss.hasPermi('system:dict:edit')\")/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SystemDictController.java

# SysShopController
sed -i '' "s/@PreAuthorize(\"hasRole('ADMIN')\")//g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysShopController.java
sed -i '' "s/@GetMapping(\"\/page\")/@PreAuthorize(\"@ss.hasPermi('system:shop:list')\")\n    @GetMapping(\"\/page\")/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysShopController.java
sed -i '' "s/public Result<Long> create(/@PreAuthorize(\"@ss.hasPermi('system:shop:add')\")\n    public Result<Long> create(/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysShopController.java
sed -i '' "s/public Result<Void> update(@PathVariable/@PreAuthorize(\"@ss.hasPermi('system:shop:edit')\")\n    public Result<Void> update(@PathVariable/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysShopController.java
sed -i '' "s/public Result<Void> delete(/@PreAuthorize(\"@ss.hasPermi('system:shop:remove')\")\n    public Result<Void> delete(/g" /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysShopController.java

