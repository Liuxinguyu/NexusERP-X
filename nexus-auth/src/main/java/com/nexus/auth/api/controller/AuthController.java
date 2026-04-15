package com.nexus.auth.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.common.core.domain.ResultCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * nexus-auth 认证控制器占位：真实认证链路已统一收口到 nexus-system 模块。
 * <p>
 * 网关将 /api/v1/auth/** 路由到 nexus-system（端口 8081），本模块不再暴露登录入口。
 * 仅保留一个健康探针以表明模块可用。
 */
@RestController
@RequestMapping("/api/v1/auth-service")
public class AuthController {

    @GetMapping("/status")
    public Result<String> status() {
        return Result.ok("nexus-auth is running; authentication is handled by nexus-system via gateway");
    }
}
