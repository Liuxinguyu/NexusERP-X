package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class ErpProductInfoApplicationService {

    private final ErpProductInfoMapper productInfoMapper;
    private final ErpProductCategoryMapper categoryMapper;

    public ErpProductInfoApplicationService(ErpProductInfoMapper productInfoMapper,
                                            ErpProductCategoryMapper categoryMapper) {
        this.productInfoMapper = productInfoMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 分页查询：支持按分类、产品名称模糊。
     */
    public IPage<ErpProductInfo> page(long current, long size, Long categoryId, String productName) {
        Long tenantId = requireTenantId();
        Page<ErpProductInfo> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpProductInfo> w = new LambdaQueryWrapper<ErpProductInfo>()
                .eq(ErpProductInfo::getTenantId, tenantId)
                .eq(ErpProductInfo::getDelFlag, 0)
                .eq(categoryId != null, ErpProductInfo::getCategoryId, categoryId)
                .like(StringUtils.hasText(productName), ErpProductInfo::getProductName, productName)
                .orderByDesc(ErpProductInfo::getId);
        return productInfoMapper.selectPage(p, w);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpFoundationDtos.ProductInfoCreateRequest req) {
        Long tenantId = requireTenantId();
        ensureCategory(tenantId, req.getCategoryId());
        assertProductCodeUnique(tenantId, req.getProductCode().trim(), null);
        ErpProductInfo e = new ErpProductInfo();
        e.setTenantId(tenantId);
        e.setProductCode(req.getProductCode().trim());
        e.setProductName(req.getProductName().trim());
        e.setCategoryId(req.getCategoryId());
        e.setSpecModel(StringUtils.hasText(req.getSpecModel()) ? req.getSpecModel().trim() : null);
        e.setUnit(StringUtils.hasText(req.getUnit()) ? req.getUnit().trim() : null);
        e.setPrice(req.getPrice());
        e.setStockQty(req.getStockQty() != null ? req.getStockQty() : 0);
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        productInfoMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpFoundationDtos.ProductInfoUpdateRequest req) {
        Long tenantId = requireTenantId();
        ErpProductInfo exist = loadOwned(id, tenantId);
        ensureCategory(tenantId, req.getCategoryId());
        assertProductCodeUnique(tenantId, req.getProductCode().trim(), id);
        exist.setProductCode(req.getProductCode().trim());
        exist.setProductName(req.getProductName().trim());
        exist.setCategoryId(req.getCategoryId());
        exist.setSpecModel(StringUtils.hasText(req.getSpecModel()) ? req.getSpecModel().trim() : null);
        exist.setUnit(StringUtils.hasText(req.getUnit()) ? req.getUnit().trim() : null);
        exist.setPrice(req.getPrice());
        if (req.getStockQty() != null) {
            exist.setStockQty(req.getStockQty());
        }
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        productInfoMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        ErpProductInfo exist = loadOwned(id, tenantId);
        productInfoMapper.deleteById(exist.getId());
    }

    private void ensureCategory(Long tenantId, Long categoryId) {
        ErpProductCategory c = categoryMapper.selectById(categoryId);
        if (c == null || !Objects.equals(c.getTenantId(), tenantId) || (c.getDelFlag() != null && c.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "归属分类不存在");
        }
    }

    private void assertProductCodeUnique(Long tenantId, String code, Long excludeId) {
        long cnt = productInfoMapper.selectCount(new LambdaQueryWrapper<ErpProductInfo>()
                .eq(ErpProductInfo::getTenantId, tenantId)
                .eq(ErpProductInfo::getProductCode, code)
                .eq(ErpProductInfo::getDelFlag, 0)
                .ne(excludeId != null, ErpProductInfo::getId, excludeId));
        if (cnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "产品编码已存在");
        }
    }

    private ErpProductInfo loadOwned(Long id, Long tenantId) {
        ErpProductInfo e = productInfoMapper.selectById(id);
        if (e == null || !Objects.equals(e.getTenantId(), tenantId) || (e.getDelFlag() != null && e.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
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
