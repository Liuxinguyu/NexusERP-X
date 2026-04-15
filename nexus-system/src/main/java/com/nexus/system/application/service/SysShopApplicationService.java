package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysShop;
import com.nexus.system.infrastructure.mapper.SysShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysShopApplicationService {

    private final SysShopMapper shopMapper;

    public IPage<SysShop> page(long current, long size, String shopName) {
        Page<SysShop> p = new Page<>(current, size);
        LambdaQueryWrapper<SysShop> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(shopName), SysShop::getShopName, shopName);
        w.orderByDesc(SysShop::getId);
        return shopMapper.selectPage(p, w);
    }

    public List<SystemAdminDtos.ShopOption> listOptions() {
        List<SysShop> shops = shopMapper.selectList(new LambdaQueryWrapper<SysShop>()
                .eq(SysShop::getDelFlag, 0)
                .orderByAsc(SysShop::getId));
        return shops.stream().map(s -> {
            SystemAdminDtos.ShopOption o = new SystemAdminDtos.ShopOption();
            o.setId(s.getId());
            o.setShopName(s.getShopName());
            return o;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAdminDtos.ShopCreateRequest req) {
        SysShop s = new SysShop();
        s.setOrgId(req.getOrgId());
        s.setShopName(req.getShopName());
        s.setShopType(req.getShopType());
        s.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        shopMapper.insert(s);
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAdminDtos.ShopUpdateRequest req) {
        SysShop exist = shopMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        exist.setOrgId(req.getOrgId());
        exist.setShopName(req.getShopName());
        exist.setShopType(req.getShopType());
        shopMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, SystemAdminDtos.ShopStatusRequest req) {
        SysShop exist = shopMapper.selectById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        exist.setStatus(req.getStatus());
        shopMapper.updateById(exist);
    }
}
