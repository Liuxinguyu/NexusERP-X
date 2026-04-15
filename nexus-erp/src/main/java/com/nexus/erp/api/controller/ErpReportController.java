package com.nexus.erp.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.service.ErpReportService;
import lombok.RequiredArgsConstructor;
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
    public Result<?> salesMonthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.ok(reportService.salesMonthly(year, month));
    }

    /** 年度销售趋势 */
    @GetMapping("/sales/trend")
    public Result<?> salesTrend(@RequestParam(defaultValue = "2026") int year) {
        return Result.ok(reportService.salesTrend(year));
    }

    /** 商品销售排行 */
    @GetMapping("/sales/product-rank")
    public Result<?> productRank(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.ok(reportService.productRank(limit, year, month));
    }

    /** 客户销售排行 */
    @GetMapping("/sales/customer-rank")
    public Result<?> customerRank(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer year) {
        return Result.ok(reportService.customerRank(limit, year));
    }

    /** 库存预警报表 */
    @GetMapping("/stock/alarm")
    public Result<?> stockAlarm() {
        return Result.ok(reportService.stockAlarm());
    }

    /** 库存汇总（按品类） */
    @GetMapping("/stock/summary")
    public Result<?> stockSummary() {
        return Result.ok(reportService.stockSummary());
    }
}
