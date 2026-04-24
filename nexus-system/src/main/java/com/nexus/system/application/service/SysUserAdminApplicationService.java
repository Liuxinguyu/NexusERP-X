package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysShop;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysShopMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysUserAdminApplicationService {

    private final SysUserMapper userMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;
    private final SysShopMapper shopMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final NexusSecurityProperties securityProperties;

    private static final String JWT_INVALID_BEFORE_KEY_PREFIX = "jwt:invalid_before:";

    public SysUserAdminApplicationService(SysUserMapper userMapper,
                                          SysUserShopRoleMapper userShopRoleMapper,
                                          SysShopMapper shopMapper,
                                          SysRoleMapper roleMapper,
                                          PasswordEncoder passwordEncoder,
                                          StringRedisTemplate redisTemplate,
                                          NexusSecurityProperties securityProperties) {
        this.userMapper = userMapper;
        this.userShopRoleMapper = userShopRoleMapper;
        this.shopMapper = shopMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    public IPage<SysUser> page(long current, long size, String username, String nickname, Long orgId) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDelFlag, 0)
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(nickname), SysUser::getRealName, nickname)
                .eq(orgId != null, SysUser::getMainOrgId, orgId)
                .orderByDesc(SysUser::getId);

        IPage<SysUser> pageResult = userMapper.selectPage(new Page<>(current, size), wrapper);
        pageResult.getRecords().forEach(u -> u.setPasswordHash(null));
        return pageResult;
    }

    public SysUser getById(Long id) {
        Long tenantId = requireTenantId();
        SysUser user = userMapper.selectById(id);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId) || Objects.equals(user.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.UserCreateRequest req) {
        Long tenantId = requireTenantId();
        boolean exists = userMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
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
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user.setMainShopId(req.getMainShopId());
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
        boolean credentialOrStatusChanged = false;

        user.setRealName(req.getRealName());
        if (req.getMainShopId() != null) {
            user.setMainShopId(req.getMainShopId());
        }
        if (req.getStatus() != null && !Objects.equals(req.getStatus(), user.getStatus())) {
            user.setStatus(req.getStatus());
            credentialOrStatusChanged = true;
        }

        // 前端传空字符串或 null 时，不允许覆盖原有密码
        if (StringUtils.hasText(req.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            credentialOrStatusChanged = true;
        }

        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());

        userMapper.updateById(user);

        if (credentialOrStatusChanged) {
            markTokenInvalidBefore(user.getId());
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getById(id);
        Long userId = user.getId();
        if (user.getId().equals(com.nexus.common.context.GatewayUserContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录的用户");
        }

        // 逻辑删除前先污染唯一键，释放 username 的唯一索引占用
        user.setUsername(user.getUsername() + "_del_" + user.getId());
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        userMapper.updateById(user);
        userMapper.deleteById(userId);
        
        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId));
        markTokenInvalidBefore(userId);
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = getById(id);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        userMapper.updateById(user);
        if (!Objects.equals(status, 1)) {
            markTokenInvalidBefore(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能为空");
        }
        SysUser user = getById(id);
        if (user.getId().equals(com.nexus.common.context.GatewayUserContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能重置当前登录用户的密码");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        userMapper.updateById(user);
        markTokenInvalidBefore(id);
    }

    public List<SystemAdminDtos.UserShopRoleItem> listUserShopRoles(Long userId) {
        getById(userId); 
        return userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getDelFlag, 0))
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
        Long tenantId = requireTenantId();
        getById(userId);

        Long currentUserId = com.nexus.common.context.GatewayUserContext.getUserId();
        if (Objects.equals(userId, currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能修改自己的角色分配");
        }

        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId));

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            Set<Long> shopIds = new HashSet<>();
            Set<Long> roleIds = new HashSet<>();
            for (SystemAdminDtos.UserShopRoleItem item : req.getItems()) {
                if (item.getShopId() != null) shopIds.add(item.getShopId());
                if (item.getRoleId() != null) roleIds.add(item.getRoleId());
            }

            if (!shopIds.isEmpty()) {
                long validShopCount = shopMapper.selectCount(new LambdaQueryWrapper<SysShop>()
                        .eq(SysShop::getTenantId, tenantId)
                        .eq(SysShop::getDelFlag, 0)
                        .in(SysShop::getId, shopIds));
                if (validShopCount != shopIds.size()) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "包含不属于当前租户的店铺");
                }
            }
            if (!roleIds.isEmpty()) {
                long validRoleCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getDelFlag, 0)
                        .in(SysRole::getId, roleIds));
                if (validRoleCount != roleIds.size()) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "包含不属于当前租户的角色");
                }
            }

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

        clearPermissionCache(userId);
        markTokenInvalidBefore(userId);
    }

    private void clearPermissionCache(Long userId) {
        if (userId == null) return;
        redisTemplate.delete("login:permissions:" + userId);

        String pattern = "login:permissions:*:" + userId + ":*";
        try (var cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern).count(200).build())) {
            java.util.List<String> batch = new java.util.ArrayList<>();
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

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }

    private void markTokenInvalidBefore(Long userId) {
        if (userId == null) {
            return;
        }
        long invalidBefore = System.currentTimeMillis();
        long ttlSeconds = securityProperties.getJwt().getExpirationSeconds();
        redisTemplate.opsForValue().set(
                JWT_INVALID_BEFORE_KEY_PREFIX + userId,
                String.valueOf(invalidBefore),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }
}
