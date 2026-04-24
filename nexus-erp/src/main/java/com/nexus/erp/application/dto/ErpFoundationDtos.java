package com.nexus.erp.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ErpFoundationDtos {
    private ErpFoundationDtos() {
    }

    @Data
    public static class ProductCategoryTreeNode implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long parentId;
        private String name;
        private Integer sort;
        private Integer status;
        private List<ProductCategoryTreeNode> children = new ArrayList<>();
    }

    @Data
    public static class ProductCategoryCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String name;
        @NotNull
        private Long parentId;
        private Integer sort;
        private Integer status;
    }

    @Data
    public static class ProductCategoryUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String name;
        @NotNull
        private Long parentId;
        private Integer sort;
        private Integer status;
    }

    @Data
    public static class ProductInfoCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String productCode;
        @NotBlank
        private String productName;
        @NotNull
        private Long categoryId;
        private String specModel;
        private String unit;
        @NotNull
        private BigDecimal price;
        private Integer stockQty;
        private Integer status;
    }

    @Data
    public static class ProductInfoUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String productCode;
        @NotBlank
        private String productName;
        @NotNull
        private Long categoryId;
        private String specModel;
        private String unit;
        @NotNull
        private BigDecimal price;
        private Integer stockQty;
        private Integer status;
    }

    @Data
    public static class WarehouseCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String warehouseCode;
        @NotBlank
        private String warehouseName;
        private String managerName;
        private String contactInfo;
        private String address;
        private Integer status;
    }

    @Data
    public static class WarehouseUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String warehouseCode;
        @NotBlank
        private String warehouseName;
        private String managerName;
        private String contactInfo;
        private String address;
        private Integer status;
    }

    @Data
    public static class SupplierCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String supplierCode;
        @NotBlank
        private String supplierName;
        private String contactName;
        private String phone;
        private String email;
        private String bankName;
        private String accountNo;
        private Integer status;
    }

    @Data
    public static class SupplierUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String supplierCode;
        @NotBlank
        private String supplierName;
        private String contactName;
        private String phone;
        private String email;
        private String bankName;
        private String accountNo;
        private Integer status;
    }

    @Data
    public static class StockRowVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long productId;
        private String productName;
        private Long warehouseId;
        private String warehouseName;
        private Integer qty;
    }

    @Data
    public static class SupplierVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String supplierCode;
        private String supplierName;
        private String contactName;
        private String phone;
        private String email;
        private Integer status;
    }
}
