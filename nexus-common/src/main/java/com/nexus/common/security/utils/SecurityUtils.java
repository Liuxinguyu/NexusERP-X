package com.nexus.common.security.utils;

import com.nexus.common.context.GatewayUserContext;
import org.springframework.stereotype.Component;

/**
 * 获取当前登录用户的基本信息
 */
@Component
public class SecurityUtils {
    public static Long getUserId() {
        return GatewayUserContext.getUserId();
    }
}
