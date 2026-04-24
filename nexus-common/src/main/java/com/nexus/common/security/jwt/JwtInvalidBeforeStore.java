package com.nexus.common.security.jwt;

/**
 * JWT 失效锚点存储接口：
 * 通过 "invalid before" 时间戳实现用户维度的历史 token 失效。
 */
public interface JwtInvalidBeforeStore {

    /**
     * 获取用户对应的 invalid-before 毫秒时间戳。
     *
     * @param userId 用户ID
     * @return 时间戳；若不存在返回 null
     */
    Long getInvalidBeforeMillis(Long userId);
}
