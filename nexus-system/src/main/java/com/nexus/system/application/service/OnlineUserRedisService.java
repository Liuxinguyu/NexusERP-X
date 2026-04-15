package com.nexus.system.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.jwt.OnlineTokenValidator;
import com.nexus.system.domain.model.SysUser;
import lombok.Data;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 在线用户：login:token:{jwt} -> JSON；online:user:{userId} 记录该用户当前 token 集合（强退用）。
 */
@Service
public class OnlineUserRedisService implements OnlineTokenValidator {

    public static final String TOKEN_PREFIX = "login:token:";
    private static final String USER_TOKENS_PREFIX = "online:user:";

    /** SCAN 每次返回的 key 数量上限，避免一次遍历太多 */
    private static final int SCAN_BATCH_SIZE = 100;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final NexusSecurityProperties securityProperties;

    public OnlineUserRedisService(RedisTemplate<String, String> redisTemplate,
                                  ObjectMapper objectMapper,
                                  NexusSecurityProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
    }

    public void recordLogin(String rawJwt, SysUser user, String ip, String userAgent) throws JsonProcessingException {
        long expSec = securityProperties.getJwt().getExpirationSeconds();
        Duration ttl = Duration.ofSeconds(expSec);
        OnlinePayload p = new OnlinePayload();
        p.setUserId(user.getId());
        p.setTenantId(user.getTenantId());
        p.setUsername(user.getUsername());
        p.setLoginTime(Instant.now().toEpochMilli());
        p.setIp(ip);
        p.setUserAgent(userAgent);
        String json = objectMapper.writeValueAsString(p);
        String tKey = TOKEN_PREFIX + rawJwt;
        redisTemplate.opsForValue().set(tKey, json, ttl);

        String uKey = USER_TOKENS_PREFIX + user.getId();
        redisTemplate.opsForSet().add(uKey, rawJwt);
        redisTemplate.expire(uKey, ttl.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * JWT 校验通过后续期（与 Token 过期一致）。
     */
    public void refreshTokenTtl(String rawJwt) {
        long expSec = securityProperties.getJwt().getExpirationSeconds();
        String tKey = TOKEN_PREFIX + rawJwt;
        Boolean ok = redisTemplate.hasKey(tKey);
        if (Boolean.TRUE.equals(ok)) {
            redisTemplate.expire(tKey, expSec, TimeUnit.SECONDS);
            String json = redisTemplate.opsForValue().get(tKey);
            if (json != null) {
                try {
                    OnlinePayload p = objectMapper.readValue(json, OnlinePayload.class);
                    if (p.getUserId() != null) {
                        redisTemplate.expire(USER_TOKENS_PREFIX + p.getUserId(), expSec, TimeUnit.SECONDS);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void removeToken(String rawJwt) {
        String tKey = TOKEN_PREFIX + rawJwt;
        String json = redisTemplate.opsForValue().get(tKey);
        redisTemplate.delete(tKey);
        if (json != null) {
            try {
                OnlinePayload p = objectMapper.readValue(json, OnlinePayload.class);
                if (p.getUserId() != null) {
                    redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + p.getUserId(), rawJwt);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 检查 token 是否仍在在线会话白名单中（Redis 中是否存在对应 key）。
     * 用于在 JWT 签名校验通过后进一步确认 token 未被注销/强退。
     */
    @Override
    public boolean isTokenOnline(String rawJwt) {
        String tKey = TOKEN_PREFIX + rawJwt;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tKey));
    }

    public void forceLogoutUser(Long userId) {
        String uKey = USER_TOKENS_PREFIX + userId;
        var tokens = redisTemplate.opsForSet().members(uKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String t : tokens) {
            redisTemplate.delete(TOKEN_PREFIX + t);
        }
        redisTemplate.delete(uKey);
    }

    public List<OnlinePayload> listByTenant(long pageNum, long pageSize, Long tenantId) throws JsonProcessingException {
        List<OnlinePayload> all = listAllMatching(tenantId);
        all.sort(Comparator.comparingLong((OnlinePayload o) -> o.getLoginTime() == null ? 0L : o.getLoginTime()).reversed());
        int from = (int) Math.max(0, (pageNum - 1) * pageSize);
        if (from >= all.size()) {
            return Collections.emptyList();
        }
        int to = (int) Math.min(all.size(), from + (int) pageSize);
        return all.subList(from, to);
    }

    public long countByTenant(Long tenantId) throws JsonProcessingException {
        return listAllMatching(tenantId).size();
    }

    /**
     * 使用 SCAN 命令遍历 Redis key，避免阻塞。
     * 与 KEYS 命令不同，SCAN 是增量遍历，不会阻塞 Redis。
     */
    private List<OnlinePayload> listAllMatching(Long tenantId) throws JsonProcessingException {
        List<OnlinePayload> all = new ArrayList<>();
        ScanOptions opts = ScanOptions.scanOptions()
                .match(TOKEN_PREFIX + "*")
                .count(SCAN_BATCH_SIZE)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(opts)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) {
                    continue;
                }
                try {
                    OnlinePayload p = objectMapper.readValue(json, OnlinePayload.class);
                    if (tenantId == null || (p.getTenantId() != null && p.getTenantId().equals(tenantId))) {
                        all.add(p);
                    }
                } catch (Exception e) {
                    // 忽略解析错误，继续处理其他 key
                }
            }
        }
        return all;
    }

    @Data
    public static class OnlinePayload {
        private Long userId;
        private Long tenantId;
        private String username;
        private Long loginTime;
        private String ip;
        private String userAgent;
    }
}
