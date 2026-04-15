package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysMenu;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysMenuApplicationService {

    private final SysMenuMapper menuMapper;

    /**
     * 租户下全量菜单树（用于角色分配权限，不受当前用户菜单权限裁剪）。
     */
    public List<SystemAdminDtos.MenuTreeNode> menuTree() {
        List<SysMenu> rows = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
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
            n.setSort(m.getSort());
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
}
