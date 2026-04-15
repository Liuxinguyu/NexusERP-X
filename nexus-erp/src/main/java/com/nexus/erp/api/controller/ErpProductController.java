package com.nexus.erp.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.common.core.page.PageQuery;
import com.nexus.common.core.page.PageResult;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.application.service.ErpProductApplicationService;
import com.nexus.erp.domain.model.ErpProduct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/erp/products")
@RequiredArgsConstructor
@Validated
public class ErpProductController {

    private final ErpProductApplicationService productApplicationService;

    @GetMapping("/page")
    public Result<PageResult<ErpProduct>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) String category) {
        return Result.ok(productApplicationService.page(pageQuery, name, category));
    }

    @OpLog(module = "产品管理", type = "新增")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ErpDtos.ProductCreateRequest req) {
        return Result.ok(productApplicationService.create(req));
    }

    @OpLog(module = "产品管理", type = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ErpDtos.ProductUpdateRequest req) {
        productApplicationService.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "产品管理", type = "状态变更")
    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @Valid @RequestBody ErpDtos.ProductStatusRequest req) {
        productApplicationService.updateStatus(id, req);
        return Result.ok();
    }
}
