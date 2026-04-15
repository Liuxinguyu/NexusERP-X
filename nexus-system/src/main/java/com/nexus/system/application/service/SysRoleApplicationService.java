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
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleApplicationService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public IPage<SysRole> page(long current, long size, String roleName, String roleCode) {
        Page<SysRole> p = new Page<>(current, size);
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName);
        w.like(StringUtils.hasText(roleCode), SysRole::getRoleCode, roleCode);
        w.orderByDesc(SysRole::getId);
        return roleMapper.selectPage(p, w);
    }

    public List<SystemAdminDtos.RoleOption> listOptions() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getDelFlag, 0)
                .orderByAsc(SysRole::getId));
        List<SystemAdminDtos.RoleOption> out = new ArrayList<>();
        for (SysRole r : roles) {
            SystemAdminDtos.RoleOption o = new SystemAdminDtos.RoleOption();
            o.setId(r.getId());
            o.setRoleName(r.getRoleName());
            o.setRoleCode(r.getRoleCode());
            o.setShopId(r.getShopId());
            o.setDataScope(r.getDataScope());
            out.add(o);
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.RoleCreateRequest req) {
        SysRole r = new SysRole();
        r.setShopId(req.getShopId());
        r.setRoleCode(req.getRoleCode());
        r.setRoleName(req.getRoleName());
        r.setDataScope(req.getDataScope());
        roleMapper.insert(r);
        return r.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.RoleUpdateRequest req) {
        SysRole exist = roleMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        exist.setShopId(req.getShopId());
        exist.setRoleCode(req.getRoleCode());
        exist.setRoleName(req.getRoleName());
        exist.setDataScope(req.getDataScope());
        roleMapper.updateById(exist);
    }

    public List<Long> listMenuIds(Long roleId) {
        List<SysRoleMenu> list = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId)
                .eq(SysRoleMenu::getDelFlag, 0));
        return list.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 分配菜单权限：先验证所有菜单ID都属于当前租户，防止跨租户越权。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, SystemAdminDtos.RoleMenuAssignRequest req) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null || (role.getDelFlag() != null && role.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }

        // 验证所有菜单ID都属于当前租户
        List<Long> menuIds = req.getMenuIds();
        if (menuIds != null && !menuIds.isEmpty()) {
            validateMenuIdsBelongToCurrentTenant(menuIds);
        }

        // 删除旧关联
        List<SysRoleMenu> old = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId)
                .eq(SysRoleMenu::getDelFlag, 0));
        for (SysRoleMenu rm : old) {
            roleMenuMapper.deleteById(rm.getId());
        }

        // 插入新关联（去重）
        Set<Long> uniq = new HashSet<>(menuIds);
        for (Long mid : uniq) {
            if (mid == null) {
                continue;
            }
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            roleMenuMapper.insert(rm);
        }
    }

    /**
     * 校验所有菜单ID都属于当前租户，防止跨租户权限授予。
     */
    private void validateMenuIdsBelongToCurrentTenant(List<Long> menuIds) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getDelFlag, 0));
        Set<Long> foundIds = menus.stream()
                .filter(m -> m.getTenantId() != null && m.getTenantId().equals(tenantId))
                .map(SysMenu::getId)
                .collect(Collectors.toSet());

        Set<Long> invalidIds = new HashSet<>(menuIds);
        invalidIds.removeAll(foundIds);
        if (!invalidIds.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "部分菜单不属于当前租户，禁止分配：" + invalidIds);
        }
    }
}
