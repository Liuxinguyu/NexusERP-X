package com.nexus.system.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.jwt.JwtTokenProvider;
import com.nexus.common.security.jwt.OnlineTokenValidator;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import lombok.Data;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 在线用户：login:token:{jti} -> JSON；online:user:{userId} 记录该用户当前 token jti 集合（强退用）。
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
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper userMapper;

    public OnlineUserRedisService(RedisTemplate<String, String> redisTemplate,
                                  ObjectMapper objectMapper,
                                  NexusSecurityProperties securityProperties,
                                  JwtTokenProvider jwtTokenProvider,
                                  SysUserMapper userMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    public void recordLogin(String rawJwt, SysUser user, String ip, String userAgent) throws JsonProcessingException {
        String jti = extractJti(rawJwt);
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("JWT 缺少 jti，无法记录在线会话");
        }
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
        String tKey = TOKEN_PREFIX + jti;
        redisTemplate.opsForValue().set(tKey, json, ttl);

        String uKey = USER_TOKENS_PREFIX + user.getId();
        redisTemplate.opsForSet().add(uKey, jti);
        redisTemplate.expire(uKey, ttl.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * JWT 校验通过后续期（与 Token 过期一致）。
     */
    public void refreshTokenTtl(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long expSec = securityProperties.getJwt().getExpirationSeconds();
        String tKey = TOKEN_PREFIX + jti;
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

    public void removeToken(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        String tKey = TOKEN_PREFIX + jti;
        String json = redisTemplate.opsForValue().get(tKey);
        redisTemplate.delete(tKey);
        if (json != null) {
            try {
                OnlinePayload p = objectMapper.readValue(json, OnlinePayload.class);
                if (p.getUserId() != null) {
                    redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + p.getUserId(), jti);
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
    public boolean isTokenOnline(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        String tKey = TOKEN_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tKey));
    }

    public void forceLogoutUser(Long userId) {
        Long currentTenant = TenantContext.getTenantId();
        if (currentTenant == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID无效");
        }
        SysUser target = userMapper.selectById(userId);
        if (target == null || !Objects.equals(target.getTenantId(), currentTenant)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        String uKey = USER_TOKENS_PREFIX + userId;
        var tokens = redisTemplate.opsForSet().members(uKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String jti : tokens) {
            redisTemplate.delete(TOKEN_PREFIX + jti);
        }
        redisTemplate.delete(uKey);
    }

    public List<OnlinePayload> listByTenant(long pageNum, long pageSize, Long tenantId) throws JsonProcessingException {
        return listByTenant(pageNum, pageSize, tenantId, null, null);
    }

    public List<OnlinePayload> listByTenant(long pageNum, long pageSize, Long tenantId, String username, String ip) throws JsonProcessingException {
        List<OnlinePayload> all = listAllMatching(tenantId, username, ip);
        all.sort(Comparator.comparingLong((OnlinePayload o) -> o.getLoginTime() == null ? 0L : o.getLoginTime()).reversed());
        int from = (int) Math.max(0, (pageNum - 1) * pageSize);
        if (from >= all.size()) {
            return Collections.emptyList();
        }
        int to = (int) Math.min(all.size(), from + (int) pageSize);
        return all.subList(from, to);
    }

    public long countByTenant(Long tenantId) throws JsonProcessingException {
        return countByTenant(tenantId, null, null);
    }

    public long countByTenant(Long tenantId, String username, String ip) throws JsonProcessingException {
        return listAllMatching(tenantId, username, ip).size();
    }

    /**
     * 使用 SCAN 命令遍历 Redis key，避免阻塞。
     * 与 KEYS 命令不同，SCAN 是增量遍历，不会阻塞 Redis。
     */
    private List<OnlinePayload> listAllMatching(Long tenantId, String username, String ip) throws JsonProcessingException {
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
                    if (tenantId != null && (p.getTenantId() == null || !p.getTenantId().equals(tenantId))) {
                        continue;
                    }
                    if (StringUtils.hasText(username)) {
                        String u = p.getUsername();
                        if (!StringUtils.hasText(u) || !u.toLowerCase().contains(username.trim().toLowerCase())) {
                            continue;
                        }
                    }
                    if (StringUtils.hasText(ip)) {
                        String pip = p.getIp();
                        if (!StringUtils.hasText(pip) || !pip.contains(ip.trim())) {
                            continue;
                        }
                    }
                    {
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

    private String extractJti(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            return null;
        }
        try {
            return jwtTokenProvider.parseTokenClaims(rawJwt).getId();
        } catch (Exception e) {
            return null;
        }
    }

}
