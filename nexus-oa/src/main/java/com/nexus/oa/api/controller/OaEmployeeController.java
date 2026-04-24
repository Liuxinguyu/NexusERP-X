package com.nexus.oa.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.core.domain.Result;
import com.nexus.oa.application.dto.OaDtos;
import com.nexus.oa.application.service.OaEmployeeApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/oa/employees")
@RequiredArgsConstructor
@Validated
public class OaEmployeeController {

    private final OaEmployeeApplicationService employeeApplicationService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('oa:employee:list')")
    public Result<IPage<OaEmployeeApplicationService.EmployeeVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String empNo) {
        return Result.ok(employeeApplicationService.page(current, size, name, empNo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('oa:employee:list')")
    public Result<OaEmployeeApplicationService.EmployeeVO> get(@PathVariable Long id) {
        return Result.ok(employeeApplicationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('oa:employee:add')")
    @OpLog(module = "OA员工档案", type = "新增")
    public Result<Long> create(@Valid @RequestBody OaDtos.EmployeeCreateRequest req) {
        return Result.ok(employeeApplicationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('oa:employee:edit')")
    @OpLog(module = "OA员工档案", type = "修改")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OaDtos.EmployeeUpdateRequest req) {
        employeeApplicationService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('oa:employee:delete')")
    @OpLog(module = "OA员工档案", type = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        employeeApplicationService.delete(id);
        return Result.ok();
    }
}
