package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpOrderDtos;
import com.nexus.erp.domain.model.ErpProductInfo;
import com.nexus.erp.domain.model.ErpPurchaseOrder;
import com.nexus.erp.domain.model.ErpPurchaseOrderItem;
import com.nexus.erp.domain.model.ErpSupplier;
import com.nexus.erp.domain.model.ErpWarehouse;
import com.nexus.erp.infrastructure.mapper.ErpProductInfoMapper;
import com.nexus.erp.infrastructure.mapper.ErpPurchaseOrderItemMapper;
import com.nexus.erp.infrastructure.mapper.ErpPurchaseOrderMapper;
import com.nexus.erp.infrastructure.mapper.ErpSupplierMapper;
import com.nexus.erp.infrastructure.mapper.ErpWarehouseMapper;
import com.nexus.erp.application.service.FinPayableApplicationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ErpPurchaseOrderApplicationService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING_REVIEW = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = -1;

    private final ErpPurchaseOrderMapper purchaseOrderMapper;
    private final ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    private final ErpProductInfoMapper productInfoMapper;
    private final ErpSupplierMapper supplierMapper;
    private final ErpWarehouseMapper warehouseMapper;
    private final FinPayableApplicationService finPayableService;

    public ErpPurchaseOrderApplicationService(ErpPurchaseOrderMapper purchaseOrderMapper,
                                              ErpPurchaseOrderItemMapper purchaseOrderItemMapper,
                                              ErpProductInfoMapper productInfoMapper,
                                              ErpSupplierMapper supplierMapper,
                                              ErpWarehouseMapper warehouseMapper,
                                              @Lazy FinPayableApplicationService finPayableService) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.productInfoMapper = productInfoMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.finPayableService = finPayableService;
    }

    public IPage<ErpPurchaseOrder> page(long current, long size, Integer status) {
        Long tenantId = requireTenantId();
        Page<ErpPurchaseOrder> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpPurchaseOrder> w = new LambdaQueryWrapper<ErpPurchaseOrder>()
                .eq(ErpPurchaseOrder::getTenantId, tenantId)
                .eq(ErpPurchaseOrder::getDelFlag, 0)
                .eq(status != null, ErpPurchaseOrder::getStatus, status)
                .orderByDesc(ErpPurchaseOrder::getId);
        return purchaseOrderMapper.selectPage(p, w);
    }

    public List<ErpPurchaseOrderItem> listItems(Long orderId) {
        Long tenantId = requireTenantId();
        loadOrder(orderId, tenantId);
        return purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<ErpPurchaseOrderItem>()
                .eq(ErpPurchaseOrderItem::getTenantId, tenantId)
                .eq(ErpPurchaseOrderItem::getOrderId, orderId)
                .eq(ErpPurchaseOrderItem::getDelFlag, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpOrderDtos.PurchaseOrderCreateRequest req) {
        Long tenantId = requireTenantId();
        ensureSupplier(tenantId, req.getSupplierId());
        ensureWarehouse(tenantId, req.getWarehouseId());

        BigDecimal total = BigDecimal.ZERO;
        for (ErpOrderDtos.PurchaseOrderLineRequest line : req.getItems()) {
            ensureProduct(tenantId, line.getProductId());
            BigDecimal sub = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(sub);
        }

        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(nextOrderNo("PO"));
        order.setSupplierId(req.getSupplierId());
        order.setWarehouseId(req.getWarehouseId());
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(STATUS_DRAFT);
        order.setRemark(StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : null);
        purchaseOrderMapper.insert(order);

        for (ErpOrderDtos.PurchaseOrderLineRequest line : req.getItems()) {
            BigDecimal sub = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            ErpPurchaseOrderItem it = new ErpPurchaseOrderItem();
            it.setTenantId(tenantId);
            it.setOrderId(order.getId());
            it.setProductId(line.getProductId());
            it.setQuantity(line.getQuantity());
            it.setUnitPrice(line.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
            it.setSubtotal(sub);
            purchaseOrderItemMapper.insert(it);
        }
        return order.getId();
    }

    /**
     * 确认入库：增加库存，单据置为已入库（仅已审核状态可操作）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmInbound(Long orderId) {
        Long tenantId = requireTenantId();
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅已审核的单据可确认入库");
        }
        List<ErpPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<ErpPurchaseOrderItem>()
                .eq(ErpPurchaseOrderItem::getTenantId, tenantId)
                .eq(ErpPurchaseOrderItem::getOrderId, orderId)
                .eq(ErpPurchaseOrderItem::getDelFlag, 0));
        for (ErpPurchaseOrderItem it : items) {
            ErpProductInfo p = productInfoMapper.selectById(it.getProductId());
            if (p == null || !Objects.equals(p.getTenantId(), tenantId) || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
                throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在: " + it.getProductId());
            }
            int stock = p.getStockQty() == null ? 0 : p.getStockQty();
            p.setStockQty(stock + it.getQuantity());
            productInfoMapper.updateById(p);
        }
        order.setStatus(3); // 3=已入库
        purchaseOrderMapper.updateById(order);

        // 自动创建应付记录
        finPayableService.createFromPurchaseOrder(order.getId());
    }

    /**
     * 提交审核：草稿 → 待审核
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交审核");
        }
        order.setStatus(STATUS_PENDING_REVIEW);
        purchaseOrderMapper.updateById(order);
    }

    /**
     * 审核通过：待审核 → 已审核
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_PENDING_REVIEW)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核状态可审核通过");
        }
        order.setStatus(STATUS_APPROVED);
        purchaseOrderMapper.updateById(order);
    }

    /**
     * 审核拒绝：待审核 → 已拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_PENDING_REVIEW)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核状态可拒绝");
        }
        order.setStatus(STATUS_REJECTED);
        purchaseOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long orderId) {
        Long tenantId = requireTenantId();
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (Objects.equals(order.getStatus(), 3) || Objects.equals(order.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已审核/已入库的单据不可删除");
        }
        List<ErpPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<ErpPurchaseOrderItem>()
                .eq(ErpPurchaseOrderItem::getOrderId, orderId)
                .eq(ErpPurchaseOrderItem::getTenantId, tenantId)
                .eq(ErpPurchaseOrderItem::getDelFlag, 0));
        for (ErpPurchaseOrderItem it : items) {
            purchaseOrderItemMapper.deleteById(it.getId());
        }
        purchaseOrderMapper.deleteById(order.getId());
    }

    private ErpPurchaseOrder loadOrder(Long orderId, Long tenantId) {
        ErpPurchaseOrder o = purchaseOrderMapper.selectById(orderId);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId) || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "采购单不存在");
        }
        return o;
    }

    private void ensureSupplier(Long tenantId, Long supplierId) {
        ErpSupplier s = supplierMapper.selectById(supplierId);
        if (s == null || !Objects.equals(s.getTenantId(), tenantId) || (s.getDelFlag() != null && s.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "供应商不存在");
        }
    }

    private void ensureWarehouse(Long tenantId, Long warehouseId) {
        ErpWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null || !Objects.equals(w.getTenantId(), tenantId) || (w.getDelFlag() != null && w.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仓库不存在");
        }
    }

    private void ensureProduct(Long tenantId, Long productId) {
        ErpProductInfo p = productInfoMapper.selectById(productId);
        if (p == null || !Objects.equals(p.getTenantId(), tenantId) || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "产品不存在: " + productId);
        }
    }

    private static String nextOrderNo(String prefix) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        // 使用 UUID 的后4位替代随机数，避免高并发下订单号碰撞
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(28);
        return prefix + ts + uuidPart;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
