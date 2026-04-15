package com.nexus.oa.workflow;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;

/**
 * 请假单状态迁移：与 system 模块中「极简请假」一致采用显式状态枚举 + 集中校验，避免长链 if-else。
 */
public final class LeaveRequestStateMachine {

    private LeaveRequestStateMachine() {
    }

    public static OaLeaveRequestStatus afterSubmit(OaLeaveRequestStatus current) {
        if (current != OaLeaveRequestStatus.DRAFT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交审批");
        }
        return OaLeaveRequestStatus.PENDING;
    }

    public static OaLeaveRequestStatus afterDecision(OaLeaveRequestStatus current, boolean approved) {
        if (current != OaLeaveRequestStatus.PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审批状态可审批");
        }
        return approved ? OaLeaveRequestStatus.APPROVED : OaLeaveRequestStatus.REJECTED;
    }
}
