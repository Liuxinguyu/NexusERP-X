package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.domain.model.ErpProductCategory;
import com.nexus.erp.domain.model.ErpProductInfo;
import com.nexus.erp.infrastructure.mapper.ErpProductCategoryMapper;
import com.nexus.erp.infrastructure.mapper.ErpProductInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ErpProductCategoryApplicationService {

    private final ErpProductCategoryMapper categoryMapper;
    private final ErpProductInfoMapper productInfoMapper;

    public ErpProductCategoryApplicationService(ErpProductCategoryMapper categoryMapper,
                                                ErpProductInfoMapper productInfoMapper) {
        this.categoryMapper = categoryMapper;
        this.productInfoMapper = productInfoMapper;
    }

    public List<ErpProductCategory> listAll() {
        Long tenantId = requireTenantId();
        return categoryMapper.selectList(new LambdaQueryWrapper<ErpProductCategory>()
                .eq(ErpProductCategory::getTenantId, tenantId)
                .eq(ErpProductCategory::getDelFlag, 0)
                .orderByAsc(ErpProductCategory::getSort)
                .orderByAsc(ErpProductCategory::getId));
    }

    /**
     * 构建租户下产品分类树（逻辑同系统菜单树：先扁平转节点，再挂父子）。
     */
    public List<ErpFoundationDtos.ProductCategoryTreeNode> buildTree() {
        List<ErpProductCategory> rows = listAll();
        List<ErpFoundationDtos.ProductCategoryTreeNode> nodes = new ArrayList<>();
        for (ErpProductCategory m : rows) {
            ErpFoundationDtos.ProductCategoryTreeNode n = new ErpFoundationDtos.ProductCategoryTreeNode();
            n.setId(m.getId());
            n.setParentId(m.getParentId());
            n.setName(m.getName());
            n.setSort(m.getSort());
            n.setStatus(m.getStatus());
            n.setChildren(new ArrayList<>());
            nodes.add(n);
        }
        nodes.sort(Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort()));
        Map<Long, ErpFoundationDtos.ProductCategoryTreeNode> byId = new HashMap<>();
        for (ErpFoundationDtos.ProductCategoryTreeNode n : nodes) {
            byId.put(n.getId(), n);
        }
        List<ErpFoundationDtos.ProductCategoryTreeNode> roots = new ArrayList<>();
        for (ErpFoundationDtos.ProductCategoryTreeNode n : nodes) {
            Long pid = n.getParentId();
            if (pid == null || pid == 0 || !byId.containsKey(pid)) {
                roots.add(n);
            } else {
                byId.get(pid).getChildren().add(n);
            }
        }
        return roots;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpFoundationDtos.ProductCategoryCreateRequest req) {
        Long tenantId = requireTenantId();
        validateParent(tenantId, req.getParentId(), null);
        ErpProductCategory e = new ErpProductCategory();
        e.setTenantId(tenantId);
        e.setName(req.getName().trim());
        e.setParentId(req.getParentId());
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        categoryMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpFoundationDtos.ProductCategoryUpdateRequest req) {
        Long tenantId = requireTenantId();
        ErpProductCategory exist = loadOwned(id, tenantId);
        validateParent(tenantId, req.getParentId(), id);
        if (Objects.equals(req.getParentId(), id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父级不能为自身");
        }
        exist.setName(req.getName().trim());
        exist.setParentId(req.getParentId());
        exist.setSort(req.getSort() != null ? req.getSort() : 0);
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        categoryMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        ErpProductCategory exist = loadOwned(id, tenantId);
        long childCnt = categoryMapper.selectCount(new LambdaQueryWrapper<ErpProductCategory>()
                .eq(ErpProductCategory::getTenantId, tenantId)
                .eq(ErpProductCategory::getParentId, id)
                .eq(ErpProductCategory::getDelFlag, 0));
        if (childCnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子分类，无法删除");
        }
        long prodCnt = productInfoMapper.selectCount(new LambdaQueryWrapper<ErpProductInfo>()
                .eq(ErpProductInfo::getTenantId, tenantId)
                .eq(ErpProductInfo::getCategoryId, id)
                .eq(ErpProductInfo::getDelFlag, 0));
        if (prodCnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类下存在产品，无法删除");
        }
        categoryMapper.deleteById(exist.getId());
    }

    private void validateParent(Long tenantId, Long parentId, Long excludeId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        ErpProductCategory p = categoryMapper.selectById(parentId);
        if (p == null || !Objects.equals(p.getTenantId(), tenantId) || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父级分类不存在");
        }
        if (excludeId != null && excludeId.equals(parentId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父级不能为自身");
        }
    }

    private ErpProductCategory loadOwned(Long id, Long tenantId) {
        ErpProductCategory e = categoryMapper.selectById(id);
        if (e == null || !Objects.equals(e.getTenantId(), tenantId) || (e.getDelFlag() != null && e.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return e;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
