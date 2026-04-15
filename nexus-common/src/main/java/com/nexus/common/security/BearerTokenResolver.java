package com.nexus.common.security;

import com.nexus.common.security.config.NexusSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * 从 Authorization 头解析 Bearer Token 原始字符串。
 */
public final class BearerTokenResolver {

    private BearerTokenResolver() {
    }

    public static String resolveRawToken(HttpServletRequest request, NexusSecurityProperties securityProperties) {
        if (request == null || securityProperties == null) {
            return null;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        NexusSecurityProperties.Jwt jwt = securityProperties.getJwt();
        if (!StringUtils.hasText(header) || jwt == null || !StringUtils.hasText(jwt.getTokenPrefix())) {
            return null;
        }
        if (!header.startsWith(jwt.getTokenPrefix())) {
            return null;
        }
        return header.substring(jwt.getTokenPrefix().length()).trim();
    }
}
