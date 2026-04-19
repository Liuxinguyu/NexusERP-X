#!/bin/bash
set -e

# Replace SystemDictController.java
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/api/controller/SystemDictController.java
package com.nexus.system.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemDictDtos;
import com.nexus.system.application.service.SysDictApplicationService;
import com.nexus.system.domain.model.SysDictItem;
import com.nexus.system.domain.model.SysDictType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Validated
public class SystemDictController {

    private final SysDictApplicationService sysDictApplicationService;

    // --- Dict Type ---

    @GetMapping("/dict-type/list")
    public Result<List<SysDictType>> listTypes() {
        return Result.ok(sysDictApplicationService.listTypes());
    }

    @GetMapping("/dict-type/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysDictType> getType(@PathVariable Long id) {
        return Result.ok(sysDictApplicationService.getType(id));
    }

    @PostMapping("/dict-type")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "字典管理", type = "新增类型")
    public Result<Long> createType(@Valid @RequestBody SystemDictDtos.DictTypeCreateRequest req) {
        return Result.ok(sysDictApplicationService.createType(req));
    }

    @PutMapping("/dict-type/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "字典管理", type = "修改类型")
    public Result<Void> updateType(@PathVariable Long id, @Valid @RequestBody SystemDictDtos.DictTypeUpdateRequest req) {
        sysDictApplicationService.updateType(id, req);
        return Result.ok();
    }

    @DeleteMapping("/dict-type/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "字典管理", type = "删除类型")
    public Result<Void> deleteType(@PathVariable Long id) {
        sysDictApplicationService.deleteType(id);
        return Result.ok();
    }

    // --- Dict Item ---

    @GetMapping("/dict-item/list-by-type")
    public Result<List<SysDictItem>> listByType(@RequestParam String dictType) {
        return Result.ok(sysDictApplicationService.listItemsByType(dictType));
    }

    @GetMapping("/dict-item/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysDictItem> getItem(@PathVariable Long id) {
        return Result.ok(sysDictApplicationService.getItem(id));
    }

    @PostMapping("/dict-item")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "字典数据", type = "新增项")
    public Result<Long> createItem(@Valid @RequestBody SystemDictDtos.DictItemCreateRequest req) {
        return Result.ok(sysDictApplicationService.createItem(req));
    }

    @PutMapping("/dict-item/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "字典数据", type = "修改项")
    public Result<Void> updateItem(@PathVariable Long id, @Valid @RequestBody SystemDictDtos.DictItemUpdateRequest req) {
        sysDictApplicationService.updateItem(id, req);
        return Result.ok();
    }

    @DeleteMapping("/dict-item/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OpLog(module = "字典数据", type = "删除项")
    public Result<Void> deleteItem(@PathVariable Long id) {
        sysDictApplicationService.deleteItem(id);
        return Result.ok();
    }
}
INNER_EOF

