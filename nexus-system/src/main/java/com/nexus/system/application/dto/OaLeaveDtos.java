package com.nexus.system.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class OaLeaveDtos {
    private OaLeaveDtos() {
    }

    @Data
    public static class SubmitRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank(message = "请假类型不能为空")
        private String leaveType;
        @NotNull
        private LocalDateTime startTime;
        @NotNull
        private LocalDateTime endTime;
        @NotNull(message = "请假天数不能为空")
        @DecimalMin(value = "0.1", message = "请假天数必须大于0")
        private BigDecimal days;
        private String reason;
    }

    @Data
    public static class ApproveRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long leaveId;
        @NotNull(message = "审批状态不能为空")
        private Boolean approved;
        private String opinion;
    }
}
