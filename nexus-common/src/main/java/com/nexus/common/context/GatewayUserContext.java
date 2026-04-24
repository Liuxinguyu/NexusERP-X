package com.nexus.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 网关在通过 JWT 校验后写入的 X-User-Id / X-Username，供无 SecurityPrincipal 或填充审计字段时使用。
 */
public final class GatewayUserContext {

    private static final ThreadLocal<Long> USER_ID = new TransmittableThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new TransmittableThreadLocal<>();

    private GatewayUserContext() {
    }

    public static void setUserId(Long userId) {
        if (userId == null) {
            USER_ID.remove();
        } else {
            USER_ID.set(userId);
        }
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUsername(String username) {
        if (username == null) {
            USERNAME.remove();
        } else {
            USERNAME.set(username);
        }
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }

    public static void remove() {
        clear();
    }
}
