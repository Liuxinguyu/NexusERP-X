package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysMenu;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysRoleMenu;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMenuMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import com.nexus.common.security.config.NexusSecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SysRoleApplicationService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;
    private final PermissionCacheService permissionCacheService;
    private final StringRedisTemplate redisTemplate;
    private final NexusSecurityProperties securityProperties;

    private static final String JWT_INVALID_BEFORE_KEY_PREFIX = "jwt:invalid_before:";

    public SysRoleApplicationService(SysRoleMapper roleMapper,
                                     SysRoleMenuMapper roleMenuMapper,
                                     SysMenuMapper menuMapper,
                                     SysUserShopRoleMapper userShopRoleMapper,
                                     PermissionCacheService permissionCacheService,
                                     StringRedisTemplate redisTemplate,
                                     NexusSecurityProperties securityProperties) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.userShopRoleMapper = userShopRoleMapper;
        this.permissionCacheService = permissionCacheService;
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
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
        if (role == null || !Objects.equals(role.getTenantId(), tenantId) || Objects.equals(role.getDelFlag(), 1)) {
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

        List<Long> affectedUserIds = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                        .eq(SysUserShopRole::getRoleId, id))
                .stream()
                .map(SysUserShopRole::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
        userShopRoleMapper.delete(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getRoleId, id));

        role.setDelFlag(1);
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        roleMapper.updateById(role);

        for (Long userId : affectedUserIds) {
            permissionCacheService.clearUserPermissions(userId);
            markTokenInvalidBefore(userId);
        }
    }

    public List<Long> listMenuIds(Long roleId) {
        getById(roleId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, SystemAdminDtos.RoleMenuAssignRequest req) {
        Long tenantId = requireTenantId();
        getById(roleId);

        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));

        if (req.getMenuIds() != null && !req.getMenuIds().isEmpty()) {
            Set<Long> menuIdSet = new HashSet<>(req.getMenuIds());
            long validMenuCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getTenantId, tenantId)
                    .eq(SysMenu::getDelFlag, 0)
                    .in(SysMenu::getId, menuIdSet));
            if (validMenuCount != menuIdSet.size()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "包含不属于当前租户的菜单");
            }

            for (Long menuId : req.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                rm.setCreateTime(LocalDateTime.now());
                roleMenuMapper.insert(rm);
            }
        }

        Set<Long> affectedUserIds = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                        .eq(SysUserShopRole::getRoleId, roleId))
                .stream()
                .map(SysUserShopRole::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long userId : affectedUserIds) {
            permissionCacheService.clearUserPermissions(userId);
            markTokenInvalidBefore(userId);
        }
    }

    private void markTokenInvalidBefore(Long userId) {
        if (userId == null) return;
        long invalidBefore = System.currentTimeMillis();
        long ttlSeconds = securityProperties.getJwt().getExpirationSeconds();
        redisTemplate.opsForValue().set(
                JWT_INVALID_BEFORE_KEY_PREFIX + userId,
                String.valueOf(invalidBefore),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
