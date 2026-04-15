package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.service.SysLoginLogApplicationService;
import com.nexus.system.domain.model.SysLoginLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/login-log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysLoginLogController {

    private final SysLoginLogApplicationService loginLogApplicationService;

    @GetMapping("/page")
    public Result<IPage<SysLoginLog>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(loginLogApplicationService.page(pageNum, pageSize));
    }
}
