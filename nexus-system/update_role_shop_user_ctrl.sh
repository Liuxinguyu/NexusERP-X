#!/bin/bash
set -e

# Update SysRoleApplicationService
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
                .orderByAsc(SysRole::getSort)
                .orderByDesc(SysRole::getId);
        return roleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<SystemAdminDtos.RoleOption> listOptions() {
        Long tenantId = requireTenantId();
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getStatus, 1)
                        .eq(SysRole::getDelFlag, 0)
                        .select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode)
                        .orderByAsc(SysRole::getSort))
                .stream()
                .map(r -> new SystemAdminDtos.RoleOption(r.getId(), r.getRoleName(), r.getRoleCode()))
                .collect(Collectors.toList());
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
        role.setSort(req.getSort() != null ? req.getSort() : 0);
        role.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        role.setRemark(req.getRemark());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setCreateBy(TenantContext.getUserId());
        role.setUpdateBy(TenantContext.getUserId());
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
        if (req.getSort() != null) role.setSort(req.getSort());
        if (req.getStatus() != null) role.setStatus(req.getStatus());
        role.setRemark(req.getRemark());
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(TenantContext.getUserId());

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
        role.setUpdateBy(TenantContext.getUserId());
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

# Update SysShopApplicationService
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysShop;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysShopMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysShopApplicationService {

    private final SysShopMapper shopMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;

    public SysShopApplicationService(SysShopMapper shopMapper, SysUserShopRoleMapper userShopRoleMapper) {
        this.shopMapper = shopMapper;
        this.userShopRoleMapper = userShopRoleMapper;
    }

    public IPage<SysShop> page(long current, long size, String shopName, String shopCode) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysShop> wrapper = new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getTenantId, tenantId)
                .eq(SysShop::getDelFlag, 0)
                .like(StringUtils.hasText(shopName), SysShop::getShopName, shopName)
                .like(StringUtils.hasText(shopCode), SysShop::getShopCode, shopCode)
                .orderByDesc(SysShop::getId);
        return shopMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<SystemAdminDtos.ShopOption> listOptions() {
        Long tenantId = requireTenantId();
        return shopMapper.selectList(new LambdaQueryWrapper<SysShop>()
                        .eq(SysShop::getTenantId, tenantId)
                        .eq(SysShop::getStatus, 1)
                        .eq(SysShop::getDelFlag, 0)
                        .select(SysShop::getId, SysShop::getShopName, SysShop::getShopCode)
                        .orderByDesc(SysShop::getId))
                .stream()
                .map(s -> new SystemAdminDtos.ShopOption(s.getId(), s.getShopName(), s.getShopCode()))
                .collect(Collectors.toList());
    }

    public SysShop getById(Long id) {
        Long tenantId = requireTenantId();
        SysShop shop = shopMapper.selectById(id);
        if (shop == null || !Objects.equals(shop.getTenantId(), tenantId) || shop.getDelFlag() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        return shop;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.ShopCreateRequest req) {
        Long tenantId = requireTenantId();
        boolean exists = shopMapper.exists(new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getTenantId, tenantId)
                .eq(SysShop::getShopCode, req.getShopCode())
                .eq(SysShop::getDelFlag, 0));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "店铺编码已存在");
        }

        SysShop shop = new SysShop();
        shop.setTenantId(tenantId);
        shop.setShopName(req.getShopName());
        shop.setShopCode(req.getShopCode());
        shop.setAddress(req.getAddress());
        shop.setContactName(req.getContactName());
        shop.setContactPhone(req.getContactPhone());
        shop.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        shop.setCreateTime(LocalDateTime.now());
        shop.setUpdateTime(LocalDateTime.now());
        shop.setCreateBy(TenantContext.getUserId());
        shop.setUpdateBy(TenantContext.getUserId());
        shop.setDelFlag(0);

        shopMapper.insert(shop);
        return shop.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.ShopUpdateRequest req) {
        Long tenantId = requireTenantId();
        SysShop shop = getById(id);

        if (!Objects.equals(shop.getShopCode(), req.getShopCode())) {
            boolean exists = shopMapper.exists(new LambdaQueryWrapper<SysShop>()
                    .eq(SysShop::getTenantId, tenantId)
                    .eq(SysShop::getShopCode, req.getShopCode())
                    .eq(SysShop::getDelFlag, 0));
            if (exists) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "店铺编码已存在");
            }
        }

        shop.setShopName(req.getShopName());
        shop.setShopCode(req.getShopCode());
        shop.setAddress(req.getAddress());
        shop.setContactName(req.getContactName());
        shop.setContactPhone(req.getContactPhone());
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(TenantContext.getUserId());

        shopMapper.updateById(shop);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysShop shop = getById(id);
        shop.setStatus(status);
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(TenantContext.getUserId());
        shopMapper.updateById(shop);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysShop shop = getById(id);
        
        boolean hasUsers = userShopRoleMapper.exists(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getShopId, id));
        if (hasUsers) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该店铺已有用户挂载，无法删除");
        }
        
        shop.setDelFlag(1);
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(TenantContext.getUserId());
        shopMapper.updateById(shop);
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

# Update SysUserAdminApplicationService
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                .eq(orgId != null, SysUser::getOrgId, orgId)
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
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
        user.setNickname(req.getNickname());
        if (StringUtils.hasText(req.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        } else {
            user.setPasswordHash(passwordEncoder.encode("123456")); 
        }
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setOrgId(req.getOrgId());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setCreateBy(TenantContext.getUserId());
        user.setUpdateBy(TenantContext.getUserId());
        user.setDelFlag(0);

        userMapper.insert(user);
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.UserUpdateRequest req) {
        SysUser user = getById(id);

        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setOrgId(req.getOrgId());
        if (req.getStatus() != null) user.setStatus(req.getStatus());

        if (StringUtils.hasText(req.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }

        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(TenantContext.getUserId());

        userMapper.updateById(user);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getById(id);
        if (user.getId().equals(TenantContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录的用户");
        }
        user.setDelFlag(1);
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(TenantContext.getUserId());
        userMapper.updateById(user);
        
        // cascade virtual removal of assignments
        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, id));
    }

    public List<SystemAdminDtos.UserShopRoleResponse> listUserShopRoles(Long userId) {
        getById(userId); 
        List<SysUserShopRole> mappings = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId));

        Map<Long, List<Long>> shopRoleMap = new HashMap<>();
        for (SysUserShopRole mapping : mappings) {
            shopRoleMap.computeIfAbsent(mapping.getShopId(), k -> new ArrayList<>()).add(mapping.getRoleId());
        }

        return shopRoleMap.entrySet().stream()
                .map(e -> new SystemAdminDtos.UserShopRoleResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserShopRoles(Long userId, SystemAdminDtos.UserShopRoleSaveRequest req) {
        getById(userId);

        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId));

        if (req.getMappings() != null) {
            for (SystemAdminDtos.UserShopRoleResponse mapping : req.getMappings()) {
                if (mapping.getShopId() != null && mapping.getRoleIds() != null) {
                    for (Long roleId : mapping.getRoleIds()) {
                        SysUserShopRole r = new SysUserShopRole();
                        r.setUserId(userId);
                        r.setShopId(mapping.getShopId());
                        r.setRoleId(roleId);
                        r.setCreateTime(LocalDateTime.now());
                        userShopRoleMapper.insert(r);
                    }
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

