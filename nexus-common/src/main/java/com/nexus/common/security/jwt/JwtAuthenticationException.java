package com.nexus.common.security.jwt;

/**
 * JWT 解析失败、过期或签名校验失败时抛出（继承 RuntimeException，避免与 Spring Security 的 AuthenticationException 混淆）。
 */
public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
