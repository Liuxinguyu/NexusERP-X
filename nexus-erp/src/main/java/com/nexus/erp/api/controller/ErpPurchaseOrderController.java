package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpOrderDtos;
import com.nexus.erp.application.service.ErpPurchaseOrderApplicationService;
import com.nexus.erp.domain.model.ErpPurchaseOrder;
import com.nexus.erp.domain.model.ErpPurchaseOrderItem;
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
@RequestMapping("/api/v1/erp/purchase-orders")
@RequiredArgsConstructor
@Validated
public class ErpPurchaseOrderController {

    private final ErpPurchaseOrderApplicationService purchaseOrderApplicationService;

    @GetMapping("/page")
    public Result<IPage<ErpPurchaseOrder>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(purchaseOrderApplicationService.page(current, size, status));
    }

    @GetMapping("/{id}/items")
    public Result<List<ErpPurchaseOrderItem>> items(@PathVariable Long id) {
        return Result.ok(purchaseOrderApplicationService.listItems(id));
    }

    @PostMapping
    @OpLog(module = "ERP采购入库", type = "新增")
    public Result<Long> create(@Valid @RequestBody ErpOrderDtos.PurchaseOrderCreateRequest req) {
        return Result.ok(purchaseOrderApplicationService.create(req));
    }

    @PutMapping("/{id}/confirm-inbound")
    @OpLog(module = "ERP采购入库", type = "修改")
    public Result<Void> confirmInbound(@PathVariable Long id) {
        purchaseOrderApplicationService.confirmInbound(id);
        return Result.ok();
    }

    @PutMapping("/{id}/submit")
    @OpLog(module = "ERP采购入库", type = "提交审核")
    public Result<Void> submit(@PathVariable Long id) {
        purchaseOrderApplicationService.submitOrder(id);
        return Result.ok();
    }

    @PutMapping("/{id}/approve")
    @OpLog(module = "ERP采购入库", type = "审核通过")
    public Result<Void> approve(@PathVariable Long id) {
        purchaseOrderApplicationService.approveOrder(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    @OpLog(module = "ERP采购入库", type = "审核拒绝")
    public Result<Void> reject(@PathVariable Long id) {
        purchaseOrderApplicationService.rejectOrder(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "ERP采购入库", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        purchaseOrderApplicationService.delete(id);
        return Result.ok();
    }
}
