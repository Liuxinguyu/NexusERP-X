package com.nexus.system.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.system.application.dto.SystemOrgDtos;
import com.nexus.system.application.dto.UserInfoDtos;
import com.nexus.system.application.service.SysOrgApplicationService;
import com.nexus.system.application.service.SystemUserInfoService;
import com.nexus.system.domain.model.SysUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/user")
@RequiredArgsConstructor
@Validated
public class SystemUserController {

    private final SystemUserInfoService systemUserInfoService;
    private final SysOrgApplicationService sysOrgApplicationService;

    @GetMapping("/info")
    public Result<UserInfoDtos.UserInfoResponse> info() {
        var p = SecurityUtils.currentPrincipal();
        if (p == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return Result.ok(systemUserInfoService.getCurrentUserInfo(p));
    }

    @GetMapping("/list-by-org")
    public Result<List<SysUser>> listByOrg(@RequestParam(required = false) Long orgId) {
        return Result.ok(sysOrgApplicationService.listUsersForCurrentTenantByOrg(orgId));
    }

    @PutMapping("/change-org")
    @OpLog(module = "用户管理", type = "修改")
    public Result<Void> changeOrg(@Valid @RequestBody SystemOrgDtos.UserChangeOrgRequest req) {
        sysOrgApplicationService.changeUserMainOrgForCurrentTenant(req.getUserId(), req.getNewOrgId());
        return Result.ok();
    }
}

