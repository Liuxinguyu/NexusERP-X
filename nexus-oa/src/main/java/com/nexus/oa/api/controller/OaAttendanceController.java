package com.nexus.oa.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.dto.OaDtos;
import com.nexus.oa.application.service.OaAttendanceApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oa/attendance")
@RequiredArgsConstructor
@Validated
public class OaAttendanceController {

    private final OaAttendanceApplicationService service;

    // ===================== 考勤规则 =====================

    @GetMapping("/rules")
    public Result<List<OaDtos.AttendanceRuleVO>> listRules() {
        return Result.ok(service.listRules());
    }

    @OpLog(module = "考勤管理", type = "新增规则")
    @PostMapping("/rules")
    public Result<Long> createRule(@Valid @RequestBody OaDtos.AttendanceRuleCreateRequest req) {
        return Result.ok(service.createRule(req));
    }

    @OpLog(module = "考勤管理", type = "删除规则")
    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        service.deleteRule(id);
        return Result.ok();
    }

    // ===================== 打卡 =====================

    @OpLog(module = "考勤打卡", type = "打卡")
    @PostMapping("/check-in")
    public Result<Void> checkIn(@Valid @RequestBody OaDtos.CheckInRequest req) {
        service.checkIn(req);
        return Result.ok();
    }

    @GetMapping("/my-today")
    public Result<OaDtos.TodayStatusVO> myToday() {
        return Result.ok(service.getTodayStatus());
    }

    @GetMapping("/records/page")
    public Result<?> pageRecords(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return Result.ok(service.pageRecords(current, size, userId, dateFrom, dateTo));
    }

    // ===================== 请假（新表） =====================

    @GetMapping("/leave/page")
    public Result<?> pageLeaves(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(service.pageLeaves(current, size, userId, status));
    }

    @OpLog(module = "请假申请", type = "新增")
    @PostMapping("/leave")
    public Result<Long> createLeave(@Valid @RequestBody OaDtos.LeaveCreateRequest req) {
        return Result.ok(service.createLeave(req));
    }

    @OpLog(module = "请假申请", type = "删除")
    @DeleteMapping("/leave/{id}")
    public Result<Void> deleteLeave(@PathVariable Long id) {
        service.deleteLeave(id);
        return Result.ok();
    }

    @OpLog(module = "请假申请", type = "提交审批")
    @PostMapping("/leave/{id}/submit")
    public Result<Void> submitLeave(@PathVariable Long id) {
        service.submitLeave(id);
        return Result.ok();
    }

    @OpLog(module = "请假申请", type = "审批")
    @PostMapping("/leave/{id}/approve")
    public Result<Void> approveLeave(@PathVariable Long id,
                                       @Valid @RequestBody OaDtos.LeaveApproveRequest req) {
        service.approveLeave(id, req);
        return Result.ok();
    }

    // ===================== 加班 =====================

    @GetMapping("/overtime/page")
    public Result<?> pageOvertimes(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(service.pageOvertimes(current, size, userId, status));
    }

    @OpLog(module = "加班申请", type = "新增")
    @PostMapping("/overtime")
    public Result<Long> createOvertime(@Valid @RequestBody OaDtos.OvertimeCreateRequest req) {
        return Result.ok(service.createOvertime(req));
    }

    @OpLog(module = "加班申请", type = "删除")
    @DeleteMapping("/overtime/{id}")
    public Result<Void> deleteOvertime(@PathVariable Long id) {
        service.deleteOvertime(id);
        return Result.ok();
    }

    @OpLog(module = "加班申请", type = "提交审批")
    @PostMapping("/overtime/{id}/submit")
    public Result<Void> submitOvertime(@PathVariable Long id) {
        service.submitOvertime(id);
        return Result.ok();
    }

    @OpLog(module = "加班申请", type = "审批")
    @PostMapping("/overtime/{id}/approve")
    public Result<Void> approveOvertime(@PathVariable Long id,
                                          @Valid @RequestBody OaDtos.LeaveApproveRequest req) {
        service.approveOvertime(id, req);
        return Result.ok();
    }

    // ===================== 考勤统计 =====================

    @GetMapping("/statistics/monthly")
    public Result<OaDtos.AttendanceStatisticsVO> statistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "4") int month) {
        return Result.ok(service.statistics(userId, year, month));
    }
}
