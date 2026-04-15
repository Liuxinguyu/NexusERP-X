package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.domain.model.ErpProductInfo;
import com.nexus.erp.domain.model.ErpStock;
import com.nexus.erp.domain.model.ErpWarehouse;
import com.nexus.erp.infrastructure.mapper.ErpProductInfoMapper;
import com.nexus.erp.infrastructure.mapper.ErpStockMapper;
import com.nexus.erp.infrastructure.mapper.ErpWarehouseMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ErpStockApplicationService {

    private final ErpStockMapper stockMapper;
    private final ErpProductInfoMapper productMapper;
    private final ErpWarehouseMapper warehouseMapper;

    public ErpStockApplicationService(ErpStockMapper stockMapper,
                                      ErpProductInfoMapper productMapper,
                                      ErpWarehouseMapper warehouseMapper) {
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public IPage<ErpFoundationDtos.StockRowVO> page(long current, long size, Long productId, Long warehouseId) {
        Long tenantId = requireTenantId();
        Page<ErpStock> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpStock> w = new LambdaQueryWrapper<ErpStock>()
                .eq(ErpStock::getTenantId, tenantId)
                .eq(ErpStock::getDelFlag, 0)
                .eq(productId != null, ErpStock::getProductId, productId)
                .eq(warehouseId != null, ErpStock::getWarehouseId, warehouseId)
                .orderByDesc(ErpStock::getId);
        IPage<ErpStock> raw = stockMapper.selectPage(p, w);

        Set<Long> pids = raw.getRecords().stream().map(ErpStock::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> wids = raw.getRecords().stream().map(ErpStock::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> productNames = Map.of();
        if (!pids.isEmpty()) {
            List<ErpProductInfo> plist = productMapper.selectList(new LambdaQueryWrapper<ErpProductInfo>().in(ErpProductInfo::getId, pids));
            productNames = plist.stream().collect(Collectors.toMap(ErpProductInfo::getId, ErpProductInfo::getProductName, (a, b) -> a));
        }
        Map<Long, String> warehouseNames = Map.of();
        if (!wids.isEmpty()) {
            List<ErpWarehouse> wlist = warehouseMapper.selectList(new LambdaQueryWrapper<ErpWarehouse>().in(ErpWarehouse::getId, wids));
            warehouseNames = wlist.stream().collect(Collectors.toMap(ErpWarehouse::getId, ErpWarehouse::getWarehouseName, (a, b) -> a));
        }

        List<ErpFoundationDtos.StockRowVO> vos = new ArrayList<>();
        for (ErpStock s : raw.getRecords()) {
            ErpFoundationDtos.StockRowVO v = new ErpFoundationDtos.StockRowVO();
            v.setId(s.getId());
            v.setProductId(s.getProductId());
            v.setProductName(productNames.getOrDefault(s.getProductId(), "-"));
            v.setWarehouseId(s.getWarehouseId());
            v.setWarehouseName(warehouseNames.getOrDefault(s.getWarehouseId(), "-"));
            v.setQty(s.getQty());
            vos.add(v);
        }

        Page<ErpFoundationDtos.StockRowVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(vos);
        return out;
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文 tenant_id");
        }
        return tenantId;
    }
}
