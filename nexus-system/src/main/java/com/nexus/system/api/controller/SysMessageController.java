package com.nexus.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.service.SysMessageApplicationService;
import com.nexus.system.domain.model.SysMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final SysMessageApplicationService messageApplicationService;

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageApplicationService.unreadCount());
    }

    /**
     * 消息分页查询。
     *
     * @param type     消息类型：unread=未读，all=全部（默认未读）
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    @GetMapping("/page")
    public Result<IPage<SysMessage>> page(
            @RequestParam(defaultValue = "unread") String type,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return switch (type.trim().toLowerCase()) {
            case "unread" -> Result.ok(messageApplicationService.pageUnread(pageNum, pageSize));
            case "all" -> Result.ok(messageApplicationService.pageAll(pageNum, pageSize));
            default -> throw new IllegalArgumentException("type 无效，可选：unread、all");
        };
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageApplicationService.markAllRead();
        return Result.ok();
    }
}
