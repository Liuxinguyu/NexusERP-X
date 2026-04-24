package com.nexus.system.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.system.application.dto.SysOrgTreeVO;
import com.nexus.system.application.dto.SystemOrgDtos;
import com.nexus.system.application.service.SysOrgApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/system/org")
@RequiredArgsConstructor
@Validated
public class SysOrgController {

    private final SysOrgApplicationService sysOrgApplicationService;

    @PreAuthorize("@ss.hasPermi('system:org:list')")
    @GetMapping("/tree")
    public Result<List<SysOrgTreeVO>> tree() {
        return Result.ok(sysOrgApplicationService.treeForCurrentTenant());
    }

    /**
     * 组织树懒加载：只加载 {@code parentId} 下一层；{@code userCount} 为该节点整棵子树人数。
     */
    @PreAuthorize("@ss.hasPermi('system:org:list')")
    @GetMapping("/tree-lazy")
    public Result<List<SysOrgTreeVO>> treeLazy(@RequestParam(required = false, defaultValue = "0") Long parentId) {
        return Result.ok(sysOrgApplicationService.treeLazyForCurrentTenant(parentId));
    }

    @OpLog(module = "组织管理", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:org:add')")
    public Result<Long> create(@Valid @RequestBody SystemOrgDtos.OrgCreateRequest req) {
        return Result.ok(sysOrgApplicationService.createForCurrentTenant(req));
    }

    @OpLog(module = "组织管理", type = "修改")
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:org:edit')")
    public Result<Void> update(@Valid @RequestBody SystemOrgDtos.OrgUpdateRequest req) {
        sysOrgApplicationService.updateForCurrentTenant(req);
        return Result.ok();
    }

    @OpLog(module = "组织管理", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:org:remove')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        sysOrgApplicationService.deleteForCurrentTenant(id);
        return Result.ok();
    }
}
