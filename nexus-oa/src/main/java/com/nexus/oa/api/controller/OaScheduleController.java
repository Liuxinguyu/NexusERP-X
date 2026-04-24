package com.nexus.oa.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.service.OaScheduleApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oa/schedules")
@RequiredArgsConstructor
@Validated
public class OaScheduleController {

    private final OaScheduleApplicationService service;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('oa:schedule:list')")
    public Result<List<OaScheduleApplicationService.ScheduleVO>> list(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(service.list(startDate, endDate));
    }

    @OpLog(module = "日程管理", type = "新建日程")
    @PreAuthorize("@ss.hasPermi('oa:schedule:add')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OaScheduleApplicationService.ScheduleCreateReq req) {
        return Result.ok(service.create(req));
    }

    @OpLog(module = "日程管理", type = "修改日程")
    @PreAuthorize("@ss.hasPermi('oa:schedule:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                                 @Valid @RequestBody OaScheduleApplicationService.ScheduleUpdateReq req) {
        service.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "日程管理", type = "删除日程")
    @PreAuthorize("@ss.hasPermi('oa:schedule:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
