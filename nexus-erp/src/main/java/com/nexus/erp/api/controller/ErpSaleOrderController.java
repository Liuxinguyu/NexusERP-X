package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpOrderDtos;
import com.nexus.erp.application.service.ErpSaleOrderApplicationService;
import com.nexus.erp.domain.model.ErpSaleOrder;
import com.nexus.erp.domain.model.ErpSaleOrderItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/erp/sale-order")
@RequiredArgsConstructor
@Validated
public class ErpSaleOrderController {

    private final ErpSaleOrderApplicationService saleOrderApplicationService;

    @GetMapping("/page")
    public Result<IPage<ErpSaleOrder>> page(
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "10") long size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "orderNo", required = false) String orderNo) {
        return Result.ok(saleOrderApplicationService.page(current, size, status, orderNo));
    }

    @GetMapping("/{id}/items")
    public Result<List<ErpSaleOrderItem>> items(@PathVariable("id") Long id) {
        return Result.ok(saleOrderApplicationService.listItems(id));
    }

    @PostMapping
    @OpLog(module = "ERP销售出库", type = "新增")
    public Result<Long> create(@Valid @RequestBody ErpOrderDtos.SaleOrderCreateRequest req) {
        return Result.ok(saleOrderApplicationService.create(req));
    }

    /**
     * 提交出库单（一步完成）：创建订单 + 扣减库存 + status=1 已出库
     */
    @PostMapping("/submit")
    @OpLog(module = "ERP销售出库", type = "提交出库")
    public Result<Long> submitOrder(@Valid @RequestBody ErpOrderDtos.SaleOrderCreateRequest req) {
        return Result.ok(saleOrderApplicationService.submitOrder(req));
    }

    @PutMapping("/{id}/submit")
    @OpLog(module = "ERP销售出库", type = "提交审核")
    public Result<Void> submit(@PathVariable("id") Long id) {
        saleOrderApplicationService.submitOrder(id);
        return Result.ok();
    }

    @PutMapping("/{id}/approve")
    @OpLog(module = "ERP销售出库", type = "审核通过")
    public Result<Void> approve(@PathVariable("id") Long id) {
        saleOrderApplicationService.approveOrder(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    @OpLog(module = "ERP销售出库", type = "审核拒绝")
    public Result<Void> reject(@PathVariable("id") Long id) {
        saleOrderApplicationService.rejectOrder(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "ERP销售出库", type = "删除")
    public Result<Void> delete(@PathVariable("id") Long id) {
        saleOrderApplicationService.delete(id);
        return Result.ok();
    }
}
