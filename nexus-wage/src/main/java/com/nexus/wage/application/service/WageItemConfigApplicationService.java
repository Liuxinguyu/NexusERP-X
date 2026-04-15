package com.nexus.wage.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.wage.application.dto.WageDtos;
import com.nexus.wage.domain.model.WageItemConfig;
import com.nexus.wage.infrastructure.mapper.WageItemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class WageItemConfigApplicationService {

    private final WageItemConfigMapper wageItemConfigMapper;

    public WageItemConfigApplicationService(WageItemConfigMapper wageItemConfigMapper) {
        this.wageItemConfigMapper = wageItemConfigMapper;
    }

    public List<WageItemConfig> listAll() {
        Long tenantId = requireTenantId();
        return wageItemConfigMapper.selectList(new LambdaQueryWrapper<WageItemConfig>()
                .eq(WageItemConfig::getTenantId, tenantId)
                .eq(WageItemConfig::getDelFlag, 0)
                .orderByAsc(WageItemConfig::getId));
    }

    public WageItemConfig getById(Long id) {
        Long tenantId = requireTenantId();
        WageItemConfig c = wageItemConfigMapper.selectById(id);
        if (c == null || !Objects.equals(c.getTenantId(), tenantId) || (c.getDelFlag() != null && c.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "薪资项不存在");
        }
        return c;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(WageDtos.ItemConfigCreateRequest req) {
        Long tenantId = requireTenantId();
        WageItemConfig c = new WageItemConfig();
        c.setTenantId(tenantId);
        c.setItemName(req.getItemName().trim());
        c.setCalcType(req.getCalcType());
        c.setDefaultAmount(req.getDefaultAmount());
        c.setItemKind(req.getItemKind());
        wageItemConfigMapper.insert(c);
        return c.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WageDtos.ItemConfigUpdateRequest req) {
        WageItemConfig exist = getById(id);
        exist.setItemName(req.getItemName().trim());
        exist.setCalcType(req.getCalcType());
        exist.setDefaultAmount(req.getDefaultAmount());
        exist.setItemKind(req.getItemKind());
        wageItemConfigMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WageItemConfig exist = getById(id);
        wageItemConfigMapper.deleteById(exist.getId());
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
