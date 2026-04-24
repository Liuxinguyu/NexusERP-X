package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.domain.model.SysOperLog;
import com.nexus.system.infrastructure.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysOperLogQueryService {

    private final SysOperLogMapper sysOperLogMapper;

    public IPage<SysOperLog> page(long current, long size, String module, String username, Integer status) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<SysOperLog>()
                .eq(SysOperLog::getTenantId, tenantId);
        if (StringUtils.hasText(module)) {
            qw.like(SysOperLog::getModule, module);
        }
        if (StringUtils.hasText(username)) {
            qw.like(SysOperLog::getUsername, username);
        }
        if (status != null) {
            qw.eq(SysOperLog::getStatus, status);
        }
        qw.orderByDesc(SysOperLog::getCreateTime);
        return sysOperLogMapper.selectPage(new Page<>(current, size), qw);
    }

    public void cleanByTenant() {
        Long tenantId = requireTenantId();
        sysOperLogMapper.delete(new LambdaQueryWrapper<SysOperLog>()
                .eq(SysOperLog::getTenantId, tenantId));
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
