package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.application.service.ErpWarehouseApplicationService;
import com.nexus.erp.domain.model.ErpWarehouse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/erp/warehouses")
@RequiredArgsConstructor
@Validated
public class ErpWarehouseController {

    private final ErpWarehouseApplicationService warehouseApplicationService;

    @GetMapping("/page")
    public Result<IPage<ErpWarehouse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String warehouseName) {
        return Result.ok(warehouseApplicationService.page(current, size, warehouseName));
    }

    @PostMapping
    @OpLog(module = "ERP仓库", type = "新增")
    public Result<Long> create(@Valid @RequestBody ErpFoundationDtos.WarehouseCreateRequest req) {
        return Result.ok(warehouseApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @OpLog(module = "ERP仓库", type = "修改")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ErpFoundationDtos.WarehouseUpdateRequest req) {
        warehouseApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "ERP仓库", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseApplicationService.delete(id);
        return Result.ok();
    }
}
