package com.nexus.system.application.service;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 校验请假类型是否在字典 oa_leave_type 的 value 集合内（走缓存）。
 */
@Component
public class OaLeaveDictValidator {

    private static final String DICT_OA_LEAVE_TYPE = "oa_leave_type";

    private final SysDictApplicationService dictApplicationService;

    public OaLeaveDictValidator(SysDictApplicationService dictApplicationService) {
        this.dictApplicationService = dictApplicationService;
    }

    public void validateLeaveType(Long tenantId, String leaveType) {
        if (leaveType == null || leaveType.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请假类型不能为空");
        }
        if (!dictApplicationService.cachedItemValues(tenantId, DICT_OA_LEAVE_TYPE).contains(leaveType.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请假类型不在系统字典允许范围内");
        }
    }
}
