package com.nexus.wage.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public final class WageDtos {

    private WageDtos() {
    }

    @Data
    public static class ItemConfigCreateRequest {
        @NotBlank
        @Size(max = 128)
        private String itemName;
        /** 1 固定值 2 手动录入 */
        @NotNull(message = "计算方式不能为空")
        @Min(value = 1, message = "计算方式非法")
        @Max(value = 2, message = "计算方式非法")
        private Integer calcType;
        @NotNull(message = "默认金额不能为空")
        @DecimalMin(value = "0", message = "默认金额不能为负数")
        private BigDecimal defaultAmount;
        /** 1 基本工资 2 补贴 3 扣款 */
        @NotNull(message = "薪资项类型不能为空")
        @Min(value = 1, message = "薪资项类型非法")
        @Max(value = 3, message = "薪资项类型非法")
        private Integer itemKind;
    }

    @Data
    public static class ItemConfigUpdateRequest {
        @NotBlank
        @Size(max = 128)
        private String itemName;
        @NotNull(message = "计算方式不能为空")
        @Min(value = 1, message = "计算方式非法")
        @Max(value = 2, message = "计算方式非法")
        private Integer calcType;
        @NotNull(message = "默认金额不能为空")
        @DecimalMin(value = "0", message = "默认金额不能为负数")
        private BigDecimal defaultAmount;
        @NotNull(message = "薪资项类型不能为空")
        @Min(value = 1, message = "薪资项类型非法")
        @Max(value = 3, message = "薪资项类型非法")
        private Integer itemKind;
    }

    @Data
    public static class GenerateMonthlyRequest {
        /** 如 2024-10 */
        @NotBlank(message = "月份不能为空")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "月份格式非法，应为 YYYY-MM（01-12）")
        private String belongMonth;
        /** 为空则取当前租户下 OA 全部在职员工 */
        private List<Long> employeeIds;
    }

    @Data
    public static class AdjustSlipRequest {
        @NotNull
        @DecimalMin(value = "0", message = "基本工资不能为负数")
        private BigDecimal baseSalary;
        @NotNull
        @DecimalMin(value = "0", message = "补贴合计不能为负数")
        private BigDecimal subsidyTotal;
        @NotNull
        @DecimalMin(value = "0", message = "扣款合计不能为负数")
        private BigDecimal deductionTotal;
    }

    @Data
    public static class ConfirmPayRequest {
        @NotEmpty
        private List<Long> slipIds;
    }
}
