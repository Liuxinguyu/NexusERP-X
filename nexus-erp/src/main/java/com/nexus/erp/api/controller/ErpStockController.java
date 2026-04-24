package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.application.service.ErpStockApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/erp/stocks")
@RequiredArgsConstructor
@Validated
public class ErpStockController {

    private final ErpStockApplicationService stockApplicationService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('erp:stock:list')")
    public Result<IPage<ErpFoundationDtos.StockRowVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId) {
        return Result.ok(stockApplicationService.page(current, size, productId, warehouseId));
    }
}
