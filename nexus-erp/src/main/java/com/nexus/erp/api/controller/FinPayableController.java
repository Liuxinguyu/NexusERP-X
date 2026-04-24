package com.nexus.erp.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.FinDtos;
import com.nexus.erp.application.service.FinPayableApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/erp/payables")
@RequiredArgsConstructor
@Validated
public class FinPayableController {

    private final FinPayableApplicationService service;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('erp:payable:list')")
    public Result<?> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) long size,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "dateFrom格式必须为yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "dateTo格式必须为yyyy-MM-dd") String dateTo) {
        return Result.ok(service.page(current, size, supplierId, status, dateFrom, dateTo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:payable:detail')")
    public Result<FinDtos.PayableVO> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @OpLog(module = "财务应付账款", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('erp:payable:add')")
    public Result<Long> create(@Valid @RequestBody FinDtos.PayableCreateRequest req) {
        return Result.ok(service.create(req));
    }

    @OpLog(module = "财务应付账款", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:payable:edit')")
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody FinDtos.PayableUpdateRequest req) {
        service.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "财务应付账款", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('erp:payable:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @OpLog(module = "财务应付账款", type = "付款")
    @PostMapping("/{id}/record")
    @PreAuthorize("@ss.hasPermi('erp:payable:record')")
    public Result<Void> recordPayment(@PathVariable Long id,
                                       @Valid @RequestBody FinDtos.PayableRecordCreate req) {
        service.recordPayment(id, req);
        return Result.ok();
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("@ss.hasPermi('erp:payable:detail')")
    public Result<List<FinDtos.PayableRecordVO>> listRecords(@PathVariable Long id) {
        return Result.ok(service.listRecords(id));
    }

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPermi('erp:payable:list')")
    public Result<FinDtos.PayableSummary> summary(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String month) {
        return Result.ok(service.summary(supplierId, month));
    }
}
