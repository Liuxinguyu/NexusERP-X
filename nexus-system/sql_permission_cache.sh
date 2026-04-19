#!/bin/bash
set -e

# Generate Custom Mapper to fetch Permissions logic
mkdir -p /Users/liuxingyu/NexusERP-X/nexus-system/src/main/resources/mapper/system/
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/resources/mapper/system/SysMenuMapper.xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.nexus.system.infrastructure.mapper.SysMenuMapper">

    <select id="selectMenuPermsByUserId" resultType="String">
        SELECT distinct m.perms
        FROM sys_menu m
        LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id
        LEFT JOIN sys_user_shop_role ur ON rm.role_id = ur.role_id
        LEFT JOIN sys_role ro ON ur.role_id = ro.id
        WHERE ur.user_id = #{userId}
          AND m.status = 1
          AND ro.status = 1
          AND m.del_flag = 0
          AND ro.del_flag = 0
    </select>
</mapper>
INNER_EOF

cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/infrastructure/mapper/SysMenuMapper.java
package com.nexus.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.system.domain.model.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户 ID 查询功能权限标识
     *
     * @param userId 用户 ID
     * @return 权限列表
     */
    List<String> selectMenuPermsByUserId(@Param("userId") Long userId);
}
INNER_EOF

# Add Cache Builder in Login logic OR Auth Service
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/PermissionCacheService.java
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
INNER_EOF

