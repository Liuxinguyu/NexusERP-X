package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.tenant.NexusTenantProperties;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysConfig;
import com.nexus.system.infrastructure.mapper.SysConfigMapper;
import org.springframework.cache.annotation.CacheEvict;
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

    public IPage<SysConfig> page(long current, long size, String configName, String configKey) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysConfig> qw = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getTenantId, tenantId)
                .eq(SysConfig::getDelFlag, 0);
        if (StringUtils.hasText(configName)) {
            qw.like(SysConfig::getConfigName, configName);
        }
        if (StringUtils.hasText(configKey)) {
            qw.like(SysConfig::getConfigKey, configKey);
        }
        qw.orderByAsc(SysConfig::getId);
        return sysConfigMapper.selectPage(new Page<>(current, size), qw);
    }

    public SysConfig getByIdForCurrentTenant(Long id) {
        Long tenantId = requireTenantId();
        SysConfig row = sysConfigMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())
                || (row.getDelFlag() != null && row.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        return row;
    }

    @CacheEvict(cacheNames = {"sysConfigValue", "sysConfigBool"}, allEntries = true)
    public Long create(SystemAdminDtos.ConfigCreateRequest req) {
        Long tenantId = requireTenantId();
        long exists = sysConfigMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getTenantId, tenantId)
                .eq(SysConfig::getConfigKey, req.getConfigKey())
                .eq(SysConfig::getDelFlag, 0));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "配置键名已存在");
        }
        SysConfig entity = new SysConfig();
        entity.setTenantId(tenantId);
        entity.setConfigName(req.getConfigName());
        entity.setConfigKey(req.getConfigKey());
        entity.setConfigValue(req.getConfigValue());
        entity.setConfigType(req.getConfigType() != null ? req.getConfigType() : "N");
        entity.setRemark(req.getRemark());
        entity.setDelFlag(0);
        sysConfigMapper.insert(entity);
        return entity.getId();
    }

    @CacheEvict(cacheNames = {"sysConfigValue", "sysConfigBool"}, allEntries = true)
    public void updateFull(Long id, SystemAdminDtos.ConfigUpdateRequest req) {
        Long currentTenantId = requireTenantId();
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        assertGlobalTenantWriteIsolation(exist.getTenantId(), currentTenantId);
        if (!currentTenantId.equals(exist.getTenantId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        if (req.getConfigName() != null) exist.setConfigName(req.getConfigName());
        if (req.getConfigValue() != null) exist.setConfigValue(req.getConfigValue());
        if (req.getConfigType() != null) exist.setConfigType(req.getConfigType());
        if (req.getRemark() != null) exist.setRemark(req.getRemark());
        sysConfigMapper.updateById(exist);
    }

    @CacheEvict(cacheNames = {"sysConfigValue", "sysConfigBool"}, allEntries = true)
    public void update(Long id, String configValue) {
        Long currentTenantId = requireTenantId();
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        assertGlobalTenantWriteIsolation(exist.getTenantId(), currentTenantId);
        if (!currentTenantId.equals(exist.getTenantId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        exist.setConfigValue(configValue);
        sysConfigMapper.updateById(exist);
    }

    @CacheEvict(cacheNames = {"sysConfigValue", "sysConfigBool"}, allEntries = true)
    public void delete(Long id) {
        Long currentTenantId = requireTenantId();
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        assertGlobalTenantWriteIsolation(exist.getTenantId(), currentTenantId);
        if (!currentTenantId.equals(exist.getTenantId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        sysConfigMapper.deleteById(id);
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

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }

    private static void assertGlobalTenantWriteIsolation(Long targetTenantId, Long currentTenantId) {
        if (targetTenantId != null && targetTenantId == 0L && currentTenantId != 0L) {
            throw new BusinessException(ResultCode.FORBIDDEN, "越权操作：普通租户绝对禁止修改或删除系统级全局配置！");
        }
    }
}
