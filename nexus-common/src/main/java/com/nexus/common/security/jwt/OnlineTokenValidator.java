package com.nexus.common.security.jwt;

/**
 * 在线 Token 校验接口：JWT 签名合法后进一步检查 token 是否仍被系统视为有效（未被注销/强退）。
 * <p>
 * 实现方通常基于 Redis 在线会话白名单，验证 token 对应的在线记录是否存在。
 * 当没有 bean 注入时（如网关模块），{@link JwtAuthenticationFilter} 会跳过此校验，
 * 仅依赖 JWT 签名与过期时间。
 */
public interface OnlineTokenValidator {

    /**
     * 检查给定 token 的 jti 是否仍然在线（有效）。
     *
     * @param jti JWT 标准字段 jti（Token ID）
     * @return true 表示 token 在线有效，false 表示已被注销/强退
     */
    boolean isTokenOnline(String jti);
}
