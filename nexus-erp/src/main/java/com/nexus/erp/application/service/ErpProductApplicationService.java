package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.OrgContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.core.page.PageQuery;
import com.nexus.common.core.page.PageResult;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.domain.model.ErpProduct;
import com.nexus.erp.infrastructure.mapper.ErpProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ErpProductApplicationService {

    private final ErpProductMapper productMapper;

    public PageResult<ErpProduct> page(PageQuery pageQuery, String name, String category) {
        Long tenantId = requireTenantId();
        Long shopId = requireShopId();
        Long orgId = requireOrgId();
        Page<ErpProduct> p = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        LambdaQueryWrapper<ErpProduct> w = new LambdaQueryWrapper<>();
        w.eq(ErpProduct::getTenantId, tenantId);
        w.eq(ErpProduct::getShopId, shopId);
        w.eq(orgId != null, ErpProduct::getOrgId, orgId);
        w.eq(ErpProduct::getDelFlag, 0);
        w.like(StringUtils.hasText(name), ErpProduct::getName, name);
        w.like(StringUtils.hasText(category), ErpProduct::getCategory, category);
        w.orderByDesc(ErpProduct::getId);
        IPage<ErpProduct> result = productMapper.selectPage(p, w);
        return PageResult.of(result.getRecords(), result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpDtos.ProductCreateRequest req) {
        ErpProduct e = new ErpProduct();
        e.setTenantId(requireTenantId());
        e.setOrgId(requireOrgId());
        e.setName(req.getName());
        e.setCategory(req.getCategory());
        e.setUnit(req.getUnit());
        e.setPrice(req.getPrice());
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        e.setShopId(requireShopId());
        productMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpDtos.ProductUpdateRequest req) {
        ErpProduct exist = getScopedProduct(id);
        exist.setOrgId(requireOrgId());
        exist.setName(req.getName());
        exist.setCategory(req.getCategory());
        exist.setUnit(req.getUnit());
        exist.setPrice(req.getPrice());
        productMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, ErpDtos.ProductStatusRequest req) {
        ErpProduct exist = getScopedProduct(id);
        exist.setStatus(req.getStatus());
        productMapper.updateById(exist);
    }

    private ErpProduct getScopedProduct(Long id) {
        Long tenantId = requireTenantId();
        Long shopId = requireShopId();
        Long orgId = requireOrgId();
        ErpProduct exist = productMapper.selectOne(new LambdaQueryWrapper<ErpProduct>()
                .eq(ErpProduct::getId, id)
                .eq(ErpProduct::getTenantId, tenantId)
                .eq(ErpProduct::getShopId, shopId)
                .eq(orgId != null, ErpProduct::getOrgId, orgId)
                .eq(ErpProduct::getDelFlag, 0));
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或无权访问");
        }
        return exist;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tenantId;
    }

    private Long requireShopId() {
        Long shopId = OrgContext.getShopId();
        if (shopId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少当前店铺上下文");
        }
        return shopId;
    }

    private Long requireOrgId() {
        Long orgId = OrgContext.getOrgId();
        if (orgId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少当前组织上下文");
        }
        return orgId;
    }
}
