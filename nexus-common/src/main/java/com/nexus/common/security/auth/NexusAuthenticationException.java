package com.nexus.common.security.auth;

/**
 * JWT 解析失败、过期或签名无效时抛出（与 Spring Security 的 AuthenticationException 区分，避免类名冲突）。
 */
public class NexusAuthenticationException extends RuntimeException {

    public NexusAuthenticationException(String message) {
        super(message);
    }

    public NexusAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
