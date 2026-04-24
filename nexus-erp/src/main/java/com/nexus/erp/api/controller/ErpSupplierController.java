package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.application.service.ErpSupplierApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/erp/suppliers")
@RequiredArgsConstructor
@Validated
public class ErpSupplierController {

    private final ErpSupplierApplicationService supplierApplicationService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('erp:supplier:list')")
    public Result<IPage<ErpFoundationDtos.SupplierVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String supplierName) {
        return Result.ok(supplierApplicationService.page(current, size, supplierName));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('erp:supplier:add')")
    @OpLog(module = "ERP供应商", type = "新增")
    public Result<Long> create(@Valid @RequestBody ErpFoundationDtos.SupplierCreateRequest req) {
        return Result.ok(supplierApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:supplier:edit')")
    @OpLog(module = "ERP供应商", type = "修改")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ErpFoundationDtos.SupplierUpdateRequest req) {
        supplierApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:supplier:delete')")
    @OpLog(module = "ERP供应商", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        supplierApplicationService.delete(id);
        return Result.ok();
    }
}
