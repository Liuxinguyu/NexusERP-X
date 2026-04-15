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
