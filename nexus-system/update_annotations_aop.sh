#!/bin/bash
set -e

# Create Annotations
mkdir -p /Users/liuxingyu/NexusERP-X/nexus-common/src/main/java/com/nexus/common/annotation
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-common/src/main/java/com/nexus/common/annotation/DataScope.java
package com.nexus.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限过滤注解 (组织级别)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名
     */
    String orgAlias() default "d";

    /**
     * 用户表的别名
     */
    String userAlias() default "u";
}
INNER_EOF

cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-common/src/main/java/com/nexus/common/annotation/ShopScope.java
package com.nexus.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限过滤注解 (门店级别)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ShopScope {

    /**
     * 门店表的别名
     */
    String shopAlias() default "s";
}
INNER_EOF

# Create the SecurityUtils and PermissionService (ss)
mkdir -p /Users/liuxingyu/NexusERP-X/nexus-common/src/main/java/com/nexus/common/security/utils
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-common/src/main/java/com/nexus/common/security/utils/SecurityUtils.java
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
INNER_EOF

cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-common/src/main/java/com/nexus/common/security/PermissionService.java
package com.nexus.common.security;

import com.nexus.common.context.GatewayUserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * RuoYi 风格的自定义权限实现，供 @PreAuthorize("@ss.hasPermi('...')") 使用
 */
@Service("ss")
public class PermissionService {

    /** 所有权限标识 */
    private static final String ALL_PERMISSION = "*:*:*";
    
    private final StringRedisTemplate redisTemplate;

    public PermissionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean hasPermi(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }

        Long userId = GatewayUserContext.getUserId();
        if (userId == null) {
            return false;
        }

        String cacheKey = "login:permissions:" + userId;
        Set<String> permissions = redisTemplate.opsForSet().members(cacheKey);

        if (CollectionUtils.isEmpty(permissions)) {
            return false;
        }

        return permissions.contains(ALL_PERMISSION) || permissions.contains(permission);
    }
}
INNER_EOF

