package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.domain.model.ErpWarehouse;
import com.nexus.erp.infrastructure.mapper.ErpWarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class ErpWarehouseApplicationService {

    private final ErpWarehouseMapper warehouseMapper;

    public ErpWarehouseApplicationService(ErpWarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    public IPage<ErpWarehouse> page(long current, long size, String warehouseName) {
        Long tenantId = requireTenantId();
        Page<ErpWarehouse> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpWarehouse> w = new LambdaQueryWrapper<ErpWarehouse>()
                .eq(ErpWarehouse::getTenantId, tenantId)
                .eq(ErpWarehouse::getDelFlag, 0)
                .like(StringUtils.hasText(warehouseName), ErpWarehouse::getWarehouseName, warehouseName)
                .orderByDesc(ErpWarehouse::getId);
        return warehouseMapper.selectPage(p, w);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpFoundationDtos.WarehouseCreateRequest req) {
        Long tenantId = requireTenantId();
        assertCodeUnique(tenantId, req.getWarehouseCode().trim(), null);
        ErpWarehouse e = new ErpWarehouse();
        e.setTenantId(tenantId);
        e.setWarehouseCode(req.getWarehouseCode().trim());
        e.setWarehouseName(req.getWarehouseName().trim());
        e.setManagerName(StringUtils.hasText(req.getManagerName()) ? req.getManagerName().trim() : null);
        e.setContactInfo(StringUtils.hasText(req.getContactInfo()) ? req.getContactInfo().trim() : null);
        e.setAddress(StringUtils.hasText(req.getAddress()) ? req.getAddress().trim() : null);
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        warehouseMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpFoundationDtos.WarehouseUpdateRequest req) {
        Long tenantId = requireTenantId();
        ErpWarehouse exist = loadOwned(id, tenantId);
        assertCodeUnique(tenantId, req.getWarehouseCode().trim(), id);
        exist.setWarehouseCode(req.getWarehouseCode().trim());
        exist.setWarehouseName(req.getWarehouseName().trim());
        exist.setManagerName(StringUtils.hasText(req.getManagerName()) ? req.getManagerName().trim() : null);
        exist.setContactInfo(StringUtils.hasText(req.getContactInfo()) ? req.getContactInfo().trim() : null);
        exist.setAddress(StringUtils.hasText(req.getAddress()) ? req.getAddress().trim() : null);
        if (req.getStatus() != null) {
            exist.setStatus(req.getStatus());
        }
        warehouseMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        ErpWarehouse exist = loadOwned(id, tenantId);
        warehouseMapper.deleteById(exist.getId());
    }

    private void assertCodeUnique(Long tenantId, String code, Long excludeId) {
        long cnt = warehouseMapper.selectCount(new LambdaQueryWrapper<ErpWarehouse>()
                .eq(ErpWarehouse::getTenantId, tenantId)
                .eq(ErpWarehouse::getWarehouseCode, code)
                .eq(ErpWarehouse::getDelFlag, 0)
                .ne(excludeId != null, ErpWarehouse::getId, excludeId));
        if (cnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仓库编码已存在");
        }
    }

    private ErpWarehouse loadOwned(Long id, Long tenantId) {
        ErpWarehouse e = warehouseMapper.selectById(id);
        if (e == null || !Objects.equals(e.getTenantId(), tenantId) || (e.getDelFlag() != null && e.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "仓库不存在");
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
