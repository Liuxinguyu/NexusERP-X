package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysPostApplicationService;
import com.nexus.system.domain.model.SysPost;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/posts")
@RequiredArgsConstructor
@Validated
public class SysPostController {

    private final SysPostApplicationService postApplicationService;

    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/page")
    public Result<IPage<SysPost>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String postCode,
            @RequestParam(required = false) String postName) {
        return Result.ok(postApplicationService.page(current, size, postCode, postName));
    }

    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/options")
    public Result<List<SysPost>> options() {
        return Result.ok(postApplicationService.listOptions());
    }

    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/detail/{id}")
    public Result<SysPost> detail(@PathVariable("id") Long id) {
        return Result.ok(postApplicationService.getByIdForCurrentTenant(id));
    }

    @OpLog(module = "岗位管理", type = "新增")
    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.PostCreateRequest req) {
        return Result.ok(postApplicationService.create(req));
    }

    @OpLog(module = "岗位管理", type = "修改")
    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody SystemAdminDtos.PostUpdateRequest req) {
        postApplicationService.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "岗位管理", type = "删除")
    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        postApplicationService.delete(id);
        return Result.ok();
    }
}
