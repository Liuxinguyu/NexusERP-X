package com.nexus.system.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysMenuApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysMenuController {

    private final SysMenuApplicationService menuApplicationService;

    @GetMapping("/tree")
    public Result<List<SystemAdminDtos.MenuTreeNode>> tree() {
        return Result.ok(menuApplicationService.menuTree());
    }
}
