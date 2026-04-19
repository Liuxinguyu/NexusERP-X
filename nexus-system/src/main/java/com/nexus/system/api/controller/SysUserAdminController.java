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

public class SysUserAdminController {

    private final SysUserAdminApplicationService userAdminApplicationService;

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/page")
    public Result<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Long orgId) {
        return Result.ok(userAdminApplicationService.page(current, size, username, nickname, orgId));
    }

    @GetMapping("/detail/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.ok(userAdminApplicationService.getById(id));
    }

    @OpLog(module = "用户管理", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.UserCreateRequest req) {
        return Result.ok(userAdminApplicationService.create(req));
    }

    @OpLog(module = "用户管理", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.UserUpdateRequest req) {
        userAdminApplicationService.update(id, req);
        return Result.ok();
    }
    
    @OpLog(module = "用户管理", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminApplicationService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/shop-roles")
    public Result<List<SystemAdminDtos.UserShopRoleItem>> listShopRoles(@PathVariable Long id) {
        return Result.ok(userAdminApplicationService.listUserShopRoles(id));
    }

    @OpLog(module = "用户管理", type = "分配店铺角色")
    @PutMapping("/{id}/shop-roles")
    public Result<Void> saveShopRoles(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.UserShopRoleSaveRequest req) {
        userAdminApplicationService.saveUserShopRoles(id, req);
        return Result.ok();
    }
}
