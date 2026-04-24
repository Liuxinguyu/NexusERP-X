package com.nexus.erp.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.application.service.ErpOpportunityApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/erp/opportunities")
@RequiredArgsConstructor
@Validated
public class ErpOpportunityController {

    private final ErpOpportunityApplicationService service;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('erp:opportunity:list')")
    public Result<?> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) Integer status) {
        return Result.ok(service.page(current, size, customerId, stage, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:opportunity:detail')")
    public Result<ErpDtos.OpportunityVO> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @OpLog(module = "CRM商机", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('erp:opportunity:add')")
    public Result<Long> create(@Valid @RequestBody ErpDtos.OpportunityCreateRequest req) {
        return Result.ok(service.create(req));
    }

    @OpLog(module = "CRM商机", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:opportunity:edit')")
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody ErpDtos.OpportunityUpdateRequest req) {
        service.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "CRM商机", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:opportunity:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @OpLog(module = "CRM商机", type = "推进阶段")
    @PutMapping("/{id}/stage")
    @PreAuthorize("@ss.hasPermi('erp:opportunity:advance')")
    public Result<Void> advanceStage(@PathVariable Long id,
                                       @RequestParam String stage) {
        service.advanceStage(id, stage);
        return Result.ok();
    }
}
