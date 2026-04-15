package com.nexus.erp.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.FinDtos;
import com.nexus.erp.application.service.FinPayableApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public Result<?> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        FinDtos.PayablePageQuery query = new FinDtos.PayablePageQuery();
        query.setSupplierId(supplierId);
        query.setStatus(status);
        query.setDateFrom(dateFrom);
        query.setDateTo(dateTo);
        return Result.ok(service.page(current, size, query));
    }

    @GetMapping("/{id}")
    public Result<FinDtos.PayableVO> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @OpLog(module = "财务应付账款", type = "新增")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FinDtos.PayableCreateRequest req) {
        return Result.ok(service.create(req));
    }

    @OpLog(module = "财务应付账款", type = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody FinDtos.PayableUpdateRequest req) {
        service.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "财务应付账款", type = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @OpLog(module = "财务应付账款", type = "付款")
    @PostMapping("/{id}/record")
    public Result<Void> recordPayment(@PathVariable Long id,
                                       @Valid @RequestBody FinDtos.PayableRecordCreate req) {
        service.recordPayment(id, req);
        return Result.ok();
    }

    @GetMapping("/{id}/records")
    public Result<List<FinDtos.PayableRecordVO>> listRecords(@PathVariable Long id) {
        return Result.ok(service.listRecords(id));
    }

    @GetMapping("/summary")
    public Result<FinDtos.PayableSummary> summary(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String month) {
        return Result.ok(service.summary(supplierId, month));
    }
}
