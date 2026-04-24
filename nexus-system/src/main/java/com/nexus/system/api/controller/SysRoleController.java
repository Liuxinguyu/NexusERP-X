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

public class SysRoleController {

    private final SysRoleApplicationService roleApplicationService;

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/page")
    public Result<IPage<SysRole>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode) {
        return Result.ok(roleApplicationService.page(current, size, roleName, roleCode));
    }

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/options")
    public Result<List<SystemAdminDtos.RoleOption>> options() {
        return Result.ok(roleApplicationService.listOptions());
    }

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/detail/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.ok(roleApplicationService.getById(id));
    }

    @OpLog(module = "角色管理", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.RoleCreateRequest req) {
        return Result.ok(roleApplicationService.create(req));
    }

    @OpLog(module = "角色管理", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.RoleUpdateRequest req) {
        roleApplicationService.update(id, req);
        return Result.ok();
    }
    
    @OpLog(module = "角色管理", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        roleApplicationService.delete(id);
        return Result.ok();
    }

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/{id}/menu-ids")
    public Result<List<Long>> menuIds(@PathVariable Long id) {
        return Result.ok(roleApplicationService.listMenuIds(id));
    }

    @OpLog(module = "角色管理", type = "分配菜单")
    @PutMapping("/{id}/menus")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.RoleMenuAssignRequest req) {
        roleApplicationService.assignMenus(id, req);
        return Result.ok();
    }
}
