package com.nexus.system.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysMenuApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/menus")
@RequiredArgsConstructor
@Validated
public class SysMenuController {

    private final SysMenuApplicationService menuApplicationService;

    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/tree")
    public Result<List<SystemAdminDtos.MenuTreeNode>> tree() {
        return Result.ok(menuApplicationService.menuTree());
    }

    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/detail/{id}")
    public Result<SystemAdminDtos.MenuTreeNode> detail(@PathVariable("id") Long id) {
        return Result.ok(menuApplicationService.getByIdForCurrentTenant(id));
    }

    @OpLog(module = "菜单管理", type = "新增")
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.MenuCreateRequest req) {
        return Result.ok(menuApplicationService.createForCurrentTenant(req));
    }

    @OpLog(module = "菜单管理", type = "修改")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody SystemAdminDtos.MenuUpdateRequest req) {
        req.setId(id);
        menuApplicationService.updateForCurrentTenant(req);
        return Result.ok();
    }

    @OpLog(module = "菜单管理", type = "删除")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        menuApplicationService.deleteForCurrentTenant(id);
        return Result.ok();
    }
}
