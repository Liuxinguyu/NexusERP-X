package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpOrderDtos;
import com.nexus.erp.domain.model.ErpProductInfo;
import com.nexus.erp.domain.model.ErpSaleOrder;
import com.nexus.erp.domain.model.ErpSaleOrderItem;
import com.nexus.erp.domain.model.ErpWarehouse;
import com.nexus.erp.infrastructure.mapper.ErpProductInfoMapper;
import com.nexus.erp.infrastructure.mapper.ErpSaleOrderItemMapper;
import com.nexus.erp.infrastructure.mapper.ErpSaleOrderMapper;
import com.nexus.erp.infrastructure.mapper.ErpStockMapper;
import com.nexus.erp.infrastructure.mapper.ErpWarehouseMapper;
import com.nexus.erp.application.service.FinReceivableApplicationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ErpSaleOrderApplicationService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING_REVIEW = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = -1;

    private final ErpSaleOrderMapper saleOrderMapper;
    private final ErpSaleOrderItemMapper saleOrderItemMapper;
    private final ErpProductInfoMapper productInfoMapper;
    private final ErpWarehouseMapper warehouseMapper;
    private final ErpStockMapper stockMapper;
    private final FinReceivableApplicationService finReceivableService;

    public ErpSaleOrderApplicationService(ErpSaleOrderMapper saleOrderMapper,
                                          ErpSaleOrderItemMapper saleOrderItemMapper,
                                          ErpProductInfoMapper productInfoMapper,
                                          ErpWarehouseMapper warehouseMapper,
                                          ErpStockMapper stockMapper,
                                          @Lazy FinReceivableApplicationService finReceivableService) {
        this.saleOrderMapper = saleOrderMapper;
        this.saleOrderItemMapper = saleOrderItemMapper;
        this.productInfoMapper = productInfoMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockMapper = stockMapper;
        this.finReceivableService = finReceivableService;
    }

    public IPage<ErpSaleOrder> page(long current, long size, Integer status, String orderNo) {
        Long tenantId = requireTenantId();
        Page<ErpSaleOrder> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpSaleOrder> w = new LambdaQueryWrapper<ErpSaleOrder>()
                .eq(ErpSaleOrder::getTenantId, tenantId)
                .eq(ErpSaleOrder::getDelFlag, 0)
                .eq(status != null, ErpSaleOrder::getStatus, status)
                .like(orderNo != null && !orderNo.isBlank(), ErpSaleOrder::getOrderNo, orderNo)
                .orderByDesc(ErpSaleOrder::getId);
        return saleOrderMapper.selectPage(p, w);
    }

    public List<ErpSaleOrderItem> listItems(Long orderId) {
        Long tenantId = requireTenantId();
        loadOrder(orderId, tenantId);
        return saleOrderItemMapper.selectList(new LambdaQueryWrapper<ErpSaleOrderItem>()
                .eq(ErpSaleOrderItem::getTenantId, tenantId)
                .eq(ErpSaleOrderItem::getOrderId, orderId)
                .eq(ErpSaleOrderItem::getDelFlag, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpOrderDtos.SaleOrderCreateRequest req) {
        Long tenantId = requireTenantId();
        ensureWarehouse(tenantId, req.getWarehouseId());

        BigDecimal total = BigDecimal.ZERO;
        for (ErpOrderDtos.SaleOrderLineRequest line : req.getItems()) {
            ensureProduct(tenantId, line.getProductId());
            BigDecimal sub = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(sub);
        }

        ErpSaleOrder order = new ErpSaleOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(nextOrderNo("SO"));
        order.setCustomerName(req.getCustomerName().trim());
        order.setWarehouseId(req.getWarehouseId());
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(STATUS_DRAFT);
        saleOrderMapper.insert(order);

        for (ErpOrderDtos.SaleOrderLineRequest line : req.getItems()) {
            BigDecimal sub = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            ErpSaleOrderItem it = new ErpSaleOrderItem();
            it.setTenantId(tenantId);
            it.setOrderId(order.getId());
            it.setProductId(line.getProductId());
            it.setQuantity(line.getQuantity());
            it.setUnitPrice(line.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
            it.setSubtotal(sub);
            saleOrderItemMapper.insert(it);
        }
        return order.getId();
    }

    /**
     * 提交出库单（一步完成）：创建订单 + 乐观锁扣减库存 + status=1（已出库）。
     * 如果任一商品库存不足，抛出 RuntimeException 导致整个事务回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(ErpOrderDtos.SaleOrderCreateRequest req) {
        Long tenantId = requireTenantId();
        ensureWarehouse(tenantId, req.getWarehouseId());

        // 1. 计算总金额 & 校验商品存在性
        BigDecimal total = BigDecimal.ZERO;
        for (ErpOrderDtos.SaleOrderLineRequest line : req.getItems()) {
            ErpProductInfo product = ensureProductAndGet(tenantId, line.getProductId());
            BigDecimal sub = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(sub);
        }

        // 2. 乐观锁扣减库存（先扣库存，扣失败直接回滚）
        for (ErpOrderDtos.SaleOrderLineRequest line : req.getItems()) {
            int rows = stockMapper.deductStock(tenantId, line.getProductId(), req.getWarehouseId(), line.getQuantity());
            if (rows == 0) {
                ErpProductInfo p = productInfoMapper.selectById(line.getProductId());
                String pName = p != null ? p.getProductName() : String.valueOf(line.getProductId());
                throw new BusinessException(ResultCode.BAD_REQUEST, "商品「" + pName + "」库存不足，需扣减=" + line.getQuantity());
            }
        }

        // 3. 插入主表（status=1 已出库）
        ErpSaleOrder order = new ErpSaleOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(nextOrderNo("SO"));
        order.setCustomerId(req.getCustomerId());
        order.setCustomerName(req.getCustomerName().trim());
        order.setWarehouseId(req.getWarehouseId());
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(1); // 已出库
        saleOrderMapper.insert(order);

        // 4. 批量插入明细
        for (ErpOrderDtos.SaleOrderLineRequest line : req.getItems()) {
            BigDecimal sub = line.getUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            ErpSaleOrderItem it = new ErpSaleOrderItem();
            it.setTenantId(tenantId);
            it.setOrderId(order.getId());
            it.setProductId(line.getProductId());
            it.setQuantity(line.getQuantity());
            it.setUnitPrice(line.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
            it.setSubtotal(sub);
            saleOrderItemMapper.insert(it);
        }

        // 5. 自动创建应收记录
        finReceivableService.createFromSaleOrder(order.getId());

        return order.getId();
    }

    /**
     * 提交审核：草稿 → 待审核（原有分步流程保留）
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交审核");
        }
        order.setStatus(STATUS_PENDING_REVIEW);
        saleOrderMapper.updateById(order);
    }

    /**
     * 审核通过：待审核 → 已审核，同时乐观锁扣减库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_PENDING_REVIEW)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核状态可审核通过");
        }
        List<ErpSaleOrderItem> items = saleOrderItemMapper.selectList(new LambdaQueryWrapper<ErpSaleOrderItem>()
                .eq(ErpSaleOrderItem::getTenantId, tenantId)
                .eq(ErpSaleOrderItem::getOrderId, orderId)
                .eq(ErpSaleOrderItem::getDelFlag, 0));

        // 乐观锁扣减库存
        for (ErpSaleOrderItem it : items) {
            int rows = stockMapper.deductStock(tenantId, it.getProductId(), order.getWarehouseId(), it.getQuantity());
            if (rows == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "库存不足，扣减失败：产品ID=" + it.getProductId() + "，需扣减=" + it.getQuantity());
            }
        }

        order.setStatus(STATUS_APPROVED);
        saleOrderMapper.updateById(order);

        // 自动创建应收记录（审核通过后生成应收账款的时机——等出库时）
        // 注：若需在审核通过时即生成应收，去掉下面注释并调用 finReceivableService.createFromSaleOrder(order.getId());
    }

    /**
     * 审核拒绝：待审核 → 已拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_PENDING_REVIEW)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核状态可拒绝");
        }
        order.setStatus(STATUS_REJECTED);
        saleOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (Objects.equals(order.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已审核的单据不可删除");
        }
        List<ErpSaleOrderItem> items = saleOrderItemMapper.selectList(new LambdaQueryWrapper<ErpSaleOrderItem>()
                .eq(ErpSaleOrderItem::getOrderId, orderId)
                .eq(ErpSaleOrderItem::getTenantId, tenantId)
                .eq(ErpSaleOrderItem::getDelFlag, 0));
        for (ErpSaleOrderItem it : items) {
            saleOrderItemMapper.deleteById(it.getId());
        }
        saleOrderMapper.deleteById(order.getId());
    }

    private ErpSaleOrder loadOrder(Long orderId, Long tenantId) {
        ErpSaleOrder o = saleOrderMapper.selectById(orderId);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId) || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "销售单不存在");
        }
        return o;
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

    private ErpProductInfo ensureProductAndGet(Long tenantId, Long productId) {
        ErpProductInfo p = productInfoMapper.selectById(productId);
        if (p == null || !Objects.equals(p.getTenantId(), tenantId) || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "产品不存在: " + productId);
        }
        return p;
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
