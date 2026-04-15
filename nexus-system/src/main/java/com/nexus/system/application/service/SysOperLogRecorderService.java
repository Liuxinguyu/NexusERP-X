package com.nexus.system.application.service;

import com.nexus.common.audit.OperLogRecord;
import com.nexus.common.audit.OperLogRecorder;
import com.nexus.system.domain.model.SysOperLog;
import com.nexus.system.infrastructure.mapper.SysOperLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysOperLogRecorderService implements OperLogRecorder {

    private final SysOperLogMapper sysOperLogMapper;

    public SysOperLogRecorderService(SysOperLogMapper sysOperLogMapper) {
        this.sysOperLogMapper = sysOperLogMapper;
    }

    @Override
    public void record(OperLogRecord r) {
        SysOperLog e = new SysOperLog();
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
        sysOperLogMapper.insert(e);
    }
}
