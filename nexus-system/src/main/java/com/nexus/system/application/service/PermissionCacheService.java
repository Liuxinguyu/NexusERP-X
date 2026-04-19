package com.nexus.system.application.service;

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

        List<String> perms = sysMenuMapper.selectMenuPermsByUserId(userId);
        
        // Handle admin separately if needed or just cache what is fetched
        Set<String> permsSet = perms.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        String cacheKey = "login:permissions:" + userId;
        redisTemplate.delete(cacheKey);
        
        if (!permsSet.isEmpty()) {
            redisTemplate.opsForSet().add(cacheKey, permsSet.toArray(new String[0]));
        }
    }
    
    public void clearUserPermissions(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete("login:permissions:" + userId);
    }
}
