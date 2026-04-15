package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.domain.model.ErpSupplier;
import com.nexus.erp.infrastructure.mapper.ErpSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class ErpSupplierApplicationService {

    private final ErpSupplierMapper supplierMapper;

    public ErpSupplierApplicationService(ErpSupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    public IPage<ErpSupplier> page(long current, long size, String supplierName) {
        Long tenantId = requireTenantId();
        Page<ErpSupplier> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpSupplier> w = new LambdaQueryWrapper<ErpSupplier>()
                .eq(ErpSupplier::getTenantId, tenantId)
                .eq(ErpSupplier::getDelFlag, 0)
                .like(StringUtils.hasText(supplierName), ErpSupplier::getSupplierName, supplierName)
                .orderByDesc(ErpSupplier::getId);
        return supplierMapper.selectPage(p, w);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpFoundationDtos.SupplierCreateRequest req) {
        Long tenantId = requireTenantId();
        assertCodeUnique(tenantId, req.getSupplierCode().trim(), null);
        ErpSupplier e = new ErpSupplier();
        e.setTenantId(tenantId);
        e.setSupplierCode(req.getSupplierCode().trim());
        e.setSupplierName(req.getSupplierName().trim());
        e.setContactName(StringUtils.hasText(req.getContactName()) ? req.getContactName().trim() : null);
        e.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null);
        e.setEmail(StringUtils.hasText(req.getEmail()) ? req.getEmail().trim() : null);
        e.setBankName(StringUtils.hasText(req.getBankName()) ? req.getBankName().trim() : null);
        e.setAccountNo(StringUtils.hasText(req.getAccountNo()) ? req.getAccountNo().trim() : null);
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        supplierMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpFoundationDtos.SupplierUpdateRequest req) {
        Long tenantId = requireTenantId();
        ErpSupplier exist = loadOwned(id, tenantId);
        assertCodeUnique(tenantId, req.getSupplierCode().trim(), id);
        exist.setSupplierCode(req.getSupplierCode().trim());
        exist.setSupplierName(req.getSupplierName().trim());
        exist.setContactName(StringUtils.hasText(req.getContactName()) ? req.getContactName().trim() : null);
        exist.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null);
        exist.setEmail(StringUtils.hasText(req.getEmail()) ? req.getEmail().trim() : null);
        exist.setBankName(StringUtils.hasText(req.getBankName()) ? req.getBankName().trim() : null);
        exist.setAccountNo(StringUtils.hasText(req.getAccountNo()) ? req.getAccountNo().trim() : null);
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        supplierMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        ErpSupplier exist = loadOwned(id, tenantId);
        supplierMapper.deleteById(exist.getId());
    }

    private void assertCodeUnique(Long tenantId, String code, Long excludeId) {
        long cnt = supplierMapper.selectCount(new LambdaQueryWrapper<ErpSupplier>()
                .eq(ErpSupplier::getTenantId, tenantId)
                .eq(ErpSupplier::getSupplierCode, code)
                .eq(ErpSupplier::getDelFlag, 0)
                .ne(excludeId != null, ErpSupplier::getId, excludeId));
        if (cnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "供应商编码已存在");
        }
    }

    private ErpSupplier loadOwned(Long id, Long tenantId) {
        ErpSupplier e = supplierMapper.selectById(id);
        if (e == null || !Objects.equals(e.getTenantId(), tenantId) || (e.getDelFlag() != null && e.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "供应商不存在");
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
