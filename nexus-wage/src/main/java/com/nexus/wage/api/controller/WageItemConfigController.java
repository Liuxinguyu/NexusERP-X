package com.nexus.wage.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wage/item-configs")
@RequiredArgsConstructor
@Validated
public class WageItemConfigController {

    private final WageItemConfigApplicationService wageItemConfigApplicationService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('wage:item:list')")
    public Result<IPage<WageDtos.ItemConfigView>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        IPage<WageItemConfig> page = wageItemConfigApplicationService.page(current, size);
        IPage<WageDtos.ItemConfigView> viewPage = page.convert(WageItemConfigController::toView);
        return Result.ok(viewPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('wage:item:list')")
    public Result<WageDtos.ItemConfigView> get(@PathVariable Long id) {
        return Result.ok(toView(wageItemConfigApplicationService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('wage:item:add')")
    @OpLog(module = "薪酬薪资项", type = "新增")
    public Result<Long> create(@Valid @RequestBody WageDtos.ItemConfigCreateRequest req) {
        return Result.ok(wageItemConfigApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('wage:item:edit')")
    @OpLog(module = "薪酬薪资项", type = "修改")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WageDtos.ItemConfigUpdateRequest req) {
        wageItemConfigApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('wage:item:delete')")
    @OpLog(module = "薪酬薪资项", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        wageItemConfigApplicationService.delete(id);
        return Result.ok();
    }

    private static WageDtos.ItemConfigView toView(WageItemConfig c) {
        WageDtos.ItemConfigView v = new WageDtos.ItemConfigView();
        v.setId(c.getId());
        v.setItemName(c.getItemName());
        v.setCalcType(c.getCalcType());
        v.setDefaultAmount(c.getDefaultAmount());
        v.setItemKind(c.getItemKind());
        return v;
    }
}
