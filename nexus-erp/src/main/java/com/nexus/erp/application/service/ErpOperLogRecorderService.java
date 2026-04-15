package com.nexus.erp.application.service;

import com.nexus.common.audit.OperLogRecord;
import com.nexus.common.audit.OperLogRecorder;
import com.nexus.erp.domain.model.ErpSysOperLog;
import com.nexus.erp.infrastructure.mapper.ErpSysOperLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ErpOperLogRecorderService implements OperLogRecorder {

    private final ErpSysOperLogMapper erpSysOperLogMapper;

    public ErpOperLogRecorderService(ErpSysOperLogMapper erpSysOperLogMapper) {
        this.erpSysOperLogMapper = erpSysOperLogMapper;
    }

    @Override
    public void record(OperLogRecord r) {
        ErpSysOperLog e = new ErpSysOperLog();
        e.setTenantId(r.getTenantId());
        e.setUserId(r.getUserId());
        e.setUsername(r.getUsername());
        e.setModule(r.getModule());
        e.setOperType(r.getOperType());
        e.setOperUrl(r.getOperUrl());
        e.setOperMethod(r.getOperMethod());
        e.setOperIp(r.getOperIp());
        e.setRequestParam(r.getRequestParam());
        e.setResponseData(r.getResponseData());
        e.setStatus(r.getStatus());
        e.setErrorMsg(r.getErrorMsg());
        e.setCostTime(r.getCostTime());
        e.setCreateTime(LocalDateTime.now());
        erpSysOperLogMapper.insert(e);
    }
}
