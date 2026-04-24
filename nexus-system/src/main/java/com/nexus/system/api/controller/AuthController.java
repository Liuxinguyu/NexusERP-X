package com.nexus.system.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.BearerTokenResolver;
import com.nexus.common.security.SecurityUtils;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.jwt.JwtTokenProvider;
import com.nexus.common.utils.HttpRequestUtils;
import com.nexus.system.application.dto.AuthDtos;
import com.nexus.system.application.service.AuthApplicationService;
import com.nexus.system.application.service.OnlineUserRedisService;
import com.nexus.system.application.service.SysLoginLogApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final OnlineUserRedisService onlineUserRedisService;
    private final NexusSecurityProperties nexusSecurityProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysLoginLogApplicationService sysLoginLogApplicationService;

    @PostMapping("/login")
    public Result<AuthDtos.PreAuthLoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest http) {
        return Result.ok(authApplicationService.login(request, http));
    }

    @PostMapping("/confirm-shop")
    public Result<AuthDtos.LoginResponse> confirmShop(@RequestBody AuthDtos.ConfirmShopRequest request,
                                                      HttpServletRequest http) {
        return Result.ok(authApplicationService.confirmShop(request, http));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest http) {
        var p = SecurityUtils.currentPrincipal();
        String raw = BearerTokenResolver.resolveRawToken(http, nexusSecurityProperties);
        String jti = p != null ? p.getJti() : null;
        if (jti == null && raw != null) {
            try {
                jti = jwtTokenProvider.parseTokenClaims(raw).getId();
            } catch (Exception ignored) {
            }
        }
        if (jti != null) {
            onlineUserRedisService.removeToken(jti);
        }
        if (p != null && p.getUsername() != null) {
            sysLoginLogApplicationService.recordSuccess(
                    p.getTenantId(), p.getUsername(),
                    HttpRequestUtils.clientIp(http), http.getHeader("User-Agent"), "用户注销");
        }
        return Result.ok();
    }

    @PostMapping("/refresh")
    public Result<AuthDtos.LoginResponse> refresh(HttpServletRequest http) {
        var p = SecurityUtils.currentPrincipal();
        String oldTokenJti = p != null ? p.getJti() : null;
        return Result.ok(authApplicationService.refreshToken(oldTokenJti, http));
    }

    @GetMapping("/shops")
    public Result<List<AuthDtos.ShopItem>> getShops() {
        var p = SecurityUtils.currentPrincipal();
        if (p == null || p.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return Result.ok(authApplicationService.getShops(p.getUserId()));
    }

    @PutMapping("/switch-shop")
    public Result<AuthDtos.LoginResponse> switchShop(@RequestBody AuthDtos.SwitchShopRequest request,
                                                     HttpServletRequest http) {
        var p = SecurityUtils.currentPrincipal();
        if (p == null || p.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        String oldTokenJti = p.getJti();
        return Result.ok(authApplicationService.switchShop(
                p.getUserId(), p.getTenantId(), p.getUsername(),
                request == null ? null : request.getShopId(),
                oldTokenJti,
                http
        ));
    }
}

