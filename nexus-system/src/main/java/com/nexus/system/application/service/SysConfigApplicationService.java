package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.tenant.NexusTenantProperties;
import com.nexus.system.domain.model.SysConfig;
import com.nexus.system.infrastructure.mapper.SysConfigMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysConfigApplicationService {

    private final SysConfigMapper sysConfigMapper;
    private final NexusTenantProperties tenantProperties;

    public SysConfigApplicationService(SysConfigMapper sysConfigMapper, NexusTenantProperties tenantProperties) {
        this.sysConfigMapper = sysConfigMapper;
        this.tenantProperties = tenantProperties;
    }

    /**
     * 按 key 读取配置值（优先当前租户，无租户上下文时使用默认租户）。
     */
    @Cacheable(cacheNames = "sysConfigValue", key = "#tenantId + ':' + #configKey")
    public String getConfigValue(Long tenantId, String configKey) {
        return fetchValue(tenantId, configKey);
    }

    public String getConfigValue(String configKey) {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            tid = defaultTenantId();
        }
        return getConfigValue(tid, configKey);
    }

    @Cacheable(cacheNames = "sysConfigBool", key = "#tenantId + ':' + #configKey")
    public boolean getBoolConfig(Long tenantId, String configKey) {
        return parseBool(fetchValue(tenantId, configKey));
    }

    public boolean getBoolConfig(String configKey) {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            tid = defaultTenantId();
        }
        return getBoolConfig(tid, configKey);
    }

    private String fetchValue(Long tenantId, String configKey) {
        if (tenantId == null) {
            tenantId = defaultTenantId();
        }
        SysConfig row = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getTenantId, tenantId)
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getDelFlag, 0)
                .last("LIMIT 1"));
        return row == null ? null : row.getConfigValue();
    }

    private Long defaultTenantId() {
        Long d = tenantProperties.getDefaultTenantId();
        return d != null ? d : 1L;
    }

    private static boolean parseBool(String v) {
        if (!StringUtils.hasText(v)) {
            return false;
        }
        String s = v.trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s);
    }
}
