package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.domain.model.SysDictItem;
import com.nexus.system.domain.model.SysDictType;
import com.nexus.system.infrastructure.mapper.SysDictItemMapper;
import com.nexus.system.infrastructure.mapper.SysDictTypeMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysDictApplicationService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    public SysDictApplicationService(SysDictTypeMapper dictTypeMapper, SysDictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    public List<SysDictType> listTypes() {
        Long tenantId = requireTenantId();
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getTenantId, tenantId)
                .eq(SysDictType::getDelFlag, 0)
                .orderByAsc(SysDictType::getDictType));
    }

    public List<SysDictItem> listItemsByType(String dictType) {
        Long tenantId = requireTenantId();
        return dictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getTenantId, tenantId)
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getDelFlag, 0)
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSort)
                .orderByAsc(SysDictItem::getId));
    }

    @Cacheable(cacheNames = "dictItemValues", key = "#tenantId + ':' + #dictType")
    public Set<String> cachedItemValues(Long tenantId, String dictType) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getTenantId, tenantId)
                        .eq(SysDictItem::getDictType, dictType)
                        .eq(SysDictItem::getDelFlag, 0)
                        .eq(SysDictItem::getStatus, 1))
                .stream()
                .map(SysDictItem::getValue)
                .collect(Collectors.toSet());
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
