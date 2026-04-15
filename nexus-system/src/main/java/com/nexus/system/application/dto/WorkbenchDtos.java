package com.nexus.system.application.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public final class WorkbenchDtos {

    private WorkbenchDtos() {}

    // ===================== 仪表盘摘要 =====================
    @Data
    public static class DashboardSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 今日销售额（已出库的销售订单） */
        private BigDecimal todaySaleAmount;
        /** 本月采购额（已入库的采购订单） */
        private BigDecimal monthlyPurchaseAmount;
        /** 客户总数 */
        private Long customerCount;
        /** 供应商总数 */
        private Long supplierCount;
        /** 待审批数（请假申请 + 采购订单） */
        private Long pendingApprovalCount;
        /** 库存预警数（当前库存低于最低库存的商品） */
        private Long stockAlarmCount;
    }

    // ===================== 销售/采购趋势 =====================
    @Data
    public static class ChartDataPoint implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String month;          // "2026-01"
        private BigDecimal amount;     // 当月金额
    }

    @Data
    public static class ChartData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<ChartDataPoint> data;
    }

    // ===================== 热销商品 TOP N =====================
    @Data
    public static class TopProduct implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long productId;
        private String productName;
        private Long saleQuantity;     // 累计销售数量
        private BigDecimal saleAmount; // 累计销售金额
    }

    // ===================== 库存预警列表 =====================
    @Data
    public static class StockAlarmItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long productId;
        private String productName;
        private String warehouseName;
        private Integer currentQty;
        private Integer minStock;
    }
}
