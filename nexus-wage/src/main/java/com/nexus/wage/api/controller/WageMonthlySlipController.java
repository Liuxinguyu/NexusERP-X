package com.nexus.wage.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

@RestController
@RequestMapping("/api/v1/wage/monthly-slips")
@RequiredArgsConstructor
@Validated
public class WageMonthlySlipController {

    private final WageMonthlySlipApplicationService wageMonthlySlipApplicationService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('wage:slip:list')")
    public Result<IPage<WageDtos.MonthlySlipView>> list(
            @RequestParam(required = false) String belongMonth,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        IPage<WageMonthlySlip> page = wageMonthlySlipApplicationService.list(current, size, belongMonth);
        IPage<WageDtos.MonthlySlipView> viewPage = page.convert(WageMonthlySlipController::toView);
        return Result.ok(viewPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('wage:slip:list')")
    public Result<WageDtos.MonthlySlipView> get(@PathVariable Long id) {
        return Result.ok(toView(wageMonthlySlipApplicationService.getById(id)));
    }

    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermi('wage:slip:generate')")
    @OpLog(module = "薪酬月工资", type = "一键生成")
    public Result<Integer> generate(@Valid @RequestBody WageDtos.GenerateMonthlyRequest req) {
        return Result.ok(wageMonthlySlipApplicationService.generateMonthly(req));
    }

    @PutMapping("/{id}/adjust")
    @PreAuthorize("@ss.hasPermi('wage:slip:edit')")
    @OpLog(module = "薪酬月工资", type = "调整工资")
    public Result<Void> adjust(@PathVariable Long id, @Valid @RequestBody WageDtos.AdjustSlipRequest req) {
        wageMonthlySlipApplicationService.adjust(id, req);
        return Result.ok();
    }

    @PostMapping("/confirm-pay")
    @PreAuthorize("@ss.hasPermi('wage:slip:confirm')")
    @OpLog(module = "薪酬月工资", type = "确认发放")
    public Result<Integer> confirmPay(@Valid @RequestBody WageDtos.ConfirmPayRequest req) {
        return Result.ok(wageMonthlySlipApplicationService.confirmPay(req));
    }

    private static WageDtos.MonthlySlipView toView(WageMonthlySlip s) {
        WageDtos.MonthlySlipView v = new WageDtos.MonthlySlipView();
        v.setId(s.getId());
        v.setBelongMonth(s.getBelongMonth());
        v.setEmployeeId(s.getEmployeeId());
        v.setBaseSalary(s.getBaseSalary());
        v.setSubsidyTotal(s.getSubsidyTotal());
        v.setDeductionTotal(s.getDeductionTotal());
        v.setNetPay(s.getNetPay());
        v.setStatus(s.getStatus());
        return v;
    }
}
