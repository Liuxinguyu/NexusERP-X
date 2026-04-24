package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemDictDtos;
import com.nexus.system.domain.model.SysDictItem;
import com.nexus.system.domain.model.SysDictType;
import com.nexus.system.infrastructure.mapper.SysDictItemMapper;
import com.nexus.system.infrastructure.mapper.SysDictTypeMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysDictApplicationService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;
    private final CacheManager cacheManager;

    public SysDictApplicationService(SysDictTypeMapper dictTypeMapper, SysDictItemMapper dictItemMapper, CacheManager cacheManager) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.cacheManager = cacheManager;
    }

    public List<SysDictType> listTypes() {
        Long tenantId = requireTenantId();
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getTenantId, tenantId)
                .eq(SysDictType::getDelFlag, 0)
                .orderByAsc(SysDictType::getDictType));
    }

    public IPage<SysDictType> pageTypes(long current, long size, String dictName, String dictType) {
        Long tenantId = requireTenantId();
        return dictTypeMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysDictType>()
                        .eq(SysDictType::getTenantId, tenantId)
                        .eq(SysDictType::getDelFlag, 0)
                        .like(org.springframework.util.StringUtils.hasText(dictName), SysDictType::getDictName, dictName)
                        .like(org.springframework.util.StringUtils.hasText(dictType), SysDictType::getDictType, dictType)
                        .orderByDesc(SysDictType::getId));
    }

    public IPage<SysDictItem> pageItems(long current, long size, String dictType, String label) {
        Long tenantId = requireTenantId();
        return dictItemMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getTenantId, tenantId)
                        .eq(SysDictItem::getDelFlag, 0)
                        .eq(org.springframework.util.StringUtils.hasText(dictType), SysDictItem::getDictType, dictType)
                        .like(org.springframework.util.StringUtils.hasText(label), SysDictItem::getLabel, label)
                        .orderByAsc(SysDictItem::getSort)
                        .orderByDesc(SysDictItem::getId));
    }

    public SysDictType getType(Long id) {
        Long tenantId = requireTenantId();
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null || !Objects.equals(type.getTenantId(), tenantId) || Objects.equals(type.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        return type;
    }

    public Long createType(SystemDictDtos.DictTypeCreateRequest req) {
        Long tenantId = requireTenantId();
        boolean exists = dictTypeMapper.exists(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getTenantId, tenantId)
                .eq(SysDictType::getDictType, req.getDictType())
                .eq(SysDictType::getDelFlag, 0));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "字典类型已存在");
        }

        SysDictType type = new SysDictType();
        type.setTenantId(tenantId);
        type.setDictName(req.getDictName());
        type.setDictType(req.getDictType());
        type.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        type.setRemark(req.getRemark());
        type.setCreateTime(LocalDateTime.now());
        type.setUpdateTime(LocalDateTime.now());
        type.setCreateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        type.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        type.setDelFlag(0);

        dictTypeMapper.insert(type);
        return type.getId();
    }

    public void updateType(Long id, SystemDictDtos.DictTypeUpdateRequest req) {
        Long currentTenantId = requireTenantId();
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null || Objects.equals(type.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        assertGlobalTenantWriteIsolation(type.getTenantId(), currentTenantId);
        if (!Objects.equals(type.getTenantId(), currentTenantId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        
        if (!Objects.equals(type.getDictType(), req.getDictType())) {
            boolean exists = dictTypeMapper.exists(new LambdaQueryWrapper<SysDictType>()
                    .eq(SysDictType::getTenantId, type.getTenantId())
                    .eq(SysDictType::getDictType, req.getDictType())
                    .eq(SysDictType::getDelFlag, 0));
            if (exists) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "字典类型已存在");
            }
        }

        type.setDictName(req.getDictName());
        type.setDictType(req.getDictType());
        if (req.getStatus() != null) type.setStatus(req.getStatus());
        type.setRemark(req.getRemark());
        type.setUpdateTime(LocalDateTime.now());
        type.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());

        dictTypeMapper.updateById(type);
    }

    public void deleteType(Long id) {
        Long currentTenantId = requireTenantId();
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null || Objects.equals(type.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        assertGlobalTenantWriteIsolation(type.getTenantId(), currentTenantId);
        if (!Objects.equals(type.getTenantId(), currentTenantId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        boolean hasItems = dictItemMapper.exists(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getTenantId, type.getTenantId())
                .eq(SysDictItem::getDictType, type.getDictType())
                .eq(SysDictItem::getDelFlag, 0));
        if (hasItems) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该字典类型下包含字典数据，不能删除");
        }

        type.setDelFlag(1);
        type.setUpdateTime(LocalDateTime.now());
        type.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        dictTypeMapper.updateById(type);
    }

    public List<SysDictItem> listItemsByType(String dictType) {
        Long tenantId = requireTenantId();
        return dictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getTenantId, tenantId)
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getDelFlag, 0)
                .orderByAsc(SysDictItem::getSort)
                .orderByAsc(SysDictItem::getId));
    }

    public SysDictItem getItem(Long id) {
        Long tenantId = requireTenantId();
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null || !Objects.equals(item.getTenantId(), tenantId) || Objects.equals(item.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        return item;
    }

    public Long createItem(SystemDictDtos.DictItemCreateRequest req) {
        Long tenantId = requireTenantId();
        SysDictItem item = new SysDictItem();
        item.setTenantId(tenantId);
        item.setDictType(req.getDictType());
        item.setLabel(req.getLabel());
        item.setValue(req.getValue());
        item.setSort(req.getSort() != null ? req.getSort() : 0);
        item.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        item.setRemark(req.getRemark());
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setCreateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        item.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        item.setDelFlag(0);

        dictItemMapper.insert(item);
        evictCache(tenantId, req.getDictType());
        return item.getId();
    }

    public void updateItem(Long id, SystemDictDtos.DictItemUpdateRequest req) {
        Long currentTenantId = requireTenantId();
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null || Objects.equals(item.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        assertGlobalTenantWriteIsolation(item.getTenantId(), currentTenantId);
        if (!Objects.equals(item.getTenantId(), currentTenantId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        item.setLabel(req.getLabel());
        item.setValue(req.getValue());
        if (req.getSort() != null) item.setSort(req.getSort());
        if (req.getStatus() != null) item.setStatus(req.getStatus());
        item.setRemark(req.getRemark());
        item.setUpdateTime(LocalDateTime.now());
        item.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());

        dictItemMapper.updateById(item);
        evictCache(item.getTenantId(), item.getDictType());
    }

    public void deleteItem(Long id) {
        Long currentTenantId = requireTenantId();
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null || Objects.equals(item.getDelFlag(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        assertGlobalTenantWriteIsolation(item.getTenantId(), currentTenantId);
        if (!Objects.equals(item.getTenantId(), currentTenantId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        item.setDelFlag(1);
        item.setUpdateTime(LocalDateTime.now());
        item.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        dictItemMapper.updateById(item);
        evictCache(item.getTenantId(), item.getDictType());
    }

    private void evictCache(Long tenantId, String dictType) {
        if (cacheManager != null) {
            var cache = cacheManager.getCache("dictItemValues");
            if (cache != null) {
                cache.evict(tenantId + ":" + dictType);
            }
        }
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

    private static void assertGlobalTenantWriteIsolation(Long targetTenantId, Long currentTenantId) {
        if (targetTenantId != null && targetTenantId == 0L && currentTenantId != 0L) {
            throw new BusinessException(ResultCode.FORBIDDEN, "越权操作：普通租户绝对禁止修改或删除系统级全局配置！");
        }
    }
}
