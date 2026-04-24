package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.OrgContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.domain.model.ErpCustomer;
import com.nexus.erp.infrastructure.mapper.ErpCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ErpCustomerApplicationService {

    private final ErpCustomerMapper customerMapper;

    public IPage<ErpCustomer> page(long current, long size, String name, String contactPhone) {
        Long tenantId = requireTenantId();
        Long shopId = requireShopId();
        Long orgId = requireOrgId();
        Page<ErpCustomer> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpCustomer> w = new LambdaQueryWrapper<>();
        w.eq(ErpCustomer::getTenantId, tenantId);
        w.eq(ErpCustomer::getShopId, shopId);
        w.eq(orgId != null, ErpCustomer::getOrgId, orgId);
        w.eq(ErpCustomer::getDelFlag, 0);
        w.like(StringUtils.hasText(name), ErpCustomer::getName, name);
        w.like(StringUtils.hasText(contactPhone), ErpCustomer::getContactPhone, contactPhone);
        w.orderByDesc(ErpCustomer::getId);
        return customerMapper.selectPage(p, w);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpDtos.CustomerCreateRequest req) {
        ErpCustomer e = new ErpCustomer();
        e.setTenantId(requireTenantId());
        e.setOrgId(requireOrgId());
        e.setName(req.getName());
        e.setContactName(req.getContactName());
        e.setContactPhone(req.getContactPhone());
        e.setLevel(req.getLevel());
        e.setCreditLimit(req.getCreditLimit() != null ? req.getCreditLimit() : BigDecimal.ZERO);
        e.setShopId(requireShopId());
        customerMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpDtos.CustomerUpdateRequest req) {
        ErpCustomer exist = getScopedCustomer(id);
        exist.setOrgId(requireOrgId());
        exist.setName(req.getName());
        exist.setContactName(req.getContactName());
        exist.setContactPhone(req.getContactPhone());
        exist.setLevel(req.getLevel());
        if (req.getCreditLimit() != null) {
            exist.setCreditLimit(req.getCreditLimit());
        }
        customerMapper.updateById(exist);
    }

    private ErpCustomer getScopedCustomer(Long id) {
        Long tenantId = requireTenantId();
        Long shopId = requireShopId();
        Long orgId = requireOrgId();
        ErpCustomer exist = customerMapper.selectOne(new LambdaQueryWrapper<ErpCustomer>()
                .eq(ErpCustomer::getId, id)
                .eq(ErpCustomer::getTenantId, tenantId)
                .eq(ErpCustomer::getShopId, shopId)
                .eq(orgId != null, ErpCustomer::getOrgId, orgId)
                .eq(ErpCustomer::getDelFlag, 0));
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在或无权访问");
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
