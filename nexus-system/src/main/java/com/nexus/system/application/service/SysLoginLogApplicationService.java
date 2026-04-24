package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.domain.model.SysLoginLog;
import com.nexus.system.infrastructure.mapper.SysLoginLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class SysLoginLogApplicationService {

    private final SysLoginLogMapper loginLogMapper;

    public SysLoginLogApplicationService(SysLoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public void recordSuccess(Long tenantId, String username, String ip, String userAgent, String msg) {
        insert(tenantId, username, 1, ip, userAgent, msg);
    }

    public void recordFailure(Long tenantId, String username, String ip, String userAgent, String msg) {
        insert(tenantId, username, 0, ip, userAgent, msg);
    }

    private void insert(Long tenantId, String username, int status, String ip, String userAgent, String msg) {
        SysLoginLog row = new SysLoginLog();
        row.setTenantId(tenantId);
        row.setUsername(username);
        row.setStatus(status);
        row.setIp(ip);
        row.setUserAgent(userAgent);
        row.setMsg(msg);
        row.setCreateTime(LocalDateTime.now());
        loginLogMapper.insert(row);
    }

    public IPage<SysLoginLog> page(long pageNum, long pageSize, String username, Integer status) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return loginLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysLoginLog>()
                        .eq(SysLoginLog::getTenantId, tenantId)
                        .like(StringUtils.hasText(username), SysLoginLog::getUsername, username)
                        .eq(status != null, SysLoginLog::getStatus, status)
                        .orderByDesc(SysLoginLog::getCreateTime));
    }

    public void cleanByTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        loginLogMapper.delete(new LambdaQueryWrapper<SysLoginLog>()
                .eq(SysLoginLog::getTenantId, tenantId));
    }
}
