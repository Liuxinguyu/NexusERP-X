package com.nexus.oa.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class OaDtos {
    private OaDtos() {
    }

    @Data
    public static class EmployeeCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String empNo;
        @NotBlank
        private String name;
        private String dept;
        private String position;
        private LocalDate hireDate;
        private String phone;
        private Integer status;
        private Long userId;
        private Long directLeaderUserId;
    }

    @Data
    public static class EmployeeUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String empNo;
        @NotBlank
        private String name;
        private String dept;
        private String position;
        private LocalDate hireDate;
        private String phone;
        private Integer status;
        private Long userId;
        private Long directLeaderUserId;
    }

    @Data
    public static class LeaveRequestCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank(message = "请假类型不能为空")
        private String leaveType;
        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;
        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;
        @NotNull(message = "请假天数不能为空")
        @DecimalMin(value = "0.5", message = "请假天数最少为0.5天")
        private BigDecimal leaveDays;
        private String reason;
    }

    @Data
    public static class LeaveRequestUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank(message = "请假类型不能为空")
        private String leaveType;
        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startTime;
        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endTime;
        @NotNull(message = "请假天数不能为空")
        @DecimalMin(value = "0.5", message = "请假天数最少为0.5天")
        private BigDecimal leaveDays;
        private String reason;
    }

    @Data
    public static class LeaveApproveRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "审批结果不能为空")
        private Boolean approved;
        private String opinion;
    }

    // ===================== 考勤规则 =====================

    @Data
    public static class AttendanceRuleCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank private String ruleName;
        @NotBlank private String checkInStart;  // "HH:mm"
        @NotBlank private String checkInEnd;
        @NotBlank private String checkOutStart;
        @NotBlank private String checkOutEnd;
        private String remark;
    }

    @Data
    public static class AttendanceRuleVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String ruleName;
        private String checkInStart;
        private String checkInEnd;
        private String checkOutStart;
        private String checkOutEnd;
        private Integer isEnable;
        private String remark;
    }

    // ===================== 考勤打卡 =====================

    @Data
    public static class CheckInRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** in=上班打卡 out=下班打卡 */
        @NotBlank private String type;
        private Integer isOuter;
        private String outerAddress;
        private String outerReason;
    }

    @Data
    public static class AttendanceRecordVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long userId;
        private String userName;
        private String checkDate;
        private String checkInTime;
        private String checkOutTime;
        private Integer workMinutes;
        private Integer status;
        private Integer isOuter;
        private String outerAddress;
        private String outerReason;
        private String remark;
    }

    @Data
    public static class TodayStatusVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Boolean checkedIn;     // 已上班打卡
        private Boolean checkedOut;     // 已下班打卡
        private String checkInTime;
        private String checkOutTime;
        private Integer status;         // 当前考勤状态
        private String statusLabel;
    }

    @Data
    public static class AttendanceStatisticsVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer presentDays;      // 出勤天数
        private Integer lateDays;         // 迟到次数
        private Integer earlyLeaveDays;    // 早退次数
        private Integer absentDays;        // 旷工天数
        private Integer missingCardDays;   // 缺卡次数
        private Integer overtimeHours;     // 加班时长（小时）
    }

    // ===================== 请假（新表） =====================

    @Data
    public static class LeaveCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank private String leaveType;
        @NotBlank private String startDate;   // "yyyy-MM-dd"
        @NotBlank private String endDate;
        @NotNull private BigDecimal leaveDays;
        private String reason;
    }

    @Data
    public static class LeaveVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String leaveNo;
        private Long userId;
        private String userName;
        private String leaveType;
        private String startDate;
        private String endDate;
        private BigDecimal leaveDays;
        private String reason;
        private Integer status;
        private String statusLabel;
        private Long approverUserId;
        private String approverOpinion;
        private String approverTime;
        private String createTime;
    }

    // ===================== 加班 =====================

    @Data
    public static class OvertimeCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank private String startTime;   // "yyyy-MM-dd HH:mm:ss"
        @NotBlank private String endTime;
        @NotNull private BigDecimal hours;
        private String reason;
    }

    @Data
    public static class OvertimeVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String overtimeNo;
        private Long userId;
        private String userName;
        private String startTime;
        private String endTime;
        private BigDecimal hours;
        private String reason;
        private Integer status;
        private String statusLabel;
        private Long approverUserId;
        private String approverOpinion;
        private String approverTime;
        private String createTime;
    }
}
