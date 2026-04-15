package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysShop;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysShopMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserAdminApplicationService {

    private final SysUserMapper userMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;
    private final SysShopMapper shopMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuthorizationService adminAuthorizationService;

    /**
     * 分页查询用户；数据范围由 {@link com.nexus.common.mybatis.datapermission.DataScopeInterceptor} 按 sys_user 主表改写。
     */
    public IPage<SysUser> page(long current, long size, String username) {
        requireAdmin();
        Long tenantId = requireTenantId();
        Page<SysUser> p = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getTenantId, tenantId);
        w.eq(SysUser::getDelFlag, 0);
        w.like(StringUtils.hasText(username), SysUser::getUsername, username);
        w.orderByDesc(SysUser::getId);
        return userMapper.selectPage(p, w);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.UserCreateRequest req) {
        requireAdmin();
        Long tenantId = requireTenantId();
        requireShopInTenant(req.getMainShopId(), tenantId);
        long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, req.getUsername())
                .eq(SysUser::getDelFlag, 0));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        SysUser u = new SysUser();
        u.setTenantId(tenantId);
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRealName(req.getRealName());
        u.setMainShopId(req.getMainShopId());
        u.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        userMapper.insert(u);
        return u.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.UserUpdateRequest req) {
        requireAdmin();
        Long tenantId = requireTenantId();
        SysUser exist = requireUserInTenant(id, tenantId);
        requireShopInTenant(req.getMainShopId(), tenantId);
        exist.setRealName(req.getRealName());
        exist.setMainShopId(req.getMainShopId());
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            exist.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        userMapper.updateById(exist);
    }

    public List<SystemAdminDtos.UserShopRoleRow> listUserShopRoles(Long userId) {
        requireAdmin();
        Long tenantId = requireTenantId();
        SysUser u = requireUserInTenant(userId, tenantId);
        List<SysUserShopRole> list = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, u.getId())
                .eq(SysUserShopRole::getDelFlag, 0));
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> shopIds = list.stream().map(SysUserShopRole::getShopId).filter(Objects::nonNull).distinct().toList();
        List<Long> roleIds = list.stream().map(SysUserShopRole::getRoleId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SysShop> shops = shopIds.isEmpty() ? Map.of() : shopMapper.selectList(
                new LambdaQueryWrapper<SysShop>()
                        .eq(SysShop::getTenantId, tenantId)
                        .eq(SysShop::getDelFlag, 0)
                        .in(SysShop::getId, shopIds)).stream()
                .collect(Collectors.toMap(SysShop::getId, s -> s, (a, b) -> a));
        Map<Long, SysRole> roles = roleIds.isEmpty() ? Map.of() : roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getDelFlag, 0)
                        .in(SysRole::getId, roleIds)).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r, (a, b) -> a));
        List<SystemAdminDtos.UserShopRoleRow> rows = new ArrayList<>();
        for (SysUserShopRole x : list) {
            if (!shops.containsKey(x.getShopId()) || !roles.containsKey(x.getRoleId())) {
                continue;
            }
            SystemAdminDtos.UserShopRoleRow row = new SystemAdminDtos.UserShopRoleRow();
            row.setShopId(x.getShopId());
            SysShop sh = shops.get(x.getShopId());
            row.setShopName(sh != null ? sh.getShopName() : "");
            row.setRoleId(x.getRoleId());
            SysRole ro = roles.get(x.getRoleId());
            row.setRoleName(ro != null ? ro.getRoleName() : "");
            rows.add(row);
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserShopRoles(Long userId, SystemAdminDtos.UserShopRoleSaveRequest req) {
        requireAdmin();
        Long tenantId = requireTenantId();
        requireUserInTenant(userId, tenantId);
        validateShopRoleAssignments(req, tenantId);
        List<SysUserShopRole> old = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getDelFlag, 0));
        for (SysUserShopRole x : old) {
            userShopRoleMapper.deleteById(x.getId());
        }
        if (req.getItems() == null) {
            return;
        }
        for (SystemAdminDtos.UserShopRoleItem item : req.getItems()) {
            if (item.getShopId() == null || item.getRoleId() == null) {
                continue;
            }
            SysUserShopRole m = new SysUserShopRole();
            m.setUserId(userId);
            m.setShopId(item.getShopId());
            m.setRoleId(item.getRoleId());
            m.setTenantId(tenantId);
            userShopRoleMapper.insert(m);
        }
    }

    private void requireAdmin() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null || !adminAuthorizationService.hasAdminRole(uid)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可操作");
        }
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tenantId;
    }

    private SysUser requireUserInTenant(Long userId, Long tenantId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getDelFlag() != null && user.getDelFlag() == 1) || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void requireShopInTenant(Long shopId, Long tenantId) {
        SysShop shop = shopMapper.selectById(shopId);
        if (shop == null || (shop.getDelFlag() != null && shop.getDelFlag() == 1) || !Objects.equals(shop.getTenantId(), tenantId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "店铺不存在或不属于当前租户");
        }
    }

    private void validateShopRoleAssignments(SystemAdminDtos.UserShopRoleSaveRequest req, Long tenantId) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            return;
        }
        Set<Long> shopIds = req.getItems().stream()
                .map(SystemAdminDtos.UserShopRoleItem::getShopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> roleIds = req.getItems().stream()
                .map(SystemAdminDtos.UserShopRoleItem::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (shopIds.size() != req.getItems().stream().map(SystemAdminDtos.UserShopRoleItem::getShopId).filter(Objects::nonNull).distinct().count()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "店铺参数非法");
        }
        if (roleIds.size() != req.getItems().stream().map(SystemAdminDtos.UserShopRoleItem::getRoleId).filter(Objects::nonNull).distinct().count()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色参数非法");
        }
        long shopCount = shopIds.isEmpty() ? 0 : shopMapper.selectCount(new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getTenantId, tenantId)
                .eq(SysShop::getDelFlag, 0)
                .in(SysShop::getId, shopIds));
        if (shopCount != shopIds.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在不属于当前租户的店铺");
        }
        long roleCount = roleIds.isEmpty() ? 0 : roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getDelFlag, 0)
                .in(SysRole::getId, roleIds));
        if (roleCount != roleIds.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在不属于当前租户的角色");
        }
    }
}
