package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.OrgContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.core.page.PageQuery;
import com.nexus.common.core.page.PageResult;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.tenant.NexusTenantProperties;
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
    private final NexusTenantProperties tenantProperties;

    public PageResult<ErpProduct> page(PageQuery pageQuery, String name, String category) {
        Page<ErpProduct> p = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        LambdaQueryWrapper<ErpProduct> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(name), ErpProduct::getName, name);
        w.like(StringUtils.hasText(category), ErpProduct::getCategory, category);
        w.orderByDesc(ErpProduct::getId);
        IPage<ErpProduct> result = productMapper.selectPage(p, w);
        return PageResult.of(result.getRecords(), result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpDtos.ProductCreateRequest req) {
        ErpProduct e = new ErpProduct();
        Long ctxOrg = OrgContext.getOrgId();
        e.setOrgId(req.getOrgId() != null ? req.getOrgId() : (ctxOrg != null ? ctxOrg : tenantProperties.getDefaultOrgId()));
        e.setName(req.getName());
        e.setCategory(req.getCategory());
        e.setUnit(req.getUnit());
        e.setPrice(req.getPrice());
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        Long sid = OrgContext.getShopId();
        if (sid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少当前店铺上下文");
        }
        e.setShopId(sid);
        productMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpDtos.ProductUpdateRequest req) {
        ErpProduct exist = productMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或无权访问");
        }
        exist.setOrgId(req.getOrgId() != null ? req.getOrgId() : exist.getOrgId());
        exist.setName(req.getName());
        exist.setCategory(req.getCategory());
        exist.setUnit(req.getUnit());
        exist.setPrice(req.getPrice());
        productMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, ErpDtos.ProductStatusRequest req) {
        ErpProduct exist = productMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或无权访问");
        }
        exist.setStatus(req.getStatus());
        productMapper.updateById(exist);
    }
}
