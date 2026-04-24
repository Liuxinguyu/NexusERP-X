package com.nexus.erp.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @Min(value = 0, message = "状态值非法")
        @Max(value = 1, message = "状态值非法")
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
        @DecimalMin(value = "0", inclusive = true, message = "信用额度必须大于等于0")
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
        @DecimalMin(value = "0", inclusive = true, message = "信用额度必须大于等于0")
        private BigDecimal creditLimit;
    }

    // ===================== 商机 =====================

    @Data
    public static class OpportunityCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull private Long customerId;
        @NotBlank private String opportunityName;
        @DecimalMin(value = "0", inclusive = true, message = "商机金额必须大于等于0")
        private BigDecimal amount;
        private String stage;
        @Min(value = 0, message = "概率范围必须在0-100")
        @Max(value = 100, message = "概率范围必须在0-100")
        private Integer probability;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "预计成交日期格式必须为yyyy-MM-dd")
        private String expectCloseDate;
        private Long ownerUserId;
        private String remark;
    }

    @Data
    public static class OpportunityUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String opportunityName;
        @DecimalMin(value = "0", inclusive = true, message = "商机金额必须大于等于0")
        private BigDecimal amount;
        private String stage;
        @Min(value = 0, message = "概率范围必须在0-100")
        @Max(value = 100, message = "概率范围必须在0-100")
        private Integer probability;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "预计成交日期格式必须为yyyy-MM-dd")
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
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "签订日期格式必须为yyyy-MM-dd")
        private String signDate;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式必须为yyyy-MM-dd")
        private String startDate;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为yyyy-MM-dd")
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
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "签订日期格式必须为yyyy-MM-dd")
        private String signDate;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式必须为yyyy-MM-dd")
        private String startDate;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为yyyy-MM-dd")
        private String endDate;
        @DecimalMin(value = "0", inclusive = true, message = "合同金额必须大于等于0")
        private BigDecimal amount;
        private String signedBy;
        private String attachmentUrls;
        @Min(value = 1, message = "合同状态非法")
        @Max(value = 3, message = "合同状态非法")
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
