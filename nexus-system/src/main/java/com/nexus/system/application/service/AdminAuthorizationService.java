package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 判断用户是否具备「店长/ADMIN」角色，用于审计类接口。
 */
@Service
public class AdminAuthorizationService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final SysUserShopRoleMapper userShopRoleMapper;
    private final SysRoleMapper roleMapper;

    public AdminAuthorizationService(SysUserShopRoleMapper userShopRoleMapper, SysRoleMapper roleMapper) {
        this.userShopRoleMapper = userShopRoleMapper;
        this.roleMapper = roleMapper;
    }

    public boolean hasAdminRole(Long userId) {
        if (userId == null) {
            return false;
        }
        List<SysUserShopRole> maps = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getDelFlag, 0));
        List<Long> roleIds = maps.stream().map(SysUserShopRole::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getDelFlag, 0));
        return roles.stream().anyMatch(r -> ROLE_ADMIN.equals(r.getRoleCode()));
    }
}
