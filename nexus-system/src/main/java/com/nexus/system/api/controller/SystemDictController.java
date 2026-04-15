package com.nexus.system.api.controller;

import com.nexus.common.core.domain.Result;
import com.nexus.system.application.service.SysDictApplicationService;
import com.nexus.system.domain.model.SysDictItem;
import com.nexus.system.domain.model.SysDictType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemDictController {

    private final SysDictApplicationService sysDictApplicationService;

    @GetMapping("/dict-type/list")
    public Result<List<SysDictType>> listTypes() {
        return Result.ok(sysDictApplicationService.listTypes());
    }

    @GetMapping("/dict-item/list-by-type")
    public Result<List<SysDictItem>> listByType(@RequestParam String dictType) {
        return Result.ok(sysDictApplicationService.listItemsByType(dictType));
    }
}
