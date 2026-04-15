package com.nexus.oa.workflow;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import lombok.Getter;

/**
 * 请假单状态（与 nexus-system 中 OaLeave 状态机思想一致：显式枚举 + 集中校验，避免散落 if-else）。
 */
@Getter
public enum OaLeaveRequestStatus {
    /** 草稿 */
    DRAFT(0),
    /** 待审批 */
    PENDING(1),
    /** 已通过 */
    APPROVED(2),
    /** 已驳回 */
    REJECTED(3);

    private final int code;

    OaLeaveRequestStatus(int code) {
        this.code = code;
    }

    public static OaLeaveRequestStatus fromCode(Integer code) {
        if (code == null) {
            return DRAFT;
        }
        for (OaLeaveRequestStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "未知的请假单状态: " + code);
    }
}
