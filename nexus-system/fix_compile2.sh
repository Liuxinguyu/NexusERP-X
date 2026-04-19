#!/bin/bash
set -e

# Replace listOptions in SysRoleApplicationService
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysRoleApplicationService.java
package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysRoleMenu;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMenuMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysRoleApplicationService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;

    public SysRoleApplicationService(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper, SysUserShopRoleMapper userShopRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userShopRoleMapper = userShopRoleMapper;
    }

    public IPage<SysRole> page(long current, long size, String roleName, String roleCode) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getDelFlag, 0)
                .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                .like(StringUtils.hasText(roleCode), SysRole::getRoleCode, roleCode)
                .orderByDesc(SysRole::getId);
        return roleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<SystemAdminDtos.RoleOption> listOptions() {
        Long tenantId = requireTenantId();
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getDelFlag, 0)
                        .orderByDesc(SysRole::getId))
                .stream()
                .map(r -> {
                    SystemAdminDtos.RoleOption o = new SystemAdminDtos.RoleOption();
                    o.setId(r.getId()); 
                    o.setRoleName(r.getRoleName()); 
                    o.setRoleCode(r.getRoleCode());
                    return o;
                }).collect(Collectors.toList());
    }

    public SysRole getById(Long id) {
        Long tenantId = requireTenantId();
        SysRole role = roleMapper.selectById(id);
        if (role == null || !Objects.equals(role.getTenantId(), tenantId) || role.getDelFlag() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.RoleCreateRequest req) {
        Long tenantId = requireTenantId();
        boolean exists = roleMapper.exists(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleCode, req.getRoleCode())
                .eq(SysRole::getDelFlag, 0));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setRoleName(req.getRoleName());
        role.setRoleCode(req.getRoleCode());
        role.setDataScope(req.getDataScope() != null ? req.getDataScope() : 1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setCreateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        role.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        role.setDelFlag(0);

        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.RoleUpdateRequest req) {
        Long tenantId = requireTenantId();
        SysRole role = getById(id);
        
        if (!Objects.equals(role.getRoleCode(), req.getRoleCode())) {
            boolean exists = roleMapper.exists(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getTenantId, tenantId)
                    .eq(SysRole::getRoleCode, req.getRoleCode())
                    .eq(SysRole::getDelFlag, 0));
            if (exists) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在");
            }
        }

        role.setRoleName(req.getRoleName());
        role.setRoleCode(req.getRoleCode());
        if (req.getDataScope() != null) role.setDataScope(req.getDataScope());
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());

        roleMapper.updateById(role);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getById(id);
        
        boolean hasUsers = userShopRoleMapper.exists(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getRoleId, id));
        if (hasUsers) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色已被分配，无法删除");
        }
        
        role.setDelFlag(1);
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        roleMapper.updateById(role);
    }

    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, SystemAdminDtos.RoleMenuAssignRequest req) {
        getById(roleId); // validate

        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));

        if (req.getMenuIds() != null && !req.getMenuIds().isEmpty()) {
            for (Long menuId : req.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                rm.setCreateTime(LocalDateTime.now());
                roleMenuMapper.insert(rm);
            }
        }
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
INNER_EOF

# Fix User listUserShopRoles return type logic to match UserShopRoleItem shape
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysUserAdminApplicationService.java
package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysUserAdminApplicationService {

    private final SysUserMapper userMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserAdminApplicationService(SysUserMapper userMapper, SysUserShopRoleMapper userShopRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userShopRoleMapper = userShopRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public IPage<SysUser> page(long current, long size, String username, String nickname, Long orgId) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDelFlag, 0)
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(nickname), SysUser::getRealName, nickname)
                .orderByDesc(SysUser::getId);

        IPage<SysUser> pageResult = userMapper.selectPage(new Page<>(current, size), wrapper);
        pageResult.getRecords().forEach(u -> u.setPasswordHash(null));
        return pageResult;
    }

    public SysUser getById(Long id) {
        Long tenantId = requireTenantId();
        SysUser user = userMapper.selectById(id);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId) || user.getDelFlag() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.UserCreateRequest req) {
        Long tenantId = requireTenantId();
        boolean exists = userMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())
                .eq(SysUser::getDelFlag, 0));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(req.getUsername());
        user.setRealName(req.getRealName());
        if (StringUtils.hasText(req.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        } else {
            user.setPasswordHash(passwordEncoder.encode("123456")); 
        }
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setCreateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        user.setDelFlag(0);

        userMapper.insert(user);
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.UserUpdateRequest req) {
        SysUser user = getById(id);

        user.setRealName(req.getRealName());
        if (req.getStatus() != null) user.setStatus(req.getStatus());

        if (StringUtils.hasText(req.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }

        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());

        userMapper.updateById(user);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getById(id);
        if (user.getId().equals(com.nexus.common.context.GatewayUserContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录的用户");
        }
        user.setDelFlag(1);
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        userMapper.updateById(user);
        
        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, id));
    }

    public List<SystemAdminDtos.UserShopRoleItem> listUserShopRoles(Long userId) {
        getById(userId); 
        return userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId))
                .stream()
                .map(r -> {
                    SystemAdminDtos.UserShopRoleItem i = new SystemAdminDtos.UserShopRoleItem();
                    i.setShopId(r.getShopId());
                    i.setRoleId(r.getRoleId());
                    return i;
                }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserShopRoles(Long userId, SystemAdminDtos.UserShopRoleSaveRequest req) {
        getById(userId);

        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId));

        if (req.getItems() != null) {
            for (SystemAdminDtos.UserShopRoleItem mapping : req.getItems()) {
                if (mapping.getShopId() != null && mapping.getRoleId() != null) {
                    SysUserShopRole r = new SysUserShopRole();
                    r.setUserId(userId);
                    r.setShopId(mapping.getShopId());
                    r.setRoleId(mapping.getRoleId());
                    r.setCreateTime(LocalDateTime.now());
                    userShopRoleMapper.insert(r);
                }
            }
        }
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
INNER_EOF

# Fix UserAdminController endpoint signature
sed -i '' 's/List<SystemAdminDtos.UserShopRoleResponse>/List<SystemAdminDtos.UserShopRoleItem>/g' /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SysUserAdminController.java

