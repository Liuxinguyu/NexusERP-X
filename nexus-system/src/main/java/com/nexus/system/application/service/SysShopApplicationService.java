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
    private final SysOrgApplicationService sysOrgApplicationService;

    public SysShopApplicationService(SysShopMapper shopMapper,
                                     SysUserShopRoleMapper userShopRoleMapper,
                                     SysOrgApplicationService sysOrgApplicationService) {
        this.shopMapper = shopMapper;
        this.userShopRoleMapper = userShopRoleMapper;
        this.sysOrgApplicationService = sysOrgApplicationService;
    }

    public IPage<SysShop> page(long current, long size, String shopName, Long orgId) {
        Long tenantId = requireTenantId();
        List<Long> orgIds = orgId == null
                ? List.of()
                : sysOrgApplicationService.listSelfAndDescendantOrgIds(tenantId, orgId);

        if (orgId != null && orgIds.isEmpty()) {
            return new Page<>(current, size);
        }

        LambdaQueryWrapper<SysShop> wrapper = new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getTenantId, tenantId)
                .eq(SysShop::getDelFlag, 0)
                .like(StringUtils.hasText(shopName), SysShop::getShopName, shopName)
                .in(orgId != null && !orgIds.isEmpty(), SysShop::getOrgId, orgIds)
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
        if (shop == null || !Objects.equals(shop.getTenantId(), tenantId) || Objects.equals(shop.getDelFlag(), 1)) {
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
        shop.setCreateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        shop.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
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
        shop.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());

        shopMapper.updateById(shop);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, SystemAdminDtos.ShopStatusRequest req) {
        SysShop shop = getById(id);
        shop.setStatus(req.getStatus());
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
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
        
        // 逻辑删除前先污染唯一键，释放店铺唯一标识占用（当前为 shopName）
        shop.setShopName(shop.getShopName() + "_del_" + shop.getId());
        shop.setUpdateTime(LocalDateTime.now());
        shop.setUpdateBy(com.nexus.common.context.GatewayUserContext.getUserId());
        shopMapper.updateById(shop);
        shopMapper.deleteById(shop.getId());
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
