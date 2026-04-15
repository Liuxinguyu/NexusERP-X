package com.nexus.erp.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FinDtos {

    private FinDtos() {}

    // ===================== 应收账款 =====================

    @Data
    public static class ReceivablePageQuery implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long customerId;
        private Integer status;
        private String dateFrom;
        private String dateTo;
    }

    @Data
    public static class ReceivableCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull private Long customerId;
        @NotBlank private String customerName;
        @NotNull private BigDecimal totalAmount;
        private String sourceType;
        private Long sourceId;
        private String invoiceNo;
        private LocalDate dueDate;
        private String remark;
    }

    @Data
    public static class ReceivableUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String invoiceNo;
        private LocalDate dueDate;
        private String remark;
    }

    @Data
    public static class ReceivableRecordCreate implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "收款金额不能为空")
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentAccount;
        private LocalDateTime paymentTime;
        private String receiptUrl;
        private String remark;
    }

    @Data
    public static class ReceivableVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String receivableNo;
        private String sourceType;
        private Long sourceId;
        private Long customerId;
        private String customerName;
        private BigDecimal totalAmount;
        private BigDecimal receivedAmount;
        private BigDecimal pendingAmount;
        private String invoiceNo;
        private LocalDate dueDate;
        private Integer status;
        private String remark;
        private LocalDateTime createTime;
    }

    @Data
    public static class ReceivableRecordVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String recordNo;
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentAccount;
        private LocalDateTime paymentTime;
        private Long handlerUserId;
        private String receiptUrl;
        private String remark;
        private LocalDateTime createTime;
    }

    @Data
    public static class ReceivableSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private BigDecimal totalReceivable;
        private BigDecimal totalReceived;
        private BigDecimal totalPending;
        private Long unpaidCount;
        private Long partPaidCount;
        private Long settledCount;
    }

    // ===================== 应付账款 =====================

    @Data
    public static class PayablePageQuery implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long supplierId;
        private Integer status;
        private String dateFrom;
        private String dateTo;
    }

    @Data
    public static class PayableCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull private Long supplierId;
        @NotBlank private String supplierName;
        @NotNull private BigDecimal totalAmount;
        private String sourceType;
        private Long sourceId;
        private String invoiceNo;
        private LocalDate dueDate;
        private String remark;
    }

    @Data
    public static class PayableUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String invoiceNo;
        private LocalDate dueDate;
        private String remark;
    }

    @Data
    public static class PayableRecordCreate implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "付款金额不能为空")
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentAccount;
        private LocalDateTime paymentTime;
        private String receiptUrl;
        private String remark;
    }

    @Data
    public static class PayableVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String payableNo;
        private String sourceType;
        private Long sourceId;
        private Long supplierId;
        private String supplierName;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal pendingAmount;
        private String invoiceNo;
        private LocalDate dueDate;
        private Integer status;
        private String remark;
        private LocalDateTime createTime;
    }

    @Data
    public static class PayableRecordVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String recordNo;
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentAccount;
        private LocalDateTime paymentTime;
        private Long handlerUserId;
        private String receiptUrl;
        private String remark;
        private LocalDateTime createTime;
    }

    @Data
    public static class PayableSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private BigDecimal totalPayable;
        private BigDecimal totalPaid;
        private BigDecimal totalPending;
        private Long unpaidCount;
        private Long partPaidCount;
        private Long settledCount;
    }
}
