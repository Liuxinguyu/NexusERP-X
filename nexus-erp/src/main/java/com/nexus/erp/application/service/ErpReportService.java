package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.domain.model.*;
import com.nexus.erp.infrastructure.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ErpReportService {

    private final ErpSaleOrderMapper saleOrderMapper;
    private final ErpSaleOrderItemMapper saleOrderItemMapper;
    private final ErpStockMapper stockMapper;
    private final ErpProductInfoMapper productInfoMapper;
    private final ErpProductCategoryMapper categoryMapper;
    private final ErpCustomerMapper customerMapper;

    // ===================== 销售报表 =====================

    /**
     * 月度销售汇总：销售额、订单数、毛利估算
     */
    public SalesMonthlyVO salesMonthly(Integer year, Integer month) {
        Long tenantId = requireTenantId();
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();

        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        LambdaQueryWrapper<ErpSaleOrder> w = new LambdaQueryWrapper<ErpSaleOrder>()
                .eq(ErpSaleOrder::getTenantId, tenantId)
                .eq(ErpSaleOrder::getDelFlag, 0)
                .eq(ErpSaleOrder::getStatus, 1) // 已出库
                .ge(ErpSaleOrder::getCreateTime, start.atStartOfDay())
                .le(ErpSaleOrder::getCreateTime, end.atTime(23, 59, 59));

        List<ErpSaleOrder> orders = saleOrderMapper.selectList(w);
        BigDecimal totalAmount = orders.stream()
                .map(ErpSaleOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SalesMonthlyVO vo = new SalesMonthlyVO();
        vo.setYear(y);
        vo.setMonth(m);
        vo.setOrderCount((long) orders.size());
        vo.setTotalAmount(totalAmount);
        return vo;
    }

    /**
     * 年度销售趋势（按月）
     */
    public List<MonthlyAmountVO> salesTrend(int year) {
        Long tenantId = requireTenantId();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<ErpSaleOrder> orders = saleOrderMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOrder>()
                        .eq(ErpSaleOrder::getTenantId, tenantId)
                        .eq(ErpSaleOrder::getDelFlag, 0)
                        .eq(ErpSaleOrder::getStatus, 1)
                        .ge(ErpSaleOrder::getCreateTime, start.atStartOfDay())
                        .le(ErpSaleOrder::getCreateTime, end.atTime(23, 59, 59)));

        // 按月聚合
        Map<Integer, BigDecimal> monthlyMap = orders.stream()
                .filter(o -> o.getCreateTime() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        o -> o.getCreateTime().getMonthValue(),
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                ErpSaleOrder::getTotalAmount,
                                BigDecimal::add)));

        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    MonthlyAmountVO v = new MonthlyAmountVO();
                    v.setMonth(year + "-" + String.format("%02d", month));
                    v.setAmount(monthlyMap.getOrDefault(month, BigDecimal.ZERO));
                    return v;
                }).toList();
    }

    /**
     * 商品销售排行
     */
    public List<ProductRankVO> productRank(int limit, Integer year, Integer month) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<ErpSaleOrder> ow = new LambdaQueryWrapper<ErpSaleOrder>()
                .eq(ErpSaleOrder::getTenantId, tenantId)
                .eq(ErpSaleOrder::getDelFlag, 0)
                .eq(ErpSaleOrder::getStatus, 1);
        if (year != null && month != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            ow.ge(ErpSaleOrder::getCreateTime, start.atStartOfDay())
              .le(ErpSaleOrder::getCreateTime, end.atTime(23, 59, 59));
        }

        List<ErpSaleOrder> orders = saleOrderMapper.selectList(ow);
        List<Long> orderIds = orders.stream().map(ErpSaleOrder::getId).toList();
        if (orderIds.isEmpty()) return List.of();

        List<ErpSaleOrderItem> items = saleOrderItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOrderItem>()
                        .eq(ErpSaleOrderItem::getTenantId, tenantId)
                        .eq(ErpSaleOrderItem::getDelFlag, 0)
                        .in(ErpSaleOrderItem::getOrderId, orderIds));

        Map<Long, java.util.List<ErpSaleOrderItem>> byProduct = items.stream()
                .collect(java.util.stream.Collectors.groupingBy(ErpSaleOrderItem::getProductId));

        return byProduct.entrySet().stream()
                .map(e -> {
                    Long productId = e.getKey();
                    List<ErpSaleOrderItem> productItems = e.getValue();
                    ErpProductInfo product = productInfoMapper.selectById(productId);
                    long qty = productItems.stream().mapToLong(ErpSaleOrderItem::getQuantity).sum();
                    BigDecimal amt = productItems.stream()
                            .map(ErpSaleOrderItem::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    ProductRankVO v = new ProductRankVO();
                    v.setProductId(productId);
                    v.setProductName(product != null ? product.getProductName() : "未知");
                    v.setSaleQuantity(qty);
                    v.setSaleAmount(amt);
                    return v;
                })
                .sorted((a, b) -> Long.compare(b.getSaleQuantity(), a.getSaleQuantity()))
                .limit(limit)
                .toList();
    }

    /**
     * 客户销售排行
     */
    public List<CustomerRankVO> customerRank(int limit, Integer year) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<ErpSaleOrder> w = new LambdaQueryWrapper<ErpSaleOrder>()
                .eq(ErpSaleOrder::getTenantId, tenantId)
                .eq(ErpSaleOrder::getDelFlag, 0)
                .eq(ErpSaleOrder::getStatus, 1)
                .isNotNull(ErpSaleOrder::getCustomerId)
                .ne(ErpSaleOrder::getCustomerId, 0L);
        if (year != null) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            w.ge(ErpSaleOrder::getCreateTime, start.atStartOfDay())
             .le(ErpSaleOrder::getCreateTime, end.atTime(23, 59, 59));
        }

        List<ErpSaleOrder> orders = saleOrderMapper.selectList(w);
        Map<Long, java.util.List<ErpSaleOrder>> byCustomer = orders.stream()
                .filter(o -> o.getCustomerId() != null)
                .collect(java.util.stream.Collectors.groupingBy(ErpSaleOrder::getCustomerId));

        return byCustomer.entrySet().stream()
                .map(e -> {
                    Long customerId = e.getKey();
                    List<ErpSaleOrder> custOrders = e.getValue();
                    ErpCustomer c = customerMapper.selectById(customerId);
                    BigDecimal amt = custOrders.stream()
                            .map(ErpSaleOrder::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    CustomerRankVO v = new CustomerRankVO();
                    v.setCustomerId(customerId);
                    v.setCustomerName(c != null ? c.getName() : "未知");
                    v.setOrderCount((long) custOrders.size());
                    v.setSaleAmount(amt);
                    return v;
                })
                .sorted((a, b) -> b.getSaleAmount().compareTo(a.getSaleAmount()))
                .limit(limit)
                .toList();
    }

    // ===================== 库存报表 =====================

    /**
     * 库存预警：当前库存低于最低库存的商品
     */
    public List<StockAlarmVO> stockAlarm() {
        Long tenantId = requireTenantId();
        return stockMapper.selectStockAlarm(tenantId).stream()
                .map(map -> {
                    StockAlarmVO v = new StockAlarmVO();
                    v.setProductId((Long) map.get("productId"));
                    v.setProductName((String) map.get("productName"));
                    v.setWarehouseName((String) map.get("warehouseName"));
                    v.setCurrentQty((Integer) map.get("currentQty"));
                    v.setMinStock((Integer) map.get("minStock"));
                    return v;
                }).toList();
    }

    /**
     * 库存汇总（按品类）
     */
    public List<StockSummaryVO> stockSummary() {
        Long tenantId = requireTenantId();
        List<ErpProductCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ErpProductCategory>()
                        .eq(ErpProductCategory::getTenantId, tenantId)
                        .eq(ErpProductCategory::getDelFlag, 0));

        return categories.stream().map(cat -> {
            List<ErpProductInfo> products = productInfoMapper.selectList(
                    new LambdaQueryWrapper<ErpProductInfo>()
                            .eq(ErpProductInfo::getTenantId, tenantId)
                            .eq(ErpProductInfo::getDelFlag, 0)
                            .eq(ErpProductInfo::getCategoryId, cat.getId()));

            int totalQty = products.stream()
                    .mapToInt(p -> p.getStockQty() != null ? p.getStockQty() : 0).sum();
            long productCount = products.size();

            StockSummaryVO v = new StockSummaryVO();
            v.setCategoryId(cat.getId());
            v.setCategoryName(cat.getName());
            v.setProductCount(productCount);
            v.setTotalQty(totalQty);
            return v;
        }).toList();
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        return tid;
    }

    // ===================== VO =====================

    @lombok.Data
    public static class SalesMonthlyVO implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private int year;
        private int month;
        private Long orderCount;
        private BigDecimal totalAmount;
    }

    @lombok.Data
    public static class MonthlyAmountVO implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String month;
        private BigDecimal amount;
    }

    @lombok.Data
    public static class ProductRankVO implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long productId;
        private String productName;
        private Long saleQuantity;
        private BigDecimal saleAmount;
    }

    @lombok.Data
    public static class CustomerRankVO implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long customerId;
        private String customerName;
        private Long orderCount;
        private BigDecimal saleAmount;
    }

    @lombok.Data
    public static class StockAlarmVO implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long productId;
        private String productName;
        private String warehouseName;
        private Integer currentQty;
        private Integer minStock;
    }

    @lombok.Data
    public static class StockSummaryVO implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long categoryId;
        private String categoryName;
        private Long productCount;
        private Integer totalQty;
    }
}
