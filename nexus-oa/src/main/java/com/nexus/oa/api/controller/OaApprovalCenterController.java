package com.nexus.oa.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.service.OaApprovalCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oa/approval")
@RequiredArgsConstructor
@Validated
public class OaApprovalCenterController {

    private final OaApprovalCenterService service;

    /** 我发起的审批 */
    @GetMapping("/tasks/my-apply")
    public Result<?> pageMyApply(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(service.pageMyApply(current, size, status));
    }

    /** 待我审批 */
    @GetMapping("/tasks/my-approve")
    public Result<?> pageMyApprove(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(service.pageMyApprove(current, size, status));
    }

    @GetMapping("/tasks/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @OpLog(module = "审批中心", type = "审批通过")
    @PostMapping("/tasks/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                  @RequestBody ApproveReq req) {
        service.approve(id, req.getApproved(), req.getOpinion());
        return Result.ok();
    }

    @OpLog(module = "审批中心", type = "审批拒绝")
    @PostMapping("/tasks/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                                 @RequestBody ApproveReq req) {
        service.approve(id, false, req.getOpinion());
        return Result.ok();
    }

    @lombok.Data
    public static class ApproveReq {
        private Boolean approved;
        private String opinion;
    }
}
