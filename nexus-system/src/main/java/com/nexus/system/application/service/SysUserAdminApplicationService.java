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
