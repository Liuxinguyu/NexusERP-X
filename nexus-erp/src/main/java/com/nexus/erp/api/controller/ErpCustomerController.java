package com.nexus.erp.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.application.service.ErpCustomerApplicationService;
import com.nexus.erp.domain.model.ErpCustomer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/erp/customers")
@RequiredArgsConstructor
@Validated
public class ErpCustomerController {

    private final ErpCustomerApplicationService customerApplicationService;

    @GetMapping("/page")
    public Result<IPage<ErpCustomer>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String contactPhone) {
        return Result.ok(customerApplicationService.page(current, size, name, contactPhone));
    }

    @OpLog(module = "客户管理", type = "新增")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ErpDtos.CustomerCreateRequest req) {
        return Result.ok(customerApplicationService.create(req));
    }

    @OpLog(module = "客户管理", type = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ErpDtos.CustomerUpdateRequest req) {
        customerApplicationService.update(id, req);
        return Result.ok();
    }
}
