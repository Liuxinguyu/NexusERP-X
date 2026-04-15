package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysShopApplicationService;
import com.nexus.system.domain.model.SysShop;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/shops")
@RequiredArgsConstructor
@Validated
public class SysShopController {

    private final SysShopApplicationService shopApplicationService;

    @GetMapping("/page")
    public Result<IPage<SysShop>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String shopName) {
        return Result.ok(shopApplicationService.page(current, size, shopName));
    }

    @GetMapping("/options")
    public Result<List<SystemAdminDtos.ShopOption>> options() {
        return Result.ok(shopApplicationService.listOptions());
    }

    @OpLog(module = "店铺管理", type = "新增")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody SystemAdminDtos.ShopCreateRequest req) {
        return Result.ok(shopApplicationService.create(req));
    }

    @OpLog(module = "店铺管理", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.ShopUpdateRequest req) {
        shopApplicationService.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "店铺管理", type = "状态变更")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> status(@PathVariable Long id, @Valid @RequestBody SystemAdminDtos.ShopStatusRequest req) {
        shopApplicationService.updateStatus(id, req);
        return Result.ok();
    }
}
