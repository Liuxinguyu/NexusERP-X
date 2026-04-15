package com.nexus.wage.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.wage.application.dto.WageDtos;
import com.nexus.wage.application.service.WageItemConfigApplicationService;
import com.nexus.wage.domain.model.WageItemConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wage/item-configs")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class WageItemConfigController {

    private final WageItemConfigApplicationService wageItemConfigApplicationService;

    @GetMapping
    public Result<List<WageItemConfig>> list() {
        return Result.ok(wageItemConfigApplicationService.listAll());
    }

    @GetMapping("/{id}")
    public Result<WageItemConfig> get(@PathVariable Long id) {
        return Result.ok(wageItemConfigApplicationService.getById(id));
    }

    @PostMapping
    @OpLog(module = "薪酬薪资项", type = "新增")
    public Result<Long> create(@Valid @RequestBody WageDtos.ItemConfigCreateRequest req) {
        return Result.ok(wageItemConfigApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @OpLog(module = "薪酬薪资项", type = "修改")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WageDtos.ItemConfigUpdateRequest req) {
        wageItemConfigApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "薪酬薪资项", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        wageItemConfigApplicationService.delete(id);
        return Result.ok();
    }
}
