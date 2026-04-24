package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpOrderDtos;
import com.nexus.erp.domain.model.ErpCustomer;
import com.nexus.erp.domain.model.ErpProductInfo;
import com.nexus.erp.domain.model.ErpSaleOrder;
import com.nexus.erp.domain.model.ErpSaleOrderItem;
import com.nexus.erp.domain.model.ErpWarehouse;
import com.nexus.erp.infrastructure.mapper.ErpCustomerMapper;
import com.nexus.erp.infrastructure.mapper.ErpProductInfoMapper;
import com.nexus.erp.infrastructure.mapper.ErpSaleOrderItemMapper;
import com.nexus.erp.infrastructure.mapper.ErpSaleOrderMapper;
import com.nexus.erp.infrastructure.mapper.ErpStockMapper;
import com.nexus.erp.infrastructure.mapper.ErpWarehouseMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ErpSaleOrderApplicationService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING_REVIEW = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_OUTBOUND = 3;
    private static final int STATUS_REJECTED = -1;

    private final ErpSaleOrderMapper saleOrderMapper;
    private final ErpSaleOrderItemMapper saleOrderItemMapper;
    private final ErpProductInfoMapper productInfoMapper;
    private final ErpCustomerMapper customerMapper;
    private final ErpWarehouseMapper warehouseMapper;
    private final ErpStockMapper stockMapper;
    private final FinReceivableApplicationService finReceivableService;

    public ErpSaleOrderApplicationService(ErpSaleOrderMapper saleOrderMapper,
                                          ErpSaleOrderItemMapper saleOrderItemMapper,
                                          ErpProductInfoMapper productInfoMapper,
                                          ErpCustomerMapper customerMapper,
                                          ErpWarehouseMapper warehouseMapper,
                                          ErpStockMapper stockMapper,
                                          @Lazy FinReceivableApplicationService finReceivableService) {
        this.saleOrderMapper = saleOrderMapper;
        this.saleOrderItemMapper = saleOrderItemMapper;
        this.productInfoMapper = productInfoMapper;
        this.customerMapper = customerMapper;
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
        ErpCustomer customer = resolveCustomer(tenantId, req);

        BigDecimal total = calculateAndFillItemPrices(req.getItems(), tenantId);

        ErpSaleOrder order = new ErpSaleOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(nextOrderNo("SO"));
        order.setCustomerId(customer != null ? customer.getId() : null);
        order.setCustomerName(resolveCustomerName(req, customer));
        order.setWarehouseId(req.getWarehouseId());
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(STATUS_DRAFT);
        saleOrderMapper.insert(order);

        for (ErpOrderDtos.SaleOrderLineRequest line : req.getItems()) {
            BigDecimal rawSubTotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            BigDecimal sub = rawSubTotal.setScale(2, RoundingMode.HALF_UP);
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

    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(ErpOrderDtos.SaleOrderCreateRequest req) {
        Long tenantId = requireTenantId();
        Long orderId = create(req);
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可执行快捷出库");
        }
        order.setStatus(STATUS_APPROVED);
        if (saleOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "销售单状态已变化，请刷新后重试");
        }
        performOutbound(order, listOrderItems(orderId, tenantId), tenantId);
        return orderId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交审核");
        }
        order.setStatus(STATUS_PENDING_REVIEW);
        if (saleOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "销售单状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_PENDING_REVIEW)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核状态可审核通过");
        }
        order.setStatus(STATUS_APPROVED);
        if (saleOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "销售单状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void outboundOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅已审核状态可出库");
        }
        performOutbound(order, listOrderItems(orderId, tenantId), tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_PENDING_REVIEW)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核状态可拒绝");
        }
        order.setStatus(STATUS_REJECTED);
        if (saleOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "销售单状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long orderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadOrder(orderId, tenantId);
        if (!Objects.equals(order.getStatus(), STATUS_DRAFT) && !Objects.equals(order.getStatus(), STATUS_REJECTED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿或已拒绝单据可删除");
        }
        List<ErpSaleOrderItem> items = listOrderItems(orderId, tenantId);
        for (ErpSaleOrderItem it : items) {
            saleOrderItemMapper.deleteById(it.getId());
        }
        saleOrderMapper.deleteById(order.getId());
    }

    private void performOutbound(ErpSaleOrder order, List<ErpSaleOrderItem> items, Long tenantId) {
        for (ErpSaleOrderItem it : items) {
            int rows = stockMapper.deductStock(tenantId, it.getProductId(), order.getWarehouseId(), it.getQuantity());
            if (rows == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "商品库存不足或已被并发占用，出库失败");
            }
        }
        order.setStatus(STATUS_OUTBOUND);
        if (saleOrderMapper.updateById(order) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "销售单状态已变化，请刷新后重试");
        }
        finReceivableService.createFromSaleOrder(order.getId());
    }

    private ErpCustomer resolveCustomer(Long tenantId, ErpOrderDtos.SaleOrderCreateRequest req) {
        if (req.getCustomerId() == null) {
            return null;
        }
        ErpCustomer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null || !Objects.equals(customer.getTenantId(), tenantId)
                || (customer.getDelFlag() != null && customer.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "客户不存在或无权访问");
        }
        return customer;
    }

    private String resolveCustomerName(ErpOrderDtos.SaleOrderCreateRequest req, ErpCustomer customer) {
        if (customer != null) {
            return customer.getName();
        }
        if (!StringUtils.hasText(req.getCustomerName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "客户名称不能为空");
        }
        return req.getCustomerName().trim();
    }

    private ErpSaleOrder loadOrder(Long orderId, Long tenantId) {
        ErpSaleOrder o = saleOrderMapper.selectById(orderId);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId) || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "销售单不存在");
        }
        return o;
    }

    private List<ErpSaleOrderItem> listOrderItems(Long orderId, Long tenantId) {
        return saleOrderItemMapper.selectList(new LambdaQueryWrapper<ErpSaleOrderItem>()
                .eq(ErpSaleOrderItem::getTenantId, tenantId)
                .eq(ErpSaleOrderItem::getOrderId, orderId)
                .eq(ErpSaleOrderItem::getDelFlag, 0));
    }

    private void ensureWarehouse(Long tenantId, Long warehouseId) {
        ErpWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null || !Objects.equals(w.getTenantId(), tenantId) || (w.getDelFlag() != null && w.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仓库不存在");
        }
    }

    private BigDecimal calculateAndFillItemPrices(List<ErpOrderDtos.SaleOrderLineRequest> items, Long tenantId) {
        List<Long> productIds = items.stream()
                .map(ErpOrderDtos.SaleOrderLineRequest::getProductId)
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
        for (ErpOrderDtos.SaleOrderLineRequest line : items) {
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

    private static String nextOrderNo(String prefix) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
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
