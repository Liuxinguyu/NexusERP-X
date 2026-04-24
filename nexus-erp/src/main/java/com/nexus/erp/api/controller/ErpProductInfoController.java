package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.application.service.ErpProductInfoApplicationService;
import com.nexus.erp.domain.model.ErpProductInfo;
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
@RequestMapping("/api/v1/erp/product-infos")
@RequiredArgsConstructor
@Validated
public class ErpProductInfoController {

    private final ErpProductInfoApplicationService productInfoApplicationService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('erp:product-info:list')")
    public Result<IPage<ErpProductInfo>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String productName) {
        return Result.ok(productInfoApplicationService.page(current, size, categoryId, productName));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('erp:product-info:add')")
    @OpLog(module = "ERP产品信息", type = "新增")
    public Result<Long> create(@Valid @RequestBody ErpFoundationDtos.ProductInfoCreateRequest req) {
        return Result.ok(productInfoApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:product-info:edit')")
    @OpLog(module = "ERP产品信息", type = "修改")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ErpFoundationDtos.ProductInfoUpdateRequest req) {
        productInfoApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:product-info:delete')")
    @OpLog(module = "ERP产品信息", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        productInfoApplicationService.delete(id);
        return Result.ok();
    }
}
