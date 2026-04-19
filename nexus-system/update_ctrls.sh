#!/bin/bash
set -e

# Update SysRoleController
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysRoleController.java
package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysRoleApplicationService;
import com.nexus.system.domain.model.SysRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class SysRoleController {

    private final SysRoleApplicationService roleApplicationService;

    @GetMapping("/page")
    public Result<IPage<SysRole>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode) {
        return Result.ok(roleApplicationService.page(current, size, roleName, roleCode));
    }

    @GetMapping("/options")
    public Result<List<SystemAdminDtos.RoleOption>> options() {
        return Result.ok(roleApplicationService.listOptions());
    }

    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.ok(roleApplicationService.getById(id));
    }

    @OpLog(module = "角色管理", type = "新增")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.RoleCreateRequest req) {
        return Result.ok(roleApplicationService.create(req));
    }

    @OpLog(module = "角色管理", type = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.RoleUpdateRequest req) {
        roleApplicationService.update(id, req);
        return Result.ok();
    }
    
    @OpLog(module = "角色管理", type = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleApplicationService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/menu-ids")
    public Result<List<Long>> menuIds(@PathVariable Long id) {
        return Result.ok(roleApplicationService.listMenuIds(id));
    }

    @OpLog(module = "角色管理", type = "分配菜单")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.RoleMenuAssignRequest req) {
        roleApplicationService.assignMenus(id, req);
        return Result.ok();
    }
}
INNER_EOF

# Update SysShopController
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysShopController.java
package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysShopApplicationService;
import com.nexus.system.domain.model.SysShop;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/shops")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class SysShopController {

    private final SysShopApplicationService shopApplicationService;

    @GetMapping("/page")
    public Result<IPage<SysShop>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String shopCode) {
        return Result.ok(shopApplicationService.page(current, size, shopName, shopCode));
    }

    @GetMapping("/options")
    public Result<List<SystemAdminDtos.ShopOption>> options() {
        return Result.ok(shopApplicationService.listOptions());
    }

    @GetMapping("/{id}")
    public Result<SysShop> getById(@PathVariable Long id) {
        return Result.ok(shopApplicationService.getById(id));
    }

    @OpLog(module = "店铺管理", type = "新增")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.ShopCreateRequest req) {
        return Result.ok(shopApplicationService.create(req));
    }

    @OpLog(module = "店铺管理", type = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.ShopUpdateRequest req) {
        shopApplicationService.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "店铺管理", type = "状态变更")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        shopApplicationService.updateStatus(id, status);
        return Result.ok();
    }
    
    @OpLog(module = "店铺管理", type = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shopApplicationService.delete(id);
        return Result.ok();
    }
}
INNER_EOF

# Update SysUserAdminController
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java
package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysUserAdminApplicationService;
import com.nexus.system.domain.model.SysUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class SysUserAdminController {

    private final SysUserAdminApplicationService userAdminApplicationService;

    @GetMapping("/page")
    public Result<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Long orgId) {
        return Result.ok(userAdminApplicationService.page(current, size, username, nickname, orgId));
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.ok(userAdminApplicationService.getById(id));
    }

    @OpLog(module = "用户管理", type = "新增")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.UserCreateRequest req) {
        return Result.ok(userAdminApplicationService.create(req));
    }

    @OpLog(module = "用户管理", type = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.UserUpdateRequest req) {
        userAdminApplicationService.update(id, req);
        return Result.ok();
    }
    
    @OpLog(module = "用户管理", type = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminApplicationService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/shop-roles")
    public Result<List<SystemAdminDtos.UserShopRoleResponse>> listShopRoles(@PathVariable Long id) {
        return Result.ok(userAdminApplicationService.listUserShopRoles(id));
    }

    @OpLog(module = "用户管理", type = "分配店铺角色")
    @PutMapping("/{id}/shop-roles")
    public Result<Void> saveShopRoles(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.UserShopRoleSaveRequest req) {
        userAdminApplicationService.saveUserShopRoles(id, req);
        return Result.ok();
    }
}
INNER_EOF

