package com.nexus.wage.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.wage.application.dto.WageDtos;
import com.nexus.wage.application.service.WageMonthlySlipApplicationService;
import com.nexus.wage.domain.model.WageMonthlySlip;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wage/monthly-slips")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class WageMonthlySlipController {

    private final WageMonthlySlipApplicationService wageMonthlySlipApplicationService;

    @GetMapping
    public Result<List<WageMonthlySlip>> list(@RequestParam(required = false) String belongMonth) {
        return Result.ok(wageMonthlySlipApplicationService.list(belongMonth));
    }

    @GetMapping("/{id}")
    public Result<WageMonthlySlip> get(@PathVariable Long id) {
        return Result.ok(wageMonthlySlipApplicationService.getById(id));
    }

    @PostMapping("/generate")
    @OpLog(module = "薪酬月工资", type = "一键生成")
    public Result<Integer> generate(@Valid @RequestBody WageDtos.GenerateMonthlyRequest req) {
        return Result.ok(wageMonthlySlipApplicationService.generateMonthly(req));
    }

    @PutMapping("/{id}/adjust")
    @OpLog(module = "薪酬月工资", type = "调整工资")
    public Result<Void> adjust(@PathVariable Long id, @Valid @RequestBody WageDtos.AdjustSlipRequest req) {
        wageMonthlySlipApplicationService.adjust(id, req);
        return Result.ok();
    }

    @PostMapping("/confirm-pay")
    @OpLog(module = "薪酬月工资", type = "确认发放")
    public Result<Integer> confirmPay(@Valid @RequestBody WageDtos.ConfirmPayRequest req) {
        return Result.ok(wageMonthlySlipApplicationService.confirmPay(req));
    }
}
