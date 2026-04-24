package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysMenu;
import com.nexus.system.domain.model.SysRoleMenu;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SysMenuApplicationService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    private static final Pattern PERMS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$");

    /**
     * 租户下全量菜单树（用于角色分配权限，不受当前用户菜单权限裁剪）。
     */
    public List<SystemAdminDtos.MenuTreeNode> menuTree() {
        Long tenantId = requireTenantId();
        List<SysMenu> rows = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, tenantId)
                .eq(SysMenu::getDelFlag, 0)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSort));
        List<SystemAdminDtos.MenuTreeNode> nodes = new ArrayList<>();
        for (SysMenu m : rows) {
            SystemAdminDtos.MenuTreeNode n = new SystemAdminDtos.MenuTreeNode();
            n.setId(m.getId());
            n.setParentId(m.getParentId());
            n.setMenuType(m.getMenuType());
            n.setMenuName(m.getMenuName());
            n.setPath(m.getPath());
            n.setComponent(m.getComponent());
            n.setPerms(m.getPerms());
            n.setIcon(m.getIcon());
            n.setSort(m.getSort());
            n.setVisible(m.getVisible());
            n.setStatus(m.getStatus());
            n.setChildren(new ArrayList<>());
            nodes.add(n);
        }
        nodes.sort(Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort()));
        Map<Long, SystemAdminDtos.MenuTreeNode> byId = new HashMap<>();
        for (SystemAdminDtos.MenuTreeNode n : nodes) {
            byId.put(n.getId(), n);
        }
        List<SystemAdminDtos.MenuTreeNode> roots = new ArrayList<>();
        for (SystemAdminDtos.MenuTreeNode n : nodes) {
            Long pid = n.getParentId();
            if (pid == null || pid == 0 || !byId.containsKey(pid)) {
                roots.add(n);
            } else {
                byId.get(pid).getChildren().add(n);
            }
        }
        return roots;
    }

    public SystemAdminDtos.MenuTreeNode getByIdForCurrentTenant(Long id) {
        Long tenantId = requireTenantId();
        SysMenu m = menuMapper.selectById(id);
        if (m == null || !Objects.equals(m.getTenantId(), tenantId)
                || (m.getDelFlag() != null && m.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在");
        }
        SystemAdminDtos.MenuTreeNode n = new SystemAdminDtos.MenuTreeNode();
        n.setId(m.getId());
        n.setParentId(m.getParentId());
        n.setMenuType(m.getMenuType());
        n.setMenuName(m.getMenuName());
        n.setPath(m.getPath());
        n.setComponent(m.getComponent());
        n.setPerms(m.getPerms());
        n.setIcon(m.getIcon());
        n.setSort(m.getSort());
        n.setVisible(m.getVisible());
        n.setStatus(m.getStatus());
        n.setChildren(new ArrayList<>());
        return n;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createForCurrentTenant(SystemAdminDtos.MenuCreateRequest req) {
        Long tenantId = requireTenantId();
        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        validateParentExists(tenantId, parentId);

        SysMenu menu = new SysMenu();
        menu.setTenantId(tenantId);
        menu.setParentId(parentId);
        menu.setMenuName(req.getMenuName().trim());
        menu.setMenuType(req.getMenuType() != null ? req.getMenuType().trim() : "C");
        menu.setPath(req.getPath());
        menu.setComponent(req.getComponent());
        menu.setPerms(validatePerms(req.getPerms()));
        menu.setIcon(req.getIcon());
        menu.setSort(req.getSort() != null ? req.getSort() : 0);
        menu.setVisible(req.getVisible() != null ? req.getVisible() : 1);
        menu.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        menu.setDelFlag(0);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteForCurrentTenant(Long id) {
        Long tenantId = requireTenantId();
        SysMenu exist = menuMapper.selectById(id);
        if (exist == null || !Objects.equals(exist.getTenantId(), tenantId)
                || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在");
        }
        long childCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, tenantId)
                .eq(SysMenu::getParentId, id)
                .eq(SysMenu::getDelFlag, 0));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子菜单，不允许删除");
        }
        exist.setDelFlag(1);
        menuMapper.updateById(exist);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateForCurrentTenant(SystemAdminDtos.MenuUpdateRequest req) {
        Long tenantId = requireTenantId();
        SysMenu exist = menuMapper.selectById(req.getId());
        if (exist == null || !Objects.equals(exist.getTenantId(), tenantId)
                || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在");
        }

        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        if (req.getParentId() != null && req.getParentId() != 0L && req.getId().equals(req.getParentId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父节点不能是自己");
        }
        validateParentExists(tenantId, parentId);
        checkParentIdNotInChildren(tenantId, req.getId(), parentId);

        exist.setParentId(parentId);
        exist.setMenuName(req.getMenuName().trim());
        if (StringUtils.hasText(req.getMenuType())) {
            exist.setMenuType(req.getMenuType().trim());
        }
        exist.setPath(req.getPath());
        exist.setComponent(req.getComponent());
        exist.setPerms(validatePerms(req.getPerms()));
        exist.setIcon(req.getIcon());
        if (req.getSort() != null) {
            exist.setSort(req.getSort());
        }
        if (req.getVisible() != null) {
            exist.setVisible(req.getVisible());
        }
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        menuMapper.updateById(exist);

        if (req.getStatus() != null && req.getStatus() == 0) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getMenuId, req.getId()));
        }
    }

    private void validateParentExists(Long tenantId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysMenu parent = menuMapper.selectById(parentId);
        if (parent == null || !Objects.equals(parent.getTenantId(), tenantId)
                || (parent.getDelFlag() != null && parent.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父节点不存在或已删除");
        }
    }

    private void checkParentIdNotInChildren(Long tenantId, Long currentId, Long newParentId) {
        if (newParentId == null || newParentId == 0L) {
            return;
        }
        Set<Long> descendants = collectDescendantMenuIds(tenantId, currentId);
        descendants.remove(currentId);
        if (descendants.contains(newParentId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父节点不能设置为当前节点的子孙节点");
        }
    }

    private Set<Long> collectDescendantMenuIds(Long tenantId, Long rootId) {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getTenantId, tenantId)
                .eq(SysMenu::getDelFlag, 0));
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysMenu menu : all) {
            if (menu.getId() == null) {
                continue;
            }
            Long pid = menu.getParentId() == null ? 0L : menu.getParentId();
            children.computeIfAbsent(pid, k -> new ArrayList<>()).add(menu.getId());
        }
        Set<Long> out = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !out.add(current)) {
                continue;
            }
            for (Long childId : children.getOrDefault(current, List.of())) {
                queue.add(childId);
            }
        }
        return out;
    }

    private String validatePerms(String perms) {
        if (perms == null || perms.isBlank()) {
            return perms;
        }
        String trimmed = perms.trim();
        if ("*:*:*".equals(trimmed)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "禁止使用通配符权限标识 *:*:*");
        }
        if (!PERMS_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "权限标识格式不合法，应为 module:entity:operation");
        }
        return trimmed;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tenantId;
    }
}
