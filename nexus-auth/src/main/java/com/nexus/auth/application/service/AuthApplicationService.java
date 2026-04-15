package com.nexus.auth.application.service;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 认证服务占位：真实认证链路已统一收口到 nexus-system 模块。
 * <p>
 * 网关（nexus-gateway）将 /api/v1/auth/** 路由到 nexus-system（端口 8081），
 * 本模块不再承载登录、注销等认证职责。保留此类仅用于模块内部其它可能的引用，
 * 任何直接调用均会返回明确错误提示。
 */
@Service
public class AuthApplicationService {

    /**
     * 登录：此入口已弃用，请通过网关访问 nexus-system 的 /api/v1/auth/login。
     */
    public Object login(Object req) {
        throw new BusinessException(ResultCode.INTERNAL_ERROR,
                "认证链路已统一收口到 nexus-system 模块，请通过网关（端口 8080）访问 /api/v1/auth/login");
    }
}
