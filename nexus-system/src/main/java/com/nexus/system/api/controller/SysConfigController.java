package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysConfigApplicationService;
import com.nexus.system.domain.model.SysConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/config")
@RequiredArgsConstructor
@Validated
public class SysConfigController {

    private final SysConfigApplicationService configApplicationService;

    @PreAuthorize("@ss.hasPermi('system:config:query')")
    @GetMapping("/page")
    public Result<IPage<SysConfig>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) String configKey) {
        return Result.ok(configApplicationService.page(current, size, configName, configKey));
    }

    @PreAuthorize("@ss.hasPermi('system:config:query')")
    @GetMapping("/detail/{id}")
    public Result<SysConfig> detail(@PathVariable("id") Long id) {
        return Result.ok(configApplicationService.getByIdForCurrentTenant(id));
    }

    @OpLog(module = "参数配置", type = "新增")
    @PreAuthorize("@ss.hasPermi('system:config:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.ConfigCreateRequest req) {
        return Result.ok(configApplicationService.create(req));
    }

    @OpLog(module = "参数配置", type = "修改")
    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody SystemAdminDtos.ConfigUpdateRequest req) {
        configApplicationService.updateFull(id, req);
        return Result.ok();
    }

    @OpLog(module = "参数配置", type = "删除")
    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        configApplicationService.delete(id);
        return Result.ok();
    }
}
