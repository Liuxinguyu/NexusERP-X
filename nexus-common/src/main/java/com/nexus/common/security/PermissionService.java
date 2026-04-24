package com.nexus.common.security;

import com.nexus.common.context.GatewayUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 自定义权限实现，对应 @PreAuthorize("@ss.hasPermi('...')")
 * <p>
 * 放置在 nexus-common 中，确保所有下游服务（nexus-erp、nexus-oa、nexus-wage）
 * 均可通过自动扫描获得此 Bean。
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

        // 构建缓存 key：优先使用 principal 中的 tenantId 和 shopId
        String cacheKey = buildCacheKey(principal, userId);
        Set<String> permissions = stringRedisTemplate.opsForSet().members(cacheKey);
        if (permissions != null && !permissions.isEmpty() && hasPermissions(permissions, normalizedPermission)) {
            return true;
        }

        // 兼容旧格式 key（无 tenant/shop 维度），便于滚动升级期间过渡
        Set<String> legacyPermissions = stringRedisTemplate.opsForSet().members("login:permissions:" + userId);
        return legacyPermissions != null && !legacyPermissions.isEmpty()
                && hasPermissions(legacyPermissions, normalizedPermission);
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

    private String buildCacheKey(NexusPrincipal principal, Long userId) {
        if (principal != null && principal.getTenantId() != null && principal.getShopId() != null) {
            return "login:permissions:" + principal.getTenantId() + ":" + userId + ":" + principal.getShopId();
        }
        return "login:permissions:" + userId;
    }
}
