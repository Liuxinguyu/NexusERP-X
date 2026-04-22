package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.application.service.SysNoticeApplicationService;
import com.nexus.system.domain.model.SysNotice;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/notice")
@RequiredArgsConstructor
@Validated
public class SysNoticeController {

    private final SysNoticeApplicationService noticeApplicationService;

    @PreAuthorize("@ss.hasPermi('oa:notice:list')")
    @GetMapping("/page")
    public Result<IPage<SysNotice>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(noticeApplicationService.page(pageNum, pageSize));
    }

    @OpLog(module = "公告管理", type = "新增")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('oa:notice:add')")
    public Result<Long> save(@Valid @RequestBody SystemAdminDtos.NoticeSaveRequest req) {
        return Result.ok(noticeApplicationService.saveOrUpdate(req));
    }

    @OpLog(module = "公告管理", type = "修改")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('oa:notice:edit')")
    public Result<Long> update(@PathVariable Long id,
                               @Valid @RequestBody SystemAdminDtos.NoticeSaveRequest req) {
        req.setId(id);
        return Result.ok(noticeApplicationService.saveOrUpdate(req));
    }

    @OpLog(module = "公告管理", type = "发布")
    @PutMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermi('oa:notice:publish')")
    public Result<Void> publish(@PathVariable Long id) {
        noticeApplicationService.publish(id);
        return Result.ok();
    }

    @OpLog(module = "公告管理", type = "删除")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('oa:notice:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        noticeApplicationService.delete(id);
        return Result.ok();
    }
}
