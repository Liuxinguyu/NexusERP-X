package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.OaLeaveDtos;
import com.nexus.system.application.service.OaLeaveApplicationService;
import com.nexus.system.domain.model.OaLeave;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oa/leave")
@RequiredArgsConstructor
@Validated
public class OaLeaveController {

    private final OaLeaveApplicationService oaLeaveApplicationService;

    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/page")
    public Result<IPage<OaLeave>> page(
            @RequestParam(defaultValue = "my_apply") String type,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(oaLeaveApplicationService.page(type, pageNum, pageSize));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:add')")
    @PostMapping("/submit")
    @OpLog(module = "请假单", type = "新增")
    public Result<Long> submit(@Valid @RequestBody OaLeaveDtos.SubmitRequest req) {
        return Result.ok(oaLeaveApplicationService.submit(req));
    }

    @PreAuthorize("@ss.hasPermi('oa:leave:approve')")
    @PostMapping("/approve")
    @OpLog(module = "请假单", type = "修改")
    public Result<Void> approve(@Valid @RequestBody OaLeaveDtos.ApproveRequest req) {
        oaLeaveApplicationService.approve(req);
        return Result.ok();
    }
}
