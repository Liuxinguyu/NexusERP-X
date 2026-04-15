package com.nexus.erp.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public final class ErpDtos {
    private ErpDtos() {
    }

    @Data
    public static class ProductCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long orgId;
        @NotBlank
        private String name;
        private String category;
        private String unit;
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "价格必须大于0")
        private BigDecimal price;
        private Integer status;
    }

    @Data
    public static class ProductUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long orgId;
        @NotBlank
        private String name;
        private String category;
        private String unit;
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "价格必须大于0")
        private BigDecimal price;
    }

    @Data
    public static class ProductStatusRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Integer status;
    }

    @Data
    public static class CustomerCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long orgId;
        @NotBlank
        private String name;
        private String contactName;
        private String contactPhone;
        private String level;
        private BigDecimal creditLimit;
    }

    @Data
    public static class CustomerUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long orgId;
        @NotBlank
        private String name;
        private String contactName;
        private String contactPhone;
        private String level;
        private BigDecimal creditLimit;
    }

    // ===================== 商机 =====================

    @Data
    public static class OpportunityCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull private Long customerId;
        @NotBlank private String opportunityName;
        private BigDecimal amount;
        private String stage;
        private Integer probability;
        private String expectCloseDate;
        private Long ownerUserId;
        private String remark;
    }

    @Data
    public static class OpportunityUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String opportunityName;
        private BigDecimal amount;
        private String stage;
        private Integer probability;
        private String expectCloseDate;
        private Long ownerUserId;
        private String remark;
    }

    @Data
    public static class OpportunityVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long customerId;
        private String customerName;
        private String opportunityName;
        private BigDecimal amount;
        private String stage;
        private Integer probability;
        private String expectCloseDate;
        private Long ownerUserId;
        private String ownerUserName;
        private String remark;
        private Integer status;
        private String createTime;
    }

    // ===================== 合同 =====================

    @Data
    public static class ContractCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull private Long customerId;
        @NotBlank private String contractName;
        private Long opportunityId;
        private String signDate;
        private String startDate;
        private String endDate;
        @NotNull @DecimalMin(value = "0", inclusive = true) private BigDecimal amount;
        private String signedBy;
        private String attachmentUrls;
        private String remark;
    }

    @Data
    public static class ContractUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String contractName;
        private Long opportunityId;
        private String signDate;
        private String startDate;
        private String endDate;
        private BigDecimal amount;
        private String signedBy;
        private String attachmentUrls;
        private Integer status;
        private String remark;
    }

    @Data
    public static class ContractVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String contractNo;
        private String contractName;
        private Long customerId;
        private String customerName;
        private Long opportunityId;
        private String opportunityName;
        private String signDate;
        private String startDate;
        private String endDate;
        private BigDecimal amount;
        private String signedBy;
        private String attachmentUrls;
        private Integer status;
        private String remark;
        private String createTime;
    }

    @Data
    public static class ContractItemVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
