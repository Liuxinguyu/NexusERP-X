package com.nexus.oa.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.service.OaTaskApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oa/tasks")
@RequiredArgsConstructor
@Validated
public class OaTaskController {

    private final OaTaskApplicationService service;

    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long assigneeId) {
        return Result.ok(service.page(current, size, status, assigneeId));
    }

    @GetMapping("/{id}")
    public Result<OaTaskApplicationService.TaskVO> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @OpLog(module = "任务看板", type = "新建任务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OaTaskApplicationService.TaskCreateReq req) {
        return Result.ok(service.create(req));
    }

    @OpLog(module = "任务看板", type = "修改任务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                                 @Valid @RequestBody OaTaskApplicationService.TaskUpdateReq req) {
        service.update(id, req);
        return Result.ok();
    }

    @OpLog(module = "任务看板", type = "删除任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @OpLog(module = "任务看板", type = "接受任务")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        service.accept(id);
        return Result.ok();
    }

    @OpLog(module = "任务看板", type = "更新进度")
    @PutMapping("/{id}/progress")
    public Result<Void> updateProgress(@PathVariable Long id, @RequestParam int progress) {
        service.updateProgress(id, progress);
        return Result.ok();
    }

    @OpLog(module = "任务看板", type = "完成任务")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        service.complete(id);
        return Result.ok();
    }

    @OpLog(module = "任务看板", type = "取消任务")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return Result.ok();
    }

    @GetMapping("/{id}/comments")
    public Result<List<OaTaskApplicationService.TaskCommentVO>> listComments(@PathVariable Long id) {
        return Result.ok(service.listComments(id));
    }

    @OpLog(module = "任务看板", type = "添加评论")
    @PostMapping("/{id}/comment")
    public Result<Void> addComment(@PathVariable Long id, @RequestBody CommentReq req) {
        service.addComment(id, req.getContent());
        return Result.ok();
    }

    @lombok.Data
    public static class CommentReq {
        private String content;
    }
}
