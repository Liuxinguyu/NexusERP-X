package com.nexus.system.api.controller;

import com.nexus.common.annotation.OpLog;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.Result;
import com.nexus.system.application.service.OnlineUserRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/online-users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysOnlineUserController {

    private final OnlineUserRedisService onlineUserRedisService;

    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) throws Exception {
        Long tid = TenantContext.getTenantId();
        List<OnlineUserRedisService.OnlinePayload> rows = onlineUserRedisService.listByTenant(pageNum, pageSize, tid);
        long total = onlineUserRedisService.countByTenant(tid);
        Map<String, Object> m = new HashMap<>();
        m.put("records", rows);
        m.put("total", total);
        m.put("pageNum", pageNum);
        m.put("pageSize", pageSize);
        return Result.ok(m);
    }

    @OpLog(module = "在线用户", type = "强退")
    @DeleteMapping("/{userId}")
    public Result<Void> kick(@PathVariable Long userId) {
        onlineUserRedisService.forceLogoutUser(userId);
        return Result.ok();
    }
}
