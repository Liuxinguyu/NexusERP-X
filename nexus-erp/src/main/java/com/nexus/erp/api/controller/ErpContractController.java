package com.nexus.erp.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.application.service.ErpContractApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/erp/contracts")
@RequiredArgsConstructor
@Validated
public class ErpContractController {

    private final ErpContractApplicationService service;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('erp:contract:list')")
    public Result<?> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return Result.ok(service.page(current, size, customerId, status, dateFrom, dateTo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:contract:detail')")
    public Result<ErpDtos.ContractVO> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("@ss.hasPermi('erp:contract:detail')")
    public Result<List<ErpDtos.ContractItemVO>> listItems(@PathVariable Long id) {
        return Result.ok(service.listItems(id));
    }

    @OpLog(module = "CRM合同", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('erp:contract:add')")
    public Result<Long> create(@Valid @RequestBody ErpDtos.ContractCreateRequest req) {
        return Result.ok(service.create(req));
    }

    @OpLog(module = "CRM合同", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:contract:edit')")
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody ErpDtos.ContractUpdateRequest req) {
        service.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "CRM合同", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:contract:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
