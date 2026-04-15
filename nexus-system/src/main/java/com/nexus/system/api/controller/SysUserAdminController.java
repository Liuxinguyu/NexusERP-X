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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(required = false) String username) {
        return Result.ok(userAdminApplicationService.page(current, size, username));
    }

    @OpLog(module = "用户管理", type = "新增", excludeParamNames = {"password", "passwordHash"})
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.UserCreateRequest req) {
        return Result.ok(userAdminApplicationService.create(req));
    }

    @OpLog(module = "用户管理", type = "修改", excludeParamNames = {"password", "passwordHash"})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.UserUpdateRequest req) {
        userAdminApplicationService.update(id, req);
        return Result.ok();
    }

    @GetMapping("/{id}/shop-roles")
    public Result<List<SystemAdminDtos.UserShopRoleRow>> shopRoles(@PathVariable Long id) {
        return Result.ok(userAdminApplicationService.listUserShopRoles(id));
    }

    @OpLog(module = "用户管理", type = "分配角色")
    @PutMapping("/{id}/shop-roles")
    public Result<Void> saveShopRoles(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.UserShopRoleSaveRequest req) {
        userAdminApplicationService.saveUserShopRoles(id, req);
        return Result.ok();
    }
}
