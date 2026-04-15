package com.nexus.oa.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.dto.OaDtos;
import com.nexus.oa.application.service.OaLeaveRequestApplicationService;
import com.nexus.oa.domain.model.OaLeaveRequest;
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

@RestController
@RequestMapping("/api/v1/oa/leave-requests")
@RequiredArgsConstructor
@Validated
public class OaLeaveRequestController {

    private final OaLeaveRequestApplicationService leaveRequestApplicationService;

    @GetMapping("/page")
    public Result<IPage<OaLeaveRequest>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(leaveRequestApplicationService.page(current, size, status));
    }

    @GetMapping("/{id}")
    public Result<OaLeaveRequest> get(@PathVariable Long id) {
        return Result.ok(leaveRequestApplicationService.getById(id));
    }

    @PostMapping
    @OpLog(module = "OA请假申请", type = "新增")
    public Result<Long> create(@Valid @RequestBody OaDtos.LeaveRequestCreateRequest req) {
        return Result.ok(leaveRequestApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @OpLog(module = "OA请假申请", type = "修改")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OaDtos.LeaveRequestUpdateRequest req) {
        leaveRequestApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "OA请假申请", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        leaveRequestApplicationService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/submit")
    @OpLog(module = "OA请假申请", type = "修改")
    public Result<Void> submit(@PathVariable Long id) {
        leaveRequestApplicationService.submit(id);
        return Result.ok();
    }

    @PutMapping("/{id}/approve")
    @OpLog(module = "OA请假申请", type = "修改")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody OaDtos.LeaveApproveRequest req) {
        leaveRequestApplicationService.approve(id, req);
        return Result.ok();
    }
}
