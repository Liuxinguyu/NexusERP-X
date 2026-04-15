package com.nexus.system.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.WorkbenchDtos;
import com.nexus.system.application.service.WorkbenchService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/workbench")
@RequiredArgsConstructor
@Validated
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    @GetMapping("/dashboard")
    public Result<WorkbenchDtos.DashboardSummary> dashboard() {
        return Result.ok(workbenchService.dashboardSummary());
    }

    @GetMapping("/sales-chart")
    public Result<WorkbenchDtos.ChartData> salesChart() {
        return Result.ok(workbenchService.saleChart());
    }

    @GetMapping("/purchase-chart")
    public Result<WorkbenchDtos.ChartData> purchaseChart() {
        return Result.ok(workbenchService.purchaseChart());
    }

    @GetMapping("/top-products")
    public Result<List<WorkbenchDtos.TopProduct>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(workbenchService.topProducts(limit));
    }

    @GetMapping("/stock-alarms")
    public Result<List<WorkbenchDtos.StockAlarmItem>> stockAlarms(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(workbenchService.stockAlarmList(limit));
    }
}
