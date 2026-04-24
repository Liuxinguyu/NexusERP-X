package com.nexus.erp.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpFoundationDtos;
import com.nexus.erp.application.service.ErpProductCategoryApplicationService;
import com.nexus.erp.domain.model.ErpProductCategory;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/erp/product-categories")
@RequiredArgsConstructor
@Validated
public class ErpProductCategoryController {

    private final ErpProductCategoryApplicationService categoryApplicationService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('erp:product-category:list')")
    public Result<List<ErpProductCategory>> list() {
        return Result.ok(categoryApplicationService.listAll());
    }

    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermi('erp:product-category:list')")
    public Result<List<ErpFoundationDtos.ProductCategoryTreeNode>> tree() {
        return Result.ok(categoryApplicationService.buildTree());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('erp:product-category:add')")
    @OpLog(module = "ERP产品分类", type = "新增")
    public Result<Long> create(@Valid @RequestBody ErpFoundationDtos.ProductCategoryCreateRequest req) {
        return Result.ok(categoryApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:product-category:edit')")
    @OpLog(module = "ERP产品分类", type = "修改")
    public Result<Void> update(@PathVariable Long id,
                             @Valid @RequestBody ErpFoundationDtos.ProductCategoryUpdateRequest req) {
        categoryApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:product-category:delete')")
    @OpLog(module = "ERP产品分类", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        categoryApplicationService.delete(id);
        return Result.ok();
    }
}
