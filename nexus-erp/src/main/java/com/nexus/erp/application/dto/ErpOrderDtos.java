package com.nexus.erp.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public final class ErpOrderDtos {
    private ErpOrderDtos() {
    }

    @Data
    public static class PurchaseOrderCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long supplierId;
        @NotNull
        private Long warehouseId;
        private String remark;
        @NotEmpty
        @Valid
        private List<PurchaseOrderLineRequest> items;
    }

    @Data
    public static class PurchaseOrderLineRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer quantity;
        @NotNull
        private BigDecimal unitPrice;
    }

    @Data
    public static class SaleOrderCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 客户ID（先留字段，可为空） */
        private Long customerId;
        @NotBlank
        private String customerName;
        @NotNull
        private Long warehouseId;
        @NotEmpty
        @Valid
        private List<SaleOrderLineRequest> items;
    }

    @Data
    public static class SaleOrderLineRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer quantity;
        @NotNull
        private BigDecimal unitPrice;
    }
}
