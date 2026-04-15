package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.system.application.dto.UserInfoDtos;
import com.nexus.system.domain.model.SysMenu;
import com.nexus.system.domain.model.SysRoleMenu;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
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
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysNoticeApplicationService sysNoticeApplicationService;

    public SystemUserInfoService(SysUserMapper userMapper,
                                 SysUserShopRoleMapper userShopRoleMapper,
                                 SysRoleMenuMapper roleMenuMapper,
                                 SysMenuMapper menuMapper,
                                 SysNoticeApplicationService sysNoticeApplicationService) {
        this.userMapper = userMapper;
        this.userShopRoleMapper = userShopRoleMapper;
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

        List<UserInfoDtos.MenuNode> menus = queryMenuTree(principal.getUserId(), principal.getShopId());
        UserInfoDtos.UserInfoResponse resp = new UserInfoDtos.UserInfoResponse();
        resp.setProfile(profile);
        resp.setMenus(menus);
        resp.setLatestNoticeTitle(sysNoticeApplicationService.latestPublishedTitle());
        return resp;
    }

    private List<UserInfoDtos.MenuNode> queryMenuTree(Long userId, Long shopId) {
        if (shopId == null) {
            return List.of();
        }
        List<SysUserShopRole> mapping = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getShopId, shopId)
                .eq(SysUserShopRole::getDelFlag, 0));
        List<Long> roleIds = mapping.stream().map(SysUserShopRole::getRoleId).filter(Objects::nonNull).distinct().toList();
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

        expandWithAncestors(menuIds);

        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getDelFlag, 0)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1));
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
    private void expandWithAncestors(Set<Long> menuIds) {
        LinkedList<Long> queue = new LinkedList<>(menuIds);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            SysMenu row = menuMapper.selectById(id);
            if (row == null || row.getParentId() == null || row.getParentId() == 0) {
                continue;
            }
            Long pid = row.getParentId();
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
