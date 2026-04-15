package com.nexus.system.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.AuthDtos;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class AuthRedisService {

    private static final Duration TTL = Duration.ofHours(12);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthRedisService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveUserShops(Long userId, List<AuthDtos.ShopItem> shops) {
        try {
            String json = objectMapper.writeValueAsString(shops);
            redisTemplate.opsForValue().set(shopsKey(userId), json, TTL);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "写入店铺缓存失败", e);
        }
    }

    public List<AuthDtos.ShopItem> getUserShops(Long userId) {
        String raw = redisTemplate.opsForValue().get(shopsKey(userId));
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<AuthDtos.ShopItem>>() {
            });
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "读取店铺缓存失败", e);
        }
    }

    public void saveSession(Long userId, SessionState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(sessionKey(userId), json, TTL);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "写入会话缓存失败", e);
        }
    }

    public Optional<SessionState> getSession(Long userId) {
        String raw = redisTemplate.opsForValue().get(sessionKey(userId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, SessionState.class));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "读取会话缓存失败", e);
        }
    }

    public void refreshTtl(Long userId) {
        redisTemplate.expire(shopsKey(userId), TTL);
        redisTemplate.expire(sessionKey(userId), TTL);
    }

    private static String shopsKey(Long uid) {
        return "nexus:auth:uid:" + uid + ":shops";
    }

    private static String sessionKey(Long uid) {
        return "nexus:auth:uid:" + uid + ":session";
    }

    public record SessionState(Long currentShopId, Integer dataScope, List<Long> accessibleShopIds) {
    }
}

