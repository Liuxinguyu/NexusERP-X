package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.system.application.dto.UserInfoDtos;
import com.nexus.system.domain.model.SysMenu;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysRoleMenu;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMenuMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SystemUserInfoService {

    private final SysUserMapper userMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysNoticeApplicationService sysNoticeApplicationService;

    public SystemUserInfoService(SysUserMapper userMapper,
                                 SysUserShopRoleMapper userShopRoleMapper,
                                 SysRoleMapper roleMapper,
                                 SysRoleMenuMapper roleMenuMapper,
                                 SysMenuMapper menuMapper,
                                 SysNoticeApplicationService sysNoticeApplicationService) {
        this.userMapper = userMapper;
        this.userShopRoleMapper = userShopRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.sysNoticeApplicationService = sysNoticeApplicationService;
    }

    public UserInfoDtos.UserInfoResponse getCurrentUserInfo(NexusPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user == null || (user.getDelFlag() != null && user.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }

        UserInfoDtos.UserProfile profile = new UserInfoDtos.UserProfile();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setRealName(user.getRealName());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setTenantId(principal.getTenantId());
        profile.setCurrentShopId(principal.getShopId());
        profile.setCurrentOrgId(principal.getOrgId());
        profile.setDataScope(principal.getDataScope());
        profile.setAccessibleShopIds(principal.getAccessibleShopIds());
        profile.setAccessibleOrgIds(principal.getAccessibleOrgIds());

        List<UserInfoDtos.MenuNode> menus = queryMenuTree(principal.getUserId(), principal.getShopId(), principal.getTenantId());
        List<String> permissions = queryPermissions(principal.getUserId(), principal.getShopId(), principal.getTenantId());
        List<String> roles = queryRoleCodes(principal.getUserId(), principal.getShopId());

        UserInfoDtos.UserInfoResponse resp = new UserInfoDtos.UserInfoResponse();
        resp.setProfile(profile);
        resp.setMenus(menus);
        resp.setPermissions(permissions);
        resp.setRoles(roles);
        resp.setLatestNoticeTitle(sysNoticeApplicationService.latestPublishedTitle());
        return resp;
    }

    /**
     * 查询用户在当前店铺下拥有的所有权限字符串（包含按钮级 F 类型菜单的 perms）。
     */
    private List<String> queryPermissions(Long userId, Long shopId, Long tenantId) {
        if (shopId == null) return List.of();

        List<Long> roleIds = getUserRoleIds(userId, shopId);
        if (roleIds.isEmpty()) return List.of();

        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds)
                .eq(SysRoleMenu::getDelFlag, 0));
        Set<Long> menuIds = new HashSet<>();
        for (SysRoleMenu rm : roleMenus) {
            if (rm.getMenuId() != null) menuIds.add(rm.getMenuId());
        }
        if (menuIds.isEmpty()) return List.of();

        LambdaQueryWrapper<SysMenu> qw = new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getDelFlag, 0)
                .eq(SysMenu::getStatus, 1)
                .isNotNull(SysMenu::getPerms)
                .select(SysMenu::getPerms);
        if (tenantId != null) qw.eq(SysMenu::getTenantId, tenantId);

        List<SysMenu> menus = menuMapper.selectList(qw);
        Set<String> perms = new HashSet<>();
        for (SysMenu m : menus) {
            String p = m.getPerms();
            if (p != null && !p.isBlank()) perms.add(p.trim());
        }
        return new ArrayList<>(perms);
    }

    /**
     * 查询用户在当前店铺下的角色标识列表（roleCode）。
     */
    private List<String> queryRoleCodes(Long userId, Long shopId) {
        if (shopId == null) return List.of();

        List<Long> roleIds = getUserRoleIds(userId, shopId);
        if (roleIds.isEmpty()) return List.of();

        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .select(SysRole::getRoleCode));
        Set<String> codes = new HashSet<>();
        for (SysRole r : roles) {
            if (r.getRoleCode() != null && !r.getRoleCode().isBlank()) {
                codes.add(r.getRoleCode());
            }
        }
        return new ArrayList<>(codes);
    }

    private List<Long> getUserRoleIds(Long userId, Long shopId) {
        List<SysUserShopRole> mapping = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getShopId, shopId)
                .eq(SysUserShopRole::getDelFlag, 0));
        return mapping.stream().map(SysUserShopRole::getRoleId).filter(Objects::nonNull).distinct().toList();
    }

    private List<UserInfoDtos.MenuNode> queryMenuTree(Long userId, Long shopId, Long tenantId) {
        if (shopId == null) {
            return List.of();
        }
        List<Long> roleIds = getUserRoleIds(userId, shopId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds)
                .eq(SysRoleMenu::getDelFlag, 0));
        Set<Long> menuIds = new HashSet<>();
        for (SysRoleMenu rm : roleMenus) {
            if (rm.getMenuId() != null) {
                menuIds.add(rm.getMenuId());
            }
        }
        if (menuIds.isEmpty()) {
            return List.of();
        }

        expandWithAncestors(menuIds, tenantId);

        LambdaQueryWrapper<SysMenu> menuQw = new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getDelFlag, 0)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1);
        if (tenantId != null) {
            menuQw.eq(SysMenu::getTenantId, tenantId);
        }
        List<SysMenu> menus = menuMapper.selectList(menuQw);
        if (menus.isEmpty()) {
            return List.of();
        }

        List<UserInfoDtos.MenuNode> nodes = new ArrayList<>();
        for (SysMenu m : menus) {
            UserInfoDtos.MenuNode n = new UserInfoDtos.MenuNode();
            n.setId(m.getId());
            n.setParentId(m.getParentId());
            n.setMenuType(m.getMenuType());
            n.setMenuName(m.getMenuName());
            n.setPath(m.getPath());
            n.setComponent(m.getComponent());
            n.setIcon(m.getIcon());
            n.setPerms(m.getPerms());
            n.setSort(m.getSort());
            n.setChildren(new ArrayList<>());
            nodes.add(n);
        }
        nodes.sort(Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort()));

        Map<Long, UserInfoDtos.MenuNode> byId = new HashMap<>();
        for (UserInfoDtos.MenuNode n : nodes) {
            byId.put(n.getId(), n);
        }

        List<UserInfoDtos.MenuNode> roots = new ArrayList<>();
        for (UserInfoDtos.MenuNode n : nodes) {
            Long pid = n.getParentId();
            if (pid == null || pid == 0 || !byId.containsKey(pid)) {
                roots.add(n);
            } else {
                byId.get(pid).getChildren().add(n);
            }
        }
        sortChildren(roots);
        assignFullPaths(roots, "");
        return roots;
    }

    private void assignFullPaths(List<UserInfoDtos.MenuNode> nodes, String parentFull) {
        if (nodes == null) {
            return;
        }
        for (UserInfoDtos.MenuNode n : nodes) {
            String seg = n.getPath();
            if (seg == null || seg.isBlank()) {
                seg = "m-" + n.getId();
            } else {
                seg = seg.replaceAll("^/+", "").replaceAll("/+$", "");
            }
            String full;
            if (parentFull == null || parentFull.isEmpty()) {
                full = "/" + seg;
            } else {
                full = parentFull + "/" + seg;
            }
            full = full.replaceAll("/+", "/");
            n.setFullPath(full);
            assignFullPaths(n.getChildren(), full);
        }
    }

    /**
     * 将授权菜单的所有父级菜单 id 并入集合，保证树形结构完整。
     */
    private void expandWithAncestors(Set<Long> menuIds, Long tenantId) {
        if (menuIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysMenu> qw = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getDelFlag, 0)
                .select(SysMenu::getId, SysMenu::getParentId);
        if (tenantId != null) {
            qw.eq(SysMenu::getTenantId, tenantId);
        }
        List<SysMenu> allMenus = menuMapper.selectList(qw);
        Map<Long, Long> parentMap = new HashMap<>();
        for (SysMenu m : allMenus) {
            if (m.getId() != null && m.getParentId() != null) {
                parentMap.put(m.getId(), m.getParentId());
            }
        }
        LinkedList<Long> queue = new LinkedList<>(menuIds);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            Long pid = parentMap.get(id);
            if (pid == null || pid == 0) {
                continue;
            }
            if (menuIds.add(pid)) {
                queue.add(pid);
            }
        }
    }

    private void sortChildren(List<UserInfoDtos.MenuNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort()));
        for (UserInfoDtos.MenuNode n : nodes) {
            sortChildren(n.getChildren());
        }
    }
}
