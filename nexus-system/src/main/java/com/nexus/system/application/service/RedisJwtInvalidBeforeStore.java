package com.nexus.system.application.service;

import com.nexus.common.security.jwt.JwtInvalidBeforeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RedisJwtInvalidBeforeStore implements JwtInvalidBeforeStore {

    private static final String KEY_PREFIX = "jwt:invalid_before:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Long getInvalidBeforeMillis(Long userId) {
        if (userId == null) {
            return null;
        }
        String raw = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
