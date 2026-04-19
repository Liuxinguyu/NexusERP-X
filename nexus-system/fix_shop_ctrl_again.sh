#!/bin/bash
set -e

# Update SysShopApplicationService
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/service/SysShopApplicationService.java
package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysShop;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysShopMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysShopApplicationService {

    private final SysShopMapper shopMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;

    public SysShopApplicationService(SysShopMapper shopMapper, SysUserShopRoleMapper userShopRoleMapper) {
        this.shopMapper = shopMapper;
        this.userShopRoleMapper = userShopRoleMapper;
    }

    public IPage<SysShop> page(long current, long size, String shopName) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysShop> wrapper = new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getTenantId, tenantId)
                .eq(SysShop::getDelFlag, 0)
                .like(StringUtils.hasText(shopName), SysShop::getShopName, shopName)
                .orderByDesc(SysShop::getId);
        return shopMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<SystemAdminDtos.ShopOption> listOptions() {
        Long tenantId = requireTenantId();
        return shopMapper.selectList(new LambdaQueryWrapper<SysShop>()
                        .eq(SysShop::getTenantId, tenantId)
                        .eq(SysShop::getStatus, 1)
                        .eq(SysShop::getDelFlag, 0)
                        .select(SysShop::getId, SysShop::getShopName)
                        .orderByDesc(SysShop::getId))
                .stream()
                .map(s -> {
                    SystemAdminDtos.ShopOption option = new SystemAdminDtos.ShopOption();
                    option.setId(s.getId());
                    option.setShopName(s.getShopName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    public SysShop getById(Long id) {
        Long tenantId = requireTenantId();
        SysShop shop = shopMapper.selectById(id);
        if (shop == null || !Objects.equals(shop.getTenantId(), tenantId) || shop.getDelFlag() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        return shop;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.ShopCreateRequest req) {
        Long tenantId = requireTenantId();
        boolean exists = shopMapper.exists(new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getTenantId, tenantId)
                .eq(SysShop::getShopName, req.getShopName())
                .eq(SysShop::getDelFlag, 0));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "店铺名称已存在");
        }

        SysShop shop = new SysShop();
        shop.setTenantId(tenantId);
        shop.setShopName(req.getShopName());
        shop.setOrgId(req.getOrgId());
        shop.setShopType(req.getShopType() != null ? req.getShopType() : 1);
        shop.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        shop.setCreateTime(LocalDateTime.now());
        shop.setUpdateTime(LocalDateTime.now());
        shop.setCreateBy(TenantContext.getUserId());
        shop.setUpdateBy(TenantContext.getUserId());
        shop.setDelFlag(0);

        shopMapper.insert(shop);
        return shop.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.ShopUpdateRequest req) {
        Long tenantId = requireTenantId();
        SysShop shop = getById(id);

        if (!Objects.equals(shop.getShopName(), req.getShopName())) {
            boolean exists = shopMapper.exists(new LambdaQueryWrapper<SysShop>()
                    .eq(SysShop::getTenantId, tenantId)
                    .eq(SysShop::getShopName, req.getShopName())
                    .eq(SysShop::getDelFlag, 0));
            if (exists) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "店铺名称已存在");
            }
        }

        shop.setShopName(req.getShopName());
        shop.setOrgId(req.getOrgId());
        shop.setShopType(req.getShopType() != null ? req.getShopType() : 1);
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(TenantContext.getUserId());

        shopMapper.updateById(shop);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, SystemAdminDtos.ShopStatusRequest req) {
        SysShop shop = getById(id);
        shop.setStatus(req.getStatus());
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(TenantContext.getUserId());
        shopMapper.updateById(shop);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysShop shop = getById(id);
        
        boolean hasUsers = userShopRoleMapper.exists(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getShopId, id));
        if (hasUsers) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该店铺已有用户挂载，无法删除");
        }
        
        shop.setDelFlag(1);
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(TenantContext.getUserId());
        shopMapper.updateById(shop);
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
INNER_EOF

# Replace SysShopController endpoints inside it
