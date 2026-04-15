package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.OrgContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.tenant.NexusTenantProperties;
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
    private final NexusTenantProperties tenantProperties;

    public IPage<ErpCustomer> page(long current, long size, String name, String contactPhone) {
        Page<ErpCustomer> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpCustomer> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(name), ErpCustomer::getName, name);
        w.like(StringUtils.hasText(contactPhone), ErpCustomer::getContactPhone, contactPhone);
        w.orderByDesc(ErpCustomer::getId);
        return customerMapper.selectPage(p, w);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpDtos.CustomerCreateRequest req) {
        ErpCustomer e = new ErpCustomer();
        Long ctxOrg = OrgContext.getOrgId();
        e.setOrgId(req.getOrgId() != null ? req.getOrgId() : (ctxOrg != null ? ctxOrg : tenantProperties.getDefaultOrgId()));
        e.setName(req.getName());
        e.setContactName(req.getContactName());
        e.setContactPhone(req.getContactPhone());
        e.setLevel(req.getLevel());
        e.setCreditLimit(req.getCreditLimit() != null ? req.getCreditLimit() : BigDecimal.ZERO);
        Long sid = OrgContext.getShopId();
        if (sid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少当前店铺上下文");
        }
        e.setShopId(sid);
        customerMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpDtos.CustomerUpdateRequest req) {
        ErpCustomer exist = customerMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在或无权访问");
        }
        exist.setOrgId(req.getOrgId() != null ? req.getOrgId() : exist.getOrgId());
        exist.setName(req.getName());
        exist.setContactName(req.getContactName());
        exist.setContactPhone(req.getContactPhone());
        exist.setLevel(req.getLevel());
        if (req.getCreditLimit() != null) {
            exist.setCreditLimit(req.getCreditLimit());
        }
        customerMapper.updateById(exist);
    }
}
