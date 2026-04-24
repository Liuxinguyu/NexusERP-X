package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.service.SysOperLogQueryService;
import com.nexus.system.domain.model.SysOperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/oper-log")
@RequiredArgsConstructor
@Validated
public class SysOperLogController {

    private final SysOperLogQueryService operLogQueryService;

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/page")
    public Result<IPage<SysOperLog>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return Result.ok(operLogQueryService.page(current, size, module, username, status));
    }

    @OpLog(module = "操作日志", type = "清空")
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        operLogQueryService.cleanByTenant();
        return Result.ok();
    }
}
