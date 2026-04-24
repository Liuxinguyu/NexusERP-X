package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SysOrgTreeVO;
import com.nexus.system.application.dto.SystemOrgDtos;
import com.nexus.system.domain.model.SysOrg;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.infrastructure.mapper.SysOrgMapper;
import com.nexus.system.infrastructure.mapper.SysPostMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class SysOrgApplicationService {

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final SysPostMapper sysPostMapper;

    public SysOrgApplicationService(SysOrgMapper orgMapper, SysUserMapper userMapper, SysPostMapper sysPostMapper) {
        this.orgMapper = orgMapper;
        this.userMapper = userMapper;
        this.sysPostMapper = sysPostMapper;
    }

    /**
     * 返回以 {@code rootOrgId} 为根的子树内全部组织 id（含根）。
     * 优先基于已维护的 {@code ancestors} 字段做一次查询，避免因内存建树遗漏无序数据；
     * 若部分历史数据的 ancestors 未刷新，再回退到 parentId 关系做兜底遍历。
     */
    public List<Long> listSelfAndDescendantOrgIds(Long tenantId, Long rootOrgId) {
        if (tenantId == null || rootOrgId == null) {
            return List.of();
        }

        SysOrg root = orgMapper.selectById(rootOrgId);
        if (root == null || !Objects.equals(root.getTenantId(), tenantId)
                || (root.getDelFlag() != null && root.getDelFlag() == 1)) {
            return List.of();
        }

        Set<Long> out = new LinkedHashSet<>();
        out.add(rootOrgId);

        String rootToken = "," + rootOrgId + ",";
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getDelFlag, 0)
                .orderByAsc(SysOrg::getParentId)
                .orderByAsc(SysOrg::getSort)
                .orderByAsc(SysOrg::getId));

        for (SysOrg org : all) {
            if (org.getId() == null) {
                continue;
            }
            String chain = "," + Optional.ofNullable(org.getAncestors()).orElse("") + ",";
            if (org.getId().equals(rootOrgId) || chain.contains(rootToken)) {
                out.add(org.getId());
            }
        }

        Map<Long, List<Long>> children = new HashMap<>();
        for (SysOrg org : all) {
            if (org.getId() == null) {
                continue;
            }
            Long parentId = Optional.ofNullable(org.getParentId()).orElse(0L);
            children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(org.getId());
        }

        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootOrgId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null) {
                continue;
            }
            for (Long childId : children.getOrDefault(current, List.of())) {
                if (childId != null && out.add(childId)) {
                    queue.add(childId);
                }
            }
        }

        return new ArrayList<>(out);
    }

    /**
     * 当前租户组织树，并在内存中聚合各节点子树人数（直属 + 子孙）。
     */
    public List<SysOrgTreeVO> treeForCurrentTenant() {
        Long tenantId = requireTenantId();
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDelFlag, 0));
        Map<Long, List<SysUser>> usersByMainOrgId = new HashMap<>();
        for (SysUser u : users) {
            Long oid = u.getMainOrgId();
            if (oid == null) {
                continue;
            }
            usersByMainOrgId.computeIfAbsent(oid, k -> new ArrayList<>()).add(u);
        }
        List<SysOrg> flat = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getDelFlag, 0)
                .orderByAsc(SysOrg::getParentId)
                .orderByAsc(SysOrg::getSort)
                .orderByAsc(SysOrg::getId));
        return buildOrgTreeVoWithUserCounts(flat, usersByMainOrgId);
    }

    /**
     * 懒加载子节点：仅返回 {@code parentId} 下一层组织；{@link SysOrgTreeVO#getUserCount()} 为该节点子树总人数（含子孙部门）。
     */
    public List<SysOrgTreeVO> treeLazyForCurrentTenant(Long parentId) {
        Long tenantId = requireTenantId();
        long pid = parentId == null ? 0L : parentId;
        List<SysOrg> children = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getDelFlag, 0)
                .eq(SysOrg::getParentId, pid)
                .orderByAsc(SysOrg::getSort)
                .orderByAsc(SysOrg::getId));
        List<SysOrgTreeVO> out = new ArrayList<>();
        for (SysOrg o : children) {
            SysOrgTreeVO v = new SysOrgTreeVO();
            v.setId(o.getId());
            v.setParentId(o.getParentId());
            v.setOrgCode(o.getOrgCode());
            v.setOrgName(o.getOrgName());
            v.setChildren(new ArrayList<>());
            List<Long> subtree = listSelfAndDescendantOrgIds(tenantId, o.getId());
            long cnt = 0L;
            if (!subtree.isEmpty()) {
                cnt = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getDelFlag, 0)
                        .in(SysUser::getMainOrgId, subtree));
            }
            v.setUserCount(cnt > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cnt);
            out.add(v);
        }
        return out;
    }

    /**
     * 按组织筛选用户：{@code orgId} 为空则当前租户全部用户；否则为指定组织及其子组织（{@link #listSelfAndDescendantOrgIds}）下主属组织命中者。
     */
    public List<SysUser> listUsersForCurrentTenantByOrg(Long orgId) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDelFlag, 0)
                .orderByDesc(SysUser::getId);
        if (orgId != null) {
            SysOrg org = orgMapper.selectById(orgId);
            if (org == null || !Objects.equals(org.getTenantId(), tenantId)
                    || (org.getDelFlag() != null && org.getDelFlag() == 1)) {
                throw new BusinessException(ResultCode.NOT_FOUND, "组织不存在");
            }
            List<Long> orgIds = listSelfAndDescendantOrgIds(tenantId, orgId);
            if (orgIds.isEmpty()) {
                return List.of();
            }
            w.in(SysUser::getMainOrgId, orgIds);
        }
        List<SysUser> users = userMapper.selectList(w);
        users.forEach(u -> u.setPasswordHash(null));
        return users;
    }

    /**
     * 将用户主属组织调整为新部门；禁止调入当前主属部门的严格子部门（避免不当层级下沉）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeUserMainOrgForCurrentTenant(Long userId, Long newOrgId) {
        Long tenantId = requireTenantId();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)
                || (user.getDelFlag() != null && user.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        SysOrg target = orgMapper.selectById(newOrgId);
        if (target == null || !Objects.equals(target.getTenantId(), tenantId)
                || (target.getDelFlag() != null && target.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标组织不存在或已删除");
        }
        if ((target.getStatus() != null && target.getStatus() == 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标组织已停用");
        }
        Long currentOrgId = user.getMainOrgId();
        if (currentOrgId != null && !Objects.equals(currentOrgId, newOrgId)) {
            List<Long> selfAndDescendants = listSelfAndDescendantOrgIds(tenantId, currentOrgId);
            Set<Long> scope = new HashSet<>(selfAndDescendants);
            if (scope.contains(newOrgId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不能将用户调至当前所属组织下的子部门");
            }
        }
        user.setMainOrgId(newOrgId);
        userMapper.updateById(user);
    }

    /**
     * 自 {@code startOrgId} 起在本部门查找拥有 {@code targetPostCode} 的用户；若无则沿 {@code ancestors} 向父级逐级查找，直至虚拟根。
     */
    public Long findUserIdByPostUpward(Long startOrgId, String targetPostCode) {
        Long tenantId = requireTenantId();
        if (startOrgId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "startOrgId 不能为空");
        }
        if (!StringUtils.hasText(targetPostCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "postCode 不能为空");
        }
        String code = targetPostCode.trim();
        Long current = startOrgId;
        while (current != null) {
            SysOrg org = orgMapper.selectById(current);
            if (org == null || !Objects.equals(org.getTenantId(), tenantId)
                    || (org.getDelFlag() != null && org.getDelFlag() == 1)) {
                return null;
            }
            Long hit = sysPostMapper.selectFirstUserIdByMainOrgAndPost(tenantId, current, code);
            if (hit != null) {
                return hit;
            }
            Long parentOrgId = resolveParentOrgIdFromAncestors(org.getAncestors());
            if (parentOrgId == null || parentOrgId == 0L) {
                return null;
            }
            current = parentOrgId;
        }
        return null;
    }

    /**
     * ancestors 末段为本组织 id，倒数第二段为直接父组织 id（如 {@code 0,1,2,5} → 父为 2）。
     */
    private static Long resolveParentOrgIdFromAncestors(String ancestors) {
        if (!StringUtils.hasText(ancestors)) {
            return null;
        }
        String[] parts = ancestors.split(",");
        if (parts.length < 2) {
            return null;
        }
        String raw = parts[parts.length - 2].trim();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createForCurrentTenant(SystemOrgDtos.OrgCreateRequest req) {
        Long tenantId = requireTenantId();
        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        validateParentExists(tenantId, parentId);
        assertOrgCodeUnique(tenantId, req.getOrgCode(), null);

        SysOrg o = new SysOrg();
        o.setTenantId(tenantId);
        o.setParentId(parentId);
        o.setAncestors("0");
        o.setOrgCode(req.getOrgCode().trim());
        o.setOrgName(req.getOrgName().trim());
        o.setOrgType(req.getOrgType() != null ? req.getOrgType() : 1);
        o.setSort(req.getSort() != null ? req.getSort() : 0);
        o.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        orgMapper.insert(o);
        refreshAncestorsForTenant(tenantId);
        return o.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateForCurrentTenant(SystemOrgDtos.OrgUpdateRequest req) {
        Long tenantId = requireTenantId();
        SysOrg exist = orgMapper.selectById(req.getId());
        if (exist == null || !Objects.equals(exist.getTenantId(), tenantId) || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组织不存在");
        }
        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        if (req.getParentId() != null && req.getParentId() != 0L && req.getId().equals(req.getParentId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父节点不能是自己");
        }
        validateParentExists(tenantId, parentId);
        assertOrgCodeUnique(tenantId, req.getOrgCode(), req.getId());
        if (!Objects.equals(parentId, Optional.ofNullable(exist.getParentId()).orElse(0L))) {
            checkParentIdNotInChildren(tenantId, req.getId(), parentId);
        }

        exist.setParentId(parentId);
        exist.setOrgCode(req.getOrgCode().trim());
        exist.setOrgName(req.getOrgName().trim());
        if (req.getOrgType() != null) {
            exist.setOrgType(req.getOrgType());
        }
        if (req.getSort() != null) {
            exist.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        orgMapper.updateById(exist);
        refreshAncestorsForTenant(tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteForCurrentTenant(Long id) {
        Long tenantId = requireTenantId();
        SysOrg exist = orgMapper.selectById(id);
        if (exist == null || !Objects.equals(exist.getTenantId(), tenantId) || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组织不存在");
        }
        long childCount = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getParentId, id)
                .eq(SysOrg::getDelFlag, 0));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子组织，无法删除");
        }
        long userBind = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getMainOrgId, id)
                .eq(SysUser::getDelFlag, 0));
        if (userBind > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在绑定该组织的用户，无法删除");
        }
        exist.setDelFlag(1);
        orgMapper.updateById(exist);
    }

    /**
     * 按当前租户内 {@code parent_id} 关系，重算并写回所有未删除组织的 {@code ancestors}（格式 {@code 0,id1,id2,...}）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshAncestorsForTenant(Long tenantId) {
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getDelFlag, 0));
        if (all.isEmpty()) {
            return;
        }
        Map<Long, List<SysOrg>> childrenMap = new HashMap<>();
        for (SysOrg o : all) {
            long pid = Optional.ofNullable(o.getParentId()).orElse(0L);
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(o);
        }
        Map<Long, String> computed = new HashMap<>();
        Deque<SysOrg> dq = new ArrayDeque<>();
        for (SysOrg o : all) {
            if (Optional.ofNullable(o.getParentId()).orElse(0L) == 0L) {
                dq.add(o);
            }
        }
        while (!dq.isEmpty()) {
            SysOrg o = dq.poll();
            long pid = Optional.ofNullable(o.getParentId()).orElse(0L);
            String anc = pid == 0L ? "0," + o.getId() : computed.get(pid) + "," + o.getId();
            computed.put(o.getId(), anc);
            for (SysOrg c : childrenMap.getOrDefault(o.getId(), List.of())) {
                dq.add(c);
            }
        }
        for (SysOrg o : all) {
            if (!computed.containsKey(o.getId())) {
                computed.put(o.getId(), "0," + o.getId());
            }
        }
        for (SysOrg o : all) {
            String target = computed.get(o.getId());
            if (target != null && !Objects.equals(o.getAncestors(), target)) {
                o.setAncestors(target);
                orgMapper.updateById(o);
            }
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tenantId;
    }

    private void validateParentExists(Long tenantId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysOrg p = orgMapper.selectById(parentId);
        if (p == null || !Objects.equals(p.getTenantId(), tenantId) || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父组织不存在或已删除");
        }
    }

    private void assertOrgCodeUnique(Long tenantId, String orgCode, Long excludeId) {
        if (!StringUtils.hasText(orgCode)) {
            return;
        }
        LambdaQueryWrapper<SysOrg> w = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getOrgCode, orgCode.trim())
                .eq(SysOrg::getDelFlag, 0);
        if (excludeId != null) {
            w.ne(SysOrg::getId, excludeId);
        }
        if (orgMapper.selectCount(w) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "组织编码已存在");
        }
    }

    private void checkParentIdNotInChildren(Long tenantId, Long currentId, Long newParentId) {
        if (newParentId == null || newParentId == 0L) {
            return;
        }
        Set<Long> descendants = collectDescendantOrgIds(tenantId, currentId);
        descendants.remove(currentId);
        if (descendants.contains(newParentId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父节点不能设置为当前节点的子孙节点");
        }
    }

    private Set<Long> collectDescendantOrgIds(Long tenantId, Long rootId) {
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getDelFlag, 0));
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysOrg o : all) {
            long pid = Optional.ofNullable(o.getParentId()).orElse(0L);
            children.computeIfAbsent(pid, k -> new ArrayList<>()).add(o.getId());
        }
        Set<Long> out = new HashSet<>();
        Deque<Long> dq = new ArrayDeque<>();
        dq.add(rootId);
        while (!dq.isEmpty()) {
            Long id = dq.poll();
            if (id == null || !out.add(id)) {
                continue;
            }
            for (Long c : children.getOrDefault(id, List.of())) {
                dq.add(c);
            }
        }
        return out;
    }

    private static List<SysOrgTreeVO> buildOrgTreeVoWithUserCounts(List<SysOrg> flat,
                                                                   Map<Long, List<SysUser>> usersByMainOrgId) {
        Map<Long, SysOrgTreeVO> nodes = new HashMap<>();
        for (SysOrg o : flat) {
            SysOrgTreeVO v = new SysOrgTreeVO();
            v.setId(o.getId());
            v.setParentId(o.getParentId());
            v.setOrgCode(o.getOrgCode());
            v.setOrgName(o.getOrgName());
            v.setChildren(new ArrayList<>());
            nodes.put(o.getId(), v);
        }
        List<SysOrgTreeVO> roots = new ArrayList<>();
        for (SysOrg o : flat) {
            SysOrgTreeVO n = nodes.get(o.getId());
            long pid = Optional.ofNullable(o.getParentId()).orElse(0L);
            if (pid == 0L || !nodes.containsKey(pid)) {
                roots.add(n);
            } else {
                nodes.get(pid).getChildren().add(n);
            }
        }
        for (SysOrgTreeVO root : roots) {
            fillUserCountRecursive(root, usersByMainOrgId);
        }
        return roots;
    }

    /**
     * @return 当前节点子树总人数（含本节点直属用户）
     */
    private static int fillUserCountRecursive(SysOrgTreeVO node, Map<Long, List<SysUser>> usersByMainOrgId) {
        int direct = usersByMainOrgId.getOrDefault(node.getId(), List.of()).size();
        int fromChildren = 0;
        for (SysOrgTreeVO c : node.getChildren()) {
            fromChildren += fillUserCountRecursive(c, usersByMainOrgId);
        }
        int total = direct + fromChildren;
        node.setUserCount(total);
        return total;
    }
}
