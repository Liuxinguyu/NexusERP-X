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
