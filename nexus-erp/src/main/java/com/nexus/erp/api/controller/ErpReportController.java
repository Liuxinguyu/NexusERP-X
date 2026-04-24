package com.nexus.erp.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.service.ErpReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/erp/reports")
@RequiredArgsConstructor
@Validated
public class ErpReportController {

    private final ErpReportService reportService;

    /** 月度销售汇总 */
    @GetMapping("/sales/monthly")
    @PreAuthorize("@ss.hasPermi('erp:report:view')")
    public Result<?> salesMonthly(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return Result.ok(reportService.salesMonthly(year, month));
    }

    /** 年度销售趋势 */
    @GetMapping("/sales/trend")
    @PreAuthorize("@ss.hasPermi('erp:report:view')")
    public Result<?> salesTrend(@RequestParam(defaultValue = "2026") @Min(2000) @Max(2100) int year) {
        return Result.ok(reportService.salesTrend(year));
    }

    /** 商品销售排行 */
    @GetMapping("/sales/product-rank")
    @PreAuthorize("@ss.hasPermi('erp:report:view')")
    public Result<?> productRank(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return Result.ok(reportService.productRank(limit, year, month));
    }

    /** 客户销售排行 */
    @GetMapping("/sales/customer-rank")
    @PreAuthorize("@ss.hasPermi('erp:report:view')")
    public Result<?> customerRank(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year) {
        return Result.ok(reportService.customerRank(limit, year));
    }

    /** 库存预警报表 */
    @GetMapping("/stock/alarm")
    @PreAuthorize("@ss.hasPermi('erp:report:view')")
    public Result<?> stockAlarm() {
        return Result.ok(reportService.stockAlarm());
    }

    /** 库存汇总（按品类） */
    @GetMapping("/stock/summary")
    @PreAuthorize("@ss.hasPermi('erp:report:view')")
    public Result<?> stockSummary() {
        return Result.ok(reportService.stockSummary());
    }
}
