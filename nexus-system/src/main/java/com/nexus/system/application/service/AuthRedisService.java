package com.nexus.system.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.AuthDtos;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthRedisService {

    private static final Duration TTL = Duration.ofHours(12);
    private static final Duration PRE_AUTH_TTL = Duration.ofMinutes(10);
    private static final DefaultRedisScript<String> GET_AND_DELETE_SCRIPT = buildGetAndDeleteScript();

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

    public String savePreAuthSession(PreAuthSession state) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(preAuthKey(token), json, PRE_AUTH_TTL);
            return token;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "写入预登录缓存失败", e);
        }
    }

    public Optional<PreAuthSession> getPreAuthSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(preAuthKey(token));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, PreAuthSession.class));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "读取预登录缓存失败", e);
        }
    }

    public Optional<PreAuthSession> consumePreAuthSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String raw = redisTemplate.execute(GET_AND_DELETE_SCRIPT, List.of(preAuthKey(token)));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, PreAuthSession.class));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "读取预登录缓存失败", e);
        }
    }

    public void clearPreAuthSession(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete(preAuthKey(token));
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

    private static String preAuthKey(String token) {
        return "nexus:auth:pre:" + token;
    }

    private static DefaultRedisScript<String> buildGetAndDeleteScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText("""
                local v = redis.call('GET', KEYS[1])
                if v ~= false then redis.call('DEL', KEYS[1]) end
                return v
                """);
        return script;
    }

    public record SessionState(Long currentShopId, Integer dataScope, List<Long> accessibleShopIds) {
    }

    public record PreAuthSession(Long userId,
                                 Long tenantId,
                                 String username,
                                 Long recommendedShopId,
                                 List<AuthDtos.ShopItem> shops) {
    }
}

