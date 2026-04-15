package com.nexus.common.audit;

import lombok.Builder;
import lombok.Value;

/**
 * 操作日志落库载荷（由 {@link com.nexus.common.audit.OpLogAspect} 组装）。
 */
@Value
@Builder
public class OperLogRecord {
    Long tenantId;
    Long userId;
    String username;
    String module;
    String operType;
    String operUrl;
    String operMethod;
    String operIp;
    String requestParam;
    String responseData;
    int status;
    String errorMsg;
    long costTime;
}
