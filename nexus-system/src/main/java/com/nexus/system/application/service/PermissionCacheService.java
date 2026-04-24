package com.nexus.system.application.service;

import com.nexus.common.context.TenantContext;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final SysMenuMapper sysMenuMapper;
    private final StringRedisTemplate redisTemplate;

    public void cacheUserPermissions(Long userId) {
        if (userId == null) {
            return;
        }

        List<String> perms = sysMenuMapper.selectMenuPermsByUserId(userId, TenantContext.getTenantId(), null);

        // Handle admin separately if needed or just cache what is fetched
        Set<String> permsSet = perms.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        String legacyKey = "login:permissions:" + userId;
        redisTemplate.delete(legacyKey);

        if (!permsSet.isEmpty()) {
            redisTemplate.opsForSet().add(legacyKey, permsSet.toArray(new String[0]));
        }
    }

    /**
     * 清除用户的所有权限缓存，包含新格式(tenantId:userId:shopId)和旧格式(userId)。
     * 使用 SCAN 替代 KEYS 避免生产环境阻塞 Redis。
     */
    public void clearUserPermissions(Long userId) {
        if (userId == null) {
            return;
        }
        // 删除旧格式 key
        redisTemplate.delete("login:permissions:" + userId);

        // 使用 SCAN 删除新格式 key: login:permissions:*:{userId}:*
        String pattern = "login:permissions:*:" + userId + ":*";
        try (var cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern).count(200).build())) {
            List<String> batch = new java.util.ArrayList<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 100) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
        }
    }
}
