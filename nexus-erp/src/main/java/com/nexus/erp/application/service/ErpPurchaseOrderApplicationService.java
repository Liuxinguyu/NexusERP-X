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
import com.nexus.erp.infrastructure.mapper.ErpStockMapper;
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
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ErpPurchaseOrderApplicationService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING_REVIEW = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_INBOUND = 3;
    private static final int STATUS_REJECTED = -1;

    private final ErpPurchaseOrderMapper purchaseOrderMapper;
    private final ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    private final ErpProductInfoMapper productInfoMapper;
    private final ErpSupplierMapper supplierMapper;
    private final ErpWarehouseMapper warehouseMapper;
    private final ErpStockMapper stockMapper;
    private final FinPayableApplicationService finPayableService;

    public ErpPurchaseOrderApplicationService(ErpPurchaseOrderMapper purchaseOrderMapper,
                                              ErpPurchaseOrderItemMapper purchaseOrderItemMapper,
                                              ErpProductInfoMapper productInfoMapper,
                                              ErpSupplierMapper supplierMapper,
                                              ErpWarehouseMapper warehouseMapper,
                                              ErpStockMapper stockMapper,
                                              @Lazy FinPayableApplicationService finPayableService) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.productInfoMapper = productInfoMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockMapper = stockMapper;
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

        BigDecimal total = calculateAndFillItemPrices(req.getItems(), tenantId);

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
            BigDecimal rawSubTotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            BigDecimal sub = rawSubTotal.setScale(2, RoundingMode.HALF_UP);
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
     * 快捷入库流程：创建草稿 -> 直接进入已审核 -> 执行入库履约。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long quickInbound(ErpOrderDtos.PurchaseOrderCreateRequest req) {
        Long tenantId = requireTenantId();
        Long orderId = create(req);
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可执行快捷入库");
        }
        order.setStatus(STATUS_APPROVED);
        if (purchaseOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "采购单状态已变化，请刷新后重试");
        }
        performInbound(order, listOrderItems(orderId, tenantId), tenantId);
        return orderId;
    }

    private BigDecimal calculateAndFillItemPrices(List<ErpOrderDtos.PurchaseOrderLineRequest> items, Long tenantId) {
        List<Long> productIds = items.stream()
                .map(ErpOrderDtos.PurchaseOrderLineRequest::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单明细中缺少商品ID");
        }
        List<ErpProductInfo> products = productInfoMapper.selectBatchIds(productIds);
        Map<Long, ErpProductInfo> productMap = products.stream()
                .filter(p -> p != null
                        && Objects.equals(p.getTenantId(), tenantId)
                        && (p.getDelFlag() == null || p.getDelFlag() == 0)
                        && (p.getStatus() == null || p.getStatus() == 1))
                .collect(Collectors.toMap(ErpProductInfo::getId, p -> p, (a, b) -> a));
        if (productMap.size() < productIds.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "部分商品不存在、已下架或无权访问，请刷新后重试");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ErpOrderDtos.PurchaseOrderLineRequest line : items) {
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "商品数量非法，必须大于0");
            }
            ErpProductInfo productInfo = productMap.get(line.getProductId());
            if (productInfo == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "部分商品不存在、已下架或无权访问，请刷新后重试");
            }
            BigDecimal realPrice = productInfo.getPrice();
            if (realPrice == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        String.format("商品 [%s] 未配置基础价格，禁止下单", productInfo.getProductName()));
            }
            line.setUnitPrice(realPrice);
            BigDecimal rawSubTotal = realPrice.multiply(BigDecimal.valueOf(line.getQuantity()));
            totalAmount = totalAmount.add(rawSubTotal);
        }
        return totalAmount;
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
        performInbound(order, listOrderItems(orderId, tenantId), tenantId);
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
        if (purchaseOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "采购单状态已变化，请刷新后重试");
        }
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
        if (purchaseOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "采购单状态已变化，请刷新后重试");
        }
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
        if (purchaseOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "采购单状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long orderId) {
        Long tenantId = requireTenantId();
        ErpPurchaseOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT) && !Objects.equals(order.getStatus(), STATUS_REJECTED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿或已拒绝单据可删除");
        }
        List<ErpPurchaseOrderItem> items = listOrderItems(orderId, tenantId);
        for (ErpPurchaseOrderItem it : items) {
            it.setDelFlag(1);
            purchaseOrderItemMapper.updateById(it);
        }
        order.setDelFlag(1);
        if (purchaseOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "采购单状态已变化，请刷新后重试");
        }
    }

    private void performInbound(ErpPurchaseOrder order, List<ErpPurchaseOrderItem> items, Long tenantId) {
        for (ErpPurchaseOrderItem it : items) {
            ensureProduct(tenantId, it.getProductId());
            stockMapper.upsertIncreaseStock(tenantId, it.getProductId(), order.getWarehouseId(), it.getQuantity());
        }
        order.setStatus(STATUS_INBOUND);
        if (purchaseOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "采购单状态已变化，请刷新后重试");
        }
        finPayableService.createFromPurchaseOrder(order.getId());
    }

    private List<ErpPurchaseOrderItem> listOrderItems(Long orderId, Long tenantId) {
        return purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<ErpPurchaseOrderItem>()
                .eq(ErpPurchaseOrderItem::getTenantId, tenantId)
                .eq(ErpPurchaseOrderItem::getOrderId, orderId)
                .eq(ErpPurchaseOrderItem::getDelFlag, 0));
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
