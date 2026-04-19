package com.nexus.system.application.service;

import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.security.SecurityUtils;
import com.nexus.common.security.NexusPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 自定义权限实现，对应 @PreAuthorize("@ss.hasPermi('...')")
 */
@Service("ss")
@RequiredArgsConstructor
public class PermissionService {

    /** 所有权限标识 */
    private static final String ALL_PERMISSION = "*:*:*";

    private final StringRedisTemplate stringRedisTemplate;

    public boolean hasPermi(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }

        String normalizedPermission = StringUtils.trimWhitespace(permission);
        NexusPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal != null && hasAuthorities(principal, normalizedPermission)) {
            return true;
        }

        Long userId = principal != null ? principal.getUserId() : GatewayUserContext.getUserId();
        if (userId == null) {
            return false;
        }

        Set<String> permissions = stringRedisTemplate.opsForSet().members("login:permissions:" + userId);
        return permissions != null && !permissions.isEmpty() && hasPermissions(permissions, normalizedPermission);
    }

    public boolean hasAnyPermi(String permissions) {
        if (!StringUtils.hasText(permissions)) {
            return false;
        }

        for (String permission : permissions.split(",")) {
            if (hasPermi(permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAuthorities(NexusPrincipal principal, String permission) {
        for (GrantedAuthority authority : principal.getAuthorities()) {
            if (authority == null || !StringUtils.hasText(authority.getAuthority())) {
                continue;
            }
            String value = authority.getAuthority().trim();
            if (ALL_PERMISSION.equals(value) || permission.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermissions(Set<String> permissions, String permission) {
        return permissions.contains(ALL_PERMISSION) || permissions.contains(permission);
    }
}

